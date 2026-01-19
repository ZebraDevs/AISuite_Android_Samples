package com.zebra.ai.barcodefinder.sdkcoordinator.support

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat

import com.zebra.ai.barcodefinder.sdkcoordinator.enums.CoordinatorState

class PermissionHandler {
    companion object {
        private const val TAG = "PermissionManager"

        /**
         * Checks if the camera permission is granted and returns the appropriate CoordinatorState.
         */
        fun checkCameraPermission(application: Application): CoordinatorState {
            val hasPermission = ContextCompat.checkSelfPermission(
                application,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            return if (hasPermission) {
                Log.d(TAG, "Camera permission granted")
                CoordinatorState.CAMERA_PERMISSION_RECEIVED
            } else {
                // Check if the permission is permanently denied
                val shouldShowRationale = application.applicationContext is Activity &&
                        (application.applicationContext as Activity)
                            .shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)

                if (!shouldShowRationale) {
                    Log.d(TAG, "Camera permission denied (permanently)")
                    CoordinatorState.CAMERA_PERMISSION_DENIED
                } else {
                    Log.d(TAG, "Camera permission required")
                    CoordinatorState.CAMERA_PERMISSION_REQUIRED
                }
            }
        }
    }
}