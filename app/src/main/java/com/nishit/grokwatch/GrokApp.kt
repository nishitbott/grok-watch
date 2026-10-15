package com.nishit.grokwatch

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.composable
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.nishit.grokwatch.ui.HistoryScreen
import com.nishit.grokwatch.ui.HomeScreen
import com.nishit.grokwatch.ui.ReplyScreen
import com.nishit.grokwatch.ui.SettingsScreen
import com.nishit.grokwatch.ui.ThinkingScreen
import com.nishit.grokwatch.viewmodel.GrokViewModel

private object Routes {
    const val Home = "home"
    const val Thinking = "thinking"
    const val Reply = "reply"
    const val History = "history"
    const val Settings = "settings"
}

@Composable
fun GrokApp(viewModel: GrokViewModel = viewModel()) {
    val navController = rememberSwipeDismissableNavController()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = Routes.Home,
    ) {
        composable(Routes.Home) {
            HomeScreen(
                onPrompt = { prompt ->
                    viewModel.ask(prompt)
                    navController.navigate(Routes.Thinking)
                },
                onHistory = { navController.navigate(Routes.History) },
                onSettings = { navController.navigate(Routes.Settings) },
            )
        }
        composable(Routes.Thinking) {
            ThinkingScreen(
                viewModel = viewModel,
                onDone = {
                    navController.navigate(Routes.Reply) {
                        popUpTo(Routes.Thinking) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.Reply) {
            ReplyScreen(
                viewModel = viewModel,
                onAskAgain = {
                    viewModel.dismissSession()
                    navController.popBackStack(Routes.Home, inclusive = false)
                },
            )
        }
        composable(Routes.History) {
            HistoryScreen(
                viewModel = viewModel,
                onOpen = { job ->
                    viewModel.open(job)
                    navController.navigate(Routes.Reply) { launchSingleTop = true }
                },
            )
        }
        composable(Routes.Settings) {
            SettingsScreen(viewModel = viewModel)
        }
    }
}
