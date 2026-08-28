// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.screens.scanresultscreen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zebra.ai.barcodebatchinventory.R
import com.zebra.ai.barcodebatchinventory.application.presentation.enums.ButtonType
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.components.ZebraButton
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.components.ZebraText
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.compose.screens.scanresultscreen.components.SimpleScanResultItem
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.theme.AppColors
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.theme.AppDimensions
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.theme.AppTextStyles
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.theme.dividerColor
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.theme.headerBackgroundColor
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.theme.lightGreyHeader
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.theme.mainInverse
import com.zebra.ai.barcodebatchinventory.application.presentation.ui.theme.white
import com.zebra.ai.barcodebatchinventory.application.presentation.viewmodel.ScanResultsViewModel

/**
 * Displays the Batch Inventory screen with a list of scanned barcodes.
 * Redesigned to match the batch inventory mode UI.
 *
 * @param onBackPressed Callback for handling back press
 * @param onResumeScanning Callback for resume scanning button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultsScreen(
    onBackPressed: () -> Unit = {},
    onResumeScanning: () -> Unit = {}
) {
    val scanResultsViewModel: ScanResultsViewModel = viewModel()
    val scanResults by scanResultsViewModel.scanResultsState.collectAsState()
    // Total items scanned = sum of all quantities
    val totalItemsScanned = scanResults.sumOf { it.quantity }
    // Unique items = number of different barcodes
    val uniqueItemCount = scanResults.size

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
                            modifier = Modifier.semantics { contentDescription = "OnScreenBackNavigation" },
                            contentDescription = null,
                            tint = white
                        )
                    }
                },
                // No settings icon - removed as per requirement
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = headerBackgroundColor
                )
            )
        },
        modifier = Modifier.semantics { contentDescription = "ScanResultsScreen" },
        containerColor = white
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (scanResults.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    ZebraText(
                        textValue = stringResource(R.string.scan_result_screen_no_results),
                        textColor = lightGreyHeader,
                        fontSize = AppDimensions.dialogTextFontSizeMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppDimensions.MediumPadding)
                    )
                }
            } else {
                // Header section with item count and CLEAR LIST
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppDimensions.MediumPadding, vertical = AppDimensions.dimension_12dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ZebraText(
                        textValue = stringResource(R.string.scan_result_screen_items_scanned, totalItemsScanned),
                        modifier = Modifier.semantics { contentDescription = "ItemsScanned" },
                        fontSize = 14.sp
                    )

                    // CLEAR LIST text button (red color)
                    ZebraText(
                        textValue = stringResource(R.string.scan_result_screen_clear_results),
                        textColor = Color.Red,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            scanResultsViewModel.clearBarcodeResults()
                        }
                    )
                }

                // INDIVIDUAL ITEMS section header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(mainInverse)
                        .padding(horizontal = AppDimensions.MediumPadding, vertical = AppDimensions.dimension_8dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ZebraText(
                        textValue = stringResource(R.string.scan_result_screen_individual_items),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    ZebraText(
                        textValue = stringResource(R.string.scan_result_screen_items_count, uniqueItemCount),
                        modifier = Modifier.semantics { contentDescription = "IndividualItems" },
                        textColor = lightGreyHeader,
                        fontSize = AppDimensions.dialogTextFontSizeSmall
                    )
                }

                // Scrollable list of results
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = "BarcodeResultsList" },
                    contentPadding = PaddingValues(AppDimensions.zeroPadding)
                ) {
                    items(scanResults.size) { index ->
                        val result = scanResults[index]
                        SimpleScanResultItem(result = result)
                        // Add divider except for last item
                        if (index < scanResults.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = AppDimensions.MediumPadding),
                                thickness = AppDimensions.navBarDividerThickness,
                                color = dividerColor
                            )
                        }
                    }
                }
            }

            // RESUME SCANNING or START SCAN button at bottom
            // Show "START SCAN" when list is empty, "RESUME SCANNING" when list has items
            ZebraButton(
                text = if (scanResults.isEmpty()) {
                    stringResource(R.string.home_screen_content_start_scan_button)
                } else {
                    stringResource(R.string.scan_result_screen_resume_scanning)
                },
                onClick = {
                    onResumeScanning()
                },
                buttonType = ButtonType.Raised,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppDimensions.MediumPadding)
            )
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

