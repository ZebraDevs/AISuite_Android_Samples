package com.zebra.aidatacapturedemo.ui.view.filters

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.data.AdvancedFilterOption
import com.zebra.aidatacapturedemo.data.BarcodeFilterData
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
    val uiState = viewModel.uiState.collectAsState().value

    var selectedAdvancedFilterOptionList by remember { mutableStateOf(uiState.barcodeFilterData.selectedAdvancedFilterOptionList) }

    var localSelectedAdvancedFilterOptionListCopy = remember(selectedAdvancedFilterOptionList) {
        mutableStateListOf<AdvancedFilterOption>().apply {
            addAll(selectedAdvancedFilterOptionList)
        }
    }

    BackHandler(enabled = true) {
        viewModel.handleBackButton(navController)
    }

    viewModel.updateAppBarTitle(stringResource(R.string.barcode_filter_title))

    uiState.toastMessage?.let {
        viewModel.toast(it)
        viewModel.updateToastMessage(message = null)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(innerPadding),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 20.dp, top = 12.dp, end = 8.dp, bottom = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Advanced: Character match
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = (localSelectedAdvancedFilterOptionListCopy.contains(
                        AdvancedFilterOption.CHARACTER_MATCH
                    )),
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            // Make sure the list doesn't contain the Preset
                            if (!localSelectedAdvancedFilterOptionListCopy.contains(
                                    AdvancedFilterOption.CHARACTER_MATCH
                                )
                            ) {
                                localSelectedAdvancedFilterOptionListCopy.add(AdvancedFilterOption.CHARACTER_MATCH)
                            }
                        } else {
                            // Make sure the list contains the Preset
                            if (localSelectedAdvancedFilterOptionListCopy.contains(
                                    AdvancedFilterOption.CHARACTER_MATCH
                                )
                            ) {
                                localSelectedAdvancedFilterOptionListCopy.remove(
                                    AdvancedFilterOption.CHARACTER_MATCH
                                )
                            }
                        }
                    },
                    colors = CheckboxDefaults.colors(
                        checkmarkColor = Color.White,
                        checkedColor = Variables.mainPrimary,
                        uncheckedColor = Variables.mainSubtle
                    )
                )
                Spacer(modifier = Modifier.width(width = 4.dp))
                Row(modifier = Modifier.clickable {
                    if (!localSelectedAdvancedFilterOptionListCopy.contains(
                            AdvancedFilterOption.CHARACTER_MATCH
                        )
                    ) {
                        localSelectedAdvancedFilterOptionListCopy.add(AdvancedFilterOption.CHARACTER_MATCH)
                    }

                    updateBarcodeFilterDataChanges(
                        oldBarcodeFilterData = uiState.barcodeFilterData, viewModel = viewModel,
                        modifiedSelectedAdvancedFilterOptionList = localSelectedAdvancedFilterOptionListCopy
                    )
                    navController.navigate(route = Screen.CharacterMatchFilter.route)
                }
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Character match",
                            style = TextStyle(
                                fontSize = 14.sp,
                                lineHeight = Variables.TypefaceLineHeight18,
                                fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                                fontWeight = FontWeight(700),
                                color = Variables.mainDefault,
                            )
                        )
                        Spacer(modifier = Modifier.height(height = 4.dp))
                        Text(
                            text = "Filter to match specific characters, e.g., starts with, contains, or exact match",
                            style = TextStyle(
                                fontSize = Variables.TypefaceFontSize12,
                                lineHeight = Variables.TypefaceLineHeight16,
                                fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                                fontWeight = FontWeight(400),
                                color = Variables.colorsTextBody,
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(width = 8.dp))
                    Image(
                        painter = painterResource(id = R.drawable.ic_right_exapand),
                        contentDescription = "image description",
                        contentScale = ContentScale.None
                    )
                }
            }

            // Advanced: String length
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = (localSelectedAdvancedFilterOptionListCopy.contains(
                        AdvancedFilterOption.STRING_LENGTH
                    )),
                    onCheckedChange = { isChecked ->
                        if (isChecked) {
                            // Make sure the list doesn't contain the Preset
                            if (!localSelectedAdvancedFilterOptionListCopy.contains(
                                    AdvancedFilterOption.STRING_LENGTH
                                )
                            ) {
                                localSelectedAdvancedFilterOptionListCopy.add(AdvancedFilterOption.STRING_LENGTH)
                            }
                        } else {
                            // Make sure the list contains the Preset
                            if (localSelectedAdvancedFilterOptionListCopy.contains(
                                    AdvancedFilterOption.STRING_LENGTH
                                )
                            ) {
                                localSelectedAdvancedFilterOptionListCopy.remove(
                                    AdvancedFilterOption.STRING_LENGTH
                                )
                            }
                        }
                    },
                    colors = CheckboxDefaults.colors(
                        checkmarkColor = Color.White,
                        checkedColor = Variables.mainPrimary,
                        uncheckedColor = Variables.mainSubtle
                    )
                )
                Spacer(modifier = Modifier.width(width = 4.dp))
                Row(modifier = Modifier.clickable {
                    if (!localSelectedAdvancedFilterOptionListCopy.contains(
                            AdvancedFilterOption.STRING_LENGTH
                        )
                    ) {
                        localSelectedAdvancedFilterOptionListCopy.add(AdvancedFilterOption.STRING_LENGTH)
                    }

                    updateBarcodeFilterDataChanges(
                        oldBarcodeFilterData = uiState.barcodeFilterData, viewModel = viewModel,
                        modifiedSelectedAdvancedFilterOptionList = localSelectedAdvancedFilterOptionListCopy
                    )

                    navController.navigate(route = Screen.StringLengthFilter.route)
                }
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "String length",
                            style = TextStyle(
                                fontSize = 14.sp,
                                lineHeight = Variables.TypefaceLineHeight18,
                                fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                                fontWeight = FontWeight(700),
                                color = Variables.mainDefault,
                            )
                        )
                        Spacer(modifier = Modifier.height(height = 4.dp))
                        Text(
                            text = "Filter by min/max limits",
                            style = TextStyle(
                                fontSize = Variables.TypefaceFontSize12,
                                lineHeight = Variables.TypefaceLineHeight16,
                                fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                                fontWeight = FontWeight(400),
                                color = Variables.colorsTextBody,
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(width = 8.dp))
                    Image(
                        painter = painterResource(id = R.drawable.ic_right_exapand),
                        contentDescription = "image description",
                        contentScale = ContentScale.None
                    )
                }
            }
//            }

        }

        // Bottom Action Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            verticalArrangement = Arrangement.spacedBy(36.dp)
        ) {

            // Setting screen shortcut row
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
                                updateBarcodeFilterDataChanges(
                                    oldBarcodeFilterData = uiState.barcodeFilterData,
                                    viewModel = viewModel,
                                    modifiedSelectedAdvancedFilterOptionList = localSelectedAdvancedFilterOptionListCopy
                                )

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
            Row(
                modifier = Modifier
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Spacer(modifier = Modifier.width(16.dp))
                // Cancel Button
                Button(
                    onClick = {
                        viewModel.handleBackButton(navController)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    border = BorderStroke(1.dp, Variables.mainLight),
                ) {
                    Text(
                        text = "Cancel",
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                            fontWeight = FontWeight(500),
                            color = Variables.mainDefault,
                            textAlign = TextAlign.Center,
                        )
                    )
                }

                // Save Button
                Button(
                    onClick = {
                        updateBarcodeFilterDataChanges(
                            oldBarcodeFilterData = uiState.barcodeFilterData, viewModel = viewModel,
                            modifiedSelectedAdvancedFilterOptionList = localSelectedAdvancedFilterOptionListCopy
                        )

                        viewModel.handleBackButton(navController)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Variables.mainPrimary,
                        contentColor = Variables.stateDefaultEnabled
                    ),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(
                        text = "Save",
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                            fontWeight = FontWeight(500),
                            color = Variables.stateDefaultEnabled,
                            textAlign = TextAlign.Center,
                        )
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
            }
        }
    }

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
    if (isPreviewScreenExistsInBackStack) {

        var areModelsReady by remember { mutableStateOf(false) }
        // model re-init required here
        if (uiState.isOCRModelEnabled && uiState.isBarcodeModelEnabled) {
            if (uiState.isOcrModelDemoReady && uiState.isBarcodeModelDemoReady) {
                areModelsReady = true
            }
        } else if (uiState.isOCRModelEnabled && !uiState.isBarcodeModelEnabled) {
            if (uiState.isOcrModelDemoReady) {
                areModelsReady = true
            }
        } else if (uiState.isBarcodeModelEnabled && !uiState.isOCRModelEnabled) {
            if (uiState.isBarcodeModelDemoReady) {
                areModelsReady = true
            }
        }

        if (!areModelsReady) {
            ModalLoadingOverlay(
                onDismissRequest = {
                    viewModel.handleBackButton(navController = navController)
                }
            )
        }
    }
}

private fun updateBarcodeFilterDataChanges(
    oldBarcodeFilterData: BarcodeFilterData,
    viewModel: AIDataCaptureDemoViewModel,
    modifiedSelectedAdvancedFilterOptionList: SnapshotStateList<AdvancedFilterOption>
) {
    oldBarcodeFilterData.selectedAdvancedFilterOptionList = modifiedSelectedAdvancedFilterOptionList
    viewModel.updateBarcodeFilterData(barcodeFilterData = oldBarcodeFilterData)
}
