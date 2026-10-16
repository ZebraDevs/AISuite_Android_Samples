package com.zebra.aidatacapturedemo.ui.view.filters

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.data.FilterField
import com.zebra.aidatacapturedemo.data.FilterMenuAction
import com.zebra.aidatacapturedemo.data.FilterType
import com.zebra.aidatacapturedemo.model.FilterUtils
import com.zebra.aidatacapturedemo.ui.view.Screen
import com.zebra.aidatacapturedemo.ui.view.Variables
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel

@Composable
fun FilterConfigScreen(
    innerPadding: PaddingValues,
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavHostController,
    filterType: FilterType
) {
    val uiState by viewModel.uiState.collectAsState()

    InitToast(uiState, viewModel)
    BackHandler(enabled = true) { viewModel.handleBackButton(navController) }

    var showAddFieldDialog by remember { mutableStateOf(false) }
    var showEditFieldDialog by remember { mutableStateOf(false) }
    var showDeleteFieldDialog by remember { mutableStateOf(false) }
    var showResetFieldDialog by remember { mutableStateOf(false) }
    var fieldToEdit by remember { mutableStateOf<FilterField?>(null) }

    val allFields = getExistingFilterData(filterType, uiState)
    val localSelected = getExistingSelectedFields(filterType, allFields, uiState)

    InitFilterDataByFilterType(filterType, viewModel, uiState)
    InitMenuActions(
        pendingFilterMenuAction = uiState.pendingFilterMenuAction,
        viewModel = viewModel,
        allFields = allFields,
        localSelected = localSelected,
        onDeleteRequested = { if (allFields.isNotEmpty()) showDeleteFieldDialog = true },
        onResetRequested = { if (allFields.isNotEmpty()) showResetFieldDialog = true }
    )
    InitFieldList(
        innerPadding = innerPadding,
        header = { FilterConfigHeader() },
        body = {
            FilterConfigBody(
                allFields = allFields,
                localSelected = localSelected,
                onConfigureFieldClick = { field ->
                    viewModel.setSelectedFilterField(field.id, field.title)
                    navController.navigate(getFilterRouteByFilterType(filterType))
                },
                onEditFieldClick = { field ->
                    fieldToEdit = field
                    showEditFieldDialog = true
                },
                viewModel = viewModel,
                uiState = uiState
            )
        },
        bottomBar = {
            FilterConfigBottomBar(
                navController,
                onAddField = { showAddFieldDialog = true },
                onScan = {
                    updateSelectedFieldIds(filterType, viewModel, localSelected)
                    if (uiState.isCaptureOrLiveEnabled == 2) {
                        navController.navigate(Screen.TargetModePreview.route)
                    } else {
                        navController.navigate(Screen.Preview.route)
                    }
                }
            )
        },
        placeHolder = { PlaceHolderEmptyField(allFields) }
    )
    InitNewFieldDialog(
        showAddFieldDialog = showAddFieldDialog,
        onDismiss = { showAddFieldDialog = false },
        onAddField = { newField ->
            updateFieldsDataByFilterType(filterType, viewModel, newField)
        }
    )
    InitEditFieldDialog(
        showEditFieldDialog = showEditFieldDialog,
        fieldToEdit = fieldToEdit,
        onDismiss = { showEditFieldDialog = false },
        onEditField = { updatedField ->
            fieldToEdit?.let {
                updateFieldsDataByFilterType(filterType, viewModel, updatedField)
            }
            showEditFieldDialog = false
        }
    )
    InitDeleteFieldDialog(
        showDeleteFieldDialog = showDeleteFieldDialog,
        onDismiss = { showDeleteFieldDialog = false },
        onDeleteField = {
            if (allFields.isEmpty()) return@InitDeleteFieldDialog
            removeFilterDataByFilterType(filterType, viewModel, localSelected)
            showDeleteFieldDialog = false
        }
    )
    InitResetFieldDialog(
        showResetFieldDialog = showResetFieldDialog,
        onDismiss = { showResetFieldDialog = false },
        onResetField = {
            if (allFields.isEmpty()) return@InitResetFieldDialog
            clearAllFilterDataByFilterType(filterType, viewModel)
            localSelected.clear()
            showResetFieldDialog = false
        }
    )
}

@Composable
private fun PlaceHolderEmptyField(allFields: List<FilterField>) {
    if (allFields.isEmpty()) Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.warning_icon),
                contentDescription = "Warning",
                tint = Variables.mainSubtle,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            AppRowTitleText("No fields configured")
        }
        Spacer(modifier = Modifier.height(20.dp))
        AppBodyText("Tap '+ New Field' below to create your first field.")
    }
}

@Composable
private fun InitFilterDataByFilterType(
    filterType: FilterType,
    viewModel: AIDataCaptureDemoViewModel,
    uiState: AIDataCaptureDemoUiState
) = LaunchedEffect(
    filterType,
    uiState.ocrFilterData.fieldList,
    uiState.barcodeFilterData.fieldList
) { viewModel.setSelectedFilterType(filterType) }

private fun getExistingFilterData(
    filterType: FilterType,
    uiState: AIDataCaptureDemoUiState
): MutableList<FilterField> = when (filterType) {
    FilterType.OCR_FILTER -> uiState.ocrFilterData.fieldList
    FilterType.BARCODE_FILTER -> uiState.barcodeFilterData.fieldList
    FilterType.NONE -> mutableListOf()
}

@Composable
private fun getExistingSelectedFields(
    filterType: FilterType,
    allFields: List<FilterField>,
    uiState: AIDataCaptureDemoUiState
): SnapshotStateList<String> {
    val localSelected = remember { mutableStateListOf<String>() }

    val savedSelected = when (filterType) {
        FilterType.OCR_FILTER -> uiState.ocrFilterData.selectedFieldIds
        FilterType.BARCODE_FILTER -> uiState.barcodeFilterData.selectedFieldIds
        FilterType.NONE -> emptyList()
    }

    LaunchedEffect(filterType, allFields, savedSelected) {
        val validIds = savedSelected.filter { id -> allFields.any { field -> field.id == id } }
        if (localSelected != validIds) {
            localSelected.clear()
            localSelected.addAll(validIds)
        }
    }

    return localSelected
}

private fun updateFieldsDataByFilterType(
    filterType: FilterType,
    viewModel: AIDataCaptureDemoViewModel,
    filterField: FilterField
) = when (filterType) {
    FilterType.OCR_FILTER -> viewModel.upsertOcrField(filterField)
    FilterType.BARCODE_FILTER -> viewModel.upsertBarcodeField(filterField)
    FilterType.NONE -> Unit
}

private fun updateSelectedFieldIds(
    filterType: FilterType,
    viewModel: AIDataCaptureDemoViewModel,
    localSelected: SnapshotStateList<String>
) = when (filterType) {
    FilterType.OCR_FILTER -> viewModel.updateSelectedOcrFieldIds(localSelected.toList())
    FilterType.BARCODE_FILTER -> viewModel.updateSelectedBarcodeFieldIds(localSelected.toList())
    FilterType.NONE -> Unit
}

private fun removeFilterDataByFilterType(
    filterType: FilterType,
    viewModel: AIDataCaptureDemoViewModel,
    localSelected: SnapshotStateList<String>
) {
    when (filterType) {
        FilterType.OCR_FILTER -> viewModel.removeOcrFields(localSelected.toList())
        FilterType.BARCODE_FILTER -> viewModel.removeBarcodeFields(localSelected.toList())
        FilterType.NONE -> Unit
    }
    localSelected.clear()
}

private fun getFilterRouteByFilterType(filterType: FilterType) =
    when (filterType) {
        FilterType.OCR_FILTER -> Screen.OCRFilterConfig.route
        FilterType.BARCODE_FILTER -> Screen.BarcodeFilterConfig.route
        else -> ""
    }

private fun clearAllFilterDataByFilterType(
    filterType: FilterType,
    viewModel: AIDataCaptureDemoViewModel
) {
    when (filterType) {
        FilterType.OCR_FILTER -> viewModel.resetOcrFields(emptyList())
        FilterType.BARCODE_FILTER -> viewModel.resetBarcodeFields(emptyList())
        FilterType.NONE -> Unit
    }
}

@Composable
private fun InitNewFieldDialog(
    showAddFieldDialog: Boolean,
    onAddField: (FilterField) -> Unit,
    onDismiss: () -> Unit
) {
    if (showAddFieldDialog) FieldDialog(
        initialFieldName = "",
        isEditing = false,
        onDismiss = onDismiss,
        onConfirm = { fieldName ->
            val newField = FilterField(
                id = FilterUtils.generateFieldId(fieldName),
                title = fieldName,
                isDefault = false
            )
            onAddField(newField)
        }
    )
}

@Composable
private fun InitEditFieldDialog(
    showEditFieldDialog: Boolean,
    fieldToEdit: FilterField?,
    onDismiss: () -> Unit,
    onEditField: (FilterField) -> Unit
) {
    if (showEditFieldDialog && fieldToEdit != null) {
        FieldDialog(
            initialFieldName = fieldToEdit.title,
            isEditing = true,
            onDismiss = onDismiss,
            onConfirm = { updatedFieldName ->
                val updatedField = fieldToEdit.copy(title = updatedFieldName)
                onEditField(updatedField)
            }
        )
    }
}

@Composable
private fun InitDeleteFieldDialog(
    showDeleteFieldDialog: Boolean,
    onDeleteField: () -> Unit,
    onDismiss: () -> Unit
) {
    if (showDeleteFieldDialog) DeleteFieldDialog(
        onDismiss = onDismiss,
        onDeleteField = onDeleteField
    )
}

@Composable
private fun InitResetFieldDialog(
    showResetFieldDialog: Boolean,
    onResetField: () -> Unit,
    onDismiss: () -> Unit
) {
    if (showResetFieldDialog) ResetFieldsDialog(
        onResetField = onResetField,
        onDismiss = onDismiss
    )
}

@Composable
private fun InitFieldList(
    innerPadding: PaddingValues,
    header: @Composable () -> Unit,
    body: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    placeHolder: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            header()
            body()
        }
        placeHolder()
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) { bottomBar() }
    }
}

@Composable
private fun FilterConfigHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFEFF2F6))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        AppHeaderTitle("Field Configuration")
        Spacer(Modifier.height(6.dp))
        AppBodyText("Use preconfigured fields to find alias variations, and optionally include hints or custom fields.")
    }
}

@Composable
private fun FilterConfigBody(
    allFields: List<FilterField>,
    localSelected: SnapshotStateList<String>,
    onConfigureFieldClick: (FilterField) -> Unit,
    onEditFieldClick: (FilterField) -> Unit,
    uiState: AIDataCaptureDemoUiState,
    modifier: Modifier = Modifier,
    viewModel: AIDataCaptureDemoViewModel
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(allFields, key = { it.id }) { field ->
            FilterFieldRow(
                field = field,
                isChecked = localSelected.contains(field.id),
                isConfigured = isFieldConfigured(field.id, uiState.selectedFilterType, uiState),
                onCheckedChange = { checked ->
                    when {
                        checked -> if (!localSelected.contains(field.id)) localSelected.add(field.id)
                        else -> localSelected.remove(field.id)
                    }
                    updateSelectedFieldIds(uiState.selectedFilterType, viewModel, localSelected)
                },
                onConfigureFieldClick = { onConfigureFieldClick(field) },
                onEditFieldClick = { onEditFieldClick(field) }
            )
            HorizontalDivider(color = Color(0xFFE8EAF0), thickness = 1.dp)
        }
    }
}

@Composable
private fun FilterFieldRow(
    field: FilterField,
    isChecked: Boolean,
    isConfigured: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onConfigureFieldClick: () -> Unit,
    onEditFieldClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkmarkColor = Color.White,
                checkedColor = Variables.mainDefault,
                uncheckedColor = Variables.mainSubtle
            )
        )

        Spacer(Modifier.width(12.dp))
        AppRowTitleText(field.title)
        Spacer(modifier = Modifier.weight(1f))

        IconButton(onClick = onEditFieldClick, modifier = Modifier.padding(end = 6.dp)) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.edit_icon),
                contentDescription = "Configure field",
                modifier = Modifier.size(20.dp),
                tint = Variables.blackText
            )
        }
        Box(modifier = Modifier.clickable { onConfigureFieldClick() }) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.filter_icon),
                contentDescription = "Configure field",
                modifier = Modifier.size(24.dp),
                tint = Variables.blackText
            )
            if (isConfigured) Image(
                imageVector = ImageVector.vectorResource(id = R.drawable.red_dot),
                contentDescription = "Active indicator",
                modifier = Modifier
                    .size(10.dp)
                    .align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
private fun FilterConfigBottomBar(
    navController: NavHostController,
    onAddField: () -> Unit,
    onScan: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AppButton(
            text = "New Field",
            onClick = onAddField,
            modifier = Modifier.weight(1f),
            icon = painterResource(id = R.drawable.plus_icon),
            style = AppButtonStyle.Outlined
        )

        AppButton(
            text = "Scan",
            onClick = onScan,
            modifier = Modifier.weight(1f),
            icon = painterResource(id = R.drawable.scan_conf),
            style = AppButtonStyle.Filled
        )
    }
}

@Composable
private fun InitMenuActions(
    pendingFilterMenuAction: FilterMenuAction,
    viewModel: AIDataCaptureDemoViewModel,
    allFields: List<FilterField>,
    localSelected: SnapshotStateList<String>,
    onDeleteRequested: () -> Unit,
    onResetRequested: () -> Unit
) {
    LaunchedEffect(pendingFilterMenuAction, allFields) {
        when (pendingFilterMenuAction) {
            FilterMenuAction.SELECT_ALL -> {
                localSelected.clear()
                localSelected.addAll(allFields.map { it.id })
            }

            FilterMenuAction.DESELECT_ALL -> localSelected.clear()
            FilterMenuAction.DELETE -> onDeleteRequested()
            FilterMenuAction.RESET -> onResetRequested()
            FilterMenuAction.NONE -> Unit
        }
        viewModel.clearPendingFilterMenuAction()
    }
}

private fun isFieldConfigured(
    fieldId: String,
    filterType: FilterType,
    uiState: AIDataCaptureDemoUiState
): Boolean {
    return when (filterType) {
        FilterType.OCR_FILTER -> {
            val cfg = uiState.ocrFilterData.fieldFilterConfigs
                .firstOrNull { it.fieldId == fieldId }
            cfg != null && (cfg.regexEnabled || cfg.characterTypeEnabled ||
                    cfg.characterMatchEnabled || cfg.stringLengthEnabled)
        }

        FilterType.BARCODE_FILTER -> {
            val cfg = uiState.barcodeFilterData.fieldFilterConfigs
                .firstOrNull { it.fieldId == fieldId }
            cfg != null && (cfg.characterMatchEnabled || cfg.stringLengthEnabled)
        }

        FilterType.NONE -> false
    }
}
