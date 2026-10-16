// Copyright (c) 2024-2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.aidatacapturedemo.ui.view

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.zebra.aidatacapturedemo.data.FilterMenuAction
import com.zebra.aidatacapturedemo.data.UsecaseState
import com.zebra.aidatacapturedemo.ui.view.Variables.mainDefault
import com.zebra.aidatacapturedemo.ui.view.Variables.mainInverse
import com.zebra.aidatacapturedemo.ui.view.filters.FieldActionsOverflowMenu
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
    scope: CoroutineScope,
    drawerState: DrawerState
) {
    // ----- TopAppBar: navigation icon visibility rules -----

    // Show segmented "All / Barcode / OCR" control only on capture screen.
    val isCaptureSegmentedControl = uiState.activeScreen == Screen.OCRBarcodeCapture
    // In OCR+Barcode Find capture-preview flow, UX requires no nav icon.
    val shouldHideNavIconForCapturePreview =
        (uiState.usecaseSelected == UsecaseState.OCRBarcodeFind.value) &&
                (uiState.isCaptureOrLiveEnabled == 0) &&
                (uiState.activeScreen == Screen.Preview || uiState.activeScreen == Screen.TargetModePreview)
    // Start screen uses hamburger (open drawer); others use back arrow.
    val isStartScreen = uiState.activeScreen == Screen.Start


    // ----- TopAppBar: actions visibility rules -----

    // Restrict advanced action icons to OCR+Barcode Find use case.
    val isOcrBarcodeFind = uiState.usecaseSelected == UsecaseState.OCRBarcodeFind.value
    // Capture mode (0) and Live mode (1) have different action sets.
    val isCaptureMode = uiState.isCaptureOrLiveEnabled == 0
    // Screen flags used to keep action logic readable.
    val isDemoStart = uiState.activeScreen == Screen.DemoStart
    val isPreview = uiState.activeScreen == Screen.Preview
    val isCaptureScreen = uiState.activeScreen == Screen.OCRBarcodeCapture
    val isResults = uiState.activeScreen == Screen.OCRBarcodeResults
    // Overflow menu is shown only on field configuration screens.
    val isFieldConfigScreen =
        uiState.activeScreen == Screen.OCRFieldConfig ||
                uiState.activeScreen == Screen.BarcodeFieldConfig


    // ----- Filter dropdown state + indicator rules -----

    // Controls filter dropdown open/close state.
    var isFilterIconClicked by remember { mutableStateOf(false) }
    // Empty selectedFieldIds means "default/no custom field selection" is active.
    val isOcrDefaultFilterSelected = uiState.ocrFilterData.selectedFieldIds.isNotEmpty()
    val isBarcodeDefaultFilterSelected = uiState.barcodeFilterData.selectedFieldIds.isNotEmpty()
    // True if any one of the filters are configured and checked
    val isFilterConfigured = isOcrDefaultFilterSelected || isBarcodeDefaultFilterSelected
    // Filter icon appears on DemoStart in capture mode; on DemoStart/Preview in live mode.
    val shouldShowFilterIcon =
        if (isCaptureMode) isDemoStart else (isDemoStart || isPreview)

    CenterAlignedTopAppBar(
        title = {
            if (uiState.activeScreen == Screen.Preview ||  uiState.activeScreen == Screen.TargetModePreview) ""
            else AIDataCaptureDemoAppBarTitle(uiState)
        },
        navigationIcon = {
            when {
                // UX requirement: hide navigation icon on OCR+Barcode Find capture-preview screen.
                shouldHideNavIconForCapturePreview -> EmptyComposable()

                // On capture screen, navigation area is replaced with All/Barcode/OCR segmented control.
                isCaptureSegmentedControl -> RectangularSingleChoiceSegmentedButton(
                    selectedIndex = uiState.allBarcodeOCRCaptureFilter,
                    onChoiceSelected = viewModel::updateAllBarcodeOCRCaptureFilter
                )

                // Default navigation behavior: hamburger on Start, back arrow elsewhere.
                else -> IconButton(
                    onClick = {
                        if (isStartScreen) scope.launch { drawerState.open() }
                        else viewModel.handleBackButton(navController)
                    }
                ) {
                    if (isStartScreen) Icon(
                        imageVector = ImageVector.vectorResource(R.drawable.hamburger_icon),
                        contentDescription = stringResource(R.string.home_button)
                    )
                    else Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_button)
                    )
                }
            }
        },
        colors = colorTopAppBar(uiState),
        actions = {
            if (isOcrBarcodeFind) {
                // Show filter entry point where this flow allows filter changes.
                if (shouldShowFilterIcon) NotificationIcon(
                    onIconClicked = { isFilterIconClicked = true },
                    isIndicatorShown = isFilterConfigured,
                    id = R.drawable.filter_icon,
                )

                // Results screen action: clear capture session and return to preview.
                if (isCaptureMode && isResults) Image(
                    painter = painterResource(id = R.drawable.ic_trash_can),
                    contentDescription = "image description",
                    contentScale = ContentScale.None,
                    modifier = Modifier
                        .padding(Variables.spacingMedium)
                        .clickable {
                            viewModel.clearOcrBarcodeCaptureSession()
                            navController.navigate(route = Screen.Preview.route) {
                                popUpTo("preview_screen") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                )

                // Capture flow uses explicit close action on preview/capture screens.
                if (isCaptureMode && (isPreview || isCaptureScreen)) {
                    RoundCloseButton(onClick = {
                        scope.launch { viewModel.handleBackButton(navController) }
                    })
                }

                // Live preview only: enable voice input trigger when OCR is enabled.
                if (!isCaptureMode && isPreview && uiState.isOCRModelEnabled) {
                    IconButton(onClick = {
                        if (!FeedbackUtils.micStatePressed) {
                            FeedbackUtils.micStatePressed = true
                            FeedbackUtils.startListening()
                        }
                    }) {
                        Icon(
                            imageVector = ImageVector.vectorResource(R.drawable.mic_icon),
                            contentDescription = "Microphone"
                        )
                    }
                }

                // Filter chooser menu for navigating to OCR/Barcode field configuration.
                if (isFilterIconClicked) FilterOptionsDropdownMenu(
                    isOcrDefaultFilterSelected = isOcrDefaultFilterSelected,
                    isBarcodeDefaultFilterSelected = isBarcodeDefaultFilterSelected,
                    onMenuDismissed = { isFilterIconClicked = false },
                    onOCRFilterOptionSelected = {
                        isFilterIconClicked = false
                        navController.navigate(route = Screen.OCRFieldConfig.route)
                    },
                    onBarcodeFilterOptionSelected = {
                        isFilterIconClicked = false
                        navController.navigate(route = Screen.BarcodeFieldConfig.route)
                    }
                )
            }

            // Global settings entry on demo start.
            if (isDemoStart) IconButton(onClick = {
                navController.navigate(route = Screen.DemoSetting.route)
            }) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.settings_icon),
                    contentDescription = "Settings"
                )
            }

            // Field configuration overflow menu (select/delete/reset actions).
            if (isFieldConfigScreen) FieldActionsOverflowMenu(
                onDeleteField = { viewModel.postFilterMenuAction(FilterMenuAction.DELETE) },
                onSelectAll = { viewModel.postFilterMenuAction(FilterMenuAction.SELECT_ALL) },
                onDeselectAll = { viewModel.postFilterMenuAction(FilterMenuAction.DESELECT_ALL) },
                onReset = { viewModel.postFilterMenuAction(FilterMenuAction.RESET) }
            )
        })
}

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

    LaunchedEffect(Unit) {
        viewModel.restoreDefaultSettings()
        viewModel.clearOcrBarcodeCaptureSession()
    }

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var selectedItem by remember { mutableStateOf("Home") }

    LaunchedEffect(uiState.activeScreen) {
        if (uiState.activeScreen != Screen.Start) drawerState.close()
    }

    val isTopBarShown = (uiState.usecaseSelected in listOf(
        UsecaseState.Product.value, UsecaseState.OCR.value
    )) && (uiState.activeScreen in listOf(
        Screen.Preview, Screen.ProductsCapture, uiState.activeScreen == Screen.TargetModePreview
    ))

    Scaffold(
        topBar = {
            if (!isTopBarShown) AIDataCaptureDemoAppBar(
                uiState = uiState,
                viewModel = viewModel,
                navController = navController,
                scope = scope,
                drawerState = drawerState
            )
        },
        content = { innerPadding ->
            AIDataCaptureModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = uiState.activeScreen == Screen.Start,
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
fun AIDataCaptureDemoAppBarTitle(uiState: AIDataCaptureDemoUiState) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(color = mainDefault)
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
            onDismissRequest = { onMenuDismissed() },
            offset = DpOffset(x = (-10).dp, y = 10.dp), // Move the Menu 10dp left and 10dp bottom
            modifier = Modifier
                .background(color = Variables.surfaceDefault)
        ) {
            // "OCR Filters" option
            MenuDropDown(
                onDropDownMenuClicked = { onOCRFilterOptionSelected() },
                dropDownText = "OCR Filters",
                isIndicatorShown = isOcrDefaultFilterSelected,
                id = R.drawable.ic_menu_ocr,
            )

            // "Barcode Filters" option
            MenuDropDown(
                onDropDownMenuClicked = { onBarcodeFilterOptionSelected() },
                dropDownText = "Barcode Filters",
                isIndicatorShown = isBarcodeDefaultFilterSelected,
                id = R.drawable.ic_menu_barcode,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun colorTopAppBar(uiState: AIDataCaptureDemoUiState): TopAppBarColors {
    return if (uiState.activeScreen == Screen.Preview || uiState.activeScreen == Screen.TargetModePreview) {
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
    }
}

@Composable
private fun MenuDropDown(
    onDropDownMenuClicked: () -> Unit,
    dropDownText: String,
    isIndicatorShown: Boolean,
    @DrawableRes id: Int
) {
    DropdownMenuItem(
        text = {
            Text(
                text = dropDownText,
                style = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                    fontWeight = FontWeight(400),
                    color = Variables.colorsTextDefault,
                )
            )
        },
        onClick = onDropDownMenuClicked,
        leadingIcon = {
            NotificationIcon(
                onIconClicked = {},
                isIndicatorShown = isIndicatorShown,
                id = id,
                tint = Variables.blackText
            )
        }
    )
}

@Composable
private fun NotificationIcon(
    onIconClicked: () -> Unit,
    isIndicatorShown: Boolean,
    @DrawableRes id: Int,
    tint: Color = LocalContentColor.current
) = Box(
    modifier = Modifier.clickable { onIconClicked() }) {
    Icon(
        imageVector = ImageVector.vectorResource(id),
        contentDescription = "Configure field",
        modifier = Modifier.size(24.dp),
        tint = tint
    )
    if (isIndicatorShown) Image(
        imageVector = ImageVector.vectorResource(id = R.drawable.red_dot),
        contentDescription = "Active indicator",
        modifier = Modifier
            .size(10.dp)
            .align(Alignment.TopEnd)
    )
}