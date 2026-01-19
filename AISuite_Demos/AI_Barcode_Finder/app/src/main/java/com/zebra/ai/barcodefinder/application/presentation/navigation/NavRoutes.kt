package com.zebra.ai.barcodefinder.application.presentation.navigation

sealed class NavRoutes(val route: String) {
    object Home : NavRoutes("home")
    object Configure : NavRoutes("configure")
    object Finder : NavRoutes("finder")
    object ScanResults : NavRoutes("scan_results?from={from}") {
        fun createRoute(from: String) = "scan_results?from=$from"
    }
    object Settings : NavRoutes("settings")
    object SettingsResolution : NavRoutes("settings_resolution")
    object SettingsModelInputSize : NavRoutes("settings_model_input_size")
    object SettingsInference : NavRoutes("settings_inference")
    object SettingsBarcodeSymbology : NavRoutes("settings_barcode_symbology")
    object About : NavRoutes("about")
    object EULA : NavRoutes("eula")
}