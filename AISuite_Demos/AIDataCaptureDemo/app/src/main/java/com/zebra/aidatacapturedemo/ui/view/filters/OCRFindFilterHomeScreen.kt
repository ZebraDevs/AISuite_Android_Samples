package com.zebra.aidatacapturedemo.ui.view.filters

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
import com.zebra.aidatacapturedemo.data.AdvancedFilterOption
import com.zebra.aidatacapturedemo.data.OcrFilterData
import com.zebra.aidatacapturedemo.data.OcrRegularFilterOption
import com.zebra.aidatacapturedemo.ui.view.Screen
import com.zebra.aidatacapturedemo.ui.view.Variables
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel

@Composable
fun OCRFindFilterHomeScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavHostController,
    innerPadding: PaddingValues
) {
    val uiState = viewModel.uiState.collectAsState().value

    //START FROM HERE on Local var copy
    var selectedRegularFilterOption by remember { mutableStateOf(uiState.ocrFilterData.selectedRegularFilterOption) }
    var selectedAdvancedFilterOptionList by remember { mutableStateOf(uiState.ocrFilterData.selectedAdvancedFilterOptionList) }

    val localSelectedAdvancedFilterOptionListCopy = remember(selectedAdvancedFilterOptionList) {
        mutableStateListOf<AdvancedFilterOption>().apply {
            addAll(selectedAdvancedFilterOptionList)
        }
    }

    BackHandler(enabled = true) {
        viewModel.handleBackButton(navController)
    }

    viewModel.updateAppBarTitle(stringResource(R.string.ocr_filter_title))

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
                .padding(start = 20.dp, top = 12.dp, end = 8.dp, bottom = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Show All
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable {
                    selectedRegularFilterOption = OcrRegularFilterOption.UNFILTERED
                }) {
                RadioButton(
                    selected = (selectedRegularFilterOption == OcrRegularFilterOption.UNFILTERED),
                    onClick = {
                        selectedRegularFilterOption = OcrRegularFilterOption.UNFILTERED
                    },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Variables.mainPrimary,
                        unselectedColor = Variables.mainDefault
                    )
                )
                Spacer(modifier = Modifier.width(width = 8.dp))
                Column {
                    Text(
                        text = "Unfiltered",
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
                        text = "Displays every available output",
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

            // Regex
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = (selectedRegularFilterOption == OcrRegularFilterOption.REGEX),
                    onClick = {
                        selectedRegularFilterOption = OcrRegularFilterOption.REGEX
                    },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Variables.mainPrimary,
                        unselectedColor = Variables.mainDefault
                    )
                )
                Spacer(modifier = Modifier.width(width = 8.dp))
                Row(modifier = Modifier.clickable {

                    selectedRegularFilterOption = OcrRegularFilterOption.REGEX

                    updateOcrFilterDataChanges(
                        oldOcrFilterData = uiState.ocrFilterData,
                        viewModel = viewModel,
                        modifiedRegularFilterOption = selectedRegularFilterOption,
                        modifiedAdvancedFilterOptionList = localSelectedAdvancedFilterOptionListCopy
                    )

                    navController.navigate(route = Screen.RegexFilter.route)
                }
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Regex",
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
                            text = "Returns text strings that match a sequence of characters defined by a RegEx pattern",
                            style = TextStyle(
                                fontSize = Variables.TypefaceFontSize12,
                                lineHeight = Variables.TypefaceLineHeight16,
                                fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                                fontWeight = FontWeight(400),
                                color = Variables.colorsTextBody,
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(width = 8.dp))
                    Image(
                        painter = painterResource(id = R.drawable.ic_right_exapand),
                        contentDescription = "image description",
                        contentScale = ContentScale.None
                    )
                }
            }

            // Advanced
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = (selectedRegularFilterOption == OcrRegularFilterOption.ADVANCED),
                    onClick = {
                        selectedRegularFilterOption = OcrRegularFilterOption.ADVANCED
                    },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = Variables.mainPrimary,
                        unselectedColor = Variables.mainDefault,
                        disabledSelectedColor = Variables.colorsSurfaceDisabled,
                        disabledUnselectedColor = Variables.colorsSurfaceDisabled
                    ),
                    enabled = localSelectedAdvancedFilterOptionListCopy.isNotEmpty()
                )
                Spacer(modifier = Modifier.width(width = 8.dp))
                Column {
                    Text(
                        text = "Advanced",
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
                        text = "Choose from the options below",
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

            Column(
                modifier = Modifier.padding(start = 16.dp, top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // Advanced: Character type
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = (localSelectedAdvancedFilterOptionListCopy.contains(
                            AdvancedFilterOption.CHARACTER_TYPE
                        )),
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                // Make sure the list doesn't contain the Preset
                                if (!localSelectedAdvancedFilterOptionListCopy.contains(
                                        AdvancedFilterOption.CHARACTER_TYPE
                                    )
                                ) {
                                    localSelectedAdvancedFilterOptionListCopy.add(
                                        AdvancedFilterOption.CHARACTER_TYPE
                                    )
                                }

                                if (selectedRegularFilterOption != OcrRegularFilterOption.ADVANCED) {
                                    selectedRegularFilterOption = OcrRegularFilterOption.ADVANCED
                                }
                            } else {
                                // Make sure the list contains the Preset
                                if (localSelectedAdvancedFilterOptionListCopy.contains(
                                        AdvancedFilterOption.CHARACTER_TYPE
                                    )
                                ) {
                                    localSelectedAdvancedFilterOptionListCopy.remove(
                                        AdvancedFilterOption.CHARACTER_TYPE
                                    )
                                }

                                if (localSelectedAdvancedFilterOptionListCopy.isEmpty()) {
                                    selectedRegularFilterOption = OcrRegularFilterOption.UNFILTERED
                                }
                            }
                        },
                        colors = CheckboxDefaults.colors(
                            checkmarkColor = Color.White,
                            checkedColor = Variables.mainPrimary,
                            uncheckedColor = Variables.mainSubtle
                        )
                    )
                    Spacer(modifier = Modifier.width(width = 4.dp))
                    Row(modifier = Modifier.clickable {
                        selectedRegularFilterOption = OcrRegularFilterOption.ADVANCED

                        if (!localSelectedAdvancedFilterOptionListCopy.contains(
                                AdvancedFilterOption.CHARACTER_TYPE
                            )
                        ) {
                            localSelectedAdvancedFilterOptionListCopy.add(AdvancedFilterOption.CHARACTER_TYPE)
                        }

                        updateOcrFilterDataChanges(
                            oldOcrFilterData = uiState.ocrFilterData,
                            viewModel = viewModel,
                            modifiedRegularFilterOption = selectedRegularFilterOption,
                            modifiedAdvancedFilterOptionList = localSelectedAdvancedFilterOptionListCopy
                        )

                        navController.navigate(route = Screen.CharacterTypeFilter.route)
                    }
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Character type",
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
                                text = "Filter by numbers, letters, and/or special characters",
                                style = TextStyle(
                                    fontSize = Variables.TypefaceFontSize12,
                                    lineHeight = Variables.TypefaceLineHeight16,
                                    fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                                    fontWeight = FontWeight(400),
                                    color = Variables.colorsTextBody,
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(width = 8.dp))
                        Image(
                            painter = painterResource(id = R.drawable.ic_right_exapand),
                            contentDescription = "image description",
                            contentScale = ContentScale.None
                        )
                    }
                }

                // Advanced: Character match
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = (localSelectedAdvancedFilterOptionListCopy.contains(
                            AdvancedFilterOption.CHARACTER_MATCH
                        )),
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                // Make sure the list doesn't contain the Preset
                                if (!localSelectedAdvancedFilterOptionListCopy.contains(
                                        AdvancedFilterOption.CHARACTER_MATCH
                                    )
                                ) {
                                    localSelectedAdvancedFilterOptionListCopy.add(
                                        AdvancedFilterOption.CHARACTER_MATCH
                                    )
                                }

                                if (selectedRegularFilterOption != OcrRegularFilterOption.ADVANCED) {
                                    selectedRegularFilterOption = OcrRegularFilterOption.ADVANCED
                                }
                            } else {
                                // Make sure the list contains the Preset
                                if (localSelectedAdvancedFilterOptionListCopy.contains(
                                        AdvancedFilterOption.CHARACTER_MATCH
                                    )
                                ) {
                                    localSelectedAdvancedFilterOptionListCopy.remove(
                                        AdvancedFilterOption.CHARACTER_MATCH
                                    )
                                }

                                if (localSelectedAdvancedFilterOptionListCopy.isEmpty()) {
                                    selectedRegularFilterOption = OcrRegularFilterOption.UNFILTERED
                                }
                            }
                        },
                        colors = CheckboxDefaults.colors(
                            checkmarkColor = Color.White,
                            checkedColor = Variables.mainPrimary,
                            uncheckedColor = Variables.mainSubtle
                        )
                    )
                    Spacer(modifier = Modifier.width(width = 4.dp))
                    Row(modifier = Modifier.clickable {
                        selectedRegularFilterOption = OcrRegularFilterOption.ADVANCED

                        if (!localSelectedAdvancedFilterOptionListCopy.contains(
                                AdvancedFilterOption.CHARACTER_MATCH
                            )
                        ) {
                            localSelectedAdvancedFilterOptionListCopy.add(AdvancedFilterOption.CHARACTER_MATCH)
                        }

                        updateOcrFilterDataChanges(
                            oldOcrFilterData = uiState.ocrFilterData,
                            viewModel = viewModel,
                            modifiedRegularFilterOption = selectedRegularFilterOption,
                            modifiedAdvancedFilterOptionList = localSelectedAdvancedFilterOptionListCopy
                        )

                        navController.navigate(route = Screen.CharacterMatchFilter.route)
                    }
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Character match",
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
                                text = "Filter to match specific characters, e.g., starts with, contains, or exact match",
                                style = TextStyle(
                                    fontSize = Variables.TypefaceFontSize12,
                                    lineHeight = Variables.TypefaceLineHeight16,
                                    fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                                    fontWeight = FontWeight(400),
                                    color = Variables.colorsTextBody,
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(width = 8.dp))
                        Image(
                            painter = painterResource(id = R.drawable.ic_right_exapand),
                            contentDescription = "image description",
                            contentScale = ContentScale.None
                        )
                    }
                }

                // Advanced: String length
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = (localSelectedAdvancedFilterOptionListCopy.contains(
                            AdvancedFilterOption.STRING_LENGTH
                        )),
                        onCheckedChange = { isChecked ->
                            if (isChecked) {
                                // Make sure the list doesn't contain the Preset
                                if (!localSelectedAdvancedFilterOptionListCopy.contains(
                                        AdvancedFilterOption.STRING_LENGTH
                                    )
                                ) {
                                    localSelectedAdvancedFilterOptionListCopy.add(
                                        AdvancedFilterOption.STRING_LENGTH
                                    )
                                }

                                if (selectedRegularFilterOption != OcrRegularFilterOption.ADVANCED) {
                                    selectedRegularFilterOption = OcrRegularFilterOption.ADVANCED
                                }
                            } else {
                                // Make sure the list contains the Preset
                                if (localSelectedAdvancedFilterOptionListCopy.contains(
                                        AdvancedFilterOption.STRING_LENGTH
                                    )
                                ) {
                                    localSelectedAdvancedFilterOptionListCopy.remove(
                                        AdvancedFilterOption.STRING_LENGTH
                                    )
                                }

                                if (localSelectedAdvancedFilterOptionListCopy.isEmpty()) {
                                    selectedRegularFilterOption = OcrRegularFilterOption.UNFILTERED
                                }
                            }
                        },
                        colors = CheckboxDefaults.colors(
                            checkmarkColor = Color.White,
                            checkedColor = Variables.mainPrimary,
                            uncheckedColor = Variables.mainSubtle
                        )
                    )
                    Spacer(modifier = Modifier.width(width = 4.dp))
                    Row(modifier = Modifier.clickable {
                        selectedRegularFilterOption = OcrRegularFilterOption.ADVANCED

                        if (!localSelectedAdvancedFilterOptionListCopy.contains(
                                AdvancedFilterOption.STRING_LENGTH
                            )
                        ) {
                            localSelectedAdvancedFilterOptionListCopy.add(AdvancedFilterOption.STRING_LENGTH)
                        }

                        updateOcrFilterDataChanges(
                            oldOcrFilterData = uiState.ocrFilterData,
                            viewModel = viewModel,
                            modifiedRegularFilterOption = selectedRegularFilterOption,
                            modifiedAdvancedFilterOptionList = localSelectedAdvancedFilterOptionListCopy
                        )

                        navController.navigate(route = Screen.StringLengthFilter.route)
                    }
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "String length",
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
                                text = "Filter by min/max limits",
                                style = TextStyle(
                                    fontSize = Variables.TypefaceFontSize12,
                                    lineHeight = Variables.TypefaceLineHeight16,
                                    fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                                    fontWeight = FontWeight(400),
                                    color = Variables.colorsTextBody,
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(width = 8.dp))
                        Image(
                            painter = painterResource(id = R.drawable.ic_right_exapand),
                            contentDescription = "image description",
                            contentScale = ContentScale.None
                        )
                    }
                }
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
                    updateOcrFilterDataChanges(
                        oldOcrFilterData = uiState.ocrFilterData,
                        viewModel = viewModel,
                        modifiedRegularFilterOption = selectedRegularFilterOption,
                        modifiedAdvancedFilterOptionList = localSelectedAdvancedFilterOptionListCopy
                    )

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

private fun updateOcrFilterDataChanges(
    oldOcrFilterData: OcrFilterData,
    viewModel: AIDataCaptureDemoViewModel,
    modifiedRegularFilterOption: OcrRegularFilterOption,
    modifiedAdvancedFilterOptionList: SnapshotStateList<AdvancedFilterOption>
) {
    // append the new state changes to the existing data
    oldOcrFilterData.selectedRegularFilterOption = modifiedRegularFilterOption
    oldOcrFilterData.selectedAdvancedFilterOptionList = modifiedAdvancedFilterOptionList
    viewModel.updateOcrFilterData(ocrFilterData = oldOcrFilterData)
}
