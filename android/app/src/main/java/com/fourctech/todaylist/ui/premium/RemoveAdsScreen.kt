package com.fourctech.todaylist.ui.premium

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fourctech.todaylist.BuildConfig
import com.fourctech.todaylist.R

@Composable
fun RemoveAdsRoute(
    onBack: () -> Unit,
    viewModel: RemoveAdsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val privacyUrl = stringResource(R.string.privacy_policy_url)
    val termsUrl = stringResource(R.string.terms_url)

    LaunchedEffect(Unit) {
        viewModel.onAppear()
    }

    LaunchedEffect(state.message) {
        val message = state.message ?: return@LaunchedEffect
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        viewModel.consumeMessage()
    }

    fun openUrl(url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(context, "That page couldn't be opened.", Toast.LENGTH_SHORT).show()
        }
    }

    RemoveAdsScreen(
        state = state,
        onBack = onBack,
        onPurchase = {
            val activity = context as? Activity
            if (activity == null) {
                Toast.makeText(context, "Purchase couldn't be completed.", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.purchase(activity)
            }
        },
        onRestore = viewModel::restore,
        onPrivacy = { openUrl(privacyUrl) },
        onTerms = { openUrl(termsUrl) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoveAdsScreen(
    state: RemoveAdsUiState,
    onBack: () -> Unit,
    onPurchase: () -> Unit,
    onRestore: () -> Unit,
    onPrivacy: () -> Unit,
    onTerms: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Remove Ads") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.adsRemoved) {
                Text(
                    text = "Ads removed",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "Thank you for supporting Today List.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "Remove Ads Forever",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Text(
                    text = "One-time purchase",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                when {
                    state.loadingProduct -> CircularProgressIndicator()
                    state.priceLabel != null -> Text(
                        text = state.priceLabel,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    else -> Text(
                        text = "Connect to see price",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text(
                    text = "Benefits",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "• Removes all Today List advertising",
                    style = MaterialTheme.typography.bodyLarge,
                )

                if (BuildConfig.DEBUG && !state.productAvailable) {
                    Text(
                        text = "Debug: Play product not found — purchase will grant ads-removed locally.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Button(
                    onClick = onPurchase,
                    enabled = !state.purchasing && !state.loadingProduct,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = when {
                            state.purchasing -> "Working…"
                            state.priceLabel != null -> "Remove Ads — ${state.priceLabel}"
                            else -> "Remove Ads"
                        },
                    )
                }
            }

            TextButton(
                onClick = onRestore,
                enabled = !state.restoring && !state.purchasing,
            ) {
                Text(if (state.restoring) "Restoring…" else "Restore Purchases")
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onPrivacy) {
                Text("Privacy Policy")
            }
            TextButton(onClick = onTerms) {
                Text("Terms of Use")
            }
        }
    }
}
