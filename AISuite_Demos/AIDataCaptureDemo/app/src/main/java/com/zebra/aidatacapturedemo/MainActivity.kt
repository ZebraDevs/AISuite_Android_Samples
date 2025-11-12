package com.zebra.aidatacapturedemo

import android.Manifest
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.zebra.aidatacapturedemo.model.FileUtils
import com.zebra.aidatacapturedemo.ui.theme.AIDataCaptureDemoTheme
import com.zebra.aidatacapturedemo.ui.view.AIDataCaptureDemoApp
import com.zebra.aidatacapturedemo.ui.view.FeedbackUtils
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel

@OptIn(ExperimentalPermissionsApi::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        FileUtils(application.filesDir.absolutePath, application as Context)
        val viewModel: AIDataCaptureDemoViewModel by viewModels { AIDataCaptureDemoViewModel.factory() }
        FeedbackUtils(viewModel, application as Context)

        val activityLifecycle = lifecycle
        setContent {

            val multiplePermissionsState = rememberMultiplePermissionsState(
                listOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
            LaunchedEffect(Unit) {
                // Request the permission when the Composable first enters the composition
                multiplePermissionsState.launchMultiplePermissionRequest()
            }

            AIDataCaptureDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { activityInnerPadding ->
                    AIDataCaptureDemoApp(viewModel, activityInnerPadding, activityLifecycle)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        FeedbackUtils.deinitialize()
    }
}