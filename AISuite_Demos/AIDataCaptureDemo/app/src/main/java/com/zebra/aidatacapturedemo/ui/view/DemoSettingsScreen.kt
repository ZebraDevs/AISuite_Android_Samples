package com.zebra.aidatacapturedemo.ui.view

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.zebra.aidatacapturedemo.BuildConfig
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.data.UsecaseState
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element


data class ExpandableSettingsItem(
    val title: String,
    var isExpanded: Boolean = false
)

data class ExpandableSettingsItemsList(
    val itemsTitle: MutableList<ExpandableSettingsItem> = mutableStateListOf()
)

@Composable
fun ExpandableSettingsItemsList.AddCommonSettings() {
    itemsTitle.add(ExpandableSettingsItem(stringResource(R.string.model_input_size)))
    itemsTitle.add(ExpandableSettingsItem(stringResource(R.string.resolution)))
    itemsTitle.add(ExpandableSettingsItem(stringResource(R.string.runtime_processor)))
}

@Composable
fun ExpandableSettingsItemsList.AddAboutSettings() {
    itemsTitle.add(ExpandableSettingsItem(stringResource(R.string.about)))
}

@Composable
fun DemoSettingsScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavController,
    innerPadding: PaddingValues
) {

    val uiState = viewModel.uiState.collectAsState().value

    // Intercept back presses on this screen
    val demo = uiState.usecaseSelected
    BackHandler(enabled = true) {
        viewModel.handleBackButton(navController)
    }
    viewModel.updateAppBarTitle(stringResource(R.string.settings))
    val settingsItemsList = ExpandableSettingsItemsList()
    settingsItemsList.AddCommonSettings()

    if (demo == UsecaseState.Barcode.value) {
        settingsItemsList.AddBarcodeSettings()
    } else if (demo == UsecaseState.Product.value) {
        settingsItemsList.AddProductSettings()
    }
    settingsItemsList.AddAboutSettings()

    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .background(color = Variables.surfaceDefault)
    ) {
        val items = remember {
            List(settingsItemsList.itemsTitle.size) { index ->
                ExpandableSettingsItem(settingsItemsList.itemsTitle[index].title)
            }
        }
        val expandedStates =
            remember { mutableStateListOf(*BooleanArray(items.size) { false }.toTypedArray()) }
        val listState = rememberLazyListState()

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Top),
            horizontalAlignment = Alignment.Start,
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            state = listState
        ) {
            itemsIndexed(items, key = { index, _ -> index }) { index, item ->
                ExpandableSettingsListItem(
                    item = item,
                    index = index,
                    isExpanded = expandedStates[index],
                    onExpandedChange = {
                        for (i in items.indices) {
                            expandedStates[i] = false
                        }
                        expandedStates[index] = it
                    },
                    viewModel, navController
                )
            }
        }
        Column(
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(bottom = 24.dp)
                .width(312.dp)
                .wrapContentHeight()
        ) {
            if (demo == UsecaseState.OCR.value) {
                BorderlessButton(
                    BorderlessButtonData(
                        R.string.advanced_settings,
                        onButtonClick = {
                            navController.navigate(Screen.AdvancedOCRSettings.route)
                        }
                    ))
            }
            BorderlessButton(
                BorderlessButtonData(
                    R.string.restore_default,
                    onButtonClick = {
                        viewModel.restoreDefaultSettings()
                        viewModel.applySettings()
                    }
                ))
        }
    }
}

@Composable
fun SettingHeader(viewModel: AIDataCaptureDemoViewModel, document: Document) {
    var isMoreInfoShown by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(color = Variables.mainInverse)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, top = 16.dp, end = 12.dp)
        ) {
            val element: Element? = document.getElementById("summary")
            var infoText = AnnotatedString("")
            if (element != null) {
                infoText = AnnotatedString.fromHtml(element.html())
            }
            Text(
                text = infoText,
                style = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                    fontWeight = FontWeight(400),
                    color = Variables.mainDefault,
                )
            )
        }
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 16.dp)
        ) {
            Text(
                text = "More >",
                Modifier
                    .clickable {
                        isMoreInfoShown = true
                    },
                style = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                    fontWeight = FontWeight(400),
                    color = Variables.mainPrimary,
                )
            )
        }
    }
    if (isMoreInfoShown) {
        isMoreInfoShown = SettingsMoreInfoScreen(viewModel, document, isMoreInfoShown)
    }
}

@Composable
fun ExpandableSettingsListItem(
    item: ExpandableSettingsItem,
    index: Int,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavController
) {
    val interactionSource = remember { MutableInteractionSource() }
    val rotationAngle by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)

    Column(
        horizontalAlignment = Alignment.Start,
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Variables.surfaceDefault)
            .clickable(interactionSource = interactionSource, indication = null) {
                onExpandedChange(!isExpanded)
            }
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .border(width = 1.dp, color = Variables.borderDefault)
                .fillMaxWidth()
                .wrapContentHeight()
                .background(color = Variables.mainLight)
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 12.dp)
        ) {
            Text(
                text = item.title,
                style = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontFamily = FontFamily(Font(R.font.ibm_plex_sans_medium)),
                    fontWeight = FontWeight(500),
                    color = Variables.mainDefault,
                )
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = ImageVector.Companion.vectorResource(id = R.drawable.down_arrow_icon),
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                modifier = Modifier
                    .graphicsLayer(rotationZ = rotationAngle)
                    .padding(1.dp)
                    .width(20.dp)
                    .height(20.dp),
                tint = Variables.mainSubtle
            )
        }
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            AddIndividualSettings(item, viewModel)
        }
    }
}

@Composable
fun AddIndividualSettings(item: ExpandableSettingsItem, viewModel: AIDataCaptureDemoViewModel) {
    val uiState = viewModel.uiState.collectAsState().value
    if (item.title.equals(stringResource(R.string.runtime_processor))) {
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val htmlString = viewModel.loadInputStreamFromAsset(fileName = "processor.html")
            val document: Document = Jsoup.parse(htmlString)
            SettingHeader(viewModel = viewModel, document)
            AddProcessorRadioButtonList(viewModel)
        }
    } else if (item.title.equals(stringResource(R.string.model_input_size))) {
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            var fileName: String = ""
            if ((uiState.usecaseSelected == UsecaseState.OCRFind.value) || (uiState.usecaseSelected == UsecaseState.OCR.value)) {
                fileName = "ocr_model_input_size.html"
            } else if (uiState.usecaseSelected == UsecaseState.Barcode.value) {
                fileName = "barcode_model_input_size.html"
            } else {
                fileName = "product_model_input_size.html"
            }
            val htmlString = viewModel.loadInputStreamFromAsset(fileName = fileName)
            val document: Document = Jsoup.parse(htmlString)
            SettingHeader(viewModel = viewModel, document)
            AddModelInputSizeRadioButtonList(viewModel)
        }
    } else if (item.title.equals(stringResource(R.string.resolution))) {
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            var fileName: String = ""
            if ((uiState.usecaseSelected == UsecaseState.OCRFind.value) || (uiState.usecaseSelected == UsecaseState.OCR.value)) {
                fileName = "ocr_resolution.html"
            } else if (uiState.usecaseSelected == UsecaseState.Barcode.value) {
                fileName = "barcode_resolution.html"
            } else {
                fileName = "product_resolution.html"
            }
            val htmlString = viewModel.loadInputStreamFromAsset(fileName = "ocr_resolution.html")
            val document: Document = Jsoup.parse(htmlString)
            SettingHeader(viewModel = viewModel, document)
            AddResolutionRadioButtonList(viewModel)
        }
    } else if (item.title.equals(stringResource(R.string.barcode_symbology))) {
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            //SettingHeader(viewModel = viewModel,"This is where the description lives, that explains to the user what the different option are and what they do")
            AddBarcodeSymbologySwitchOption(viewModel)
        }
    } else if (item.title.equals(stringResource(R.string.detection_parameters))) {
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            //SettingHeader(viewModel = viewModel,"This is where the description lives, that explains to the user what the different option are and what they do")
            AddOCRDetectionOptions(viewModel)
        }
    } else if (item.title.equals(stringResource(R.string.recognition_parameters))) {
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            //SettingHeader(viewModel = viewModel,"This is where the description lives, that explains to the user what the different option are and what they do")
            AddOCRRecognitionOptions(viewModel)
        }
    } else if (item.title.equals(stringResource(R.string.grouping))) {
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            //SettingHeader(viewModel = viewModel,"This is where the description lives, that explains to the user what the different option are and what they do")
            AddEnableOCRGroupingOptions(viewModel)
        }
    } else if (item.title.equals(stringResource(R.string.import_database))) {
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AddImportDatabaseOptions(viewModel)
        }
    } else if (item.title.equals(stringResource(R.string.export_database))) {
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AddExportDatabaseOptions(viewModel)
        }
    } else if (item.title.equals(stringResource(R.string.clear_active_database))) {
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AddClearActiveDatabaseOptions(viewModel)
        }
    } else if (item.title.equals(stringResource(R.string.about))) {
        Column(
            verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AddAboutInformation(viewModel)
        }
    }
}

@Composable
fun AddProcessorRadioButtonList(viewModel: AIDataCaptureDemoViewModel) {
    val currentUIState = viewModel.uiState.collectAsState().value
    val listOfProcessors = listOf<RadioButtonData>(
        RadioButtonData(
            stringResource(R.string.processor_auto),
            getSettingDescription(
                currentUIState.usecaseSelected,
                R.string.runtime_processor,
                0
            ),
            0,
            onItemSelected = { selectedProcessor ->
                viewModel.updateSelectedProcessor(selectedProcessor)
            }),
        RadioButtonData(
            stringResource(R.string.processor_dsp),
            getSettingDescription(
                currentUIState.usecaseSelected,
                R.string.runtime_processor,
                1
            ),
            1,
            onItemSelected = { selectedProcessor ->
                viewModel.updateSelectedProcessor(selectedProcessor)
            }
        ),
        RadioButtonData(
            stringResource(R.string.processor_gpu),
            getSettingDescription(
                currentUIState.usecaseSelected,
                R.string.runtime_processor,
                2
            ),
            2,
            onItemSelected = { selectedProcessor ->
                viewModel.updateSelectedProcessor(selectedProcessor)
            }),
        RadioButtonData(
            stringResource(R.string.processor_cpu),
            getSettingDescription(
                currentUIState.usecaseSelected,
                R.string.runtime_processor,
                3
            ),
            3,
            onItemSelected = { selectedProcessor ->
                viewModel.updateSelectedProcessor(selectedProcessor)
            })
    )
    viewModel.getProcessorSelectedIndex()?.let {
        ListOfRadioButtonOptions(it, listOfProcessors)
    }
}

@Composable
fun AddModelInputSizeRadioButtonList(viewModel: AIDataCaptureDemoViewModel) {
    val currentUIState = viewModel.uiState.collectAsState().value
    val listOfModelInputSizes = mutableListOf<RadioButtonData>(
        RadioButtonData(
            stringResource(R.string.model_input_size_640),
            getSettingDescription(
                currentUIState.usecaseSelected,
                R.string.model_input_size,
                0
            ),
            0,
            onItemSelected = { selectedModelInputSize ->
                viewModel.updateSelectedDimensions(selectedModelInputSize)
            }),
        RadioButtonData(
            stringResource(R.string.model_input_size_1280),
            getSettingDescription(
                currentUIState.usecaseSelected,
                R.string.model_input_size,
                1
            ),
            1,
            onItemSelected = { selectedModelInputSize ->
                viewModel.updateSelectedDimensions(selectedModelInputSize)
            }),
        RadioButtonData(
            stringResource(R.string.model_input_size_1600),
            getSettingDescription(
                currentUIState.usecaseSelected,
                R.string.model_input_size,
                2
            ),
            2,
            onItemSelected = { selectedModelInputSize ->
                viewModel.updateSelectedDimensions(selectedModelInputSize)
            }
        ),
        RadioButtonData(
            stringResource(R.string.model_input_size_2560),
            getSettingDescription(
                currentUIState.usecaseSelected,
                R.string.model_input_size,
                3
            ),
            3,
            onItemSelected = { selectedModelInputSize ->
                viewModel.updateSelectedDimensions(selectedModelInputSize)
            }
        )
    )
    if (currentUIState.usecaseSelected == UsecaseState.Barcode.value) {
        // Remove inputSize 2560 option for Barcode Decoder
        listOfModelInputSizes.removeAt(listOfModelInputSizes.size - 1)
    }
    var selectedIndex = 0
    if (viewModel.getInputSizeSelected() == 1280) {
        selectedIndex = 1
    } else if (viewModel.getInputSizeSelected() == 1600) {
        selectedIndex = 2
    } else if (viewModel.getInputSizeSelected() == 2560) {
        selectedIndex = 3
    }
    ListOfRadioButtonOptions(selectedIndex, listOfModelInputSizes)
}

@Composable
fun AddResolutionRadioButtonList(viewModel: AIDataCaptureDemoViewModel) {
    val currentUIState = viewModel.uiState.collectAsState().value
    val listOfResolutionSizes = listOf<RadioButtonData>(
        RadioButtonData(
            stringResource(R.string.resolution_size_1280),
            getSettingDescription(
                currentUIState.usecaseSelected,
                R.string.resolution,
                0

            ),
            0,
            onItemSelected = { selectedResolution ->
                viewModel.updateSelectedResolution(selectedResolution)
            }),
        RadioButtonData(
            stringResource(R.string.resolution_size_1920),
            getSettingDescription(
                currentUIState.usecaseSelected,
                R.string.resolution,
                1
            ),
            1,
            onItemSelected = { selectedResolution ->
                viewModel.updateSelectedResolution(selectedResolution)
            }),
        RadioButtonData(
            stringResource(R.string.resolution_size_2688),
            getSettingDescription(
                currentUIState.usecaseSelected,
                R.string.resolution,
                2
            ),
            2,
            onItemSelected = { selectedResolution ->
                viewModel.updateSelectedResolution(selectedResolution)
            }
        ),
        RadioButtonData(
            stringResource(R.string.resolution_size_3840),
            getSettingDescription(
                currentUIState.usecaseSelected,
                R.string.resolution,
                3
            ),
            3,
            onItemSelected = { selectedResolution ->
                viewModel.updateSelectedResolution(selectedResolution)
            }
        )
    )
    viewModel.getSelectedResolution()?.let {
        ListOfRadioButtonOptions(it, listOfResolutionSizes)
    }
}

@Composable
fun AddAboutInformation(viewModel: AIDataCaptureDemoViewModel) {
    val uiState = viewModel.uiState.collectAsState().value
    val versionPair = when (uiState.usecaseSelected) {
        UsecaseState.OCRFind.value -> {
            Pair(first = "Ocr Find Version", second = BuildConfig.TextOcrRecognizer_Version)
        }

        UsecaseState.OCR.value -> {
            Pair(
                first = "Text/Ocr Recognizer Version",
                second = BuildConfig.TextOcrRecognizer_Version
            )
        }

        UsecaseState.Barcode.value -> {
            Pair(
                first = "Barcode Recognizer Version",
                second = BuildConfig.BarcodeLocalizer_Version
            )
        }

        UsecaseState.Product.value -> {
            Pair(
                first = "Product & Shelf Recognizer Version",
                second = BuildConfig.ProductAndShelfRecognizer_Version
            )
        }

        UsecaseState.Retail.value -> {
            Pair(
                first = "Product & Shelf Localizer Version",
                second = BuildConfig.ProductAndShelfRecognizer_Version
            )
        }

        else -> {
            TODO("On AddAboutInformation() - Invalid use case ${uiState.usecaseSelected} selected")
        }
    }

    Row(
        modifier = Modifier
            .wrapContentHeight()
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = versionPair.first,
            style = TextStyle(
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontFamily = FontFamily(Font(R.font.ibm_plex_sans_medium)),
                fontWeight = FontWeight(500),
                color = Color(0xFF1D1E23),
            ),
            modifier = Modifier.padding(top = 18.dp, bottom = 6.dp, start = 14.4.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = versionPair.second,

            style = TextStyle(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                fontWeight = FontWeight(400),
                color = Color(0xFF646A78),
                textAlign = TextAlign.Right,
            ),
            modifier = Modifier.padding(end = 22.dp, top = 14.dp)
        )
    }
}
