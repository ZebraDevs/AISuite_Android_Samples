// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.handlers;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.zebra.aisuite_quickstart.java.CameraXLivePreviewActivity;

/**
 * BoundingBoxMapper handles the mapping of bounding boxes from image coordinates
 * to overlay coordinates, including rotation transformations.
 */
public class BoundingBoxMapper {
    private static final String TAG = "BoundingBoxMapper";

    private final Context context;
    private int imageWidth;
    private int imageHeight;
    private int initialRotation;
    private boolean isFrontCamera;

    private static final int ROTATION_0 = 0;
    private static final int ROTATION_90 = 1;
    private static final int ROTATION_180 = 2;
    private static final int ROTATION_270 = 3;

    private final CameraXLivePreviewActivity activity;
    private boolean isTablet = false;
    private int cameraOrientation = 0;
    private boolean isHorizontalCameraTablet = false;

    public BoundingBoxMapper(CameraXLivePreviewActivity activity,Context context) {
        this.activity = activity;
        this.context = context;

        isTablet = isTablet(context);
        detectCameraOrientation();
    }

    public void setImageDimensions(int width, int height) {
        this.imageWidth = width;
        this.imageHeight = height;
    }

    public void setInitialRotation(int rotation) {
        this.initialRotation = rotation;
    }

    public void setFrontCamera(boolean frontCamera) {
        this.isFrontCamera = frontCamera;
    }

    public Rect mapBoundingBoxToOverlay(Rect bbox) {
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay();
        int currentRotation = display.getRotation();

        int relativeRotation = ((currentRotation - initialRotation + 4) % 4);
        Log.d(TAG, "current rotation :"+currentRotation+" initial rotation "+initialRotation+" relative "+relativeRotation);

        // This would need to be passed from the activity or calculated
        int overlayWidth = activity.getBinding().previewView.getWidth(); // Need to get from GraphicOverlay
        int overlayHeight = activity.getBinding().previewView.getHeight(); // Need to get from GraphicOverlay

        if (overlayWidth == 0 || overlayHeight == 0) {
            return bbox;
        }


        Rect transformedBbox = transformBoundingBoxForRotation(bbox, relativeRotation);

        int effectiveImageWidth = imageWidth;
        int effectiveImageHeight = imageHeight;

        if(isHorizontalCameraTablet) {
            if ((isTablet && (relativeRotation == ROTATION_0 || relativeRotation == ROTATION_180)) ||
                    (!isTablet && (relativeRotation == ROTATION_90 || relativeRotation == ROTATION_270))) {
                effectiveImageWidth = imageHeight;
                effectiveImageHeight = imageWidth;
            }
        }
        Log.d(TAG,"overlay width and height"+overlayWidth+" "+ overlayHeight+ "effective width n height"+effectiveImageWidth+" "+ effectiveImageHeight);

        float scaleX = (float) overlayWidth / effectiveImageWidth;
        float scaleY = (float) overlayHeight / effectiveImageHeight;
        float scale = Math.max(scaleX, scaleY);

        float offsetX = (overlayWidth - effectiveImageWidth * scale) / 2f;
        float offsetY = (overlayHeight - effectiveImageHeight * scale) / 2f;

        // Handle mirroring for front camera
        if (isFrontCamera) {
            int left = transformedBbox.left;
            int right = transformedBbox.right;
            transformedBbox.left = effectiveImageWidth - right;
            transformedBbox.right = effectiveImageWidth - left;
        }

        return new Rect(
                (int) (transformedBbox.left * scale + offsetX),
                (int) (transformedBbox.top * scale + offsetY),
                (int) (transformedBbox.right * scale + offsetX),
                (int) (transformedBbox.bottom * scale + offsetY)
        );
    }

    private Rect transformBoundingBoxForRotation(Rect bbox, int relativeRotation) {
        switch (relativeRotation) {
            case ROTATION_0:
                return new Rect(bbox);
            case ROTATION_90:
                return new Rect(
                        bbox.top,
                        imageWidth - bbox.right,
                        bbox.bottom,
                        imageWidth - bbox.left
                );
            case ROTATION_180:
                return new Rect(
                        imageWidth - bbox.right,
                        imageHeight - bbox.bottom,
                        imageWidth - bbox.left,
                        imageHeight - bbox.top
                );
            case ROTATION_270:
                return new Rect(
                        imageHeight - bbox.bottom,
                        bbox.left,
                        imageHeight - bbox.top,
                        bbox.right
                );
            default:
                Log.w(TAG, "Unknown relative rotation: " + relativeRotation + ", using original bbox");
                return new Rect(bbox);
        }
    }

    private boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }
private void detectCameraOrientation() {
    try {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        String[] cameraIdList = cameraManager.getCameraIdList();
        String selectedCameraId = null;
        for (String id : cameraIdList) {
            CameraCharacteristics chars = cameraManager.getCameraCharacteristics(id);
            Integer facing = chars.get(CameraCharacteristics.LENS_FACING);
            if ((isFrontCamera && facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) ||
                    (!isFrontCamera && facing != null && facing == CameraCharacteristics.LENS_FACING_BACK)) {
                selectedCameraId = id;
                break;
            }
        }
        if (selectedCameraId != null) {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(selectedCameraId);
            Integer orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            cameraOrientation = orientation != null ? orientation : 0;

            // Detect horizontal camera tablets (you may need to adjust this logic)
            isHorizontalCameraTablet = isTablet && (cameraOrientation == 0 || cameraOrientation == 180);

            Log.d(TAG, "Camera orientation: "+cameraOrientation+", isHorizontalCameraTablet: " + isHorizontalCameraTablet);
        } else {
            Log.e(TAG, "No suitable camera found for the requested facing direction.");
        }

    } catch (Exception e) {
        Log.e(TAG, "Failed to get camera orientation", e);
    }
}
}