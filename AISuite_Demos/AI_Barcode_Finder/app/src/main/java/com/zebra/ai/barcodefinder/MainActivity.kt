package com.zebra.ai.barcodefinder

import android.R
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.zebra.ai.barcodefinder.application.presentation.navigation.AppNavHost
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.AppTheme

class MainActivity : AppCompatActivity() {

    private var isAppInBackground = false
    private var isFinishing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure status bar color to match app title bar
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = getColor(R.color.black)

        // Set status bar content to light (white icons/text) since we're using dark background
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        // Show the main application content
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.Companion.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost(
                        onExit = {
                            finish()  // This will close the activity and exit the application
                        }
                    )
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