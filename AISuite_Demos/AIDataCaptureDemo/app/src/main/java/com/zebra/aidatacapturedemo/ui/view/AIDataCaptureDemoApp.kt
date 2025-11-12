// Copyright (c) 2024-2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.aidatacapturedemo.ui.view

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.data.UsecaseState
import com.zebra.aidatacapturedemo.ui.view.Variables.mainDefault
import com.zebra.aidatacapturedemo.ui.view.Variables.mainInverse
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "AIDataCaptureDemoApp"

/**
 * AppBar for the application with title, backbutton and menu
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
            IconButton(onClick = {
                if (uiState.activeScreen == Screen.Start) {
                    scope.launch {
                        drawerState.open()
                    }
                } else {
                    // Back Handler
                    viewModel.handleBackButton(navController)
                }
            }) {
                if (uiState.activeScreen == Screen.Start) {
                    Icon(
                        imageVector = ImageVector.Companion.vectorResource(R.drawable.hamburger_icon),
                        contentDescription = stringResource(R.string.home_button)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_button)
                    )
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
            if (uiState.usecaseSelected == UsecaseState.OCRFind.value) {
                var isOCRFilterAlertDialogShown by remember { mutableStateOf(false) }

                IconButton(onClick = {
                    isOCRFilterAlertDialogShown = true
                }) {
                    Icon(
                        ImageVector.Companion.vectorResource(R.drawable.filter_icon),
                        contentDescription = "Filter"
                    )
                }

                if (uiState.activeScreen == Screen.Preview) {
                    IconButton(onClick = {
                        if (FeedbackUtils.micStatePressed == false) {
                            FeedbackUtils.micStatePressed = true
                            FeedbackUtils.startListening()
                        }
                    }) {
                        Icon(
                            ImageVector.Companion.vectorResource(R.drawable.mic_icon),
                            contentDescription = "Microphone"
                        )
                    }
                }

                if (isOCRFilterAlertDialogShown) {
                    OCRFilterScreen(
                        isOCRFilterAlertDialogShown = { isOCRFilterAlertDialogShown = false },
                        viewModel = viewModel
                    )
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
    viewModel.updateAppBarTitle(stringResource(id = R.string.app_name))

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var selectedItem by remember { mutableStateOf("Home") }

    Scaffold(
        topBar = {
            if ((uiState.usecaseSelected == UsecaseState.Product.value ||
                        uiState.usecaseSelected == UsecaseState.OCR.value) &&
                (uiState.activeScreen == Screen.Preview || uiState.activeScreen == Screen.Capture)
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