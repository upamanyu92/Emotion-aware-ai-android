package com.example.emotionawareai.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.emotionawareai.ui.ChatViewModel
import com.example.emotionawareai.ui.screen.ChatScreen
import com.example.emotionawareai.ui.screen.DiaryScreen
import com.example.emotionawareai.ui.screen.EvaluationScreen
import com.example.emotionawareai.ui.screen.GoalsScreen
import com.example.emotionawareai.ui.screen.InsightsScreen
import com.example.emotionawareai.ui.screen.SettingsScreen
import com.example.emotionawareai.ui.theme.NeonCyan
import com.example.emotionawareai.ui.theme.NeonPurple

sealed class AppRoute(val route: String, val label: String) {
    object Chat : AppRoute("chat", "Home")
    object Diary : AppRoute("diary", "Diary")
    object Insights : AppRoute("insights", "Insights")
    object Goals : AppRoute("goals", "Goals")
    object Evaluation : AppRoute("evaluation", "AI Eval")
    object Settings : AppRoute("settings", "Profile")
}

@Composable
fun MainNavigation(viewModel: ChatViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val tabs = listOf(
        Triple(AppRoute.Chat, Icons.AutoMirrored.Filled.Chat, "Home"),
        Triple(AppRoute.Diary, Icons.Filled.AutoAwesome, "Diary"),
        Triple(AppRoute.Insights, Icons.Filled.BarChart, "Insights"),
        Triple(AppRoute.Goals, Icons.Filled.TrackChanges, "Goals"),
        Triple(AppRoute.Evaluation, Icons.Filled.Analytics, "AI Eval"),
        Triple(AppRoute.Settings, Icons.Filled.Person, "Profile")
    )

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF0D1117).copy(alpha = 0.95f),
                contentColor = Color.White
            ) {
                tabs.forEach { (route, icon, label) ->
                    val selected = currentDestination?.hierarchy?.any { it.route == route.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(route.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (selected) NeonCyan else Color.White.copy(alpha = 0.5f)
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) NeonCyan else Color.White.copy(alpha = 0.5f)
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            indicatorColor = NeonPurple.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AppRoute.Chat.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(AppRoute.Chat.route) { ChatScreen(viewModel = viewModel) }
            composable(AppRoute.Diary.route) { DiaryScreen(viewModel = viewModel) }
            composable(AppRoute.Insights.route) { InsightsScreen(viewModel = viewModel) }
            composable(AppRoute.Goals.route) { GoalsScreen(viewModel = viewModel) }
            composable(AppRoute.Evaluation.route) { EvaluationScreen(viewModel = viewModel) }
            composable(AppRoute.Settings.route) { SettingsScreen(viewModel = viewModel) }
        }
    }
}
