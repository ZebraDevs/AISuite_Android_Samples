// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.palletchecker.domain.enums

import com.google.gson.annotations.SerializedName
import com.zebra.ai.palletchecker.R

/**
 * Enum representing available input sizes for the AI model.
 * Each value provides a display name resource (for localization), width, and height.
 * Used for configuring model input size and displaying options in the UI.
 */

enum class ModelInput(
    val displayNameResId: Int, // Resource ID for localized display name
    val homeDisplayNameResId: Int, // Resource ID for localized display name in home screen
) {
    /** Small input size  */
    SMALL(
        R.string.model_input_small,
        R.string.model_input_small
    ),

    /** Medium input size  */
    MEDIUM(
        R.string.model_input_medium,
        R.string.model_input_medium
    ),

    /** Large input size */
    LARGE(
        R.string.model_input_large,
        R.string.model_input_large
    ),

    /**
     * Exact pixel size (width/height held in AppSettings). Maps to the SDK's
     * InferencerOptions.Resolution rather than a ModelResolution. Its home-screen display is
     * dynamic (the actual pixels), so these string resources are only a fallback label.
     *
     * The EXTRA_LARGE alternate keeps settings saved by older app versions readable: the old
     * Extra Large option was 2560 x 2560, which is the default Custom size.
     */
    @SerializedName(value = "CUSTOM", alternate = ["EXTRA_LARGE"])
    CUSTOM(
        R.string.model_input_custom,
        R.string.model_input_custom
    )
}