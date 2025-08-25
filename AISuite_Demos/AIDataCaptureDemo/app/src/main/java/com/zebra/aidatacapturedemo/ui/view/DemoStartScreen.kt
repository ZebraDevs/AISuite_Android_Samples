package com.zebra.aidatacapturedemo.ui.view

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
import androidx.navigation.NavController
import com.zebra.ai.vision.detector.InferencerOptions
import com.zebra.aidatacapturedemo.R
import com.zebra.aidatacapturedemo.data.AIDataCaptureDemoUiState
import com.zebra.aidatacapturedemo.ui.view.Variables.mainDisabled
import com.zebra.aidatacapturedemo.ui.view.Variables.mainPrimary
import com.zebra.aidatacapturedemo.viewmodel.AIDataCaptureDemoViewModel

@Composable
fun DemoStartScreen(
    viewModel: AIDataCaptureDemoViewModel,
    navController: NavController,
    innerPadding: PaddingValues
) {
    val uiState = viewModel.uiState.collectAsState().value
    viewModel.updateAppBarTitle(stringResource(getDemoTitle(uiState.usecaseSelected)))

    // Intercept back presses on this screen
    BackHandler(enabled = true) {
        viewModel.handleBackButton(navController)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(color = Variables.surfaceDefault)
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(innerPadding)
    ) {

        // Icon
        Spacer(Modifier.height(30.dp))
        UsecaseIcon(selectedUsecase = uiState.usecaseSelected)

        Spacer(Modifier.height(30.dp))
        Column(
            modifier = Modifier
                .wrapContentHeight()
                .padding(16.dp)
        ) {
            // Heading:
            val titleStringId = getSettingHeading(uiState.usecaseSelected)
            if (titleStringId == 0) {
                TextviewBold(info = "")
            } else {
                TextviewBold(info = stringResource(titleStringId))
            }

            // Model Input Details:
            Spacer(modifier = Modifier.height(14.dp))
            Row {
                TextviewBold(info = "\u2022   Model Input:")
                TextviewNormal(info = "  ${viewModel.getInputSizeSelected()} x ${viewModel.getInputSizeSelected()}")
            }

            // Resolution Details:
            Spacer(modifier = Modifier.height(10.dp))
            Row {
                TextviewBold(info = "\u2022   Resolution:")
                val resolution = getSelectedResolution(viewModel.getSelectedResolution())
                TextviewNormal(info = "  $resolution")
            }

            // Inference Type Details:
            Spacer(modifier = Modifier.height(10.dp))
            Row {
                TextviewBold(info = "\u2022   Inference (processor) Type:")
                val inferenceType = getSelectedInferenceType(viewModel.getProcessorSelectedIndex())
                TextviewNormal(info = "  $inferenceType")
            }

            // Restore Clickable Text:
            Spacer(modifier = Modifier.height(34.dp))
            Text(
                text = stringResource(R.string.restore_default),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clickable {
                        viewModel.restoreDefaultSettings()
                        viewModel.applySettings()
                    },
                style = TextStyle(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontFamily = FontFamily(Font(R.font.ibm_plex_sans_medium)),
                    fontWeight = FontWeight(500),
                    color = Variables.borderPrimaryMain,
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxHeight()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            StartScanButton(uiState, navController)
        }
    }
}

private fun getSelectedInferenceType(processorSelectedIndex: Int): String {
    return when (processorSelectedIndex) {
        0 -> {
            "DSP"
        }

        1 -> {
            "GPU"
        }

        2 -> {
            "CPU"
        }

        else -> {
            TODO("Unknown Inference Type found $processorSelectedIndex")
        }
    }
}

@Composable
private fun getSelectedResolution(resolutionSelectedIndex: Int): String {
    return when (resolutionSelectedIndex) {
        0 -> {
            "${stringResource(R.string.resolution_size_1280)}"
        }

        1 -> {
            "${stringResource(R.string.resolution_size_1920)}"
        }

        2 -> {
            "${stringResource(R.string.resolution_size_2688)}"
        }

        3 -> {
            "${stringResource(R.string.resolution_size_3840)}"
        }

        else -> {
            TODO("Unknown Resolution found $resolutionSelectedIndex")
        }
    }
}

@Composable
fun UsecaseIcon(selectedUsecase: String) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .width(102.dp)
            .height(102.dp)
            .background(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                brush = Brush.verticalGradient(
                    colors = listOf(
                        getIconMainColor(selectedUsecase),
                        getIconSecondaryColor(selectedUsecase)
                    )
                )
            )
            .padding(
                start = 13.33333.dp,
                top = 13.33333.dp,
                end = 13.33333.dp,
                bottom = 13.33333.dp
            )
    ) {
        val iconId = getIconId(selectedUsecase)
        if (iconId != 0) {
            Image(
                painter = painterResource(id = iconId),
                contentDescription = "image description",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .padding(2.66667.dp)
                    .width(60.dp)
                    .height(60.dp)
            )
        }
    }
}

@Composable
fun StartScanButton(uiState: AIDataCaptureDemoUiState, navController: NavController) {
    if (uiState.modelDemoReady) {
        ButtonOption(ButtonData(R.string.start_scan, mainPrimary, 1.0F, true, onButtonClick = {
            navController.navigate(route = Screen.Preview.route)
        }))
    } else {
        ButtonOption(ButtonData(R.string.start_scan, mainDisabled, 1.0F, false, onButtonClick = {
        }))
    }
}