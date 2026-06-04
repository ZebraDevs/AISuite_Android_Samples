package com.zebra.aidatacapturedemo.ui.view.filters

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import com.zebra.aidatacapturedemo.data.DetectionLevel
import com.zebra.aidatacapturedemo.data.RegexData
import com.zebra.aidatacapturedemo.model.FilterUtils
import com.zebra.aidatacapturedemo.ui.view.Variables
import com.zebra.aidatacapturedemo.ui.view.Variables.mainPrimary
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel

@Composable
fun RegexFilterScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavHostController,
    innerPadding: PaddingValues
) {
    val uiState = viewModel.uiState.collectAsState().value

    var regexDetectionLevel by remember { mutableStateOf(uiState.ocrFilterData.selectedRegexFilterData.detectionLevel) }
    var regexDefaultString by remember { mutableStateOf(uiState.ocrFilterData.selectedRegexFilterData.regexDefaultString) }
    var regexAdditionalStringList by remember { mutableStateOf(uiState.ocrFilterData.selectedRegexFilterData.regexAdditionalStringList) }

    //  Create a Compose-observable state that preserves local changes across recompositions
    val localRegexAdditionalStringListCopy = remember(regexAdditionalStringList) {
        mutableStateListOf<String>().apply {
            addAll(regexAdditionalStringList)
        }
    }

    BackHandler(enabled = true) {
        viewModel.handleBackButton(navController)
    }

    uiState.toastMessage?.let {
        viewModel.toast(it)
        viewModel.updateToastMessage(message = null)
    }

    viewModel.updateAppBarTitle(stringResource(R.string.ocr_filter_regex_title))

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
                            regexDetectionLevel =
                                DetectionLevel.WORD
                        }
                        .then(
                            if (regexDetectionLevel == DetectionLevel.WORD) {
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
                            fontWeight = if (regexDetectionLevel == DetectionLevel.WORD) {
                                FontWeight(700)
                            } else {
                                FontWeight(500)
                            },
                            color = if (regexDetectionLevel == DetectionLevel.WORD) {
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
                            regexDetectionLevel =
                                DetectionLevel.LINE
                        }
                        .then(
                            if (regexDetectionLevel == DetectionLevel.LINE) {
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
                            fontWeight = if (regexDetectionLevel == DetectionLevel.LINE) {
                                FontWeight(700)
                            } else {
                                FontWeight(500)
                            },
                            color = if (regexDetectionLevel == DetectionLevel.LINE) {
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
            Spacer(modifier = Modifier.height(16.dp))

            // Default regex row.
            // Note: This row view will be always visible, cannot be deleted
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                InputTextFieldDefaultRegex(
                    stringValue = regexDefaultString,
                    onStringValueChange = { regexDefaultString = it }
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Additional Regex Row(s)
            localRegexAdditionalStringListCopy.forEachIndexed { index, value ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {

                    InputTextFieldAdditionalRegex(
                        stringValue = value,
                        onStringValueChange = { newValue ->
                            localRegexAdditionalStringListCopy[index] = newValue
                        },
                        onTrashCanButtonClicked = {
                            localRegexAdditionalStringListCopy.remove(value)
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Add more value Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp)
                    .clickable {
                        localRegexAdditionalStringListCopy.add("")
                    }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_plus),
                    contentDescription = "image description",
                    contentScale = ContentScale.None
                )

                Spacer(modifier = Modifier.width(width = Variables.spacingMinimum))

                Text(
                    text = "Add Value",
                    style = TextStyle(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                        fontWeight = FontWeight(500),
                        color = Variables.borderPrimaryMain,
                        textAlign = TextAlign.Center,
                    )
                )
            }

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

                    if (regexDefaultString.isBlank()) {
                        viewModel.updateToastMessage(message = "RegEx field cannot be empty")
                        return@Button
                    }

                    if (FilterUtils.validateRegexSyntax(regexDefaultString) == null) {
                        viewModel.updateToastMessage(message = "Check RegEx for errors.")
                        return@Button
                    }

                    var isAdditionalRegexStringInvalid = false
                    run loop@{
                        localRegexAdditionalStringListCopy.forEachIndexed { index, regexString ->
                            if (regexDefaultString.isBlank() || FilterUtils.validateRegexSyntax(
                                    regexString
                                ) == null
                            ) {
                                viewModel.updateToastMessage(message = "Check RegEx for errors.")
                                isAdditionalRegexStringInvalid = true
                                return@loop
                            }
                        }
                    }

                    if (isAdditionalRegexStringInvalid) {
                        return@Button
                    }

                    viewModel.updateToastMessage("Save was successful.")

                    val defaultOcrFilterData = uiState.ocrFilterData
                    defaultOcrFilterData.selectedRegexFilterData = RegexData(
                        detectionLevel = regexDetectionLevel,
                        regexDefaultString = regexDefaultString,
                        regexAdditionalStringList = localRegexAdditionalStringListCopy.filter { it.isNotBlank() }
                            .toMutableList()
                    )
                    viewModel.updateOcrFilterData(ocrFilterData = defaultOcrFilterData)


                    viewModel.handleBackButton(navController)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = mainPrimary,
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

@Composable
private fun InputTextFieldDefaultRegex(
    stringValue: String,
    onStringValueChange: (String) -> Unit
) {
    var isRegexValid by remember { mutableStateOf(true) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(color = Variables.colorsSurfaceCool)
    ) {
        Column(
            modifier = Modifier.padding(
                start = 20.dp,
                end = 20.dp,
                top = 14.dp,
                bottom = 10.dp
            )
        ) {
            OutlinedTextField(
                value = stringValue,
                onValueChange = {
                    FilterUtils.validateRegexSyntax(it)?.let {
                        isRegexValid = true
                    } ?: run {
                        isRegexValid = false
                    }
                    onStringValueChange(it)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    selectionColors = TextSelectionColors(
                        handleColor = mainPrimary,
                        backgroundColor = mainPrimary
                    ),
                    cursorColor = mainPrimary,
                    focusedContainerColor = Variables.surfaceDefault,
                    unfocusedContainerColor = Variables.surfaceDefault,
                    focusedBorderColor = if (isRegexValid) {
                        mainPrimary
                    } else {
                        Variables.colorsIconNegative
                    },
                    unfocusedBorderColor = if (isRegexValid) {
                        Variables.borderDefault
                    } else {
                        Variables.colorsIconNegative
                    },
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

            Spacer(modifier = Modifier.padding(top = 4.dp))

            if (isRegexValid) {
                Text(
                    text = "Enter value",
                    style = TextStyle(
                        fontSize = Variables.TypefaceFontSize12,
                        lineHeight = Variables.TypefaceLineHeight16,
                        fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                        fontWeight = FontWeight(400),
                        color = Variables.colorsMainSubtle,
                    )
                )
            } else {
                Text(
                    text = "Value not valid",
                    style = TextStyle(
                        fontSize = Variables.TypefaceFontSize12,
                        lineHeight = Variables.TypefaceLineHeight16,
                        fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                        fontWeight = FontWeight(400),
                        color = Variables.colorsMainNegative,
                    )
                )
            }

        }
    }
}

@Composable
private fun InputTextFieldAdditionalRegex(
    stringValue: String,
    onStringValueChange: (String) -> Unit,
    onTrashCanButtonClicked: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(color = Variables.colorsSurfaceCool)
    ) {
        Column(
            modifier = Modifier.padding(
                start = 20.dp,
                end = 8.dp,
                top = 14.dp,
                bottom = 10.dp
            ),
        ) {
            var isRegexValid by remember { mutableStateOf(true) }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = stringValue,
                    onValueChange = {
                        FilterUtils.validateRegexSyntax(it)?.let {
                            isRegexValid = true
                        } ?: run {
                            isRegexValid = false
                        }
                        onStringValueChange(it)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        selectionColors = TextSelectionColors(
                            handleColor = mainPrimary,
                            backgroundColor = mainPrimary
                        ),
                        cursorColor = mainPrimary,
                        focusedContainerColor = Variables.surfaceDefault,
                        unfocusedContainerColor = Variables.surfaceDefault,
                        focusedBorderColor = if (isRegexValid) {
                            mainPrimary
                        } else {
                            Variables.colorsIconNegative
                        },
                        unfocusedBorderColor = if (isRegexValid) {
                            Variables.borderDefault
                        } else {
                            Variables.colorsIconNegative
                        },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
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

                Spacer(modifier = Modifier.width(width = 4.dp))

                Image(
                    painter = painterResource(id = R.drawable.ic_trash_can),
                    colorFilter = ColorFilter.tint(color =Variables.mainSubtle),
                    contentDescription = "image description",
                    contentScale = ContentScale.None,
                    modifier = Modifier.Companion
                        .padding(Variables.spacingMedium)
                        .clickable {
                            onTrashCanButtonClicked(true)
                        }
                )
            }
            Spacer(modifier = Modifier.height(height = 4.dp))

            if (isRegexValid) {
                Text(
                    text = "Enter value",
                    style = TextStyle(
                        fontSize = Variables.TypefaceFontSize12,
                        lineHeight = Variables.TypefaceLineHeight16,
                        fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                        fontWeight = FontWeight(400),
                        color = Variables.colorsMainSubtle,
                    )
                )
            } else {
                Text(
                    text = "Value not valid",
                    style = TextStyle(
                        fontSize = Variables.TypefaceFontSize12,
                        lineHeight = Variables.TypefaceLineHeight16,
                        fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                        fontWeight = FontWeight(400),
                        color = Variables.colorsMainNegative,
                    )
                )
            }

        }
    }
}

@Composable
fun CustomTickToast(message: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.DarkGray)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Tick Mark",
            tint = Color.Green,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message,
            color = Color.White,
            fontSize = 16.sp
        )
    }
}