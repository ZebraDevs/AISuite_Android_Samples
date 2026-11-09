package com.zebra.ai.barcodefinder.sdkcoordinator.enums

import com.zebra.ai.barcodefinder.R

/**
 * Enum representing the Direct Part Mark (DPM) decoding modes for the barcode decoder.
 *
 * DPM codes are marked directly onto a surface (e.g., laser-etched or dot-peened) rather than
 * printed on a label. Each value provides a localized display name and description resource ID
 * for the Settings UI, and maps to the AI Data Capture SDK's
 * BarcodeDecoder.Settings.DpmSettings.DpmMode when applied.
 */
enum class DpmMode(
    val displayNameResId: Int,  // Resource ID for localized display name
    val descriptionResId: Int   // Resource ID for localized description
) {
    /** Standard barcode decoding (DPM disabled). */
    DISABLED(
        R.string.setting_screen_dpm_disabled_title,
        R.string.setting_screen_dpm_disabled_subtitle
    ),

    /** Optimized for small Direct Part Mark codes. */
    SMALL(
        R.string.setting_screen_dpm_small_title,
        R.string.setting_screen_dpm_small_subtitle
    ),

    /** Optimized for large Direct Part Mark codes. */
    LARGE(
        R.string.setting_screen_dpm_large_title,
        R.string.setting_screen_dpm_large_subtitle
    );
}
