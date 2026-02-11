package com.zebra.ai.ppod.ui.components.mainScreen

import android.util.Log
import android.util.Size
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.MeteringPoint
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

private const val TAG = "CameraPreview"

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA,
    imageCaptureResolution: Size? = null,
    onImageCapture: ((ImageCapture) -> Unit)? = null,
    onAnalyze: ((ImageProxy) -> Unit)? = null,
    imageAnalysisResolution: Size? = null,
    onCameraControl: (CameraControl) -> Unit = { },
    onCameraInfo: (CameraInfo) -> Unit = { },
    onTap: ((meteringPoint: MeteringPoint, tappedOffset: Offset) -> Unit)? = null,
    onLongPress: ((meteringPoint: MeteringPoint, tappedOffset: Offset) -> Unit)? = null,
    onDoubleTap: ((meteringPoint: MeteringPoint, tappedOffset: Offset) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    val previewView = remember { PreviewView(context) }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }

    LaunchedEffect(Unit) {
        cameraProvider = ProcessCameraProvider.getInstance(context).get()
    }

    DisposableEffect(cameraProvider, cameraSelector, imageCaptureResolution, onAnalyze, imageAnalysisResolution) {
        val provider = cameraProvider ?: return@DisposableEffect onDispose {}

        val preview = Preview.Builder().build().apply {
            surfaceProvider = previewView.surfaceProvider
        }

        val useCases = buildList {
            add(preview)

            onImageCapture?.let { onCapture ->
                val builder = ImageCapture.Builder()
                imageCaptureResolution?.let { resolution ->
                    val selector = ResolutionSelector.Builder()
                        .setResolutionStrategy(ResolutionStrategy(resolution, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                        .build()
                    builder.setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    builder.setResolutionSelector(selector)
                }
                val imageCapture = builder.build()
                onCapture(imageCapture)
                add(imageCapture)
            }

            onAnalyze?.let { onAnalysis ->
                val builder = ImageAnalysis.Builder()
                imageAnalysisResolution?.let { resolution ->
                    val selector = ResolutionSelector.Builder()
                        .setResolutionStrategy(ResolutionStrategy(resolution, ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER))
                        .build()
                    builder.setResolutionSelector(selector)
                }
                val imageAnalysis = builder
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                imageAnalysis.setAnalyzer(mainExecutor) { image ->
                    onAnalysis(image)
                    image.close()
                }
                add(imageAnalysis)
            }
        }

        try {
            val camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                *useCases.toTypedArray()
            )
            onCameraControl(camera.cameraControl)
            onCameraInfo(camera.cameraInfo)
        } catch (ex: Exception) {
            Log.e(TAG, "Failed to bind camera use cases", ex)
        }

        onDispose {
            provider.unbindAll()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                previewView.apply {
                    scaleType = PreviewView.ScaleType.FIT_CENTER
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(onTap, onLongPress, onDoubleTap) {
                    detectTapGestures(
                        onLongPress = { offset ->
                            onLongPress?.let {
                                val point = previewView.meteringPointFactory.createPoint(offset.x, offset.y)
                                it(point, offset)
                            }
                        },
                        onDoubleTap = { offset ->
                            onDoubleTap?.let {
                                val point = previewView.meteringPointFactory.createPoint(offset.x, offset.y)
                                it(point, offset)
                            }
                        },
                        onTap = { offset ->
                            onTap?.let {
                                val point = previewView.meteringPointFactory.createPoint(offset.x, offset.y)
                                it(point, offset)
                            }
                        }
                    )
                }
        )
    }
}
