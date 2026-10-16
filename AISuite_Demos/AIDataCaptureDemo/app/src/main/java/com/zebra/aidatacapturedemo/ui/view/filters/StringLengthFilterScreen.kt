package com.zebra.aidatacapturedemo.ui.view.filters

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.data.DefaultValues
import com.zebra.aidatacapturedemo.data.FilterType
import com.zebra.aidatacapturedemo.model.FilterUtils
import com.zebra.aidatacapturedemo.ui.view.Variables
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel

private val defStringLengthRange =
    DefaultValues.STRING_LENGTH_RANGE_MIN_VALUE..DefaultValues.STRING_LENGTH_RANGE_MAX_VALUE

@Composable
fun StringLengthFilterScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavHostController,
    innerPadding: PaddingValues
) {
    val uiState = viewModel.uiState.collectAsState().value

    val fieldId = uiState.selectedFilterFieldId ?: ""
    val fieldTitle = uiState.selectedFilterFieldTitle ?: ""
    val filterType = uiState.selectedFilterType

    val existingRange: ClosedFloatingPointRange<Float> = when (filterType) {
        FilterType.OCR_FILTER -> {
            val cfg = uiState.ocrFilterData.fieldFilterConfigs
                .firstOrNull { it.fieldId == fieldId }
            cfg?.stringLengthRange ?: defStringLengthRange
        }

        FilterType.BARCODE_FILTER -> {
            val cfg = uiState.barcodeFilterData.fieldFilterConfigs
                .firstOrNull { it.fieldId == fieldId }
            cfg?.stringLengthRange ?: defStringLengthRange
        }

        FilterType.NONE -> defStringLengthRange
    }

    var stringLengthRange by remember(fieldId, filterType) {
        mutableStateOf(existingRange)
    }

    InitToast(uiState, viewModel)
    BackHandler(enabled = true) { viewModel.handleBackButton(navController) }

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

        FilterBottomBar(
            onSave = {
                val minVal = stringLengthRange.start.toInt()
                val maxVal = stringLengthRange.endInclusive.toInt()

                if (minVal > maxVal) {
                    viewModel.updateToastMessage("Min length cannot be greater than max length.")
                    return@FilterBottomBar
                }

                when (filterType) {
                    FilterType.OCR_FILTER -> {
                        val baseConfig =
                            FilterUtils.getOrCreateOcrFieldConfig(uiState, fieldId, fieldTitle)

                        val updated = baseConfig.copy(
                            stringLengthRange = minVal.toFloat()..maxVal.toFloat(),
                            stringLengthEnabled = baseConfig.stringLengthEnabled,
                            regexData = baseConfig.regexData,
                            regexEnabled = baseConfig.regexEnabled,
                            characterTypeOptions = baseConfig.characterTypeOptions,
                            characterTypeEnabled = baseConfig.characterTypeEnabled,
                            characterMatchData = baseConfig.characterMatchData,
                            characterMatchEnabled = baseConfig.characterMatchEnabled
                        )
                        viewModel.saveOcrFieldConfig(updated)
                    }

                    FilterType.BARCODE_FILTER -> {
                        val baseConfig =
                            FilterUtils.getOrCreateBarcodeFieldConfig(uiState, fieldId, fieldTitle)

                        val updated = baseConfig.copy(
                            stringLengthRange = minVal.toFloat()..maxVal.toFloat(),
                            stringLengthEnabled = baseConfig.stringLengthEnabled,
                            characterMatchData = baseConfig.characterMatchData,
                            characterMatchEnabled = baseConfig.characterMatchEnabled
                        )
                        viewModel.saveBarcodeFieldConfig(updated)
                    }

                    FilterType.NONE -> Unit
                }
                viewModel.handleBackButton(navController)
            },
            onCancel = { viewModel.handleBackButton(navController) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputRangeSliderFieldNew(
    rangeSliderValue: ClosedFloatingPointRange<Float>,
    onRangeSliderValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    allowedRange: ClosedFloatingPointRange<Float> = defStringLengthRange,
    modifier: Modifier = Modifier
) {
    val startLabel = rangeSliderValue.start.toInt().toString()
    val endLabel = rangeSliderValue.endInclusive.toInt().toString()

    Column(
        modifier = modifier.background(color = Variables.colorsSurfaceCool)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp)
        ) {
            Text(
                text = startLabel,
                style = TextStyle(
                    fontSize = Variables.TypefaceFontSize14,
                    lineHeight = Variables.TypefaceLineHeight16,
                    fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                    fontWeight = FontWeight(400),
                    color = Variables.colorsMainSubtle,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.width(28.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            RangeSlider(
                value = rangeSliderValue,
                onValueChange = onRangeSliderValueChange,
                valueRange = allowedRange,
                modifier = Modifier.weight(1f),
                startThumb = { CircularThumb() },
                endThumb = { CircularThumb() },
                track = { state ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Variables.mainSubtle)
                        )

                        SliderDefaults.Track(
                            rangeSliderState = state,
                            modifier = Modifier.height(4.dp),
                            colors = SliderDefaults.colors(
                                activeTrackColor = Variables.mainPrimary,
                                inactiveTrackColor = Color.Transparent
                            )
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = endLabel,
                style = TextStyle(
                    fontSize = Variables.TypefaceFontSize14,
                    lineHeight = Variables.TypefaceLineHeight16,
                    fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                    fontWeight = FontWeight(400),
                    color = Variables.colorsMainSubtle,
                    textAlign = TextAlign.Center,
                ),
                modifier = Modifier.width(28.dp)
            )
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
