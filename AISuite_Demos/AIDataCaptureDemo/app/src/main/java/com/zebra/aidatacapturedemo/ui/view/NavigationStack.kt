package com.zebra.aidatacapturedemo.ui.view

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.data.FilterType
import com.zebra.aidatacapturedemo.data.UsecaseState
import com.zebra.aidatacapturedemo.ui.view.filters.BarcodeFindFilterHomeScreen
import com.zebra.aidatacapturedemo.ui.view.filters.CharacterMatchFilterScreen
import com.zebra.aidatacapturedemo.ui.view.filters.CharacterTypeFilterScreen
import com.zebra.aidatacapturedemo.ui.view.filters.FilterConfigScreen
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
    object RetailShelfCapture : Screen("retail_shelf_capture")
    object TargetModePreview : Screen("target_mode_preview_screen")

    object OCRFieldConfig : Screen("ocr_field_filter_config_screen")
    object BarcodeFieldConfig : Screen("barcode_field_filter_config_screen")
    object OCRFilterConfig : Screen("ocr_filter_config_screen")
    object BarcodeFilterConfig : Screen("barcode_filter_config_screen")
    object CharacterTypeFilter : Screen("character_type_filter_screen")
    object CharacterMatchFilter : Screen("character_match_filter_screen")
    object StringLengthFilter : Screen("string_length_filter_screen")
    object RegexFilter : Screen("regex_filter_screen")
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
            updateTitleForScreen(viewModel, Screen.Start, context)
            AIDataCaptureStartScreen(viewModel, navController = navController, innerPadding)
        }
        composable(route = Screen.DemoStart.route) {
            viewModel.updateActiveScreenData(activeScreen = Screen.DemoStart)
            updateTitleForScreen(viewModel, Screen.DemoStart, context)
            DemoStartScreen(
                viewModel,
                navController = navController,
                innerPadding,
                context = context
            )
        }
        composable(route = Screen.DemoSetting.route) {
            viewModel.updateActiveScreenData(activeScreen = Screen.DemoSetting)
            updateTitleForScreen(viewModel, Screen.DemoSetting, context)
            DemoSettingsScreen(viewModel, navController = navController, innerPadding)
        }
        composable(route = Screen.AdvancedOCRSettings.route) {
            viewModel.updateActiveScreenData(activeScreen = Screen.AdvancedOCRSettings)
            updateTitleForScreen(viewModel, Screen.AdvancedOCRSettings, context)
            AdvancedOCRSettingsScreen(
                viewModel,
                navController = navController,
                innerPadding,
                context = context
            )
        }
        composable(route = Screen.Preview.route) {
            viewModel.updateActiveScreenData(activeScreen = Screen.Preview)
            updateTitleForScreen(viewModel, Screen.Preview, context)
            CameraPreviewScreen(
                viewModel,
                navController = navController,
                context,
                activityInnerPadding,
                innerPadding,
                activityLifecycle
            )
        }
        composable(route = Screen.TargetModePreview.route) {
            viewModel.updateActiveScreenData(activeScreen = Screen.TargetModePreview)
            TargetModePreviewScreen(
                viewModel,
                navController = navController,
                context,
                activityInnerPadding,
                activityLifecycle
            )
        }
        composable(route = Screen.ProductsCapture.route) {
            viewModel.updateActiveScreenData(activeScreen = Screen.ProductsCapture)
            updateTitleForScreen(viewModel, Screen.ProductsCapture, context)
            ProductsResultCapturedScreen(
                viewModel,
                navController = navController,
                innerPadding,
                context = context
            )
        }
        composable(route = Screen.OCRBarcodeCapture.route) {
            viewModel.updateActiveScreenData(activeScreen = Screen.OCRBarcodeCapture)
            updateTitleForScreen(viewModel, Screen.OCRBarcodeCapture, context)
            OCRBarcodeResultCapturedScreen(
                viewModel,
                navController = navController,
                innerPadding,
                context = context
            )
        }
        composable(route = Screen.OCRBarcodeResults.route) {
            viewModel.updateActiveScreenData(activeScreen = Screen.OCRBarcodeResults)
            updateTitleForScreen(viewModel, Screen.OCRBarcodeResults, context)
            OCRBarcodeResultScreen(
                viewModel,
                navController = navController,
                innerPadding,
                context = context
            )
        }
        composable(route = Screen.SingleResult.route + "?text={text}&bbox={bbox}&isBarcode={isBarcode}") { backStackEntry ->
            viewModel.updateActiveScreenData(activeScreen = Screen.SingleResult)
            updateTitleForScreen(viewModel, Screen.SingleResult, context)

            val textArg = backStackEntry.arguments?.getString("text")
            val bboxStr = backStackEntry.arguments?.getString("bbox").orEmpty()
            val isBarcode = backStackEntry.arguments?.getString("isBarcode") == "true"

            val bboxParts = bboxStr.split(",")
            val boundingBox = if (bboxParts.size == 4) {
                android.graphics.Rect(
                    bboxParts[0].toIntOrNull() ?: 0,
                    bboxParts[1].toIntOrNull() ?: 0,
                    bboxParts[2].toIntOrNull() ?: 0,
                    bboxParts[3].toIntOrNull() ?: 0
                )
            } else null

            SingleResultScreen(
                viewModel = viewModel,
                navController = navController,
                innerPadding = innerPadding,
                context = context,
                resultRowData = FieldMatchedValue(
                    text = textArg,
                    boundingBox = boundingBox,
                    isBarcode = isBarcode,
                )
            )
        }
        composable(route = Screen.OCRFilterConfig.route) {
            val activeScreen = Screen.OCRFilterConfig
            val filterType = FilterType.OCR_FILTER
            viewModel.updateActiveScreenData(activeScreen)
            viewModel.updateSelectedFilterType(filterType)
            updateTitleForScreen(viewModel, activeScreen, context)
            OCRFindFilterHomeScreen(viewModel, navController, innerPadding)
        }
        composable(route = Screen.BarcodeFilterConfig.route) {
            val activeScreen = Screen.BarcodeFilterConfig
            val filterType = FilterType.BARCODE_FILTER
            viewModel.updateActiveScreenData(activeScreen)
            viewModel.updateSelectedFilterType(filterType)
            updateTitleForScreen(viewModel, activeScreen, context)
            BarcodeFindFilterHomeScreen(viewModel, navController, innerPadding)
        }
        composable(route = Screen.OCRFieldConfig.route) {
            val activeScreen = Screen.OCRFieldConfig
            val filterType = FilterType.OCR_FILTER
            viewModel.updateActiveScreenData(activeScreen)
            viewModel.updateSelectedFilterType(filterType)
            updateTitleForScreen(viewModel, activeScreen, context)
            FilterConfigScreen(innerPadding, viewModel, navController, filterType)
        }
        composable(route = Screen.BarcodeFieldConfig.route) {
            val activeScreen = Screen.BarcodeFieldConfig
            val filterType = FilterType.BARCODE_FILTER
            viewModel.updateActiveScreenData(activeScreen)
            viewModel.updateSelectedFilterType(filterType)
            updateTitleForScreen(viewModel, activeScreen, context)
            FilterConfigScreen(innerPadding, viewModel, navController, filterType)
        }
        composable(route = Screen.RegexFilter.route) {
            viewModel.updateActiveScreenData(activeScreen = Screen.RegexFilter)
            updateTitleForScreen(viewModel, Screen.RegexFilter, context)
            RegexFilterScreen(viewModel, navController = navController, innerPadding)
        }
        composable(route = Screen.CharacterTypeFilter.route) {
            viewModel.updateActiveScreenData(activeScreen = Screen.CharacterTypeFilter)
            updateTitleForScreen(viewModel, Screen.CharacterTypeFilter, context)
            CharacterTypeFilterScreen(viewModel, navController = navController, innerPadding)
        }
        composable(route = Screen.CharacterMatchFilter.route) {
            viewModel.updateActiveScreenData(activeScreen = Screen.CharacterMatchFilter)
            updateTitleForScreen(viewModel, Screen.CharacterMatchFilter, context)
            CharacterMatchFilterScreen(viewModel, navController = navController, innerPadding)
        }
        composable(route = Screen.StringLengthFilter.route) {
            viewModel.updateActiveScreenData(activeScreen = Screen.StringLengthFilter)
            updateTitleForScreen(viewModel, Screen.StringLengthFilter, context)
            StringLengthFilterScreen(viewModel, navController = navController, innerPadding)
        }
        composable(route = Screen.RetailShelfCapture.route) {
            viewModel.updateActiveScreenData(activeScreen = Screen.RetailShelfCapture)
            RetailShelfCaptureScreen(
                viewModel,
                navController = navController,
                context = context
            )
        }
    }
}

private fun updateTitleForScreen(
    viewModel: AIDataCaptureDemoViewModel,
    screen: Screen,
    context: Context
) {
    val titleResId: Int? = when (screen) {
        Screen.Start -> R.string.app_name

        Screen.DemoStart -> when (viewModel.uiState.value.usecaseSelected) {
            UsecaseState.Barcode.value -> R.string.barcode_demo
            UsecaseState.OCR.value -> R.string.ocr_demo
            UsecaseState.Retail.value -> R.string.retail_shelf_demo
            UsecaseState.Product.value -> R.string.product_enrollment_recognition_demo
            UsecaseState.OCRBarcodeFind.value -> R.string.ocr_barcode_find
            else -> R.string.app_name
        }

        Screen.AdvancedOCRSettings -> R.string.advanced_settings
        Screen.DemoSetting -> R.string.settings
        Screen.SingleResult -> R.string.back_to_all_results
        Screen.OCRFieldConfig -> R.string.title_app_bar_ocr_field_config
        Screen.BarcodeFieldConfig -> R.string.title_app_bar_bar_code_field_config
        Screen.RegexFilter -> R.string.title_app_bar_regex_filter
        Screen.CharacterTypeFilter -> R.string.title_app_bar_char_type_filter
        Screen.CharacterMatchFilter -> R.string.title_app_bar_char_match_filter
        Screen.StringLengthFilter -> R.string.title_app_bar_str_length_filter
        Screen.OCRFilterConfig -> R.string.title_app_bar_ocr_filter_config
        Screen.BarcodeFilterConfig -> R.string.title_app_bar_bar_code_filter_config
        Screen.OCRBarcodeResults -> R.string.results

        else -> null
    }

    val title = titleResId?.let(context::getString) ?: ""
    viewModel.updateAppBarTitle(title)
}