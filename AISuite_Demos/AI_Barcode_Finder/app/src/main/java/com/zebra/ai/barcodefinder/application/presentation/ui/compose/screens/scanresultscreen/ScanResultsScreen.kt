// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.application.presentation.ui.compose.screens.scanresultscreen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zebra.ai.barcodefinder.R
import com.zebra.ai.barcodefinder.application.presentation.enums.ButtonType
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.components.ZebraButton
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.components.ZebraText
import com.zebra.ai.barcodefinder.application.presentation.ui.compose.screens.scanresultscreen.components.SimpleScanResultItem
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.AppColors
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.AppTextStyles
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.borderPrimaryMain
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.dividerColor
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.headerBackgroundColor
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.lightGreyHeader
import com.zebra.ai.barcodefinder.application.presentation.ui.theme.white
import com.zebra.ai.barcodefinder.application.presentation.viewmodel.ScanResultsViewModel

/**
 * Displays the Scan Results screen with a list of scanned barcodes and their statuses.
 * Handles navigation and result display.
 *
 * @param finderViewModel The ViewModel for barcode scan results
 * @param onBackPressed Callback for handling back press
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultsScreen(
    onBackPressed: () -> Unit = {}
) {
    // Observe EntityTracker UI state
    val scanResultsViewModel: ScanResultsViewModel = viewModel()
    val scanResults by scanResultsViewModel.scanResultsState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ZebraText(
                        textValue = stringResource(R.string.scan_result_screen_scan_results),
                        style = AppTextStyles.TitleTextLight,
                        textColor = AppColors.TextWhite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            modifier = Modifier.semantics{contentDescription = "OnScreenBackNavigation"},
                            contentDescription = null,
                            tint = white
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = headerBackgroundColor// Dark gray header like in PNG
                )
            )
        },
        modifier = Modifier.semantics{contentDescription = "ScanResultsScreen"},
        containerColor = white // White background
    ) { innerPadding ->
        if (scanResults.isEmpty()) {
            // Simple empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(AppDimensions.modifier16)
                ) {
                    ZebraText(
                        textValue = stringResource(R.string.scan_result_screen_no_results),
                        textColor = lightGreyHeader,
                        fontSize = AppDimensions.dialogTextFontSizeMedium
                    )
                }
            }
        } else {
            // Results list matching PNG design
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Scrollable list of results
                LazyColumn(
                    modifier = Modifier.weight(AppDimensions.WeightFull),
                    contentPadding = PaddingValues(AppDimensions.zeroPadding)
                ) {
                    items(scanResults.size) { index ->
                        val result = scanResults[index]
                        // No ActionableBarcode reference here, just pass null or update SimpleScanResultItem if needed
                        SimpleScanResultItem(result = result, barcode = null)
                        // Add divider except for last item
                        if (index < scanResults.size) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = AppDimensions.MediumPadding),
                                thickness = AppDimensions.navBarDividerThickness,
                                color = dividerColor
                            )
                        }
                    }
                }


                // Clear Results button at bottom
                Column {
                    // replacement for  Clear Results button at bottom in ZebraButton
                    ZebraButton(
                        text = stringResource(R.string.scan_result_screen_clear_results),
                        textColor = borderPrimaryMain,
                        onClick = {
                            scanResultsViewModel.clearBarcodeResults()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = null,
                                tint = borderPrimaryMain,
                                modifier = Modifier.size(AppDimensions.modifier20)
                            )
                        },
                        buttonType = ButtonType.Text,
                        modifier = Modifier.padding(
                            start = AppDimensions.dimension_24dp,
                            bottom = AppDimensions.dimension_12dp
                        ),
                    )
                }
            }
        }
    }

    BackHandler {
        onBackPressed()
    }

}


@Preview(showBackground = true, name = "Scan Results Screen - Empty")
@Composable
fun ScanResultsScreenEmptyPreview() {
    // Preview would need a mock ViewModel, but this shows the structure
}
