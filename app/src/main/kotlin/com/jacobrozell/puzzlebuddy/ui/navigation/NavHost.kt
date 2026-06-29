package com.jacobrozell.puzzlebuddy.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jacobrozell.puzzlebuddy.domain.surface.ProductSurface
import com.jacobrozell.puzzlebuddy.domain.surface.RootTab
import com.jacobrozell.puzzlebuddy.ui.designsystem.AdaptiveLayout
import com.jacobrozell.puzzlebuddy.ui.designsystem.BrandBackground
import com.jacobrozell.puzzlebuddy.ui.puzzles.PuzzleDetailScreen
import com.jacobrozell.puzzlebuddy.ui.puzzles.PuzzleFormScreen
import com.jacobrozell.puzzlebuddy.ui.puzzles.PuzzleListScreen
import com.jacobrozell.puzzlebuddy.ui.settings.SettingsScreen
import com.jacobrozell.puzzlebuddy.ui.stats.CollectionStatsScreen

@Composable
fun PuzzleBuddyNavHost(onReplayOnboarding: () -> Unit = {}) {
    val navController = rememberNavController()
    val shellViewModel: ShellViewModel = hiltViewModel()
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val currentTab = ProductSurface.rootTabs.firstOrNull { tab ->
        destination?.hierarchy?.any { it.route == tab.route } == true
    } ?: RootTab.PUZZLES
    val showRootNavigation = destination?.route in ProductSurface.rootTabs.map { it.route }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = AdaptiveLayout.usesNavigationRail(maxWidth.value)

        fun navigateToTab(tab: RootTab) {
            shellViewModel.onTabSelected(tab.route)
            navController.navigate(tab.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        @Composable
        fun RootNavigationRail(modifier: Modifier = Modifier) {
            NavigationRail(modifier = modifier) {
                ProductSurface.rootTabs.forEach { tab ->
                    val selected = currentTab == tab
                    NavigationRailItem(
                        selected = selected,
                        onClick = { navigateToTab(tab) },
                        icon = {
                            Icon(
                                imageVector = tab.icon(selected),
                                contentDescription = tab.label,
                            )
                        },
                        label = { Text(tab.label) },
                    )
                }
            }
        }

        @Composable
        fun RootNavigationBar() {
            NavigationBar {
                ProductSurface.rootTabs.forEach { tab ->
                    val selected = currentTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { navigateToTab(tab) },
                        icon = {
                            Icon(
                                imageVector = tab.icon(selected),
                                contentDescription = tab.label,
                            )
                        },
                        label = { Text(tab.label) },
                    )
                }
            }
        }

        @Composable
        fun AppNavHost(modifier: Modifier = Modifier) {
            NavHost(
                navController = navController,
                startDestination = RootTab.PUZZLES.route,
                modifier = modifier,
            ) {
                composable(RootTab.PUZZLES.route) {
                    PuzzleListScreen(
                        onOpenPuzzle = { id -> navController.navigate("puzzle/$id") },
                        onAddPuzzle = { barcode -> navController.navigate(PuzzleRoutes.newPuzzleRoute(barcode)) },
                        onQuickAdd = { request -> navController.navigate(PuzzleRoutes.newPuzzleRoute(request)) },
                    )
                }
                composable(RootTab.STATS.route) {
                    CollectionStatsScreen()
                }
                composable(RootTab.SETTINGS.route) {
                    SettingsScreen(onReplayOnboarding = onReplayOnboarding)
                }
                composable(
                    route = "puzzle/new?barcode={barcode}&name={name}&pieces={pieces}&source={source}&lookup_notice={lookup_notice}",
                    arguments = listOf(
                        navArgument("barcode") { type = NavType.StringType; defaultValue = "" },
                        navArgument("name") { type = NavType.StringType; defaultValue = "" },
                        navArgument("pieces") { type = NavType.StringType; defaultValue = "" },
                        navArgument("source") { type = NavType.StringType; defaultValue = "" },
                        navArgument("lookup_notice") { type = NavType.StringType; defaultValue = "" },
                    ),
                ) {
                    PuzzleFormScreen(
                        puzzleId = null,
                        onFinished = { navController.popBackStack() },
                    )
                }
                composable("puzzle/{id}") { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    PuzzleDetailScreen(
                        puzzleId = id,
                        onBack = { navController.popBackStack() },
                        onEdit = { navController.navigate("puzzle/$id/edit") },
                    )
                }
                composable("puzzle/{id}/edit") { entry ->
                    val id = entry.arguments?.getString("id").orEmpty()
                    PuzzleFormScreen(
                        puzzleId = id,
                        onFinished = { navController.popBackStack() },
                    )
                }
            }
        }

        if (useNavigationRail) {
            Row(Modifier.fillMaxSize()) {
                if (showRootNavigation) {
                    RootNavigationRail()
                }
                BrandBackground(modifier = Modifier.weight(1f)) {
                    AppNavHost(Modifier.fillMaxSize())
                }
            }
        } else {
            Scaffold(
                bottomBar = { if (showRootNavigation) RootNavigationBar() },
            ) { padding ->
                BrandBackground {
                    AppNavHost(Modifier.padding(padding))
                }
            }
        }
    }
}

private fun RootTab.icon(selected: Boolean): ImageVector = when (this) {
    RootTab.PUZZLES -> if (selected) Icons.AutoMirrored.Filled.List else Icons.Outlined.List
    RootTab.STATS -> if (selected) Icons.Filled.BarChart else Icons.Outlined.BarChart
    RootTab.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
}
