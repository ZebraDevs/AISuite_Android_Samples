package com.zebra.ai.palletchecker.helpers

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.zebra.ai.vision.analyzer.tracking.EntityTrackerAnalyzer
import com.zebra.ai.vision.detector.AIVisionSDKException
import com.zebra.ai.vision.detector.ImageData
import com.zebra.ai.vision.entity.BarcodeEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

class BarcodeProcessHelper(
    val scope: CoroutineScope,
) {
    val TAG = "BarcodeHelper"
    private val executor = Executors.newSingleThreadExecutor()

    var barcodeResult = MutableSharedFlow<List<BarcodeEntity>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    fun analyze(image: ImageProxy) {
        scope.launch(Dispatchers.IO) {
            try {
                val decoder = ModelsStorage.getBarcodeDecoder() ?: run {
                    image.close()
                    return@launch
                }
                val results = decoder.process(ImageData.fromImageProxy(image)).await()
                LOGD(TAG, "Decode Success ${results?.size}")
                barcodeResult.tryEmit(results ?: emptyList())
            } catch (e: AIVisionSDKException) {
                LOGE(TAG, "AISDK Exception ${e.message}")
            } finally {
                image.close()
            }
        }
    }

    fun clear() {
        barcodeResult.tryEmit(emptyList())
    }

    fun createEntityBarcodeAnalyzer(): EntityTrackerAnalyzer {
        return EntityTrackerAnalyzer(
            listOf(ModelsStorage.getConfigBarcodeDecoder()),
            ImageAnalysis.COORDINATE_SYSTEM_ORIGINAL,
            executor
        ) { entities ->
            val barcodeList = mutableListOf<BarcodeEntity>()
            val ent = entities.getValue(ModelsStorage.getConfigBarcodeDecoder()!!)
            if (ent?.isNotEmpty() == true) {
                ent.forEach {
                    if (it is BarcodeEntity) barcodeList.add(it)
                }
                barcodeResult.tryEmit(barcodeList)
            } else {
                barcodeResult.tryEmit(emptyList())
            }
        }
    }
}
