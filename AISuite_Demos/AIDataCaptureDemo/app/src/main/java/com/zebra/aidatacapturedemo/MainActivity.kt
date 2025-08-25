package com.zebra.aidatacapturedemo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.zebra.aidatacapturedemo.model.FileUtils
import com.zebra.aidatacapturedemo.ui.theme.AIDataCaptureDemoTheme
import com.zebra.aidatacapturedemo.ui.view.AIDataCaptureDemoApp
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel

private const val TAG = "MainActivity"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkCameraPermissions()
        checkStoragePermissions()

        FileUtils(application.filesDir.absolutePath, application as Context)
        val viewModel: AIDataCaptureDemoViewModel by viewModels { AIDataCaptureDemoViewModel.factory() }

        val activityLifecycle = lifecycle
        setContent {
            AIDataCaptureDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { activityInnerPadding ->
                    AIDataCaptureDemoApp(viewModel, activityInnerPadding, activityLifecycle)
                }
            }
        }
    }

    /**
     * Check for camera permissions
     */
    private fun checkCameraPermissions() {
        val cameraPermissionRequest =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    Log.i(TAG, "Camera Permissions Granted")
                } else {
                    Log.i(TAG, "Camera Permissions Not Granted")
                }

            }

        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) -> {
                Log.i(TAG, "Camera Permissions Already Granted")
            }

            else -> {
                cameraPermissionRequest.launch(Manifest.permission.CAMERA)
            }
        }
    }

    /**
     * Check for storage permissions
     */
    private fun checkStoragePermissions() {
        val storagePermissionRequest =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    Log.i(TAG, "Storage Permissions Granted")
                } else {
                    Log.i(TAG, "Storage Permissions Not Granted")
                }

            }

        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.MANAGE_EXTERNAL_STORAGE
            ) -> {
                Log.i(TAG, "Storage Permissions Already Granted")
            }

            else -> {
                storagePermissionRequest.launch(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
            }
        }
    }
}