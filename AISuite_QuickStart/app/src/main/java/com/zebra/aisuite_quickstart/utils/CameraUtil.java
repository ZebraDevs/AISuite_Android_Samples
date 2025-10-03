package com.zebra.aisuite_quickstart.utils;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.util.Log;

public class CameraUtil {

    public static int getPreferredCameraFacing(Context context) {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);

        try {
            // List all available camera IDs
            String[] cameraIds = cameraManager.getCameraIdList();

            for (String cameraId : cameraIds) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);

                // Check if the camera is facing back
                Integer lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (lensFacing != null && lensFacing == CameraMetadata.LENS_FACING_BACK) {
                    // Back camera found
                    return CameraMetadata.LENS_FACING_BACK;
                }
            }

            // If no back camera is found, return front camera as default
            return CameraMetadata.LENS_FACING_FRONT;

        } catch (CameraAccessException e) {
            Log.e("CameraUtil", "Error : "+e.getMessage());
            // If an exception occurs, default to front camera
            return CameraMetadata.LENS_FACING_FRONT;
        }
    }
}