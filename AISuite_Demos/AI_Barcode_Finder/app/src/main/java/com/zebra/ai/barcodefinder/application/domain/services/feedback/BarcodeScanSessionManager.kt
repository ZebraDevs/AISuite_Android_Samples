package com.zebra.ai.barcodefinder.application.domain.services.feedback

import android.util.Log
import com.zebra.ai.barcodefinder.application.domain.model.IdentifiedBarcode
import com.zebra.ai.barcodefinder.application.domain.services.feedback.FeedbackService
import com.zebra.ai.barcodefinder.sdkcoordinator.model.FeedbackType
import com.zebra.ai.vision.entity.BarcodeEntity
import com.zebra.ai.vision.entity.Entity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

/**
 * Manages barcode detection sessions.
 * Pure Domain Class: No Context, No Android UI dependencies.
 */
class BarcodeScanSessionManager private constructor(
    private val feedbackService: FeedbackService, // Injected Dependency
    private val coroutineContext: CoroutineContext = Dispatchers.Default
) {
    private val identifiedBarcodeMap = ConcurrentHashMap<String, IdentifiedBarcode>()
    private val idGenerator = AtomicInteger(1)
    private val TAG = "BarcodeScanSession"

    private var currentFeedbackSettings: FeedbackType? = null

    init {
        Log.d(TAG, "BarcodeScanSessionManager initialized.")
    }

    companion object {
        @Volatile
        private var INSTANCE: BarcodeScanSessionManager? = null

        /**
         * Returns the Singleton instance.
         * Note: You must pass the implementation of FeedbackService from the UI/Data layer.
         */
        fun getInstance(feedbackService: FeedbackService): BarcodeScanSessionManager {
            return INSTANCE ?: synchronized(this) {
                val instance = INSTANCE
                if (instance != null) {
                    instance
                } else {
                    Log.d("BarcodeScanSessionManager", "Creating new singleton instance.")
                    val newInstance = BarcodeScanSessionManager(feedbackService)
                    INSTANCE = newInstance
                    newInstance
                }
            }
        }
    }

    fun bind(feedbackType: FeedbackType) {
        Log.d(TAG, "Binding with feedback settings: Audio=${feedbackType.audio}, Haptics=${feedbackType.haptics}")
        this.currentFeedbackSettings = feedbackType
        // Delegate to the service
        feedbackService.resume()
    }

    fun unbind() {
        Log.d(TAG, "Unbinding. Pausing feedback.")
        currentFeedbackSettings = null
        // Delegate to the service
        feedbackService.pause()
    }

    suspend fun processBarcodes(entities: List<Entity>) {
        if (currentFeedbackSettings == null) {
            Log.v(TAG, "Skipping: session not bound.") // Uncomment for verbose logging
            return
        }

        try {
            withContext(coroutineContext) {
                if (!isActive) return@withContext

                entities.forEach { entity ->
                    if (entity is BarcodeEntity) {
                        val barcodeValue = entity.value
                        if (!barcodeValue.isNullOrEmpty()) {
                            identifiedBarcodeMap.computeIfAbsent(barcodeValue) { key ->

                                // Trigger feedback via the Interface
                                currentFeedbackSettings?.let { settings ->
                                    feedbackService.triggerFeedback(settings)
                                }

                                val newId = idGenerator.getAndIncrement()
                                Log.d(TAG, "New unique barcode: '$key', ID: $newId.")

                                IdentifiedBarcode(id = newId, value = key)
                            }
                        }
                    }
                    ensureActive()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (t: Throwable) {
            Log.e(TAG, "Error during barcode processing.", t)
        }
    }

    fun getUniqueBarcodes(): List<IdentifiedBarcode> {
        return identifiedBarcodeMap.values.toList()
    }

    fun destroy() {
        Log.d(TAG, "Destroying Manager and releasing service.")
        feedbackService.release()
        INSTANCE = null // Clean up singleton reference
    }

    // Optional: Exposed for testing or reset logic
    fun resetSessionState() {
        idGenerator.set(1)
        identifiedBarcodeMap.clear()
    }
}