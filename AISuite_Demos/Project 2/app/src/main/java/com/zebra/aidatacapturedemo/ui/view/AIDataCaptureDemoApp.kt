// Copyright (c) 2024-2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.aidatacapturedemo.ui.view

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.data.OcrRegularFilterOption
import com.zebra.aidatacapturedemo.data.UsecaseState
import com.zebra.aidatacapturedemo.ui.view.Variables.mainDefault
import com.zebra.aidatacapturedemo.ui.view.Variables.mainInverse
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "AIDataCaptureDemoApp"

/**
 * AIDataCaptureDemoApp.kt is the main entry point for the AI Data Capture Demo application.
 * It defines the overall structure of the app's UI using Jetpack Compose.
 * The file includes the main Scaffold, which contains a TopAppBar and a content area that hosts
 * the navigation drawer and different screens based on user interactions.
 * The TopAppBar is customized to show different icons and options depending on the current screen
 * and use case selected by the user. The app also manages state using a ViewModel, allowing for
 * reactive updates to the UI as users interact with it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIDataCaptureDemoAppBar(
    uiState: AIDataCaptureDemoUiState,
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavHostController,
    context: Context,
    scope: CoroutineScope,
    drawerState: DrawerState
) {
    CenterAlignedTopAppBar(
        title = {
            if (uiState.activeScreen == Screen.Preview) {
                ""
            } else {
                AIDataCaptureDemoAppBarTitle(viewModel, uiState)
            }
        },
        navigationIcon = {
            if (uiState.activeScreen == Screen.OCRBarcodeCapture) {
                RectangularSingleChoiceSegmentedButton(
                    uiState.allBarcodeOCRCaptureFilter,
                    onChoiceSelected = { viewModel.updateAllBarcodeOCRCaptureFilter(it) })
            } else {
                if ((uiState.usecaseSelected == UsecaseState.OCRBarcodeFind.value) && (uiState.isCaptureOrLiveEnabled == 0) && (uiState.activeScreen == Screen.Preview)) {
                    // Show different TopBar per Ux requirement for OCRBarcodeFind Usecase Image Capture mode.
                    // Add round close button to actions of the TopAppBar and remove navigation icon by passing empty composable here.
                    EmptyComposable()
                } else {
                    IconButton(onClick = {
                        if (uiState.activeScreen == Screen.Start) {
                            scope.launch {
                                drawerState.open()
                            }
                        } else {
                            viewModel.handleBackButton(navController)
                        }
                    }) {
                        if (uiState.activeScreen == Screen.Start) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.hamburger_icon),
                                contentDescription = stringResource(R.string.home_button)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back_button)
                            )
                        }
                    }
                }
            }
        },
        colors = if (uiState.activeScreen == Screen.Preview) {
            TopAppBarDefaults.mediumTopAppBarColors(
                containerColor = Color.Transparent.copy(alpha = 0.3F),
                titleContentColor = Color.Transparent,
                navigationIconContentColor = mainInverse,
                actionIconContentColor = mainInverse
            )
        } else {
            TopAppBarDefaults.mediumTopAppBarColors(
                containerColor = mainDefault,
                titleContentColor = mainInverse,
                navigationIconContentColor = mainInverse,
                actionIconContentColor = mainInverse
            )
        },
        actions = {

            if (uiState.usecaseSelected == UsecaseState.OCRBarcodeFind.value) {
                var isFilterIconClicked by remember { mutableStateOf(false) }

                val isOcrDefaultFilterSelected =
                    uiState.ocrFilterData.selectedRegularFilterOption == OcrRegularFilterOption.UNFILTERED
                val isBarcodeDefaultFilterSelected =
                    uiState.barcodeFilterData.selectedAdvancedFilterOptionList.isEmpty()
                if (uiState.isCaptureOrLiveEnabled == 0) {
                    // Camera Capture flow

                    if (uiState.activeScreen == Screen.DemoStart) {
                        IconButton(onClick = {
                            isFilterIconClicked = true
                        }) {
                            Icon(
                                ImageVector.vectorResource(
                                    id = if (isOcrDefaultFilterSelected && isBarcodeDefaultFilterSelected) {
                                        R.drawable.ic_filter_default
                                    } else {
                                        R.drawable.ic_filter_selected
                                    }
                                ),
                                contentDescription = "Filter icon description",
                                tint = Color.Unspecified
                            )
                        }
                    }
                    if (uiState.activeScreen == Screen.OCRBarcodeResults) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_trash_can),
                            contentDescription = "image description",
                            contentScale = ContentScale.None,
                            modifier = Modifier.Companion
                                .padding(Variables.spacingMedium)
                                .clickable {
                                    viewModel.clearOcrBarcodeCaptureSession()
                                    navController.navigate(route = Screen.Preview.route) {
                                        popUpTo("preview_screen") {
                                            inclusive = true
                                        }
                                        launchSingleTop = true // Prevents multiple copies of the same destination at the top of the stack
                                    }
                                }
                        )
                    }
                    if ((uiState.activeScreen == Screen.Preview) || (uiState.activeScreen == Screen.OCRBarcodeCapture)) {
                        // Show different TopBar per Ux requirement for OCRBarcodeFind Usecase Image Capture mode.
                        // Add round close button to actions of the TopAppBar
                        RoundCloseButton(onClick = {
                            scope.launch {
                                viewModel.handleBackButton(navController)
                            }
                        })
                    } else {
                        EmptyComposable()
                    }
                } else {
                    // Live Camera flow

                    // filter icon
                    if (uiState.activeScreen == Screen.DemoStart || uiState.activeScreen == Screen.Preview) {
                        IconButton(onClick = {
                            isFilterIconClicked = true
                        }) {
                            Icon(
                                ImageVector.vectorResource(
                                    id = if (isOcrDefaultFilterSelected && isBarcodeDefaultFilterSelected) {
                                        R.drawable.ic_filter_default
                                    } else {
                                        R.drawable.ic_filter_selected
                                    }
                                ),
                                contentDescription = "Filter icon description",
                                tint = Color.Unspecified
                            )
                        }
                    }

                    // mic icon
                    if (uiState.activeScreen == Screen.Preview && uiState.isOCRModelEnabled) {
                        IconButton(onClick = {
                            if (FeedbackUtils.micStatePressed == false) {
                                FeedbackUtils.micStatePressed = true
                                FeedbackUtils.startListening(uiState)
                            }
                        }) {
                            Icon(
                                ImageVector.Companion.vectorResource(R.drawable.mic_icon),
                                contentDescription = "Microphone"
                            )
                        }
                    }

                }

                if (isFilterIconClicked) {
                    FilterOptionsDropdownMenu(
                        isOcrDefaultFilterSelected = isOcrDefaultFilterSelected,
                        isBarcodeDefaultFilterSelected = isBarcodeDefaultFilterSelected,
                        onMenuDismissed = {
                            isFilterIconClicked = false
                        },
                        onOCRFilterOptionSelected = {
                            isFilterIconClicked = false
                            navController.navigate(route = Screen.OCRFindFilterHome.route)
                        },
                        onBarcodeFilterOptionSelected = {
                            isFilterIconClicked = false
                            navController.navigate(route = Screen.BarcodeFindFilterHome.route)
                        })
                }
            }
            if (uiState.activeScreen == Screen.DemoStart) {
                IconButton(onClick = {
                    navController.navigate(route = Screen.DemoSetting.route)
                }) {
                    Icon(
                        ImageVector.Companion.vectorResource(id = R.drawable.settings_icon),
                        contentDescription = "Settings"
                    )
                }
            }
        })
}
//describe the app's UI
/**
 * Main entry point for the application, that
 */
@Composable
fun AIDataCaptureDemoApp(
    viewModel: AIDataCaptureDemoViewModel,
    activityInnerPadding: PaddingValues,
    activityLifecycle: Lifecycle
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    viewModel.restoreDefaultSettings()
    viewModel.clearOcrBarcodeCaptureSession()
    viewModel.updateAppBarTitle(stringResource(id = R.string.app_name))

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var selectedItem by remember { mutableStateOf("Home") }

    Scaffold(
        topBar = {
            if ((uiState.usecaseSelected == UsecaseState.Product.value || uiState.usecaseSelected == UsecaseState.OCR.value) &&
                (uiState.activeScreen == Screen.Preview || uiState.activeScreen == Screen.ProductsCapture)
            ) {
                // Don't show the TopBar here for Product Recognition Use case Preview & Capture Screen
            } else {
                AIDataCaptureDemoAppBar(
                    uiState = uiState,
                    viewModel = viewModel,
                    navController = navController,
                    context = context,
                    scope = scope,
                    drawerState = drawerState
                )
            }
        },
        content = { innerPadding ->
            AIDataCaptureModalNavigationDrawer(
                drawerState = drawerState,
                innerPadding = innerPadding,
                activityInnerPadding = activityInnerPadding,
                selectedItem = selectedItem,
                onSelectedItemValuesChange = { selectedItem = it },
                scope = scope,
                navController = navController,
                viewModel = viewModel,
                context = context,
                activityLifecycle = activityLifecycle
            )
        },
        containerColor = Variables.surfaceDefault
    )
}

@Composable
fun AIDataCaptureDemoAppBarTitle(
    viewModel: AIDataCaptureDemoViewModel,
    uiState: AIDataCaptureDemoUiState
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(color = Variables.mainDefault)
            .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)
    ) {
        Text(
            text = uiState.appBarTitle,
            softWrap = true,
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight(),
            style = TextStyle(
                fontSize = 18.sp,
                lineHeight = 28.sp,
                fontFamily = FontFamily(Font(R.font.ibm_plex_sans_medium)),
                fontWeight = FontWeight(500),
                color = mainInverse,
            )
        )
    }
}

@Composable
fun RectangularSingleChoiceSegmentedButton(
    selectedIndex: Int,
    onChoiceSelected: (Int) -> Unit
) {
    val options = listOf("All", "Barcode", "OCR")
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .width(240.dp)
            .padding(1.dp)
            .background(Variables.backgroundDark)
            .border(
                width = 1.dp,
                color = Variables.colorsMainSubtle,
                shape = RoundedCornerShape(8.dp)
            )
    ) {
        options.forEachIndexed { index, label ->
            SegmentedButton(
                shape = RoundedCornerShape(8.dp),
                colors = SegmentedButtonColors(
                    Variables.blackText,
                    Color.White,
                    Variables.blackText,
                    inactiveContainerColor = Variables.backgroundDark,
                    inactiveContentColor = Variables.colorsMainSubtle,
                    inactiveBorderColor = Variables.backgroundDark,
                    disabledActiveContainerColor = Variables.backgroundDark,
                    disabledActiveContentColor = Variables.colorsMainSubtle,
                    disabledActiveBorderColor = Variables.backgroundDark,
                    disabledInactiveContainerColor = Variables.backgroundDark,
                    disabledInactiveContentColor = Variables.colorsMainSubtle,
                    disabledInactiveBorderColor = Variables.backgroundDark,
                ),
                modifier = Modifier
                    .wrapContentWidth()
                    .padding(1.dp),
                onClick = { onChoiceSelected(index) },
                selected = index == selectedIndex,
                label = { Text(label, color = Color.White) },
                icon = {}
            )
        }
    }
}

@Composable
fun FilterOptionsDropdownMenu(
    onMenuDismissed: () -> Unit,
    onOCRFilterOptionSelected: () -> Unit,
    onBarcodeFilterOptionSelected: () -> Unit,
    isOcrDefaultFilterSelected: Boolean,
    isBarcodeDefaultFilterSelected: Boolean
) {
    Box(
        modifier = Modifier
            .wrapContentSize(Alignment.BottomStart)
            .background(
                color = Variables.surfaceDefault
            )
            .border(
                width = 1.dp,
                color = Variables.borderDefault,
                shape = RoundedCornerShape(size = Variables.radiusRounded)
            )
    ) {
        DropdownMenu(
            expanded = true,
            onDismissRequest = {
                onMenuDismissed()
            },
            offset = DpOffset(x = (-10).dp, y = 10.dp), // Move the Menu 10dp left and 10dp bottom
            modifier = Modifier
                .background(color = Variables.surfaceDefault)
        ) {
            // "OCR Filters" option
            DropdownMenuItem(
                text = {
                    Text(
                        text = "OCR Filters",
                        style = TextStyle(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                            fontWeight = FontWeight(400),
                            color = Variables.colorsTextDefault,
                        )
                    )
                },
                onClick = {
                    onOCRFilterOptionSelected()
                },
                leadingIcon = {
                    if (isOcrDefaultFilterSelected) {
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_menu_ocr),
                            contentDescription = "menu ocr",
                            tint = Variables.blackText
                        )
                    } else {
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_ocr_filter_selected),
                            contentDescription = "menu ocr",
                            tint = Color.Unspecified
                        )
                    }
                }
            )

            // "Barcode Filters" option
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Barcode Filters",
                        style = TextStyle(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                            fontWeight = FontWeight(400),
                            color = Variables.colorsTextDefault,
                        )
                    )
                },
                onClick = {
                    onBarcodeFilterOptionSelected()
                },
                leadingIcon = {
                    if (isBarcodeDefaultFilterSelected) {
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_menu_barcode),
                            contentDescription = "menu barcode",
                            tint = Variables.blackText
                        )
                    } else {
                        Icon(
                            ImageVector.vectorResource(R.drawable.ic_barcode_filter_selected),
                            contentDescription = "menu barcode",
                            tint = Color.Unspecified
                        )
                    }
                }
            )
        }
    }
}
