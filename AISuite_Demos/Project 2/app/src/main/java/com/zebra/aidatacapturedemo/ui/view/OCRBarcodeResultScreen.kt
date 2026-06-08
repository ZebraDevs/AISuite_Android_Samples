package com.zebra.aidatacapturedemo.ui.view

import android.content.Context
import android.graphics.Rect
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.model.FileUtils.Companion.loadOcrBarcodeCaptureSessionDataFromPrefs
import com.zebra.aidatacapturedemo.ui.view.Variables.mainPrimary
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel

/**
 * This file defines the OCRBarcodeResultScreen composable function, which displays the results of
 * OCR and barcode scanning sessions. It includes a header showing the current session index,
 * a list of results with their bounding boxes, and navigation buttons to switch between sessions
 * or return to the scanning screen.
 * The results are loaded from the ViewModel's state or from shared preferences
 * when navigating between sessions.
 */
private const val TAG = "OCRBarcodeResultCapturedScreen"

data class ResultRowData(val text: String, val boundingBox: Rect, val isBarcode: Boolean)

@Composable
fun OCRBarcodeResultScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavHostController,
    innerPadding: PaddingValues,
    context: Context
) {
    val uiState = viewModel.uiState.collectAsState().value
    val resultList = remember { mutableStateListOf<ResultRowData>() }
    val updateResults = remember { mutableStateOf(false) }
    val updateResultsTrigger = remember { mutableStateOf(0) }
    val sessionExpirationDate = remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = true) {
        viewModel.handleBackButton(navController)
    }
    viewModel.updateAppBarTitle(stringResource(R.string.results))

    LaunchedEffect(updateResultsTrigger.value) {
        if(uiState.ocrBarcodeCaptureSessionCount == uiState.ocrBarcodeCaptureSessionIndex){
            val ocrList = uiState.ocrResults.filter { it.text.isNotEmpty() }.map {
                ResultRowData(it.text, it.boundingBox, isBarcode = false)
            }
            val barcodeList = uiState.barcodeResults.filter { it.text.isNotEmpty() }.map {
                ResultRowData(it.text, it.boundingBox, isBarcode = true)
            }
            resultList.clear()
            sessionExpirationDate.value = uiState.extractedExpirationDate
            if (sessionExpirationDate.value == null || sessionExpirationDate.value == "Not found") {
                resultList += ocrList + barcodeList
            }
        } else {
            if (updateResults.value) {
                val sessionJson = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    loadOcrBarcodeCaptureSessionDataFromPrefs(context, uiState.ocrBarcodeCaptureSessionIndex.toString())
                }
                val ocrList = if (!sessionJson?.ocrResults.isNullOrEmpty()) {
                    sessionJson!!.ocrResults.filter { it.text.isNotEmpty() }.map {
                        ResultRowData(it.text, it.boundingBox, isBarcode = false)
                    }
                } else {
                    emptyList()
                }
                val barcodeList = if (!sessionJson?.barcodeResults.isNullOrEmpty()) {
                    sessionJson.barcodeResults.filter { it.text.isNotEmpty() }.map {
                        ResultRowData(it.text, it.boundingBox, isBarcode = true)
                    }
                } else {
                    emptyList()
                }
                resultList.clear()
                sessionExpirationDate.value = sessionJson?.extractedExpirationDate
                if (sessionExpirationDate.value == null || sessionExpirationDate.value == "Not found") {
                    resultList += ocrList + barcodeList
                }
                Log.d(TAG, "loadSessionResults = ${resultList.size}")
                updateResults.value = false
            }
        }
    }
    Column(modifier = Modifier
        .fillMaxSize()
        .padding(
            top = innerPadding.calculateTopPadding(),
            bottom = innerPadding.calculateBottomPadding()
        )) {

        Header(uiState.ocrBarcodeCaptureSessionIndex)

        // Expiration Date Button and Text
        var showExpMessage by remember { mutableStateOf(false) }
        if (sessionExpirationDate.value != null && sessionExpirationDate.value != "Not found") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ButtonWithIconOption(
                        ButtonData(
                            titleId = R.string.expiration_date,
                            color = Variables.mainPrimary,
                            alpha = 1f,
                            enabled = true,
                            onButtonClick = { showExpMessage = !showExpMessage }
                        ),
                        drawableRes = R.drawable.ic_check
                    )
                    
                    if (showExpMessage) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "expiration date is: ${sessionExpirationDate.value}",
                            style = TextStyle(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Variables.mainPrimary,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .background(color = Variables.colorsSurfaceCool)
                    .fillMaxWidth()) {
                items(resultList) { item ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(color = Variables.surfaceDefault)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 32.dp, top = 5.dp, end = 16.dp, bottom = 5.dp)
                        ) {
                            Text(
                                text = item.text,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .weight(1f)
                                    .align(Alignment.CenterVertically)
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                            Image(
                                painter = painterResource(id = R.drawable.ic_location),
                                contentDescription = item.text,
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clickable {
                                        val boundingBoxStr =
                                            "${item.boundingBox.left},${item.boundingBox.top},${item.boundingBox.right},${item.boundingBox.bottom}"
                                        navController.navigate(
                                            "${Screen.SingleResult.route}?text=${item.text}&bbox=$boundingBoxStr&isBarcode=${item.isBarcode}"
                                        )
                                    }
                            )
                        }
                    }
                    Spacer(Modifier.height(1.dp))
                }
            }
        }
        Bottom(viewModel, navController, uiState, updateResultsChanged = {updateResults.value = it}, incrementResultsTrigger = {updateResultsTrigger.value++})
    }
}

@Composable
fun Header(session : Int){
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
                fontSize = 16.sp,
                lineHeight = 20.sp,
                fontFamily = FontFamily(Font(R.font.ibm_plex_sans)),
                fontWeight = FontWeight(500),
                color = Variables.colorsMainSubtle,
                letterSpacing = 0.sp,
            ),
            modifier = Modifier
                .weight(1f)
                .align(Alignment.CenterVertically)
        )
    }
}

@Composable
fun Bottom(viewModel : AIDataCaptureDemoViewModel, navController: NavHostController, uiState: AIDataCaptureDemoUiState,
           updateResultsChanged: (Boolean) -> Unit,
           incrementResultsTrigger: () -> Unit) {

    Row(modifier = Modifier
        .fillMaxWidth()
        .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)
        .border(1.dp, Variables.mainInverse),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Row(modifier = Modifier
            .padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {

            RoundedIconButton(R.drawable.ic_previoussession, onClick = {
                val index = uiState.ocrBarcodeCaptureSessionIndex - 1
                if (index >= 1) {
                    viewModel.updateOcrBarcodeCaptureSessionIndex(index)
                    updateResultsChanged(true)
                    incrementResultsTrigger()
                }
            })
            RoundedIconButton(R.drawable.ic_nextsession, onClick = {
                val index = uiState.ocrBarcodeCaptureSessionIndex + 1
                if (index <= uiState.ocrBarcodeCaptureSessionCount) {
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
                    true,
                    onButtonClick = {
                        navController.navigate(route = Screen.Preview.route) {
                            popUpTo("preview_screen") {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                ),
                R.drawable.ic_scan
            )
        }
    }
}
