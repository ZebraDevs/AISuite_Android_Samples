package com.zebra.ai.barcodefinder.sdkcoordinator.enums

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
        R.string.model_input_640_title_settings,
        R.string.model_input_640_title_home,
        640, 640
    ),

    /** Medium input size: 1280x1280 */
    MEDIUM_1280(
        R.string.model_input_1280_title_settings,
        R.string.model_input_1280_title_home,
        1280, 1280
    ),

    /** Large input size: 1600x1600 */
    LARGE_1600(
        R.string.model_input_1600_title_settings,
        R.string.model_input_1600_title_home,
        1600, 1600
    );
}