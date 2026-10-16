package com.zebra.aidatacapturedemo.ui.view.filters

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.data.CharacterTypeFilterOption
import com.zebra.aidatacapturedemo.model.FilterUtils
import com.zebra.aidatacapturedemo.ui.view.Variables
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel

@Composable
fun CharacterTypeFilterScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavHostController,
    innerPadding: PaddingValues
) {
    val uiState = viewModel.uiState.collectAsState().value

    val fieldId = uiState.selectedFilterFieldId ?: ""
    val fieldTitle = uiState.selectedFilterFieldTitle ?: ""
    val existing = uiState.ocrFilterData.fieldFilterConfigs
        .firstOrNull { it.fieldId == fieldId }
    val characterTypeOptions = existing?.characterTypeOptions.orEmpty()
    val selectedOptions = remember(fieldId) {
        mutableStateListOf<CharacterTypeFilterOption>().apply {
            addAll(characterTypeOptions)
        }
    }
    val localSelectedCharacterTypeFilterOptionListCopy = remember(fieldId) {
        mutableStateListOf<CharacterTypeFilterOption>().apply {
            addAll(characterTypeOptions)
        }
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
                .padding(start = 20.dp, top = 12.dp, end = 12.dp, bottom = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Select All
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (localSelectedCharacterTypeFilterOptionListCopy.contains(
                                CharacterTypeFilterOption.SELECT_ALL
                            )
                        ) {
                            localSelectedCharacterTypeFilterOptionListCopy.clear() // clear all the selection
                        } else {
                            localSelectedCharacterTypeFilterOptionListCopy.add(
                                CharacterTypeFilterOption.SELECT_ALL
                            )
                            localSelectedCharacterTypeFilterOptionListCopy.add(
                                CharacterTypeFilterOption.ALPHA
                            )
                            localSelectedCharacterTypeFilterOptionListCopy.add(
                                CharacterTypeFilterOption.NUMERIC
                            )
                            localSelectedCharacterTypeFilterOptionListCopy.add(
                                CharacterTypeFilterOption.INCLUDE_SPECIAL_CHARACTERS
                            )
                        }
                    }) {
                Checkbox(
                    checked = (localSelectedCharacterTypeFilterOptionListCopy.contains(
                        CharacterTypeFilterOption.SELECT_ALL
                    )),
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkmarkColor = Color.White,
                        checkedColor = Variables.mainPrimary,
                        uncheckedColor = Variables.mainSubtle
                    )
                )
                Spacer(modifier = Modifier.width(width = 8.dp))
                Column {
                    Text(
                        text = "Select All",
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
                        text = "Include alphanumeric and special characters",
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

            // Alpha
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (localSelectedCharacterTypeFilterOptionListCopy.contains(
                                CharacterTypeFilterOption.ALPHA
                            )
                        ) {
                            localSelectedCharacterTypeFilterOptionListCopy.remove(
                                CharacterTypeFilterOption.ALPHA
                            )

                            // Explicitly handle SelectAll removal
                            if (localSelectedCharacterTypeFilterOptionListCopy.contains(
                                    CharacterTypeFilterOption.SELECT_ALL
                                )
                            ) {
                                localSelectedCharacterTypeFilterOptionListCopy.remove(
                                    CharacterTypeFilterOption.SELECT_ALL
                                )
                            }
                        } else {
                            localSelectedCharacterTypeFilterOptionListCopy.add(
                                CharacterTypeFilterOption.ALPHA
                            )

                            // Explicitly handle SelectAll inclusion
                            if (localSelectedCharacterTypeFilterOptionListCopy.contains(
                                    CharacterTypeFilterOption.NUMERIC
                                ) &&
                                localSelectedCharacterTypeFilterOptionListCopy.contains(
                                    CharacterTypeFilterOption.INCLUDE_SPECIAL_CHARACTERS
                                )
                            ) {
                                localSelectedCharacterTypeFilterOptionListCopy.add(
                                    CharacterTypeFilterOption.SELECT_ALL
                                )
                            }
                        }
                    }) {
                Checkbox(
                    checked = (localSelectedCharacterTypeFilterOptionListCopy.contains(
                        CharacterTypeFilterOption.ALPHA
                    )),
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkmarkColor = Color.White,
                        checkedColor = Variables.mainPrimary,
                        uncheckedColor = Variables.mainSubtle
                    )
                )
                Spacer(modifier = Modifier.width(width = 8.dp))
                Column {
                    Text(
                        text = "Alpha",
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
                        text = "Shows results with letters (e.g., \"AaBbCc\")",
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

            // Numeric
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (localSelectedCharacterTypeFilterOptionListCopy.contains(
                                CharacterTypeFilterOption.NUMERIC
                            )
                        ) {
                            localSelectedCharacterTypeFilterOptionListCopy.remove(
                                CharacterTypeFilterOption.NUMERIC
                            )

                            // Explicitly handle SelectAll removal
                            if (localSelectedCharacterTypeFilterOptionListCopy.contains(
                                    CharacterTypeFilterOption.SELECT_ALL
                                )
                            ) {
                                localSelectedCharacterTypeFilterOptionListCopy.remove(
                                    CharacterTypeFilterOption.SELECT_ALL
                                )
                            }
                        } else {
                            localSelectedCharacterTypeFilterOptionListCopy.add(
                                CharacterTypeFilterOption.NUMERIC
                            )

                            // Explicitly handle SelectAll inclusion
                            if (localSelectedCharacterTypeFilterOptionListCopy.contains(
                                    CharacterTypeFilterOption.ALPHA
                                ) &&
                                localSelectedCharacterTypeFilterOptionListCopy.contains(
                                    CharacterTypeFilterOption.INCLUDE_SPECIAL_CHARACTERS
                                )
                            ) {
                                localSelectedCharacterTypeFilterOptionListCopy.add(
                                    CharacterTypeFilterOption.SELECT_ALL
                                )
                            }
                        }
                    }) {
                Checkbox(
                    checked = (localSelectedCharacterTypeFilterOptionListCopy.contains(
                        CharacterTypeFilterOption.NUMERIC
                    )),
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkmarkColor = Color.White,
                        checkedColor = Variables.mainPrimary,
                        uncheckedColor = Variables.mainSubtle
                    )
                )
                Spacer(modifier = Modifier.width(width = 8.dp))
                Column {
                    Text(
                        text = "Numeric",
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
                        text = "Shows results with numbers (e.g., \"12345\")",
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

            // Include special characters
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (localSelectedCharacterTypeFilterOptionListCopy.contains(
                                CharacterTypeFilterOption.INCLUDE_SPECIAL_CHARACTERS
                            )
                        ) {
                            localSelectedCharacterTypeFilterOptionListCopy.remove(
                                CharacterTypeFilterOption.INCLUDE_SPECIAL_CHARACTERS
                            )

                            // Explicitly handle SelectAll removal
                            if (localSelectedCharacterTypeFilterOptionListCopy.contains(
                                    CharacterTypeFilterOption.SELECT_ALL
                                )
                            ) {
                                localSelectedCharacterTypeFilterOptionListCopy.remove(
                                    CharacterTypeFilterOption.SELECT_ALL
                                )
                            }
                        } else {
                            localSelectedCharacterTypeFilterOptionListCopy.add(
                                CharacterTypeFilterOption.INCLUDE_SPECIAL_CHARACTERS
                            )

                            // Explicitly handle SelectAll inclusion
                            if (localSelectedCharacterTypeFilterOptionListCopy.contains(
                                    CharacterTypeFilterOption.ALPHA
                                ) &&
                                localSelectedCharacterTypeFilterOptionListCopy.contains(
                                    CharacterTypeFilterOption.NUMERIC
                                )
                            ) {
                                localSelectedCharacterTypeFilterOptionListCopy.add(
                                    CharacterTypeFilterOption.SELECT_ALL
                                )
                            }
                        }
                    }) {
                Checkbox(
                    checked = (localSelectedCharacterTypeFilterOptionListCopy.contains(
                        CharacterTypeFilterOption.INCLUDE_SPECIAL_CHARACTERS
                    )),
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkmarkColor = Color.White,
                        checkedColor = Variables.mainPrimary,
                        uncheckedColor = Variables.mainSubtle
                    )
                )
                Spacer(modifier = Modifier.width(width = 8.dp))
                Column {
                    Text(
                        text = "Include special characters",
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
                        text = "Show special characters with Alpha or Numeric selection (e.g., \"$-/@\")",
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
        }

        FilterBottomBar(
            onSave = {
                val baseConfig = FilterUtils.getOrCreateOcrFieldConfig(uiState, fieldId, fieldTitle)

                val updated = baseConfig.copy(
                    characterTypeOptions = localSelectedCharacterTypeFilterOptionListCopy.toMutableList(),
                    characterTypeEnabled = localSelectedCharacterTypeFilterOptionListCopy.isNotEmpty(),
                    regexData = baseConfig.regexData,
                    regexEnabled = baseConfig.regexEnabled,
                    characterMatchData = baseConfig.characterMatchData,
                    characterMatchEnabled = baseConfig.characterMatchEnabled,
                    stringLengthRange = baseConfig.stringLengthRange,
                    stringLengthEnabled = baseConfig.stringLengthEnabled
                )
                viewModel.saveOcrFieldConfig(updated)
                viewModel.handleBackButton(navController)
            },
            onCancel = { viewModel.handleBackButton(navController) }
        )
    }
}
