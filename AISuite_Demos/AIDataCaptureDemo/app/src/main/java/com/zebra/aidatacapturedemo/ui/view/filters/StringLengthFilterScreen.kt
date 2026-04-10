package com.zebra.aidatacapturedemo.ui.view.filters

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.zebra.aidatacapturedemo.data.FilterType
import com.zebra.aidatacapturedemo.ui.view.Variables
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel

@Composable
fun StringLengthFilterScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavHostController,
    innerPadding: PaddingValues
) {
    val uiState = viewModel.uiState.collectAsState().value
    var stringLengthRange by remember {
        mutableStateOf(
            if (uiState.selectedFilterType == FilterType.OCR_FILTER) {
                uiState.ocrFilterData.selectedStringLengthRange
            } else {
                uiState.barcodeFilterData.selectedStringLengthRange
            }
        )
    }

    BackHandler(enabled = true) {
        viewModel.handleBackButton(navController)
    }

    viewModel.updateAppBarTitle(stringResource(R.string.ocr_filter_string_length_title))

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
                .padding(top = 16.dp, bottom = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            InputRangeSliderFieldNew(
                rangeSliderValue = stringLengthRange,
                onRangeSliderValueChange = { stringLengthRange = it }
            )
        }


        // Bottom Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
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
                    viewModel.updateToastMessage("Save was successful.")
                    if (uiState.selectedFilterType == FilterType.OCR_FILTER) {

                        val defaultOcrFilterData = uiState.ocrFilterData
                        defaultOcrFilterData.selectedStringLengthRange = stringLengthRange
                        viewModel.updateOcrFilterData(ocrFilterData = defaultOcrFilterData)
                    } else {
                        val defaultBarcodeFilterData = uiState.barcodeFilterData
                        defaultBarcodeFilterData.selectedStringLengthRange = stringLengthRange
                        viewModel.updateBarcodeFilterData(barcodeFilterData = defaultBarcodeFilterData)
                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputRangeSliderFieldNew(
    rangeSliderValue: ClosedFloatingPointRange<Float>,
    onRangeSliderValueChange: (ClosedFloatingPointRange<Float>) -> Unit
) {
    Column(
        modifier = Modifier.background(color = Variables.colorsSurfaceCool)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = rangeSliderValue.start.toInt()
                    .toString(),
                style = TextStyle(
                    fontSize = Variables.TypefaceFontSize14,
                    lineHeight = Variables.TypefaceLineHeight16,
                    fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                    fontWeight = FontWeight(400),
                    color = Variables.colorsMainSubtle,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.weight(1f))
            RangeSlider(
                value = rangeSliderValue,
                onValueChange = {
                    onRangeSliderValueChange(it)
                },
                valueRange = 2f..15f,
                modifier = Modifier.weight(8f),
                startThumb = {
                    CircularThumb()
                },
                endThumb = {
                    CircularThumb()
                },
//                track = { sliderState ->
//                    // Custom track implementation
//                    Box(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .height(4.dp) // Set your desired track height here
//                            .background(Variables.mainLight) // Background color for the full track
//                    ) {
//                        // Determine the progress and apply the active track color to a sub-Box
////                        val fraction = (sliderState.value.endInclusive - sliderState.value.start) / (sliderState.valueRange.endInclusive - sliderState.valueRange.start)
//                        val start = (sliderState.valueRange.start - sliderState.valueRange.start) / (sliderState.valueRange.endInclusive - sliderState.valueRange.start)
//
//                        Box(
//                            modifier = Modifier
//                                .fillMaxWidth(0.6f) // Fills the fraction of the track that is active
//                                .height(4.dp) // Same height
//                                .background(Variables.mainPrimary) // Color for the active portion
//                            // Need to manually place the active part at the correct start position.
//                            // A more robust solution might involve custom drawing with Canvas or a MeasurePolicy.
//                            // For simplicity here, the thumb still manages its own positioning relative to the *full* Box width.
//                        )
//                    }
//                }

                // Working #1
                track = { state ->
                    // Custom Track: Inactive (gray) + Active (primary)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                    ) {
                        // Inactive part
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Variables.mainSubtle)
                        )

                        // Active part (using RangeSliderState to calculate positioning)
                        SliderDefaults.Track(
                            rangeSliderState = state,
                            modifier = Modifier.height(4.dp),
                            colors = SliderDefaults.colors(activeTrackColor = Variables.mainPrimary)
                        )
                    }
                }
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = rangeSliderValue.endInclusive.toInt()
                    .toString(),
                style = TextStyle(
                    fontSize = Variables.TypefaceFontSize14,
                    lineHeight = Variables.TypefaceLineHeight16,
                    fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                    fontWeight = FontWeight(400),
                    color = Variables.colorsMainSubtle,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.weight(1f))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Filter by character lengths",
                style = TextStyle(
                    fontSize = 10.sp,
                    lineHeight = Variables.TypefaceLineHeight16,
                    fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                    fontWeight = FontWeight(400),
                    color = Variables.colorsMainSubtle,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

@Composable
fun CircularThumb() {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(shape = RoundedCornerShape(50))
            .background(color = Variables.mainPrimary),
    )
}
