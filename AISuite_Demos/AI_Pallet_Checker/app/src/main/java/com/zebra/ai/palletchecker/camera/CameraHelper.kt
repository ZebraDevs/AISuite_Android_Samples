package com.zebra.ai.palletchecker.camera

import android.content.Context
import android.util.Size
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.AspectRatioStrategy.FALLBACK_RULE_AUTO
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.zebra.ai.palletchecker.domain.enums.SettingsMode
import com.zebra.ai.palletchecker.domain.model.AppSettings
import com.zebra.ai.palletchecker.helpers.LOGD
import com.zebra.ai.palletchecker.helpers.LOGE
import com.zebra.ai.palletchecker.helpers.LOGV
import com.zebra.ai.palletchecker.presentation.viewmodel.PROCESS_TYPE
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

@ExperimentalCamera2Interop
class CameraHelper(
    val settings: (SettingsMode) -> AppSettings,
    val context: Context,
    val processType: () -> PROCESS_TYPE,
    val callback: CameraCallback
) {

    private var mPreviewUsecase: Preview? = null
    private var mImgAnalysisUsecase: ImageAnalysis? = null
    private var mImgCaptureUsecase: ImageCapture? = null
    private var mImgCaptureBuilder: ImageCapture.Builder? = null
    private var mCameraProvider: ProcessCameraProvider? = null
    private val TAG = "CameraXUtil"
    private var camera: Camera? = null
    private var customAnalyzer: ImageAnalysis.Analyzer? = null
    private var stopAnalyzer = false
    private var lifecycleOwner: LifecycleOwner? = null
    private var previewView: PreviewView? = null

    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private var useImageAnalysis = false

    fun captureImage() {
        mImgCaptureUsecase?.let {
            it.takePicture(
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        LOGV(TAG, "Capture Image ")
                        if (settings(SettingsMode.SNAP).debugSettings.dumpSnapImagesToFilesystem) {
                            dumpImageToFilesystem(image)
                        }
                        callback.onImageCapture(image)
                    }
                })
        }
    }

    /**
     * Saves the JPEG-based [ImageProxy] to Pictures/PalletCheckerDebug/ with a timestamped filename.
     * Does not close the proxy, leaving that responsibility to the caller.
     */
    private fun dumpImageToFilesystem(image: ImageProxy) {
        try {
            val dir = File(
                context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES),
                "PalletCheckerDebug"
            )
            if (!dir.exists()) dir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
            val file = File(dir, "snap_$timestamp.jpg")

            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            // Rewind so downstream consumers (callback) still see the full buffer
            buffer.rewind()

            FileOutputStream(file).use { it.write(bytes) }
            LOGD(TAG, "dumpImageToFilesystem: saved ${bytes.size} bytes → ${file.absolutePath}")
        } catch (e: Exception) {
            LOGE(TAG, "dumpImageToFilesystem: failed to save image", e)
        }
    }

    fun startLiveCamera(
        previewView: PreviewView,
        lifecycleOwner: LifecycleOwner,
        useImageAnalysis: Boolean = false
    ) {
        this.lifecycleOwner = lifecycleOwner
        this.previewView = previewView
        stopAnalyzer = false
        this.useImageAnalysis = useImageAnalysis
        try {

            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            LOGD(TAG, "startLiveCamera $cameraProviderFuture")
            cameraProviderFuture.addListener({
                mCameraProvider = cameraProviderFuture.get()
                LOGD(TAG, "Get camera provider $mCameraProvider")
                bindAllUsecases(lifecycleOwner)
            }, ContextCompat.getMainExecutor(context))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun needsImageCapture(): Boolean =
        processType() == PROCESS_TYPE.CAPTURE_PALLET_BOX

    private fun bindAllUsecases(lifecycleOwner: LifecycleOwner) {
        mCameraProvider?.let {
            it.unbindAll()
            LOGD(TAG, "bindAllUsecases (needsImageCapture=${needsImageCapture()})")

            bindPreviewUsecase()
            if (needsImageCapture()) {
                bindImageCapture(lifecycleOwner)
            } else {
                mImgCaptureUsecase = null
                mImgCaptureBuilder = null
                LOGD(TAG, "bindAllUsecases: ImageCapture skipped – not needed for ${processType()}")
            }
            bindImageAnalysisUsecase(lifecycleOwner, context)
        }
    }

    private fun bindPreviewUsecase() {
        LOGD(TAG, "bindPreviewUsecase")
        mCameraProvider?.let { provider ->
            mPreviewUsecase?.let { it ->
                provider.unbind(it)
            }
            val previewBuilder = Preview.Builder()
            if (processType() == PROCESS_TYPE.CAPTURE_PALLET_BOX) {
                previewBuilder.setResolutionSelector(getResolutionForMode(SettingsMode.SNAP))
            } else {
                previewBuilder.setResolutionSelector(getResolutionForMode(SettingsMode.WAND))
            }
            mPreviewUsecase = previewBuilder.build()
            mPreviewUsecase?.surfaceProvider = (previewView?.surfaceProvider)
        }

    }

    private fun bindImageCapture(lifecycleOwner: LifecycleOwner) {
        LOGD(TAG, "bindImageCapture")
        mCameraProvider?.let { provider ->
            mImgCaptureUsecase?.let { it ->
                provider.unbind(it)
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val imageCapture = ImageCapture.Builder()
            imageCapture.setResolutionSelector(getResolutionForMode(SettingsMode.SNAP))

            mImgCaptureBuilder = imageCapture

            mImgCaptureUsecase = imageCapture.build()

            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                mPreviewUsecase,
                mImgCaptureUsecase
            )
        }

    }

    private fun bindImageAnalysisUsecase(lifecycleOwner: LifecycleOwner, context: Context) {
        LOGD(TAG, "bindImageAnalysisUsecase")

        mCameraProvider?.let { provider ->
            mImgAnalysisUsecase?.let { it ->
                provider.unbind(it)
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val imgAnalysisBuilder = ImageAnalysis.Builder()
                .setImageQueueDepth(1)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)

            if (processType() == PROCESS_TYPE.CONFIGURE_BARCODE || processType() == PROCESS_TYPE.AUDIT_PARTAIL_PALLET_BOX) {
                imgAnalysisBuilder.setResolutionSelector(getHighResPreviewSelector(SettingsMode.WAND))
            } else {
                imgAnalysisBuilder.setResolutionSelector(getResolutionForMode(SettingsMode.SNAP))
            }

            mImgAnalysisUsecase = imgAnalysisBuilder.build()
            var frame = 0
            if (customAnalyzer != null) {
                mImgAnalysisUsecase?.setAnalyzer(analysisExecutor, customAnalyzer!!)
            } else {
                if (useImageAnalysis) {
                    mImgAnalysisUsecase?.setAnalyzer(analysisExecutor) { proxy ->
                        LOGD(TAG, "Image Analyzer")
                        if (!stopAnalyzer) {
                            frame++
                            callback.onLivePreview(proxy)
                        } else {
                            proxy.close()
                            return@setAnalyzer
                        }
                    }
                } else {
                    LOGD(TAG, "Analyzer is not attached to this usecase")
                }
            }

            val useCases = mutableListOf<androidx.camera.core.UseCase>()
            mPreviewUsecase?.let { useCases.add(it) }
            mImgCaptureUsecase?.let { useCases.add(it) }
            mImgAnalysisUsecase?.let { useCases.add(it) }

            LOGD(TAG, "bindImageAnalysisUsecase: binding ${useCases.size} use cases: ${useCases.map { it::class.simpleName }}")

            camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                *useCases.toTypedArray()
            )

            mImgAnalysisUsecase?.resolutionInfo?.let { info ->
                val rot = info.rotationDegrees
                val sz = info.resolution   // android.util.Size (width × height in sensor orientation)
                LOGD(TAG, "ImageAnalysis negotiated resolution: ${sz.width}×${sz.height}, rotation=$rot")
                callback.onResolutionAvailable(sz.width, sz.height, rot)
            }

            // Also log the Preview's negotiated resolution for diagnostics
            mPreviewUsecase?.resolutionInfo?.let { info ->
                val sz = info.resolution
                val rot = info.rotationDegrees
                LOGD(TAG, "Preview negotiated resolution: ${sz.width}×${sz.height}, rotation=$rot")
                callback.onPreviewResolutionAvailable(sz.width, sz.height, rot)
            }
        }
    }

    /** Unbind only use cases owned by this instance to avoid killing active sessions.
     * Never call unbindAll() since ProcessCameraProvider is a shared singleton.
     */
    fun stop() {
        LOGD(TAG, "stop $mCameraProvider")
        stopAnalyzer = true
        customAnalyzer = null
        useImageAnalysis = false
        mCameraProvider?.let { provider ->
            mPreviewUsecase?.let { provider.unbind(it) }
            mImgCaptureUsecase?.let { provider.unbind(it) }
            mImgAnalysisUsecase?.let { provider.unbind(it) }
        }
        mPreviewUsecase = null
        mImgCaptureUsecase = null
        mImgAnalysisUsecase = null
        analysisExecutor.shutdown()
    }

    /** Rebind only when the camera session is fully operational and Preview is active.
     * Skip if Preview is null, as subsequent startLiveCamera/bindAllUsecases will handle binding.
     */
    fun setCustomAnalyzer(analyzer: ImageAnalysis.Analyzer?) {
        LOGD(TAG, "Set Custom Analyzer1 $analyzer")
        customAnalyzer = analyzer
        if (mCameraProvider != null && mPreviewUsecase != null) {
            bindImageAnalysisUsecase(lifecycleOwner!!, context)
        } else {
            LOGD(TAG, "setCustomAnalyzer: skipping rebind – camera not fully operational " +
                    "(provider=${mCameraProvider != null}, preview=${mPreviewUsecase != null})")
        }

    }



    private fun getResolutionForMode(mode: SettingsMode): ResolutionSelector {
        val appSettings = settings(mode)
        val resolution = appSettings.resolution
        val builder = ResolutionSelector.Builder()
            .setAspectRatioStrategy(
                AspectRatioStrategy(
                    aspectRatioForSettings(appSettings),
                    FALLBACK_RULE_AUTO
                )
            )

        if (resolution == com.zebra.ai.palletchecker.domain.enums.Resolution.MAX) {
            val w = appSettings.effectiveWidth()
            val h = appSettings.effectiveHeight()
            if (w > 0 && h < Int.MAX_VALUE) {
                builder.setResolutionStrategy(
                    ResolutionStrategy(
                        Size(w, h),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                    )
                )
            } else {
                builder.setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
            }
            builder.setAllowedResolutionMode(ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)
        } else {
            builder.setResolutionStrategy(
                ResolutionStrategy(
                    Size(resolution.width, resolution.height),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
        }

        return builder.build()
    }

    /**
     * Resolution selector prioritizing higher resolution over capture rate for better barcode decoding.
     * Used in wanding/barcode modes where ImageAnalysis resolution may exceed the Preview resolution.
     * Requires applying [CameraCallback.onPreviewResolutionAvailable] correction if a resolution mismatch occurs.
     */
    private fun getHighResPreviewSelector(mode: SettingsMode): ResolutionSelector {
        val appSettings = settings(mode)
        val resolution = appSettings.resolution
        val builder = ResolutionSelector.Builder()
            .setAspectRatioStrategy(
                AspectRatioStrategy(
                    aspectRatioForSettings(appSettings),
                    FALLBACK_RULE_AUTO
                )
            )
            .setAllowedResolutionMode(ResolutionSelector.PREFER_HIGHER_RESOLUTION_OVER_CAPTURE_RATE)

        if (resolution == com.zebra.ai.palletchecker.domain.enums.Resolution.MAX) {
            val w = appSettings.effectiveWidth()
            val h = appSettings.effectiveHeight()
            if (w > 0 && h < Int.MAX_VALUE) {
                builder.setResolutionStrategy(
                    ResolutionStrategy(
                        Size(w, h),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                    )
                )
            } else {
                builder.setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
            }
        } else {
            builder.setResolutionStrategy(
                ResolutionStrategy(
                    Size(resolution.width, resolution.height),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
        }

        return builder.build()
    }

    /**
     * Determines the CameraX AspectRatio constant that best matches the selected resolution.
     * Returns RATIO_4_3 for 4:3-like resolutions, RATIO_16_9 for everything else.
     */
    private fun aspectRatioForSettings(appSettings: AppSettings): Int {
        val w = appSettings.effectiveWidth()
        val h = appSettings.effectiveHeight()
        if (w <= 0 || h <= 0 || w == Int.MAX_VALUE) return AspectRatio.RATIO_16_9
        val ratio = w.toFloat() / h.toFloat()
        // 4:3 ≈ 1.333…
        return if (kotlin.math.abs(ratio - 4f / 3f) < 0.05f) {
            AspectRatio.RATIO_4_3
        } else {
            AspectRatio.RATIO_16_9
        }
    }
}

interface CameraCallback {
    fun onImageCapture(proxy: ImageProxy)
    fun onLivePreview(proxy: ImageProxy)
    fun onResolutionAvailable(width: Int, height: Int, rotationDegrees: Int) {}
    fun onPreviewResolutionAvailable(width: Int, height: Int, rotationDegrees: Int) {}
}