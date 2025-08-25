// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.ui.compose.screens.scanresultscreen

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
import androidx.compose.ui.tooling.preview.Preview
import com.zebra.ai.barcodefinder.R
import com.zebra.ai.barcodefinder.common.enums.ButtonType
import com.zebra.ai.barcodefinder.ui.compose.components.ZebraButton
import com.zebra.ai.barcodefinder.ui.compose.components.ZebraText
import com.zebra.ai.barcodefinder.ui.compose.screens.scanresultscreen.components.SimpleScanResultItem
import com.zebra.ai.barcodefinder.ui.theme.AppColors
import com.zebra.ai.barcodefinder.ui.theme.AppDimensions
import com.zebra.ai.barcodefinder.ui.theme.AppTextStyles
import com.zebra.ai.barcodefinder.ui.theme.borderPrimaryMain
import com.zebra.ai.barcodefinder.ui.theme.dividerColor
import com.zebra.ai.barcodefinder.ui.theme.headerBackgroundColor
import com.zebra.ai.barcodefinder.ui.theme.lightGreyHeader
import com.zebra.ai.barcodefinder.ui.theme.white
import com.zebra.ai.barcodefinder.viewmodel.EntityTrackerViewModel

/**
 * Displays the Scan Results screen with a list of scanned barcodes and their statuses.
 * Handles navigation and result display.
 *
 * @param barcodeViewModel The ViewModel for barcode scan results
 * @param onBackPressed Callback for handling back press
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultsScreen(
    barcodeViewModel: EntityTrackerViewModel,
    onBackPressed: () -> Unit = {}
) {
    // Observe EntityTracker UI state
    val uiState by barcodeViewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ZebraText(
                        textValue = stringResource(R.string.scan_results),
                        style = AppTextStyles.TitleTextLight,
                        textColor = AppColors.TextWhite
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_label),
                            tint = white
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = headerBackgroundColor// Dark gray header like in PNG
                )
            )
        },
        containerColor = white // White background
    ) { innerPadding ->
        if (uiState.scanResults.isEmpty()) {
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
                        textValue = stringResource(R.string.no_scan_results),
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
                    items(uiState.scanResults.size) { index ->
                        val result = uiState.scanResults[index]
                        // No ActionableBarcode reference here, just pass null or update SimpleScanResultItem if needed
                        SimpleScanResultItem(result = result, barcode = null)
                        // Add divider except for last item
                        if (index < uiState.scanResults.size) {
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
                        text = stringResource(R.string.clear_scan_results),
                        textColor = borderPrimaryMain,
                        onClick = {
                            barcodeViewModel.clearBarcodeResults()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.clear_all_scan_results),
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
