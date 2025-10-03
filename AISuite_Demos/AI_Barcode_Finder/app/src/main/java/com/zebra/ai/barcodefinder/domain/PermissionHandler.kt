package com.zebra.ai.barcodefinder.domain

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.zebra.ai.barcodefinder.domain.enums.RepositoryState

/**
 * Class responsible for handling camera permission checks and updates within the application.
 *
 * Responsibilities:
 * - Checks the current state of camera permissions and updates the repository state accordingly.
 * - Provides methods to handle changes in camera permission status, such as granted or denied.
 * - Logs permission status updates for debugging and tracking purposes.
 *
 * Usage:
 * - Use this class to check whether camera permission is granted using `checkCameraPermission`.
 * - Update permission state when permissions change using `onCameraPermissionGranted` and `onCameraPermissionDenied`.
 * - Integrate with other components to ensure proper permission handling and state management.
 * - Logs permission-related events to assist in debugging and monitoring application behavior regarding permissions.
 */
class PermissionHandler(private val application: Application) {
    private val TAG = "PermissionManager"

    /**
     * Checks if the camera permission is granted and returns the appropriate RepositoryState.
     */
    fun checkCameraPermission(): RepositoryState {
        val hasPermission = ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        return if (hasPermission) {
            Log.d(TAG, "Camera permission granted")
            RepositoryState.CAMERA_PERMISSION_RECEIVED
        } else {
            Log.d(TAG, "Camera permission required")
            RepositoryState.CAMERA_PERMISSION_REQUIRED
        }
    }

    /**
     * Call this method when camera permission is granted and return the new RepositoryState.
     */
    fun onCameraPermissionGranted(currentState: RepositoryState): RepositoryState {
        return if (currentState == RepositoryState.CAMERA_PERMISSION_REQUIRED ||
            currentState == RepositoryState.ENTITY_TRACKER_INITIALIZED) {
            Log.d(TAG, "Camera permission granted by user")
            RepositoryState.CAMERA_PERMISSION_RECEIVED
        } else {
            currentState
        }
    }

    /**
     * Call this method when camera permission is denied and return the new RepositoryState.
     */
    fun onCameraPermissionDenied(): RepositoryState {
        Log.d(TAG, "Camera permission denied by user")
        return RepositoryState.CAMERA_PERMISSION_DENIED
    }
}