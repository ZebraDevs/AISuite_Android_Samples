package com.zebra.aisuite_quickstart.kotlin.detectors.warehouselocalizer

import android.content.Context
import com.zebra.aisuite_quickstart.utils.AppLog
import androidx.camera.core.ImageAnalysis
import androidx.core.content.ContextCompat
import com.zebra.ai.vision.detector.InferencerOptions
import com.zebra.ai.vision.detector.Localizer
import com.zebra.aisuite_quickstart.kotlin.CameraXLivePreviewActivity
import com.zebra.aisuite_quickstart.utils.CommonUtils
import java.util.concurrent.Executors

class WareHouseLocalizerHandler(
    private val context: Context,
    private val callback: CameraXLivePreviewActivity,
    private val imageAnalysis: ImageAnalysis,
    private val loadingCallback: ((Boolean) -> Unit)? = null
) {
    companion object {
        private const val TAG = "WareHouseLocalizerHandler"
        private const val CAPTURE_SIZE = 1280 // Higher resolution for capture
    }

    private var wareHouseLocalizer: Localizer? = null // For live preview
    var captureLocalizer: Localizer? = null // For capture mode
    private val executor = Executors.newSingleThreadExecutor()
    private val captureExecutor = Executors.newSingleThreadExecutor()
    var wareHouseAnalyzer: WareHouseAnalyzer? = null
    private val mavenModelName = "pallet-and-box-localizer"
    private val sharedPreferences = context.getSharedPreferences(CommonUtils.SETTINGS_PREFS, Context.MODE_PRIVATE)

    init {
        initializeWareHouseLocalizer()
        initializeCaptureLocalizer()
    }

    /**
     * Creates localizer settings with specified input size.
     *
     * @param inputSize The input dimension size for the localizer model.
     * @return Configured Localizer.Settings instance.
     */
    private fun createLocalizerSettings(inputSize: Int): Localizer.Settings {
        return Localizer.Settings(mavenModelName).apply {
            val rpo = arrayOf(
                InferencerOptions.DSP,
                InferencerOptions.CPU,
                InferencerOptions.GPU
            )
            inferencerOptions.runtimeProcessorOrder = rpo
            inferencerOptions.defaultDims.height = inputSize
            inferencerOptions.defaultDims.width = inputSize
        }
    }

    /**
     * Initializes the live preview WareHouse localizer with smaller input size for real-time processing.
     */
    private fun initializeWareHouseLocalizer() {
        val modelInputSize = sharedPreferences.getInt(CommonUtils.PREF_MODEL_INPUT_SIZE, 640)
        try {
            val liveLocalizerSettings = createLocalizerSettings(modelInputSize)
            createWareHouseLocalizer(liveLocalizerSettings)
        } catch (ex: Exception) {
            loadingCallback?.invoke(false)
            AppLog.e(
                TAG,
                "Model Loading: Pallet and Box Localizer returned with exception ${ex.message}"
            )
        }
    }

    /**
     * Initializes the capture localizer with higher resolution settings.
     */
    fun initializeCaptureLocalizer() {
        try {
            val captureLocalizerSettings = createLocalizerSettings(CAPTURE_SIZE)
            createCaptureLocalizer(captureLocalizerSettings)
        } catch (ex: Exception) {
            loadingCallback?.invoke(false)
            AppLog.e(TAG, "Capture localizer initialization failed: ${ex.message}")
        }
    }

    /**
     * Creates the live preview WareHouse Localizer instance.
     * Only notifies loading complete and attaches analyzer when both models are loaded.
     */
    private fun createWareHouseLocalizer(localizerSettings: Localizer.Settings) {
        val startTime = System.currentTimeMillis()
        Localizer.getLocalizer(localizerSettings, executor)
            .thenAccept { localizerInstance ->
                wareHouseLocalizer = localizerInstance

                if (captureLocalizer != null) {
                    loadingCallback?.invoke(true)
                    attachAnalysisAfterModelLoading()
                }

                AppLog.i(TAG, "Pallet and Box Localizer model loading time: ${System.currentTimeMillis() - startTime} milli sec and input size: ${localizerSettings.inferencerOptions.defaultDims.width}")
            }
            .exceptionally { e ->
                loadingCallback?.invoke(false)
                AppLog.e(TAG, "Pallet and Box Localizer model loading failed - ${e.message}")
                null
            }
    }

    /**
     * Creates the capture WareHouse Localizer instance.
     * Only notifies loading complete and attaches analyzer when both models are loaded.
     */
    private fun createCaptureLocalizer(localizerSettings: Localizer.Settings) {
        val startTime = System.currentTimeMillis()
        Localizer.getLocalizer(localizerSettings, captureExecutor)
            .thenAccept { localizerInstance ->
                captureLocalizer = localizerInstance

                if (wareHouseLocalizer != null) {
                    loadingCallback?.invoke(true)
                    attachAnalysisAfterModelLoading()
                }

                AppLog.i(TAG, "Capture Pallet and Box Localizer model loading time: ${System.currentTimeMillis() - startTime} milli sec and input size: ${localizerSettings.inferencerOptions.defaultDims.width}")
            }
            .exceptionally { e ->
                loadingCallback?.invoke(false)
                AppLog.e(TAG, "Capture Pallet and Box Localizer model loading failed: ${e.message}")
                null
            }
    }

    /**
     * Attaches the WareHouseAnalyzer to the ImageAnalysis once both models are loaded.
     */
    private fun attachAnalysisAfterModelLoading() {
        wareHouseAnalyzer = WareHouseAnalyzer(callback, wareHouseLocalizer)
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), wareHouseAnalyzer!!)
    }

    /**
     * Stops the executor services and disposes of both WareHouse Localizer instances,
     * releasing any resources held.
     */
    fun stop() {
        executor.shutdownNow()
        captureExecutor.shutdownNow()
        wareHouseLocalizer?.let {
            it.dispose()
            AppLog.i(TAG, "Live preview Pallet and Box Localizer disposed")
            wareHouseLocalizer = null
        }
        captureLocalizer?.let {
            it.dispose()
            AppLog.i(TAG, "Capture Pallet and Box Localizer disposed")
            captureLocalizer = null
        }
    }

}