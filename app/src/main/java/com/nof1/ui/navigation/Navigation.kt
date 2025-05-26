package com.nof1.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nof1.ui.screens.AddProjectScreen
import com.nof1.ui.screens.LogEntryScreen
import com.nof1.ui.screens.ProjectDetailScreen
import com.nof1.ui.screens.ProjectListScreen

/**
 * Main navigation component for the app.
 */
@Composable
fun Nof1Navigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "projects"
    ) {
        composable("projects") {
            ProjectListScreen(
                onNavigateToProject = { projectId ->
                    navController.navigate("project/$projectId")
                },
                onNavigateToAddProject = {
                    navController.navigate("add_project")
                }
            )
        }
        
        composable("add_project") {
            AddProjectScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("project/{projectId}") { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId")?.toLongOrNull() ?: 0L
            ProjectDetailScreen(
                projectId = projectId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToHypothesis = { hypothesisId ->
                    navController.navigate("hypothesis/$hypothesisId")
                },
                onNavigateToAddHypothesis = { projectId ->
                    navController.navigate("add_hypothesis/$projectId")
                }
            )
        }
        
        composable("hypothesis/{hypothesisId}") { backStackEntry ->
            val hypothesisId = backStackEntry.arguments?.getString("hypothesisId")?.toLongOrNull() ?: 0L
            // TODO: Implement HypothesisDetailScreen
        }
        
        composable("add_hypothesis/{projectId}") { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId")?.toLongOrNull() ?: 0L
            // TODO: Implement AddHypothesisScreen
        }
        
        composable("experiment/{experimentId}") { backStackEntry ->
            val experimentId = backStackEntry.arguments?.getString("experimentId")?.toLongOrNull() ?: 0L
            // TODO: Implement ExperimentDetailScreen
        }
        
        composable("log/{experimentId}") { backStackEntry ->
            val experimentId = backStackEntry.arguments?.getString("experimentId")?.toLongOrNull() ?: 0L
            LogEntryScreen(
                experimentId = experimentId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable("log/{experimentId}/{fromNotification}") { backStackEntry ->
            val experimentId = backStackEntry.arguments?.getString("experimentId")?.toLongOrNull() ?: 0L
            val fromNotification = backStackEntry.arguments?.getString("fromNotification")?.toBoolean() ?: false
            LogEntryScreen(
                experimentId = experimentId,
                isFromNotification = fromNotification,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
} 