package com.zebra.ai.ppod.ui.screens

import android.graphics.Bitmap
import androidx.camera.core.CameraControl
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.zebra.ai.ppod.R
import com.zebra.ai.ppod.repositories.OnPreferenceChangedListener
import com.zebra.ai.ppod.repositories.PreferenceKeys.PREF_KEY_CAPTURE_RESOLUTION
import com.zebra.ai.ppod.ui.components.mainScreen.BorderGlow
import com.zebra.ai.ppod.ui.components.mainScreen.BorderState
import com.zebra.ai.ppod.ui.components.mainScreen.CameraPreview
import com.zebra.ai.ppod.ui.components.mainScreen.CountDownTimer
import com.zebra.ai.ppod.ui.components.mainScreen.CrossHair
import com.zebra.ai.ppod.ui.components.mainScreen.Report
import com.zebra.ai.ppod.ui.components.mainScreen.ReportType
import com.zebra.ai.ppod.ui.components.settingsScreen.ZPreferenceScreen
import com.zebra.ai.ppod.viewmodels.AppViewModel
import com.zebra.ai.ppod.viewmodels.ReportStatus
import kotlinx.coroutines.delay

@Composable
fun MainScreen(appViewModel: AppViewModel) {
    val context = LocalContext.current
    val footerColors = listOf(Color(0xFF2E2E2E), Color(0xFF263238))
    val countdownTimer: Int by appViewModel.countDownTimer.collectAsStateWithLifecycle()
    val results: ReportStatus? by appViewModel.reportStatus.collectAsStateWithLifecycle()
    val borderState: BorderState by appViewModel.borderState.collectAsStateWithLifecycle()
    val resultingImage: Bitmap? by appViewModel.resultingImage.collectAsStateWithLifecycle()
    val torchEnabled: Boolean by appViewModel.torchEnabled.collectAsStateWithLifecycle()
    val zoomState: Float by appViewModel.zoomState.collectAsStateWithLifecycle()
    val licenseValid: Boolean by appViewModel.licenseValid.collectAsStateWithLifecycle()
    val settingsEnabled:Boolean by appViewModel.settingsEnabled.collectAsStateWithLifecycle()
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var cameraControl: CameraControl? by remember { mutableStateOf(null) }
    var captureSize by remember { mutableStateOf(appViewModel.getCaptureSize()) }
    var expiryWarning by remember { mutableStateOf(true) }

    /***********************************************************************************/
    fun takePicture() {
        val imageCapture = imageCapture ?: return
        imageCapture.takePicture(ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                appViewModel.processImage(image)
            }
            override fun onError(exception: ImageCaptureException) {

            }
        })
    }
    /***********************************************************************************/
    LaunchedEffect(torchEnabled,cameraControl) {
        cameraControl?.enableTorch(torchEnabled)
    }

    LaunchedEffect(zoomState,cameraControl) {
        cameraControl?.setLinearZoom(zoomState)
    }

    LaunchedEffect(key1 = true) {
        delay(10000L)
        expiryWarning = false
    }

    DisposableEffect(appViewModel.preferences) {
        val listener = OnPreferenceChangedListener {
            captureSize = appViewModel.getCaptureSize()
        }
        appViewModel.preferences.addPreferenceListener(listOf(PREF_KEY_CAPTURE_RESOLUTION), listener)
        onDispose {
            appViewModel.preferences.removePreferenceListener(listener)
        }
    }

    /***********************************************************************************/
    Column(modifier = Modifier.fillMaxWidth()
        .fillMaxHeight()) {

        // Main Screen
        Box(
            modifier = Modifier.fillMaxWidth()
                .weight(5f)
        ) {
            CameraPreview(
                onImageCapture = { capture -> imageCapture = capture },
                onCameraControl = { control -> cameraControl = control },
                imageCaptureResolution = captureSize,
                onLongPress = { _, _ -> appViewModel.setSettingDisplay(true) }
            )

            // Cross Hair
            CrossHair(modifier = Modifier.align(Center))

            // Captured Image
            resultingImage?.let {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    bitmap = it.asImageBitmap(),
                    contentDescription = "result bitmap",
                    contentScale = ContentScale.Fit
                )
            }

            // Results
            results?.let {
                if (it.reportType != null) {
                    Report(
                        reportType = it.reportType,
                        header = it.header,
                        issues = it.issues,
                        onRetake = it.onRetake,
                        onContinue = it.onContinue,
                        onClose = { appViewModel.clearResults() }
                    )
                }
            }

            // Border Glow
            BorderGlow(borderState = borderState)

            // Countdown Display
            if (countdownTimer != 0) {
                CountDownTimer(
                    timer = countdownTimer,
                    onTick = { appViewModel.countDownTick() })
            }

            if (!licenseValid) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        modifier = Modifier
                            .padding(12.dp)
                            .background(
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .border(width = 1.dp, color = Color.White, shape = RoundedCornerShape(4.dp))
                            .padding(6.dp),
                        text = stringResource(R.string.license_error),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            // Settings Screen
            if (settingsEnabled) {
                ZPreferenceScreen( preferences = appViewModel.preferences, onClose = {appViewModel.setSettingDisplay(false)})
            }

            if (expiryWarning) {
                Report(
                    reportType = ReportType.INFO,
                    header = stringResource(R.string.expiry_warning)
                )
            }
        }

        // Footer
        Row(modifier = Modifier.fillMaxWidth()
            .height(100.dp)
            .background(brush = Brush.verticalGradient( colors = footerColors))
            .padding(start=20.dp,end = 20.dp),
            verticalAlignment = CenterVertically,
            horizontalArrangement =  Arrangement.SpaceAround
        ) {

            // Touch button
            IconButton(
                modifier = Modifier
                    .size( 40.dp)
                    .background(color = if (torchEnabled) Color(0xFF1565C0) else Color(0xFF757575), shape = CircleShape)
                    .clip(CircleShape),
                onClick = { appViewModel.toggleTorchEnabled() }
            ) {
                Image(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .fillMaxHeight(0.5f),
                    painter = painterResource(id = R.drawable.ic_flashlight),
                    contentDescription = "Touch",
                )
            }

            // Camera Shutter
            IconButton(modifier = Modifier.size(64.dp),
                enabled = results == null,
                onClick = { takePicture() }) {
                Image(
                    modifier = Modifier.size(64.dp),
                    painter = painterResource(id = R.drawable.ic_shutter_button),
                    contentDescription = "Shutter",
                )
            }

            // Zoom Button
            IconButton(
                modifier = Modifier
                    .size( 40.dp)
                    .background(color = Color(0xFF757575), shape = CircleShape)
                    .clip(CircleShape),
                enabled = results == null,
                onClick = { appViewModel.performZoomClick()  }
            ) {
                val zoomValue = (1 + (zoomState * 2)).toString()
                val zoomText = zoomValue.replace("\\.?0*$".toRegex(), "") + "x"
                Text(
                    text=zoomText,
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                )
            }
        }
    }
}

