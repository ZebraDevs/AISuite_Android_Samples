package com.zebra.aidatacapturedemo.ui.view.filters

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.data.OcrFieldFilterConfig
import com.zebra.aidatacapturedemo.model.FilterUtils
import com.zebra.aidatacapturedemo.ui.view.Screen
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel

@Composable
fun OCRFindFilterHomeScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavHostController,
    innerPadding: PaddingValues
) {
    val uiState by viewModel.uiState.collectAsState()

    val fieldId = uiState.selectedFilterFieldId ?: ""
    val fieldTitle = uiState.selectedFilterFieldTitle ?: ""
    val existing = uiState.ocrFilterData.fieldFilterConfigs
        .firstOrNull { it.fieldId == fieldId }

    InitToast(uiState, viewModel)
    BackHandler(enabled = true) { viewModel.handleBackButton(navController) }
    InitFilterBody(fieldId, fieldTitle, existing, viewModel, navController, innerPadding, uiState)
}

@Composable
private fun InitFilterBody(
    fieldId: String,
    fieldTitle: String,
    existingConfig: OcrFieldFilterConfig?,
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavHostController,
    innerPadding: PaddingValues,
    uiState: AIDataCaptureDemoUiState
) {
    val regexChecked = existingConfig?.regexEnabled ?: false
    val charTypeChecked = existingConfig?.characterTypeEnabled ?: false
    val charMatchChecked = existingConfig?.characterMatchEnabled ?: false
    val stringLengthChecked = existingConfig?.stringLengthEnabled ?: false

    var localRegexChecked by remember(fieldId) { mutableStateOf(regexChecked) }
    var localCharTypeChecked by remember(fieldId) { mutableStateOf(charTypeChecked) }
    var localCharMatchChecked by remember(fieldId) { mutableStateOf(charMatchChecked) }
    var localStringLengthChecked by remember(fieldId) { mutableStateOf(stringLengthChecked) }

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
            FilterRow(
                title = "Regex",
                subTitle = "Returns text strings that match a sequence of characters defined by a RegEx pattern",
                checked = localRegexChecked,
                onCheckedChange = { localRegexChecked = it },
                onRowItemClicked = { navController.navigate(route = Screen.RegexFilter.route) }
            )
            FilterRow(
                title = "Character type",
                subTitle = "Filter by numbers, letters, and/or special characters",
                checked = localCharTypeChecked,
                onCheckedChange = { localCharTypeChecked = it },
                onRowItemClicked = { navController.navigate(route = Screen.CharacterTypeFilter.route) }
            )
            FilterRow(
                title = "Character Match",
                subTitle = "Filter to match specific characters, e.g., starts with, contains, or exact match",
                checked = localCharMatchChecked,
                onCheckedChange = { localCharMatchChecked = it },
                onRowItemClicked = { navController.navigate(route = Screen.CharacterMatchFilter.route) }
            )
            FilterRow(
                title = "String Length",
                subTitle = "Filter by min/max limits",
                checked = localStringLengthChecked,
                onCheckedChange = { localStringLengthChecked = it },
                onRowItemClicked = { navController.navigate(route = Screen.StringLengthFilter.route) }
            )
        }
        FilterBottomBar(
            onCancel = { viewModel.handleBackButton(navController) },
            onSave = {
                val existing = FilterUtils.getOrCreateOcrFieldConfig(uiState, fieldId, fieldTitle)

                viewModel.saveOcrFieldConfig(
                    existing.copy(
                        regexEnabled = localRegexChecked,
                        characterTypeEnabled = localCharTypeChecked,
                        characterMatchEnabled = localCharMatchChecked,
                        stringLengthEnabled = localStringLengthChecked
                    )
                )

                viewModel.handleBackButton(navController)
            }
        )
    }
}