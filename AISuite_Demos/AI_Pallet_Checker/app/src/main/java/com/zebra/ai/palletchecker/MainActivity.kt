package com.zebra.ai.palletchecker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.zebra.ai.palletchecker.presentation.ui.compose.components.LocalModelStoreOwner
import com.zebra.ai.palletchecker.presentation.ui.compose.components.ViewModelOwner
import com.zebra.ai.palletchecker.presentation.ui.compose.screens.homescreen.HomeScreen
import com.zebra.ai.palletchecker.presentation.ui.theme.AppTheme

class MainActivity : AppCompatActivity() {

    private var isAppInBackground = false
    private var isFinishing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = getColor(R.color.black)

        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false

        setContent {
            AppTheme {
                val owner = ViewModelOwner(this)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CompositionLocalProvider(LocalModelStoreOwner provides owner) {
                        HomeScreen(
                            onBackPressed = {
                                finish()
                            })
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
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