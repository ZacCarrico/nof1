package com.nof1

import android.Manifest
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
                    Nof1Navigation()
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
} 