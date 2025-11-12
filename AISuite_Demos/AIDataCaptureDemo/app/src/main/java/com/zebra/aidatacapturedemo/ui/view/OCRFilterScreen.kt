package com.zebra.aidatacapturedemo.ui.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.data.OCRFilterData
import com.zebra.aidatacapturedemo.data.OCRFilterType
import com.zebra.aidatacapturedemo.model.TextOCRUtils
import com.zebra.aidatacapturedemo.ui.view.Variables.mainPrimary
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OCRFilterScreen(
    isOCRFilterAlertDialogShown: () -> Unit,
    viewModel: AIDataCaptureDemoViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    var ocrFilterType by remember { mutableStateOf(uiState.selectedOCRFilterData.ocrFilterType) }
    var exactMatchString by remember { mutableStateOf(uiState.selectedOCRFilterData.exactMatchStringList.joinToString()) } // Default separator is a comma
    var startsWithString by remember { mutableStateOf(uiState.selectedOCRFilterData.startsWithString) }
    var containsString by remember { mutableStateOf(uiState.selectedOCRFilterData.containsString) }
    var regexString by remember { mutableStateOf(uiState.selectedOCRFilterData.regexString) }
    var numericCharLengthRange by remember { mutableStateOf(uiState.selectedOCRFilterData.numericCharLengthRange) }
    var alphaCharLengthRange by remember { mutableStateOf(uiState.selectedOCRFilterData.alphaCharLengthRange) }
    var alphaNumericCharLengthRange by remember { mutableStateOf(uiState.selectedOCRFilterData.alphaNumericCharLengthRange) }

    uiState.toastMessage?.let {
        viewModel.toast(it)
        viewModel.updateToastMessage(message = null)
    }

    AlertDialog(
        onDismissRequest = isOCRFilterAlertDialogShown,
        confirmButton = {

            val ocrFilterData = when (ocrFilterType) {
                OCRFilterType.SHOW_ALL -> {
                    OCRFilterData(
                        ocrFilterType = OCRFilterType.SHOW_ALL
                    )
                }

                OCRFilterType.NUMERIC_CHARACTERS_ONLY -> {
                    OCRFilterData(
                        ocrFilterType = OCRFilterType.NUMERIC_CHARACTERS_ONLY,
                        numericCharLengthRange = numericCharLengthRange
                    )
                }

                OCRFilterType.ALPHA_CHARACTERS_ONLY -> {
                    OCRFilterData(
                        ocrFilterType = OCRFilterType.ALPHA_CHARACTERS_ONLY,
                        alphaCharLengthRange = alphaCharLengthRange
                    )
                }

                OCRFilterType.ALPHA_NUMERIC_CHARACTERS_ONLY -> {
                    OCRFilterData(
                        ocrFilterType = OCRFilterType.ALPHA_NUMERIC_CHARACTERS_ONLY,
                        alphaNumericCharLengthRange = alphaNumericCharLengthRange
                    )
                }

                OCRFilterType.EXACT_MATCH -> {
                    val exactMatchStringList = exactMatchString.split(",").map { it.trim() }
                    OCRFilterData(
                        ocrFilterType = OCRFilterType.EXACT_MATCH,
                        exactMatchStringList = exactMatchStringList
                    )
                }

                OCRFilterType.STARTS_WITH -> {
                    OCRFilterData(
                        ocrFilterType = OCRFilterType.STARTS_WITH,
                        startsWithString = startsWithString
                    )
                }

                OCRFilterType.CONTAINS -> {
                    OCRFilterData(
                        ocrFilterType = OCRFilterType.CONTAINS,
                        containsString = containsString
                    )
                }

                OCRFilterType.REGEX -> {
                    OCRFilterData(
                        ocrFilterType = OCRFilterType.REGEX,
                        regexString = regexString
                    )
                }
            }
            viewModel.updateOcrFilterData(ocrFilterData = ocrFilterData)

            Button(
                onClick = {
                    if (ocrFilterType == OCRFilterType.REGEX) { // validate the user input regex string
                        if (TextOCRUtils.validateRegexSyntax(regexString) != null) {
                            isOCRFilterAlertDialogShown.invoke() // valid regex found
                        } else {
                            viewModel.updateToastMessage(message = "Incorrect Regex Syntax")
                        }
                    } else {
                        isOCRFilterAlertDialogShown.invoke()
                    }
                },
                modifier = Modifier
                    .wrapContentHeight()
                    .wrapContentWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Variables.mainPrimary
                ),
                shape = RoundedCornerShape(4.dp)
            )
            {
                Text(
                    text = stringResource(R.string.apply),
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
        },
        dismissButton = {

            Button(
                onClick = isOCRFilterAlertDialogShown,
                modifier = Modifier
                    .wrapContentHeight()
                    .wrapContentWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                )
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = TextStyle(
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                        fontWeight = FontWeight(500),
                        color = Variables.mainPrimary,
                        textAlign = TextAlign.Center,
                    )
                )
            }
        },
        title = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 20.dp, end = 20.dp)
            ) {
                Text(
                    text = "OCR Finder Settings",
                    style = TextStyle(
                        fontSize = 20.sp,
                        lineHeight = 28.sp,
                        fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                        fontWeight = FontWeight(500),
                        color = Variables.mainDefault,
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, top = 12.dp, bottom = 12.dp)
                    .verticalScroll(rememberScrollState()) // Add vertical scrolling
            )
            {
                // SHOW_ALL
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            ocrFilterType = OCRFilterType.SHOW_ALL
                        }
                        .padding(vertical = 5.dp)
                ) {
                    RadioButton(
                        selected = (ocrFilterType == OCRFilterType.SHOW_ALL),
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Variables.mainPrimary,
                            unselectedColor = Variables.mainDefault
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.ocr_showall),
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                            fontWeight = FontWeight(400),
                            color = Variables.mainDefault,
                        )
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))

                // NUMERIC_CHARACTERS_ONLY
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            ocrFilterType = OCRFilterType.NUMERIC_CHARACTERS_ONLY
                        }
                        .padding(vertical = 5.dp)
                ) {
                    RadioButton(
                        selected = (ocrFilterType == OCRFilterType.NUMERIC_CHARACTERS_ONLY),
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Variables.mainPrimary,
                            unselectedColor = Variables.mainDefault
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.ocr_numericcharacters),
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                            fontWeight = FontWeight(400),
                            color = Variables.mainDefault,
                        )
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))

                if (ocrFilterType == OCRFilterType.NUMERIC_CHARACTERS_ONLY) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Filter by Character Length",
                            style = TextStyle(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                                fontWeight = FontWeight(400),
                                color = Variables.mainDefault,
                            )
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = numericCharLengthRange.start.toInt()
                                .toString(), modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        RangeSlider(
                            value = numericCharLengthRange,
                            onValueChange = {
                                numericCharLengthRange = it
                            },
                            valueRange = 2f..15f,
                            modifier = Modifier.weight(8f),
                            colors = SliderDefaults.colors(
                                thumbColor = Variables.mainPrimary,
                                activeTrackColor = Variables.mainPrimary,
                                inactiveTrackColor = Variables.mainPrimary.copy(
                                    alpha = 0.24f
                                )
                            )
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = numericCharLengthRange.endInclusive.toInt()
                                .toString(), modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                // ALPHA_CHARACTERS_ONLY
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            ocrFilterType = OCRFilterType.ALPHA_CHARACTERS_ONLY
                        }
                        .padding(vertical = 5.dp)
                ) {
                    RadioButton(
                        selected = (ocrFilterType == OCRFilterType.ALPHA_CHARACTERS_ONLY),
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Variables.mainPrimary,
                            unselectedColor = Variables.mainDefault
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.ocr_alphacharacters),
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                            fontWeight = FontWeight(400),
                            color = Variables.mainDefault,
                        )
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))

                if (ocrFilterType == OCRFilterType.ALPHA_CHARACTERS_ONLY) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Filter by Character Length",
                            style = TextStyle(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                                fontWeight = FontWeight(400),
                                color = Variables.mainDefault,
                            )
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = alphaCharLengthRange.start.toInt()
                                .toString(), modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        RangeSlider(
                            value = alphaCharLengthRange,
                            onValueChange = {
                                alphaCharLengthRange = it
                            },
                            valueRange = 2f..15f,
                            modifier = Modifier.weight(8f),
                            colors = SliderDefaults.colors(
                                thumbColor = Variables.mainPrimary,
                                activeTrackColor = Variables.mainPrimary,
                                inactiveTrackColor = Variables.mainPrimary.copy(
                                    alpha = 0.24f
                                )
                            )
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = alphaCharLengthRange.endInclusive.toInt()
                                .toString(), modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                // ALPHA_NUMERIC_CHARACTERS_ONLY
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            ocrFilterType = OCRFilterType.ALPHA_NUMERIC_CHARACTERS_ONLY
                        }
                        .padding(vertical = 5.dp)
                ) {
                    RadioButton(
                        selected = (ocrFilterType == OCRFilterType.ALPHA_NUMERIC_CHARACTERS_ONLY),
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Variables.mainPrimary,
                            unselectedColor = Variables.mainDefault
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.ocr_alphanumericcharacters),
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                            fontWeight = FontWeight(400),
                            color = Variables.mainDefault,
                        )
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))

                if (ocrFilterType == OCRFilterType.ALPHA_NUMERIC_CHARACTERS_ONLY) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Filter by Character Length",
                            style = TextStyle(
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                                fontWeight = FontWeight(400),
                                color = Variables.mainDefault,
                            )
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = alphaNumericCharLengthRange.start.toInt()
                                .toString(), modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        RangeSlider(
                            value = alphaNumericCharLengthRange,
                            onValueChange = {
                                alphaNumericCharLengthRange = it
                            },
                            valueRange = 2f..15f,
                            modifier = Modifier.weight(8f),
                            colors = SliderDefaults.colors(
                                thumbColor = Variables.mainPrimary,
                                activeTrackColor = Variables.mainPrimary,
                                inactiveTrackColor = Variables.mainPrimary.copy(
                                    alpha = 0.24f
                                )
                            )
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = alphaNumericCharLengthRange.endInclusive.toInt()
                                .toString(), modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                // EXACT_MATCH
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            ocrFilterType = OCRFilterType.EXACT_MATCH
                        }
                        .padding(vertical = 5.dp)
                ) {
                    RadioButton(
                        selected = (ocrFilterType == OCRFilterType.EXACT_MATCH),
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Variables.mainPrimary,
                            unselectedColor = Variables.mainDefault
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.ocr_exactmatch),
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                            fontWeight = FontWeight(400),
                            color = Variables.mainDefault,
                        )
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (ocrFilterType == OCRFilterType.EXACT_MATCH) {
                    TextField(
                        value = exactMatchString,
                        onValueChange = {
                            exactMatchString = it
                        },
                        label = {
                            Text(
                                text = "Type the keyword here",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    lineHeight = 24.sp,
                                    fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                                    fontWeight = FontWeight(400),
                                    color = Variables.mainDisabled,
                                )
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Variables.mainInverse,
                            unfocusedContainerColor = Variables.mainInverse,
                            cursorColor = Variables.mainPrimary,
                            focusedIndicatorColor = Variables.mainPrimary,
                            unfocusedIndicatorColor = Variables.mainPrimary,
                            selectionColors = TextSelectionColors(
                                handleColor = mainPrimary,
                                backgroundColor = mainPrimary
                            )
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // START_WITH
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            ocrFilterType = OCRFilterType.STARTS_WITH
                        }
                        .padding(vertical = 5.dp)
                ) {
                    RadioButton(
                        selected = (ocrFilterType == OCRFilterType.STARTS_WITH),
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Variables.mainPrimary,
                            unselectedColor = Variables.mainDefault
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.ocr_starts_with),
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                            fontWeight = FontWeight(400),
                            color = Variables.mainDefault,
                        )
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (ocrFilterType == OCRFilterType.STARTS_WITH) {
                    TextField(
                        value = startsWithString,
                        onValueChange = {
                            startsWithString = it
                        },
                        label = {
                            Text(
                                text = "Starts with keyword",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    lineHeight = 24.sp,
                                    fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                                    fontWeight = FontWeight(400),
                                    color = Variables.mainDisabled,
                                )
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Variables.mainInverse,
                            unfocusedContainerColor = Variables.mainInverse,
                            cursorColor = Variables.mainPrimary,
                            focusedIndicatorColor = Variables.mainPrimary,
                            unfocusedIndicatorColor = Variables.mainPrimary,
                            selectionColors = TextSelectionColors(
                                handleColor = mainPrimary,
                                backgroundColor = mainPrimary
                            )
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Contains
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            ocrFilterType = OCRFilterType.CONTAINS
                        }
                        .padding(vertical = 5.dp)
                ) {
                    RadioButton(
                        selected = (ocrFilterType == OCRFilterType.CONTAINS),
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Variables.mainPrimary,
                            unselectedColor = Variables.mainDefault
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.ocr_contains),
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                            fontWeight = FontWeight(400),
                            color = Variables.mainDefault,
                        )
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (ocrFilterType == OCRFilterType.CONTAINS) {
                    TextField(
                        value = containsString,
                        onValueChange = {
                            containsString = it
                        },
                        label = {
                            Text(
                                text = "Contains keyword",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    lineHeight = 24.sp,
                                    fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                                    fontWeight = FontWeight(400),
                                    color = Variables.mainDisabled,
                                )
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Variables.mainInverse,
                            unfocusedContainerColor = Variables.mainInverse,
                            cursorColor = Variables.mainPrimary,
                            focusedIndicatorColor = Variables.mainPrimary,
                            unfocusedIndicatorColor = Variables.mainPrimary,
                            selectionColors = TextSelectionColors(
                                handleColor = mainPrimary,
                                backgroundColor = mainPrimary
                            )
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Regex
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            ocrFilterType = OCRFilterType.REGEX
                        }
                        .padding(vertical = 5.dp)
                ) {
                    RadioButton(
                        selected = (ocrFilterType == OCRFilterType.REGEX),
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = Variables.mainPrimary,
                            unselectedColor = Variables.mainDefault
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.ocr_regex),
                        style = TextStyle(
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                            fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                            fontWeight = FontWeight(400),
                            color = Variables.mainDefault,
                        )
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (ocrFilterType == OCRFilterType.REGEX) {
                    TextField(
                        value = regexString,
                        onValueChange = {
                            regexString = it
                        },
                        label = {
                            Text(
                                text = "Regex keyword",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    lineHeight = 24.sp,
                                    fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                                    fontWeight = FontWeight(400),
                                    color = Variables.mainDisabled,
                                )
                            )
                        },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Variables.mainInverse,
                            unfocusedContainerColor = Variables.mainInverse,
                            cursorColor = Variables.mainPrimary,
                            focusedIndicatorColor = Variables.mainPrimary,
                            unfocusedIndicatorColor = Variables.mainPrimary,
                            selectionColors = TextSelectionColors(
                                handleColor = mainPrimary,
                                backgroundColor = mainPrimary
                            )
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        containerColor = Variables.surfaceDefault
    )
}