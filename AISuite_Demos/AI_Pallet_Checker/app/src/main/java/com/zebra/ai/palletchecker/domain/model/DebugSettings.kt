// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.palletchecker.domain.model

/**
 * DebugSettings holds debug configuration options for the pallet checker workflow.
 * These settings allow developers and testers to visualize internal states
 * during the pallet capture and wanding process.
 */
data class DebugSettings(

    val debugModeEnabled: Boolean = false,

    val showPreSnapBoundingBoxes: Boolean = false,

    val showSpatialMapIndices: Boolean = false,

    val showSnapBarcodeLabels: Boolean = false,

    val showSnapTrackingIds: Boolean = false,

    val showWandBarcodeLabels: Boolean = false,

    val showWandTrackingIds: Boolean = false,

    val showCapturedSnapBarcodeLabels: Boolean = false,

    val dumpSnapImagesToFilesystem: Boolean = false
) {
    /**
     * Compares this DebugSettings instance to another for equality of all fields.
     */
    fun isEquals(other: DebugSettings): Boolean {
        return debugModeEnabled == other.debugModeEnabled &&
                showPreSnapBoundingBoxes == other.showPreSnapBoundingBoxes &&
                showSpatialMapIndices == other.showSpatialMapIndices &&
                showSnapBarcodeLabels == other.showSnapBarcodeLabels &&
                showSnapTrackingIds == other.showSnapTrackingIds &&
                showWandBarcodeLabels == other.showWandBarcodeLabels &&
                showWandTrackingIds == other.showWandTrackingIds &&
                showCapturedSnapBarcodeLabels == other.showCapturedSnapBarcodeLabels &&
                dumpSnapImagesToFilesystem == other.dumpSnapImagesToFilesystem
    }
}

