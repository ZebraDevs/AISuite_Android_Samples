package com.zebra.aidatacapturedemo.ui.view.filters

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.data.BarcodeFieldFilterConfig
import com.zebra.aidatacapturedemo.model.FilterUtils
import com.zebra.aidatacapturedemo.ui.view.ModalLoadingOverlay
import com.zebra.aidatacapturedemo.ui.view.Screen
import com.zebra.aidatacapturedemo.ui.view.Variables
import com.zebra.aidatacapturedemo.ui.view.checkIfScreenExistsInStack
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel

@Composable
fun BarcodeFindFilterHomeScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavHostController,
    innerPadding: PaddingValues
) {
    val uiState by viewModel.uiState.collectAsState()

    val fieldId = uiState.selectedFilterFieldId ?: ""
    val fieldTitle = uiState.selectedFilterFieldTitle ?: ""
    val existing = uiState.barcodeFilterData.fieldFilterConfigs
        .firstOrNull { it.fieldId == fieldId }

    InitToast(uiState, viewModel)
    BackHandler(enabled = true) { viewModel.handleBackButton(navController) }
    InitFilterBody(fieldId, fieldTitle, existing, innerPadding, viewModel, navController, uiState)
}

@Composable
private fun InitFilterBody(
    fieldId: String,
    fieldTitle: String,
    existingConfig: BarcodeFieldFilterConfig?,
    innerPadding: PaddingValues,
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavHostController,
    uiState: AIDataCaptureDemoUiState
) {
    val charMatchChecked = existingConfig?.characterMatchEnabled ?: false
    val stringLengthChecked = existingConfig?.stringLengthEnabled ?: false

    var localCharMatchChecked by remember(fieldId) { mutableStateOf(charMatchChecked) }
    var localStringLengthChecked by remember(fieldId) { mutableStateOf(stringLengthChecked) }

    LaunchedEffect(existingConfig) {
        localCharMatchChecked = charMatchChecked
        localStringLengthChecked = stringLengthChecked
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(innerPadding),
    ) {
        // Filter Body
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 20.dp, top = 12.dp, end = 8.dp, bottom = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FilterRow(
                title = "Character Match",
                subTitle = "Filter to match specific characters, e.g., starts with, contains, or exact match",
                checked = localCharMatchChecked,
                onCheckedChange = { localCharMatchChecked = it },
                onRowItemClicked = { navController.navigate(route = Screen.CharacterMatchFilter.route) }
            )
            FilterRow(
                title = "String Length",
                subTitle = "Filter by min/max limits",
                checked = localStringLengthChecked,
                onCheckedChange = { localStringLengthChecked = it },
                onRowItemClicked = { navController.navigate(route = Screen.StringLengthFilter.route) }
            )
        }

        // Bottom Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            verticalArrangement = Arrangement.spacedBy(36.dp)
        ) {
            SettingsToggle(navController = navController)
            FilterBottomBar(
                onCancel = { viewModel.handleBackButton(navController) },
                onSave = {
                    val existing =
                        FilterUtils.getOrCreateBarcodeFieldConfig(uiState, fieldId, fieldTitle)

                    viewModel.saveBarcodeFieldConfig(
                        existing.copy(
                            characterMatchEnabled = localCharMatchChecked,
                            stringLengthEnabled = localStringLengthChecked
                        )
                    )
                    viewModel.handleBackButton(navController)
                }
            )
        }

        // Handling Backstack from Different screens and showing loading screen based on that
        LoadingEffectHandler(navController, viewModel, uiState)
    }
}

@Composable
private fun LoadingEffectHandler(
    navController: NavHostController,
    viewModel: AIDataCaptureDemoViewModel,
    uiState: AIDataCaptureDemoUiState
) {
    var isPreviewScreenExistsInBackStack by remember { mutableStateOf(false) }

    // There are 2 ways a user may navigate to this screen:
    // #1 Route: DemoStartScreen -> Start Scan -> CameraPreviewScreen -> Filter Menu -> Barcode Filter -> BarcodeFindFilterHomeScreen
    // #2 Route: DemoStartScreen -> Filter Menu -> Barcode Filter -> BarcodeFindFilterHomeScreen
    // Now, a user may click Go button to navigate DemoSettingsScreen and come back to the same BarcodeFindFilterHomeScreen.
    // During Route #1, uniquely identify this and check if Loading screen is required for Model init
    LaunchedEffect(key1 = "Make Barcode Symbology view expand") {
        // Check if Screen.Preview exists inside the navigation Controller Stack.
        if (checkIfScreenExistsInStack(navController, Screen.Preview.route)) {
            isPreviewScreenExistsInBackStack = true
        }
    }

    LoadingScreen(
        viewModel,
        navController,
        uiState = uiState,
        isPreviewScreenExistsInBackStack = isPreviewScreenExistsInBackStack
    )
}

@Composable
private fun LoadingScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavHostController,
    uiState: AIDataCaptureDemoUiState,
    isPreviewScreenExistsInBackStack: Boolean
) {
    if (!isPreviewScreenExistsInBackStack) return

    val areModelsReady = when {
        uiState.isOCRModelEnabled && uiState.isBarcodeModelEnabled ->
            uiState.isOcrModelDemoReady && uiState.isBarcodeModelDemoReady

        uiState.isOCRModelEnabled ->
            uiState.isOcrModelDemoReady

        uiState.isBarcodeModelEnabled ->
            uiState.isBarcodeModelDemoReady

        else -> true
    }

    if (!areModelsReady) {
        ModalLoadingOverlay(
            onTimeout = {
                viewModel.toast("Loading timed out. Please try again.")
                viewModel.handleBackButton(navController)
            }
        )
    }
}

@Composable
private fun SettingsToggle(navController: NavHostController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(
                color = Variables.colorsSurfaceSelected,
                shape = RoundedCornerShape(size = Variables.radiusMinimal)
            ),
        horizontalArrangement = Arrangement.spacedBy(10.dp)

    ) {
        Row(
            modifier = Modifier
                .padding(
                    start = Variables.spacingLarge,
                    top = Variables.spacingSmall,
                    end = Variables.spacingLarge,
                    bottom = Variables.spacingSmall
                ),
            horizontalArrangement = Arrangement.spacedBy(
                Variables.spacingNone,
                Alignment.CenterHorizontally
            ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Visit settings to toggle barcode types.",
                style = TextStyle(
                    fontSize = Variables.TypefaceFontSize12,
                    lineHeight = Variables.TypefaceLineHeight16,
                    fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                    fontWeight = FontWeight(400),
                    color = Variables.colorsTextDefault,
                ),
                modifier = Modifier
                    .weight(1f)
            )
            Box(
                modifier = Modifier
                    .border(
                        width = 2.dp,
                        color = Variables.colorsMainLight,
                        shape = RoundedCornerShape(size = 4.dp)
                    )
                    .wrapContentWidth()
                    .wrapContentHeight()
                    .background(
                        color = Variables.colorsMainSubtle,
                        shape = RoundedCornerShape(size = 4.dp)
                    )
                    .padding(
                        start = Variables.spacingSmall,
                        top = Variables.spacingSmall,
                        end = Variables.spacingSmall,
                        bottom = Variables.spacingSmall
                    )
                    .clickable {
                        navController.navigate(route = Screen.DemoSetting.route)
                    }
            ) {
                Text(
                    text = "Go",
                    style = TextStyle(
                        fontSize = Variables.TypefaceFontSize12,
                        lineHeight = Variables.TypefaceLineHeight16,
                        fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                        fontWeight = FontWeight(400),
                        color = Variables.stateDefaultEnabled,
                        textAlign = TextAlign.Center,
                    ),
                    modifier = Modifier.padding(
                        start = Variables.spacingSmall,
                        end = Variables.spacingSmall
                    )
                )
            }
        }
    }
}