// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.zebra.ai.vision.detector.AIVisionSDK;
import com.zebra.aisuite_quickstart.databinding.ActivityEntryChoiceBinding;
import com.zebra.aisuite_quickstart.java.CameraXLivePreviewActivity;

/**
 * The EntryChoiceActivity class is an Android activity that serves as an entry point for selecting
 * between different implementations of camera-based functionalities. It checks for camera permissions,
 * initializes the AI Vision SDK, and provides options to navigate to Java or Kotlin versions of CameraX
 * Live Preview activity.

 * Usage:
 * - This activity is started as part of the application launch sequence.
 * - It displays buttons that allow the user to choose between Java and Kotlin implementations of CameraXLivePreviewActivity.
 * - It ensures that camera permissions are granted and initializes the AI Vision SDK.

 * Dependencies:
 * - Android Activity: Provides the lifecycle management and user interface capabilities.
 * - ActivityEntryChoiceBinding: Used for view binding to access UI components.
 * - AIVisionSDK: Provides functionality for initializing AI Vision capabilities.
 * - CameraXLivePreviewActivity: The target activity for the Java implementation.
 * - com.zebra.aisuite_quickstart.kotlin.CameraXLivePreviewActivity: The target activity for the Kotlin implementation.

 * Permissions:
 * - Camera permission is required for the app to function. This activity handles permission checks and requests.

 * Exception Handling:
 * - Handles UnsupportedOperationException during AI Vision SDK initialization.
 * - Displays error dialogs to inform the user of issues.

 * Note: Ensure that the appropriate permissions are configured in the AndroidManifest to utilize camera capabilities.
 */
public class EntryChoiceActivity extends AppCompatActivity {

    private ActivityEntryChoiceBinding binding;
    private final String TAG = "EntryChoiceActivity";
    // Define a request code for camera permission
    private static final int REQUEST_CAMERA_PERMISSION = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEntryChoiceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initializing the AI Vision SDK
        try {
            boolean isInitDone = AIVisionSDK.getInstance(this.getApplicationContext()).init();
            Log.i(TAG, "AI Vision SDK Init ret = " + isInitDone);
        } catch (UnsupportedOperationException ex) {
            Log.e(TAG, "AI Vision SDK Initialization failed");
        }

        binding.javaEntryPoint.setOnClickListener(v -> {
            Intent mainIntent = new Intent(this, CameraXLivePreviewActivity.class);
            startActivity(mainIntent);
        });

        binding.kotlinEntryPoint.setOnClickListener(v -> {
            Intent mainIntent = new Intent(this, com.zebra.aisuite_quickstart.kotlin.CameraXLivePreviewActivity.class);
            startActivity(mainIntent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkCameraPermission();

        // Initializing the AI Vision SDK
        try {
            boolean isInitDone = AIVisionSDK.getInstance(this.getApplicationContext()).init();
            Log.i(TAG, "AI Vision SDK Init = " + isInitDone);
            Log.i(TAG, "AI Vision SDK version = " + AIVisionSDK.getInstance(this.getApplicationContext()).getSDKVersion());
        } catch (UnsupportedOperationException ex) {
            runOnUiThread(() -> showErrorDialog(ex.getMessage()));
        }
    }

    /**
     * Displays an error dialog with a specified message.
     *
     * @param message The message to display in the dialog.
     */
    private void showErrorDialog(String message) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setMessage(message);
        builder.setCancelable(false);

        builder.setPositiveButton(
                "Ok",
                (dialog, id) -> {
                    dialog.cancel();
                    EntryChoiceActivity.this.finish();
                });
        builder.create().show();
    }

    /**
     * Checks for camera permission and requests it if not granted.
     */
    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                // Show an explanation to the user why the permission is needed
                showNonCancellablePermissionRationaleDialog();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        REQUEST_CAMERA_PERMISSION);
            }
        } else {
            Log.v(TAG, "Camera permission granted");
        }
    }

    /**
     * Displays a dialog explaining why camera permission is needed and provides options to open settings or exit.
     */
    private void showNonCancellablePermissionRationaleDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Camera Permission Needed")
                .setMessage("This app needs the Camera permission to function. Please grant the permission in the app settings.")
                .setPositiveButton("Open Settings", (dialog, which) -> openAppSettings())
                .setNegativeButton("Exit", (dialog, which) -> {
                    // Exit the app or disable the functionality
                    finish();
                })
                .setCancelable(false);

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * Opens the app settings in the device settings menu.
     */
    private void openAppSettings() {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Camera permission granted");
            } else {
                Toast.makeText(this, "Camera permission is required to use this feature", Toast.LENGTH_SHORT).show();
                showNonCancellablePermissionRationaleDialog();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
