package com.fourctech.todaylist.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fourctech.todaylist.domain.model.TaskLocation
import com.fourctech.todaylist.ui.history.HistoryDetailScreen
import com.fourctech.todaylist.ui.history.HistoryScreen
import com.fourctech.todaylist.ui.later.LaterScreen
import com.fourctech.todaylist.ui.premium.RemoveAdsRoute
import com.fourctech.todaylist.ui.quickadd.QuickAddSheet
import com.fourctech.todaylist.ui.settings.SettingsScreen
import com.fourctech.todaylist.ui.taskdetail.TaskDetailScreen
import com.fourctech.todaylist.ui.today.TodayScreen

private data class BottomDestination(
    val route: Route,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun TodayListApp(
    deepLinkTaskId: String? = null,
    onDeepLinkConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    var showQuickAdd by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(deepLinkTaskId) {
        val taskId = deepLinkTaskId ?: return@LaunchedEffect
        navController.navigate(Route.TaskDetail(taskId).path) {
            launchSingleTop = true
        }
        onDeepLinkConsumed()
    }

    val destinations = listOf(
        BottomDestination(Route.Today, "Today", Icons.Outlined.CheckCircle),
        BottomDestination(Route.Later, "Later", Icons.Filled.DateRange),
        BottomDestination(Route.History, "History", Icons.AutoMirrored.Filled.List),
        BottomDestination(Route.Settings, "Settings", Icons.Filled.Settings),
    )

    val onBottomBar = destinations.any { it.route.path == currentRoute }
    val showFab = currentRoute == Route.Today.path || currentRoute == Route.Later.path
    val quickAddLocation = if (currentRoute == Route.Later.path) {
        TaskLocation.LATER
    } else {
        TaskLocation.TODAY
    }

    Scaffold(
        bottomBar = {
            if (onBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ) {
                    destinations.forEach { dest ->
                        NavigationBarItem(
                            selected = currentRoute == dest.route.path,
                            onClick = {
                                navController.navigate(dest.route.path) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = { showQuickAdd = true },
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Quick add")
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Route.Today.path,
            modifier = Modifier.padding(padding),
        ) {
            composable(Route.Today.path) {
                TodayScreen(
                    onOpenTask = { id -> navController.navigate(Route.TaskDetail(id).path) },
                )
            }
            composable(Route.Later.path) {
                LaterScreen(
                    onOpenTask = { id -> navController.navigate(Route.TaskDetail(id).path) },
                )
            }
            composable(Route.History.path) {
                HistoryScreen(
                    onOpenCompletion = { id ->
                        navController.navigate(Route.HistoryDetail(id).path)
                    },
                )
            }
            composable(
                route = Route.HistoryDetail.pattern,
                arguments = listOf(navArgument(Route.HistoryDetail.arg) { type = NavType.StringType }),
            ) {
                HistoryDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Route.TaskDetail.pattern,
                arguments = listOf(navArgument(Route.TaskDetail.arg) { type = NavType.StringType }),
            ) {
                TaskDetailScreen(onBack = { navController.popBackStack() })
            }
            composable(Route.Settings.path) {
                SettingsScreen(
                    onRemoveAds = { navController.navigate(Route.RemoveAds.path) },
                )
            }
            composable(Route.RemoveAds.path) {
                RemoveAdsRoute(onBack = { navController.popBackStack() })
            }
        }
    }

    if (showQuickAdd) {
        QuickAddSheet(
            defaultLocation = quickAddLocation,
            onDismiss = { showQuickAdd = false },
        )
    }
}
