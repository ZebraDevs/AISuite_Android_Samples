// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import com.zebra.ai.barcodefinder.ui.compose.screens.homescreen.HomeScreen
import com.zebra.ai.barcodefinder.viewmodel.EntityTrackerViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var entityTrackerViewModel: EntityTrackerViewModel
    private var isAppInBackground = false
    private var isFinishing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure status bar color to match app title bar
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = getColor(android.R.color.black)

        // Set status bar content to light (white icons/text) since we're using dark background
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        // Initialize ViewModel
        entityTrackerViewModel = ViewModelProvider(this)[EntityTrackerViewModel::class.java]

        // Show the main application content
        setContent {
            com.zebra.ai.barcodefinder.ui.theme.AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background
                ) {
                    // Let HomeScreen handle all navigation including BarcodeFinderScreen and ScanResultsScreen
                    HomeScreen(
                        entityTrackerViewModel = entityTrackerViewModel, onBackPressed = {
                            finish()  // This will close the activity and exit the application
                        })
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Mark that we might be going to background
        isAppInBackground = true
    }

    override fun onResume() {
        super.onResume()
        isAppInBackground = false
    }

    override fun finish() {
        isFinishing = true
        super.finish()
    }
}