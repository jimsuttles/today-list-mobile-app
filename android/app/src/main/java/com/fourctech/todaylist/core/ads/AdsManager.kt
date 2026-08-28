package com.fourctech.todaylist.core.ads

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class AdsManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _initialized = MutableStateFlow(false)
    val initialized: StateFlow<Boolean> = _initialized.asStateFlow()

    @Volatile
    private var starting = false

    fun initialize() {
        if (_initialized.value || starting) return
        starting = true
        MobileAds.initialize(context) { status ->
            _initialized.value = true
            starting = false
            Log.d(TAG, "MobileAds initialized: ${status.adapterStatusMap.keys}")
        }
    }

    companion object {
        private const val TAG = "AdsManager"
    }
}
