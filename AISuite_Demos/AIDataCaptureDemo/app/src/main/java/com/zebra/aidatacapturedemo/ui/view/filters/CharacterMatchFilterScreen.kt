package com.zebra.aidatacapturedemo.ui.view.filters

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
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
import com.zebra.aidatacapturedemo.data.CharacterMatchData
import com.zebra.aidatacapturedemo.data.CharacterMatchFilterOption
import com.zebra.aidatacapturedemo.data.DetectionLevel
import com.zebra.aidatacapturedemo.data.FieldFilterConfig
import com.zebra.aidatacapturedemo.data.FilterType
import com.zebra.aidatacapturedemo.model.FilterUtils
import com.zebra.aidatacapturedemo.ui.view.Variables
import com.zebra.aidatacapturedemo.ui.view.Variables.mainPrimary
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel

@Composable
fun CharacterMatchFilterScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavHostController,
    innerPadding: PaddingValues
) {
    val uiState = viewModel.uiState.collectAsState().value

    val fieldId = uiState.selectedFilterFieldId ?: ""
    val fieldTitle = uiState.selectedFilterFieldTitle ?: ""
    val filterType = uiState.selectedFilterType
    val existing = getConfigByFilterType(filterType, uiState, fieldId)

    val characterMatchData = existing?.characterMatchData
    val charMatchType = characterMatchData?.type ?: CharacterMatchFilterOption.CONTAINS
    val charDetLvl = characterMatchData?.detectionLevel ?: DetectionLevel.WORD
    val charStartText = characterMatchData?.startsWithStringList?.joinToString() ?: ""
    val charContainText = characterMatchData?.containsStringList?.joinToString() ?: ""
    val charExactText = characterMatchData?.exactMatchStringList?.joinToString() ?: ""

    var matchType by remember(fieldId) { mutableStateOf(charMatchType) }
    var detectionLevel by remember(fieldId) { mutableStateOf(charDetLvl) }
    var startsWithText by remember(fieldId) { mutableStateOf(charStartText) }
    var containsText by remember(fieldId) { mutableStateOf(charContainText) }
    var exactText by remember(fieldId) { mutableStateOf(charExactText) }

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

            if (uiState.selectedFilterType == FilterType.OCR_FILTER) {
                // Word vs Line Level Selection Row
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .align(Alignment.CenterHorizontally)
                        .background(color = Variables.mainLight),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clickable {
                                detectionLevel = DetectionLevel.WORD
                            }
                            .then(
                                if (detectionLevel == DetectionLevel.WORD) {
                                    Modifier.background(
                                        color = Variables.surfaceDefault,
                                        shape = RoundedCornerShape(size = Variables.radiusMinimal)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        Text(
                            text = "Word Level",
                            style = TextStyle(
                                fontSize = Variables.TypefaceFontSize14,
                                lineHeight = Variables.TypefaceLineHeight20,
                                fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                                fontWeight = FontWeight(500),
                                color = if (detectionLevel == DetectionLevel.WORD) {
                                    Variables.colorsTextDefault
                                } else {
                                    Variables.mainSubtle
                                },
                                textAlign = TextAlign.Center,
                            ),
                            modifier = Modifier
                                .padding(
                                    start = Variables.spacingLarge,
                                    top = Variables.spacingMinimum,
                                    end = Variables.spacingLarge,
                                    bottom = Variables.spacingMinimum
                                )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .clickable {
                                detectionLevel = DetectionLevel.LINE
                            }
                            .then(
                                if (detectionLevel == DetectionLevel.LINE) {
                                    Modifier.background(
                                        color = Variables.surfaceDefault,
                                        shape = RoundedCornerShape(size = Variables.radiusMinimal)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        Text(
                            text = "Line Level",
                            style = TextStyle(
                                fontSize = Variables.TypefaceFontSize14,
                                lineHeight = Variables.TypefaceLineHeight20,
                                fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                                fontWeight = FontWeight(500),
                                color = if (detectionLevel == DetectionLevel.LINE) {
                                    Variables.colorsTextDefault
                                } else {
                                    Variables.mainSubtle
                                },
                                textAlign = TextAlign.Center,
                            ),
                            modifier = Modifier
                                .padding(
                                    start = Variables.spacingLarge,
                                    top = Variables.spacingMinimum,
                                    end = Variables.spacingLarge,
                                    bottom = Variables.spacingMinimum
                                )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Starts with
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 16.dp)
                    .clickable {
                        matchType = CharacterMatchFilterOption.STARTS_WITH
                    }
            ) {
                RadioButton(
                    selected = (matchType == CharacterMatchFilterOption.STARTS_WITH),
                    onClick = null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = mainPrimary,
                        unselectedColor = Variables.mainDefault
                    )
                )
                Spacer(modifier = Modifier.width(width = 8.dp))
                Column {
                    Text(
                        text = "Starts with",
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
                        text = "Filters results starting with specific text (e.g., \"45\" or \"P\")",
                        style = TextStyle(
                            fontSize = Variables.TypefaceFontSize12,
                            lineHeight = Variables.TypefaceLineHeight16,
                            fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                            fontWeight = FontWeight(400),
                            color = Variables.colorsTextBody,
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(height = 4.dp))
            if (matchType == CharacterMatchFilterOption.STARTS_WITH) {
                InputTextField(
                    stringValue = startsWithText,
                    onStringValueChange = { startsWithText = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Contains
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 16.dp)
                    .clickable {
                        matchType = CharacterMatchFilterOption.CONTAINS
                    }
            ) {
                RadioButton(
                    selected = (matchType == CharacterMatchFilterOption.CONTAINS),
                    onClick = null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = mainPrimary,
                        unselectedColor = Variables.mainDefault
                    )
                )
                Spacer(modifier = Modifier.width(width = 8.dp))
                Column {
                    Text(
                        text = "Contains",
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
                        text = "Filters results containing specific text anywhere in the string (e.g., \"2025\")",
                        style = TextStyle(
                            fontSize = Variables.TypefaceFontSize12,
                            lineHeight = Variables.TypefaceLineHeight16,
                            fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                            fontWeight = FontWeight(400),
                            color = Variables.colorsTextBody,
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(height = 4.dp))
            if (matchType == CharacterMatchFilterOption.CONTAINS) {
                InputTextField(
                    stringValue = containsText,
                    onStringValueChange = { containsText = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Exact match
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 16.dp)
                    .clickable {
                        matchType = CharacterMatchFilterOption.EXACT_MATCH
                    }
            ) {
                RadioButton(
                    selected = (matchType == CharacterMatchFilterOption.EXACT_MATCH),
                    onClick = null,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = mainPrimary,
                        unselectedColor = Variables.mainDefault
                    )
                )
                Spacer(modifier = Modifier.width(width = 8.dp))
                Column {
                    Text(
                        text = "Exact match",
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
                        text = "Only shows results that exactly match your input",
                        style = TextStyle(
                            fontSize = Variables.TypefaceFontSize12,
                            lineHeight = Variables.TypefaceLineHeight16,
                            fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                            fontWeight = FontWeight(400),
                            color = Variables.colorsTextBody,
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(height = 4.dp))
            if (matchType == CharacterMatchFilterOption.EXACT_MATCH) {
                InputTextField(
                    stringValue = exactText,
                    onStringValueChange = { exactText = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        FilterBottomBar(
            onSave = {
                val newData = CharacterMatchData(
                    detectionLevel = detectionLevel,
                    type = matchType,
                    startsWithStringList = startsWithText.split(",").map { it.trim() }
                        .filter { it.isNotBlank() },
                    containsStringList = containsText.split(",").map { it.trim() }
                        .filter { it.isNotBlank() },
                    exactMatchStringList = exactText.split(",").map { it.trim() }
                        .filter { it.isNotBlank() }
                )

                when (filterType) {
                    FilterType.OCR_FILTER -> {
                        val baseConfig =
                            FilterUtils.getOrCreateOcrFieldConfig(uiState, fieldId, fieldTitle)
                        val hasMatchData = newData.startsWithStringList.isNotEmpty() ||
                            newData.containsStringList.isNotEmpty() ||
                            newData.exactMatchStringList.isNotEmpty()
                        viewModel.saveOcrFieldConfig(
                            baseConfig.copy(
                                characterMatchData = newData,
                                regexData = baseConfig.regexData,
                                regexEnabled = baseConfig.regexEnabled,
                                characterTypeOptions = baseConfig.characterTypeOptions,
                                characterTypeEnabled = baseConfig.characterTypeEnabled,
                                stringLengthRange = baseConfig.stringLengthRange,
                                stringLengthEnabled = baseConfig.stringLengthEnabled,
                                characterMatchEnabled = hasMatchData
                            )
                        )
                    }

                    FilterType.BARCODE_FILTER -> {
                        val baseConfig =
                            FilterUtils.getOrCreateBarcodeFieldConfig(uiState, fieldId, fieldTitle)
                        val hasMatchData = newData.startsWithStringList.isNotEmpty() ||
                            newData.containsStringList.isNotEmpty() ||
                            newData.exactMatchStringList.isNotEmpty()
                        viewModel.saveBarcodeFieldConfig(
                            baseConfig.copy(
                                characterMatchData = newData,
                                stringLengthRange = baseConfig.stringLengthRange,
                                stringLengthEnabled = baseConfig.stringLengthEnabled,
                                characterMatchEnabled = hasMatchData
                            )
                        )
                    }

                    FilterType.NONE -> Unit
                }
                viewModel.handleBackButton(navController)
            },
            onCancel = { viewModel.handleBackButton(navController) }
        )
    }
}


@Composable
private fun getConfigByFilterType(
    filterType: FilterType,
    uiState: AIDataCaptureDemoUiState,
    fieldId: String?
): FieldFilterConfig? = when (filterType) {
    FilterType.OCR_FILTER -> uiState.ocrFilterData.fieldFilterConfigs
        .firstOrNull { it.fieldId == fieldId }

    FilterType.BARCODE_FILTER -> uiState.barcodeFilterData.fieldFilterConfigs
        .firstOrNull { it.fieldId == fieldId }

    FilterType.NONE -> null
}

@Composable
private fun InputTextField(
    stringValue: String,
    onStringValueChange: (String) -> Unit,
    showTextFieldHint: Boolean = true
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(color = Variables.colorsSurfaceCool)
    ) {
        Column(
            modifier = Modifier.padding(
                start = 40.dp,
                end = 16.dp,
                top = 14.dp,
                bottom = 14.dp
            )
        ) {
            OutlinedTextField(
                value = stringValue,
                onValueChange = { onStringValueChange(it) },
                colors = OutlinedTextFieldDefaults.colors(
                    selectionColors = TextSelectionColors(
                        handleColor = mainPrimary,
                        backgroundColor = mainPrimary
                    ),
                    cursorColor = mainPrimary,
                    focusedContainerColor = Variables.surfaceDefault,
                    unfocusedContainerColor = Variables.surfaceDefault,
                    focusedBorderColor = mainPrimary,
                    unfocusedBorderColor = Variables.borderDefault,
                ),
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(size = Variables.radiusMinimal),
                trailingIcon = {
                    if (stringValue.isNotEmpty()) {
                        IconButton(onClick = {
                            onStringValueChange("")
                        }) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_close_black),
                                contentDescription = "Clear text",
                                tint = Variables.mainSubtle
                            )
                        }
                    }
                }
            )
            if (showTextFieldHint) {
                Spacer(modifier = Modifier.padding(top = 4.dp))
                Text(
                    text = "Use comma’s for multiple options",
                    style = TextStyle(
                        fontSize = 10.sp,
                        lineHeight = Variables.TypefaceLineHeight24,
                        fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                        fontWeight = FontWeight(400),
                        color = Variables.colorsTextBody,
                    )
                )
            }
        }
    }
}