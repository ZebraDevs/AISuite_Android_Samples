package com.zebra.ai.barcodebatchinventory.application.presentation.enums

/**
 * Enum representing all navigation destinations in the batch inventory app.
 * Each value corresponds to a distinct UI screen or modal that can be shown to the user.
 * Used for navigation logic, screen transitions, and UI state management.
 *
 * Screens:
 * - Home: Main dashboard screen
 * - Settings: General settings screen
 * - SettingsResolution: Camera resolution selection
 * - SettingsModelInputSize: Model input size selection
 * - SettingsInference: Processor type selection
 * - SettingsBarcodeSymbology: Barcode symbology selection
 * - About: About/info screen
 * - EULA: End User License Agreement screen
 * - Finder: Batch inventory scan screen
 */
enum class Screen {
    Home,
    Settings,
    SettingsResolution,
    SettingsModelInputSize,
    SettingsInference,
    SettingsBarcodeSymbology,
    About,
    EULA,
    Finder,
    ScanResults
}