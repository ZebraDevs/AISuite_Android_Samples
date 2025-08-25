// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodefinder.common.enums

import com.zebra.ai.barcodefinder.R

/**
 * Enum representing available input sizes for the AI model.
 * Each value provides a display name resource (for localization), width, and height.
 * Used for configuring model input size and displaying options in the UI.
 */
enum class ModelInput(
    val displayNameResId: Int, // Resource ID for localized display name
    val homeDisplayNameResId: Int, // Resource ID for localized display name in home screen
    val width: Int,            // Input width for the model
    val height: Int            // Input height for the model
) {
    /** Small input size: 640x640 */
    SMALL_640(
        R.string.setting_model_input_640_title,
        R.string.home_model_input_640_title,
        640, 640
    ),

    /** Medium input size: 1280x1280 */
    MEDIUM_1280(
        R.string.setting_model_input_1280_title,
        R.string.home_model_input_1280_title,
        1280, 1280
    ),

    /** Large input size: 1600x1600 */
    LARGE_1600(
        R.string.setting_model_input_1600_title,
        R.string.home_model_input_1600_title,
        1600, 1600
    ),

    /** Extra large input size: 2560x2560 */
    EXTRA_LARGE_2560(
        R.string.setting_model_input_2560_title,
        R.string.home_model_input_2560_title,
        2560, 2560
    );
}