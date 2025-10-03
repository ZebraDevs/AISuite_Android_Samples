// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodefinder.domain.enums

import com.zebra.ai.barcodefinder.R

/**
 * Enum representing available camera resolutions for the barcode finder app.
 * Each value provides a display name resource (for localization), width, and height.
 * Used for configuring camera resolution and displaying options in the UI.
 */
enum class Resolution(
    val displayNameResId: Int, // Resource ID for localized display name
    val width: Int,            // Camera resolution width
    val height: Int            // Camera resolution height
) {
    /** 1 Megapixel resolution: 1280x720 */
    ONE_MP(
        R.string.resolution_1mp,
        1280, 720
    ),

    /** 2 Megapixel resolution: 1920x1080 */
    TWO_MP(
        R.string.resolution_2mp,
        1920, 1080
    ),

    /** 4 Megapixel resolution: 2688x1512 */
    FOUR_MP(
        R.string.resolution_4mp,
        2688, 1512
    ),

    /** 8 Megapixel resolution: 3840x2160 */
    EIGHT_MP(
        R.string.resolution_8mp,
        3840, 2160
    );
}
