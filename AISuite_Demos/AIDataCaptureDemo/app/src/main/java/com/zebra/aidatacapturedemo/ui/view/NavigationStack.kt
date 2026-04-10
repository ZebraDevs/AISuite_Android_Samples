package com.zebra.aidatacapturedemo.ui.view

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.zebra.aidatacapturedemo.data.FilterType
import com.zebra.aidatacapturedemo.ui.view.filters.BarcodeFindFilterHomeScreen
import com.zebra.aidatacapturedemo.ui.view.filters.CharacterMatchFilterScreen
import com.zebra.aidatacapturedemo.ui.view.filters.CharacterTypeFilterScreen
import com.zebra.aidatacapturedemo.ui.view.filters.OCRFindFilterHomeScreen
import com.zebra.aidatacapturedemo.ui.view.filters.RegexFilterScreen
import com.zebra.aidatacapturedemo.ui.view.filters.StringLengthFilterScreen
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel

/**
 * Screen is a sealed class that defines the different routes for navigation in the
 * AI Data Capture Demo app. Each object within the sealed class represents a specific screen
 * in the app, identified by a unique route string.
 * This structure allows for type-safe navigation and easy management of the app's screens.
 */
sealed class Screen(val route: String) {
    object Start : Screen("start_screen")
    object DemoStart : Screen("demo_start_screen")
    object DemoSetting : Screen("demo_setting_screen")
    object DemoSettingMore : Screen("demo_setting_more_screen")
    object AdvancedOCRSettings : Screen("advanced_ocr_setting_screen")
    object Preview : Screen("preview_screen")
    object ProductsCapture : Screen("products_capture_screen")
    object OCRBarcodeCapture : Screen("ocrbarcode_capture_screen")
    object OCRBarcodeResults : Screen("ocrbarcode_results_screen")
    object SingleResult : Screen("single_result_screen")

    /**
     * Filter related Screen
     */
    object OCRFindFilterHome : Screen("ocr_find_filter_home_screen")
    object CharacterTypeFilter : Screen("character_type_filter_screen")
    object CharacterMatchFilter : Screen("character_match_filter_screen")
    object StringLengthFilter : Screen("string_length_filter_screen")
    object RegexFilter : Screen("regex_filter_screen")
    object BarcodeFindFilterHome : Screen("barcode_find_filter_home_screen")
}

@Composable
fun NavigationStack(
    navController: NavHostController,
    viewModel: AIDataCaptureDemoViewModel,
    activityInnerPadding: PaddingValues,
    innerPadding: PaddingValues,
    context: Context,
    activityLifecycle: Lifecycle
) {

    NavHost(navController = navController, startDestination = Screen.Start.route) {
        composable(route = Screen.Start.route) {
            viewModel.updateActiveScreenData(activeScreen = Screen.Start)
            AIDataCaptureStartScreen(viewModel, navController = navController, innerPadding)
        }
        composable(route = Screen.DemoStart.route) {
            viewModel.updateActiveScreenData(activeScreen = Screen.DemoStart)
            DemoStartScreen(viewModel, navController = navController, innerPadding, context = context)
        }
        composable(route = Screen.DemoSetting.route) {
            viewModel.updateActiveScreenData(activeScreen = Screen.DemoSetting)
            DemoSettingsScreen(viewModel, navController = navController, innerPadding)
        }
        composable(route = Screen.AdvancedOCRSettings.route) {
            viewModel.updateActiveScreenData(activeScreen = Screen.AdvancedOCRSettings)
            AdvancedOCRSettingsScreen(
                viewModel,
                navController = navController,
                innerPadding,
                context = context
            )
        }
        composable(route = Screen.Preview.route) {
            viewModel.updateActiveScreenData(activeScreen = Screen.Preview)
            CameraPreviewScreen(
                viewModel,
                navController = navController,
                context,
                activityInnerPadding,
                activityLifecycle
            )
        }
        composable(route = Screen.ProductsCapture.route) {
            viewModel.updateActiveScreenData(activeScreen = Screen.ProductsCapture)
            ProductsResultCapturedScreen(
                viewModel,
                navController = navController,
                innerPadding,
                context = context
            )
        }
        composable(route = Screen.OCRBarcodeCapture.route) {
            viewModel.updateActiveScreenData(activeScreen = Screen.OCRBarcodeCapture)
            OCRBarcodeResultCapturedScreen(
                viewModel,
                navController = navController,
                innerPadding,
                context = context
            )
        }
        composable(route = Screen.OCRBarcodeResults.route) {
            viewModel.updateActiveScreenData(activeScreen = Screen.OCRBarcodeResults)
            OCRBarcodeResultScreen(
                viewModel,
                navController = navController,
                innerPadding,
                context = context
            )
        }
        composable(route = Screen.SingleResult.route + "?text={text}&bbox={bbox}&isBarcode={isBarcode}") { backStackEntry ->
            viewModel.updateActiveScreenData(activeScreen = Screen.SingleResult)
            val text = backStackEntry.arguments?.getString("text") ?: ""
            val bboxStr = backStackEntry.arguments?.getString("bbox") ?: ""
            val isBarcodeStr = backStackEntry.arguments?.getString("isBarcode") ?: "false"
            val bboxParts = bboxStr.split(",")
            val boundingBox = if (bboxParts.size == 4) {
                android.graphics.Rect(
                    bboxParts[0].toIntOrNull() ?: 0,
                    bboxParts[1].toIntOrNull() ?: 0,
                    bboxParts[2].toIntOrNull() ?: 0,
                    bboxParts[3].toIntOrNull() ?: 0
                )
            } else {
                android.graphics.Rect()
            }
            val isBarcode = isBarcodeStr == "true"
            val resultRowData = ResultRowData(
                text = text,
                boundingBox = boundingBox,
                isBarcode = isBarcode
            )
            SingleResultScreen(
                viewModel,
                navController = navController,
                innerPadding,
                context = context,
                resultRowData = resultRowData
            )
        }

        // filter related Navigation
        composable(route = Screen.OCRFindFilterHome.route) {
            viewModel.updateActiveScreenData(activeScreen = Screen.OCRFindFilterHome)
            viewModel.updateSelectedFilterType(filterType = FilterType.OCR_FILTER)
            OCRFindFilterHomeScreen(viewModel, navController = navController, innerPadding)
        }
        composable(route = Screen.BarcodeFindFilterHome.route) {
            viewModel.updateActiveScreenData(activeScreen = Screen.BarcodeFindFilterHome)
            viewModel.updateSelectedFilterType(filterType = FilterType.BARCODE_FILTER)
            BarcodeFindFilterHomeScreen(viewModel, navController = navController, innerPadding)
        }
        composable(route = Screen.RegexFilter.route) {
            viewModel.updateActiveScreenData(activeScreen = Screen.RegexFilter)
            RegexFilterScreen(viewModel, navController = navController, innerPadding)
        }
        composable(route = Screen.CharacterTypeFilter.route) {
            viewModel.updateActiveScreenData(activeScreen = Screen.CharacterTypeFilter)
            CharacterTypeFilterScreen(viewModel, navController = navController, innerPadding)
        }
        composable(route = Screen.CharacterMatchFilter.route) {
            viewModel.updateActiveScreenData(activeScreen = Screen.CharacterMatchFilter)
            CharacterMatchFilterScreen(viewModel, navController = navController, innerPadding)
        }
        composable(route = Screen.StringLengthFilter.route) {
            viewModel.updateActiveScreenData(activeScreen = Screen.StringLengthFilter)
            StringLengthFilterScreen(viewModel, navController = navController, innerPadding)
        }
    }
}