package com.zebra.aidatacapturedemo.ui.view

import android.content.Context
import android.graphics.Rect
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.data.ResultData
import com.zebra.aidatacapturedemo.model.FileUtils.Companion.loadOcrBarcodeCaptureSessionDataFromPrefs
import com.zebra.aidatacapturedemo.model.FilterUtils
import com.zebra.aidatacapturedemo.ui.view.Variables.mainPrimary
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class FilterMatchType {
    REGEX,
    CHARACTER_MATCH,
    CHARACTER_TYPE,
    STRING_LENGTH
}

data class FieldMatchedValue(
    val text: String?,
    val boundingBox: Rect?,
    val isBarcode: Boolean,
    val matchedFilterType: FilterMatchType? = null
)

private data class FieldResultGroupRow(
    val fieldId: String,
    val fieldName: String,
    val values: List<FieldMatchedValue>
)

@Composable
fun OCRBarcodeResultScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavHostController,
    innerPadding: PaddingValues,
    context: Context
) {
    val uiState = viewModel.uiState.collectAsState().value
    val updateResults = remember { mutableStateOf(false) }
    val updateResultsTrigger = remember { mutableIntStateOf(0) }

    val ocrRowsState = remember { mutableStateOf<List<FieldResultGroupRow>>(emptyList()) }
    val barcodeRowsState = remember { mutableStateOf<List<FieldResultGroupRow>>(emptyList()) }

    BackHandler(enabled = true) { viewModel.handleBackButton(navController) }

    LaunchedEffect(updateResultsTrigger.intValue, uiState.ocrBarcodeCaptureSessionIndex) {
        val isCurrentSession =
            uiState.ocrBarcodeCaptureSessionCount == uiState.ocrBarcodeCaptureSessionIndex

        val (ocrSource, barcodeSource) = if (isCurrentSession) {
            Pair(
                uiState.ocrResults.filter { it.text.isNotBlank() },
                uiState.barcodeResults.filter { it.text.isNotBlank() }
            )
        } else {
            if (updateResults.value) {
                val session = withContext(Dispatchers.IO) {
                    loadOcrBarcodeCaptureSessionDataFromPrefs(
                        context,
                        uiState.ocrBarcodeCaptureSessionIndex.toString()
                    )
                }
                updateResults.value = false
                Pair(
                    session?.ocrResults?.filter { it.text.isNotBlank() } ?: emptyList(),
                    session?.barcodeResults?.filter { it.text.isNotBlank() } ?: emptyList()
                )
            } else {
                Pair(emptyList(), emptyList())
            }
        }

        ocrRowsState.value = buildOcrFieldRows(uiState, ocrSource)
        barcodeRowsState.value = buildBarcodeFieldRows(uiState, barcodeSource)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding()
            )
    ) {
        Header(uiState.ocrBarcodeCaptureSessionIndex)

        val ocrExpanded = remember { mutableStateOf(false) }
        val barcodeExpanded = remember { mutableStateOf(false) }

        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier
                .weight(1f)
                .background(color = Variables.colorsSurfaceCool)
                .fillMaxWidth()
        ) {
            if (ocrRowsState.value.isNotEmpty()) {
                item {
                    ExpandableSectionHeader(
                        title = "OCR FIELD RESULTS",
                        expanded = ocrExpanded.value,
                        onToggle = { ocrExpanded.value = !ocrExpanded.value }
                    )
                }
                if (ocrExpanded.value) {
                    itemsIndexed(
                        ocrRowsState.value,
                        key = { _, row -> "ocr_${row.fieldId}" }) { index, row ->
                        FieldResultRow(item = row, navController = navController)
                        if (index != ocrRowsState.value.lastIndex) {
                            HorizontalDivider(color = Color(0xFFE8EAF0), thickness = 1.dp)
                        }
                    }
                }
            }

            if (ocrRowsState.value.isNotEmpty() && barcodeRowsState.value.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = Color(0xFFE8EAF0), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (barcodeRowsState.value.isNotEmpty()) {
                item {
                    ExpandableSectionHeader(
                        title = "BARCODE FIELD RESULTS",
                        expanded = barcodeExpanded.value,
                        onToggle = { barcodeExpanded.value = !barcodeExpanded.value }
                    )
                }
                if (barcodeExpanded.value) {
                    itemsIndexed(
                        barcodeRowsState.value,
                        key = { _, row -> "barcode_${row.fieldId}" }) { index, row ->
                        FieldResultRow(item = row, navController = navController)
                        if (index != barcodeRowsState.value.lastIndex) {
                            HorizontalDivider(color = Color(0xFFE8EAF0), thickness = 1.dp)
                        }
                    }
                }
            }
        }

        Bottom(
            viewModel = viewModel,
            navController = navController,
            uiState = uiState,
            updateResultsChanged = { updateResults.value = it },
            incrementResultsTrigger = { updateResultsTrigger.intValue++ }
        )
    }
}

@Composable
private fun FieldResultRow(
    item: FieldResultGroupRow,
    navController: NavHostController
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Variables.surfaceDefault)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Field Name Header
        ShowFieldName(fieldName = item.fieldName)
        Spacer(modifier = Modifier.height(8.dp))

        if (item.values.isEmpty()) ShowValueNoMatch() else {
            // Group values by filter type
            val groupedByFilter = item.values.groupBy {
                it.matchedFilterType
            }

            groupedByFilter.forEach { (filterType, values) ->
                // Filter Type Header
                ShowFilterType(filterType)
                Spacer(modifier = Modifier.height(4.dp))

                // Values under this filter type
                values.forEach { value -> ShowValuesRow(value, navController) }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun Header(session: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(107.dp, Alignment.Start),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(color = Variables.colorsSurfaceCool)
            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.results_session) + session.toString(),
            style = TextStyle(
                fontSize = Variables.TypefaceFontSize16,
                lineHeight = Variables.TypefaceLineHeight20,
                fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                fontWeight = FontWeight(600),
                color = Variables.colorsMainSubtle,
                letterSpacing = Variables.TypefaceLetterSpacingTitle
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ShowFieldName(fieldName: String) {
    Text(
        text = "Field: ${fieldName}",
        modifier = Modifier.padding(horizontal = 4.dp),
        style = TextStyle(
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
            fontWeight = FontWeight(600),
            color = Variables.colorsTextDefault,
        )
    )
}

@Composable
private fun ShowValueNoMatch() {
    Text(
        text = "Value: No match",
        modifier = Modifier.padding(horizontal = 4.dp),
        style = TextStyle(
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontFamily =
                FontFamily(Font(R.font.ibm_plex_sans)),
            fontWeight = FontWeight(400),
            color = Variables.colorsMainSubtle
        )
    )
}

@Composable
private fun ShowFilterType(filterType: FilterMatchType?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color(0xFFF5F5F5))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = R.drawable.filter_icon),
            contentDescription = "Configure field",
            modifier = Modifier.size(16.dp),
            tint = Variables.blackText
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = getFilterTypeDisplayName(filterType),
            style = TextStyle(
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                fontWeight = FontWeight(500),
                color = Variables.colorsMainSubtle
            )
        )
    }

}

@Composable
private fun ShowValuesRow(
    value: FieldMatchedValue,
    navController: NavHostController
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 8.dp)
    ) {
        Text(
            text = "• ${value.text}",
            modifier = Modifier.weight(1f),
            style = TextStyle(
                fontSize = 13.sp,
                lineHeight = 12.sp,
                fontFamily =
                    FontFamily(Font(R.font.ibm_plex_sans)),
                fontWeight = FontWeight(400),
                color = Variables.colorsTextDefault
            )
        )
        Image(
            painter = painterResource(id = R.drawable.ic_location),
            contentDescription = value.text,
            modifier = Modifier
                .padding(start = 8.dp)
                .size(20.dp)
                .clickable {
                    val b = value.boundingBox ?: return@clickable
                    val bboxStr = "${b.left},${b.top},${b.right},${b.bottom}"
                    navController.navigate(
                        route = Screen.SingleResult.route +
                                "?text=${value.text}" +
                                "&bbox=$bboxStr" +
                                "&isBarcode=${value.isBarcode}"
                    )
                }
        )
    }
}

// Helper function to display filter type name
private fun getFilterTypeDisplayName(
    filterType: FilterMatchType?
): String = when (filterType) {
    FilterMatchType.REGEX -> "Regex"
    FilterMatchType.CHARACTER_MATCH -> "Character Match"
    FilterMatchType.CHARACTER_TYPE -> "Character Type"
    FilterMatchType.STRING_LENGTH -> "String Length"
    null -> "No Filter"
}

@Composable
fun Bottom(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavHostController,
    uiState: AIDataCaptureDemoUiState,
    updateResultsChanged: (Boolean) -> Unit,
    incrementResultsTrigger: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .border(1.dp, Variables.mainInverse),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundedIconButton(R.drawable.ic_previoussession, onClick = {
                val index = uiState.ocrBarcodeCaptureSessionIndex - 1
                if (index in 1..uiState.ocrBarcodeCaptureSessionCount) {
                    viewModel.updateOcrBarcodeCaptureSessionIndex(index)
                    updateResultsChanged(true)
                    incrementResultsTrigger()
                }
            })

            RoundedIconButton(R.drawable.ic_nextsession, onClick = {
                val index = uiState.ocrBarcodeCaptureSessionIndex + 1
                if (index in 1..uiState.ocrBarcodeCaptureSessionCount) {
                    viewModel.updateOcrBarcodeCaptureSessionIndex(index)
                    updateResultsChanged(true)
                    incrementResultsTrigger()
                }
            })

            Spacer(modifier = Modifier.weight(1f))

            ButtonWithIconOption(
                ButtonData(
                    R.string.scan,
                    mainPrimary,
                    1.0F,
                    true
                ) {
                    navController.navigate(route = Screen.Preview.route) {
                        popUpTo("preview_screen") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                R.drawable.ic_scan
            )
        }
    }
}

@Composable
private fun ExpandableSectionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Variables.colorsSurfaceCool)
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = TextStyle(
                fontSize = 16.sp, // increased size
                lineHeight = 22.sp,
                fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                fontWeight = FontWeight(600),
                color = Variables.colorsMainSubtle
            )
        )

        Icon(
            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
            contentDescription = if (expanded) "Collapse" else "Expand",
            tint = Variables.colorsMainSubtle
        )
    }
}

private fun buildOcrFieldRows(
    uiState: AIDataCaptureDemoUiState,
    ocrResults: List<ResultData>
): List<FieldResultGroupRow> {
    val selectedIds = uiState.ocrFilterData.selectedFieldIds.toSet()

    // No filter fields configured — show all detected OCR results in a default row
    if (selectedIds.isEmpty()) {
        if (ocrResults.isEmpty()) return emptyList()
        return listOf(
            FieldResultGroupRow(
                fieldId = "default",
                fieldName = "All Results",
                values = ocrResults.map {
                    FieldMatchedValue(
                        text = it.text,
                        boundingBox = it.boundingBox,
                        isBarcode = false
                    )
                }
            )
        )
    }

    val fieldsById = uiState.ocrFilterData.fieldList.associateBy { it.id }
    val orderedConfigs =
        uiState.ocrFilterData.fieldFilterConfigs.filter { it.fieldId in selectedIds }

    return orderedConfigs.map { cfg ->
        val fieldName = fieldsById[cfg.fieldId]?.title ?: cfg.fieldTitle ?: cfg.fieldId
        val matches = ocrResults
            .mapNotNull { result ->
                val filterType =
                    FilterUtils.getOcrMatchingFilterType(result.text, cfg)
                if (filterType != null) FieldMatchedValue(
                    text = result.text,
                    boundingBox = result.boundingBox,
                    isBarcode = false,
                    matchedFilterType = filterType
                ) else null
            }

        FieldResultGroupRow(
            fieldId = cfg.fieldId ?: "",
            fieldName = fieldName ?: "",
            values = matches
        )
    }
}

private fun buildBarcodeFieldRows(
    uiState: AIDataCaptureDemoUiState,
    barcodeResults: List<ResultData>
): List<FieldResultGroupRow> {
    val selectedIds = uiState.barcodeFilterData.selectedFieldIds.toSet()

    // No filter fields configured — show all detected barcode results in a default row
    if (selectedIds.isEmpty()) {
        if (barcodeResults.isEmpty()) return emptyList()
        return listOf(
            FieldResultGroupRow(
                fieldId = "default",
                fieldName = "All Results",
                values = barcodeResults.map {
                    FieldMatchedValue(
                        text = it.text,
                        boundingBox = it.boundingBox,
                        isBarcode = true
                    )
                }
            )
        )
    }

    val fieldsById = uiState.barcodeFilterData.fieldList.associateBy { it.id }
    val orderedConfigs =
        uiState.barcodeFilterData.fieldFilterConfigs.filter { it.fieldId in selectedIds }

    return orderedConfigs.map { cfg ->
        val fieldName = fieldsById[cfg.fieldId]?.title ?: cfg.fieldTitle ?: cfg.fieldId
        val matches = barcodeResults
            .mapNotNull { result ->
                val filterType =
                    FilterUtils.getBarcodeMatchingFilterType(result.text, cfg)
                if (filterType != null) FieldMatchedValue(
                    text = result.text,
                    boundingBox = result.boundingBox,
                    isBarcode = true,
                    matchedFilterType = filterType
                ) else null
            }

        FieldResultGroupRow(
            fieldId = cfg.fieldId ?: "",
            fieldName = fieldName ?: "",
            values = matches
        )
    }
}