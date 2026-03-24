package com.zebra.aisuite_quickstart.kotlin.detectors.warehouselocalizer

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.core.content.ContextCompat
import com.zebra.ai.vision.detector.AIVisionSDK
import com.zebra.ai.vision.detector.InferencerOptions
import com.zebra.ai.vision.detector.Localizer
import com.zebra.aisuite_quickstart.kotlin.CameraXLivePreviewActivity
import java.util.concurrent.Executors
import java.util.function.Consumer
import java.util.function.Function

class WareHouseLocalizerHandler (
    private val context: Context,
    private val callback: CameraXLivePreviewActivity,
    private val imageAnalysis: ImageAnalysis
)  {
    private val TAG = "WareHouseLocalizerHandler"
    private var localizer: Localizer? = null
    private val executor = Executors.newSingleThreadExecutor()
    private var wareHouseAnalyzer: WareHouseAnalyzer? = null
    private val mavenModelName = "warehouse-localizer"

    init {
        initializeLocalizer()
    }

    private fun initializeLocalizer() {
        try {
            val mStart = System.currentTimeMillis()
            val locSettings = Localizer.Settings(mavenModelName)
            val diff = System.currentTimeMillis() - mStart
            Log.e("Profiling", "WareHouse Localizer.settings() obj creation time =$diff milli sec")

            val rpo = arrayOfNulls<Int>(3)
            rpo[0] = InferencerOptions.DSP
            rpo[1] = InferencerOptions.CPU
            rpo[2] = InferencerOptions.GPU

            locSettings.inferencerOptions.runtimeProcessorOrder = rpo
            locSettings.inferencerOptions.defaultDims.height = 640
            locSettings.inferencerOptions.defaultDims.width = 640

            val start = System.currentTimeMillis()
            Localizer.getLocalizer(locSettings, executor)
                .thenAccept(Consumer { localizerInstance: Localizer? ->
                    localizer = localizerInstance
                    Log.e("Profiling", "WareHouse Localizer(locSettings) obj creation / model loading time =" + (System.currentTimeMillis() - start) + " milli sec")
                    val classes = localizer?.supportedClasses
                    val list = listOf(classes)
                    Log.e(TAG, "classes=$list")
                    wareHouseAnalyzer = WareHouseAnalyzer(callback, localizer)
                    imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), wareHouseAnalyzer!!)
                }).exceptionally(Function { e: Throwable? ->
                    Log.e(TAG, "Error occurred :" + e!!.message)
                    null
                })
            Log.e("Profiling", "WareHouse Localizer model archive info=" + AIVisionSDK.getInstance(context).getModelArchiveInfo(mavenModelName))

        } catch (e: Exception) {
            Log.e(TAG, "Fatal error: load failed - " + e.message)
        }
    }

    fun stop() {
        executor.shutdownNow()
        localizer?.let {
            it.dispose()
            Log.d(TAG, "WareHouse Localizer is disposed")
            localizer = null
        }
    }
    fun getWareHouseAnalyzer(): WareHouseAnalyzer? {
        return wareHouseAnalyzer
    }
}