package com.nof1

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.nof1.ui.components.NotificationPermissionDialog
import com.nof1.ui.navigation.Nof1Navigation
import com.nof1.ui.theme.Nof1Theme
import com.nof1.utils.NotificationHelper
import com.nof1.utils.PreferencesHelper

class MainActivity : ComponentActivity() {
    
    private lateinit var preferencesHelper: PreferencesHelper
    
    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        preferencesHelper.setNotificationPermissionRequested()
        if (isGranted) {
            // Permission granted, user can receive notifications
        } else {
            // Permission denied, handle accordingly
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        preferencesHelper = PreferencesHelper(this)
        
        // Initialize notification channel
        NotificationHelper.createNotificationChannel(this)
        
        setContent {
            var showNotificationDialog by remember { mutableStateOf(false) }
            val navController = rememberNavController()
            
            // Handle notification navigation
            LaunchedEffect(intent) {
                // Small delay to ensure NavController is initialized
                kotlinx.coroutines.delay(100)
                handleNotificationIntent(navController)
            }
            
            // Check if we should show notification permission dialog
            LaunchedEffect(Unit) {
                val isFirstLaunch = preferencesHelper.isFirstLaunch()
                val hasPermissionBeenRequested = preferencesHelper.isNotificationPermissionRequested()
                val hasNotificationPermission = NotificationHelper.hasNotificationPermission(this@MainActivity)
                
                if (isFirstLaunch && !hasPermissionBeenRequested && !hasNotificationPermission) {
                    showNotificationDialog = true
                }
                
                // Mark first launch as completed
                if (isFirstLaunch) {
                    preferencesHelper.setFirstLaunchCompleted()
                }
            }
            
            Nof1Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Nof1Navigation(navController = navController)
                }
                
                // Show notification permission dialog if needed
                if (showNotificationDialog) {
                    NotificationPermissionDialog(
                        onAllowNotifications = {
                            showNotificationDialog = false
                            requestNotificationPermission()
                        },
                        onDenyNotifications = {
                            showNotificationDialog = false
                            preferencesHelper.setNotificationPermissionRequested()
                        },
                        onDismiss = {
                            showNotificationDialog = false
                            preferencesHelper.setNotificationPermissionRequested()
                        }
                    )
                }
            }
        }
    }
    
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                    preferencesHelper.setNotificationPermissionRequested()
                }
                else -> {
                    // Request permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // For Android 12 and below, notifications are enabled by default
            preferencesHelper.setNotificationPermissionRequested()
        }
    }
    
    private fun handleNotificationIntent(navController: NavHostController) {
        // Handle navigation from notification clicks
        intent?.let { notificationIntent ->
            val hypothesisId = notificationIntent.getLongExtra("hypothesis_id", -1L)
            val projectId = notificationIntent.getLongExtra("project_id", -1L)
            val experimentId = notificationIntent.getLongExtra("experiment_id", -1L)
            val fromReminder = notificationIntent.getBooleanExtra("from_reminder", false)
            val fromNotification = notificationIntent.getBooleanExtra("from_notification", false)
            
            // Debug logging to understand what's in the intent
            android.util.Log.d("MainActivity", "Processing notification intent:")
            android.util.Log.d("MainActivity", "  hypothesis_id: $hypothesisId")
            android.util.Log.d("MainActivity", "  project_id: $projectId")
            android.util.Log.d("MainActivity", "  experiment_id: $experimentId")
            android.util.Log.d("MainActivity", "  from_reminder: $fromReminder")
            android.util.Log.d("MainActivity", "  from_notification: $fromNotification")
            
            when {
                hypothesisId > 0 && fromReminder -> {
                    // Navigate directly to hypothesis from hypothesis reminder notification
                    android.util.Log.d("MainActivity", "Navigating to hypothesis $hypothesisId")
                    navController.navigate("hypothesis/$hypothesisId") {
                        // Clear back stack to prevent going back to projects
                        popUpTo("projects") { inclusive = false }
                    }
                }
                projectId > 0 && fromReminder -> {
                    // Navigate to project from project reminder notification
                    android.util.Log.d("MainActivity", "Navigating to project $projectId")
                    navController.navigate("project/$projectId") {
                        popUpTo("projects") { inclusive = false }
                    }
                }
                experimentId > 0 && fromNotification -> {
                    // Navigate to experiment log entry from experiment notification
                    android.util.Log.d("MainActivity", "Navigating to experiment log $experimentId")
                    navController.navigate("log/$experimentId/true") {
                        popUpTo("projects") { inclusive = false }
                    }
                }
                else -> {
                    android.util.Log.d("MainActivity", "No valid navigation target found in intent")
                }
            }
            
            // Clear the intent extras to prevent re-navigation on configuration changes
            intent.removeExtra("hypothesis_id")
            intent.removeExtra("project_id")
            intent.removeExtra("experiment_id")
            intent.removeExtra("from_reminder")
            intent.removeExtra("from_notification")
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Handle new intents when app is already running
        setIntent(intent)
    }
} 