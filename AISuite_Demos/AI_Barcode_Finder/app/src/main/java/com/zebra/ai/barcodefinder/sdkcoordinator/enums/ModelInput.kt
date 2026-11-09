package com.zebra.ai.barcodefinder.sdkcoordinator.enums

import com.google.gson.annotations.SerializedName
import com.zebra.ai.barcodefinder.R

/**
 * App-side model input size. SMALL/MEDIUM/LARGE map to the AI Data Capture SDK's
 * [com.zebra.ai.vision.detector.InferencerOptions.ModelResolution]; CUSTOM maps to an exact
 * [com.zebra.ai.vision.detector.InferencerOptions.Resolution] (width/height held in
 * AppSettings). Both are applied by
 * [com.zebra.ai.barcodefinder.sdkcoordinator.support.BarcodeDecoderSettingsBuilder].
 *
 * Persisted by Gson using the constant name. The [SerializedName] alternates keep settings
 * written by older app versions (SMALL_640 / MEDIUM_1280 / LARGE_1600) readable after this
 * rename, so existing users' saved model-input choice is preserved across an app update.
 */
enum class ModelInput(
    val displayNameResId: Int,     // Resource ID for localized display name (settings)
    val homeDisplayNameResId: Int  // Resource ID for localized display name (home screen)
) {
    @SerializedName(value = "SMALL", alternate = ["SMALL_640"])
    SMALL(
        R.string.model_input_640_title_settings,
        R.string.model_input_640_title_home
    ),

    @SerializedName(value = "MEDIUM", alternate = ["MEDIUM_1280"])
    MEDIUM(
        R.string.model_input_1280_title_settings,
        R.string.model_input_1280_title_home
    ),

    @SerializedName(value = "LARGE", alternate = ["LARGE_1600"])
    LARGE(
        R.string.model_input_1600_title_settings,
        R.string.model_input_1600_title_home
    ),

    // Exact pixel size (width/height held in AppSettings). Applied via the SDK's
    // setModelResolution(Resolution) rather than the ModelResolution enum. Its display is dynamic
    // (the actual pixels), so these string resources are only a fallback label.
    @SerializedName(value = "CUSTOM")
    CUSTOM(
        R.string.model_input_custom_title_settings,
        R.string.model_input_custom_title_home
    );
}