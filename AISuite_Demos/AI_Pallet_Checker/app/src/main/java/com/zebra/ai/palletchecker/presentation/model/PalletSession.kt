package com.zebra.ai.palletchecker.presentation.model

import android.graphics.Bitmap
import com.zebra.ai.palletchecker.helpers.PalletSpatialMap

/**
 * Determines the audit workflow mode after snap processing.
 *
 * PALLET_FINITE    — boxes seen during snap >= boxes to audit. The full spatial map
 *                    is built from snap, PIP thumbnail is shown, and wand navigates
 *                    through a pre-built map of known boxes.
 *
 * SHELF_CONTINUOUS — boxes seen < boxes to audit. No PIP. The spatial map grows
 *                    dynamically during wand as new unique primary barcodes are
 *                    discovered. The wand timer resets on every new discovery.
 *                    Session ends when unique primary barcodes found == expectedBoxes.
 */
enum class SessionMode { PALLET_FINITE, SHELF_CONTINUOUS }

data class PalletSession(
    val storedPalletDetails: List<PalletBox> = emptyList(),
    val selectedBoxToScan: PalletBox? = null,
    val partialDetection: List<PBoxUIModel> = emptyList(),
    val storedPalletCaptureImage: Bitmap? = null,
    val storedImageRotation: Int = 0,
    val mappedBarcodeQty: Map<String, Int> = mapOf(),
    val spatialMap: PalletSpatialMap? = null,
    val sessionMode: SessionMode = SessionMode.PALLET_FINITE,
    val expectedBoxes: Int = 0,
    val hasUnmappedWandBoxes: Boolean = false,
    val isHybridWand: Boolean = false
)
