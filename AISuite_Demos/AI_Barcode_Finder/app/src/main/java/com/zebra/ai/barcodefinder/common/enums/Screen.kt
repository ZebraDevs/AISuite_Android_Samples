// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.ai.barcodefinder.common.enums

/**
 * Enum representing all navigation destinations (screens) in the barcode finder app.
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
 * - Configure: Demo configuration screen
 * - About: About/info screen
 * - EULA: End User License Agreement screen
 * - Finder: Barcode finder/scan screen
 */
enum class Screen {
    Home,
    Settings,
    SettingsResolution,
    SettingsModelInputSize,
    SettingsInference,
    SettingsBarcodeSymbology,
    Configure,
    About,
    EULA,
    Finder,
    ScanResults
}