package com.zebra.ai.barcodebatchinventory.application.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zebra.ai.barcodebatchinventory.application.data.services.SystemFeedbackService
import com.zebra.ai.barcodebatchinventory.application.data.source.repository.CapturedBarcodeRepository
import com.zebra.ai.barcodebatchinventory.application.domain.model.ScanResult
import com.zebra.ai.barcodebatchinventory.application.domain.services.feedback.BarcodeScanSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for grouped batch inventory scan results.
 */
class ScanResultsViewModel(application: Application): AndroidViewModel(application) {
    private val TAG = "ScanResultsViewModel"

    private val capturedBarcodeRepository = CapturedBarcodeRepository.getInstance(application)

    private val _scanResultsState = MutableStateFlow<List<ScanResult>>(emptyList())
    val scanResultsState = _scanResultsState.asStateFlow()

    private val feedbackEngine = SystemFeedbackService(application)
    private val barcodeScanSessionManager = BarcodeScanSessionManager.getInstance(feedbackEngine)

    init {
        observeCapturedBarcodes()
    }

    private fun observeCapturedBarcodes() {
        Log.d(TAG, "Retrieving the list of captured barcodes")
        viewModelScope.launch {
            capturedBarcodeRepository.capturedBarcodes.collect { capturedBarcodes ->
                val barcodeCounts = linkedMapOf<String, Int>()
                capturedBarcodes.forEach { barcodeData ->
                    barcodeCounts[barcodeData] = (barcodeCounts[barcodeData] ?: 0) + 1
                }

                val groupedResults = barcodeCounts.map { (barcodeData, count) ->
                    ScanResult(
                        barcode = barcodeData,
                        quantity = count
                    )
                }
                Log.d(
                    TAG,
                    "BatchResultsGrouped: capturedTotal=${capturedBarcodes.size} " +
                            "groupedItems=${groupedResults.size}"
                )
                _scanResultsState.value = groupedResults
            }
        }
    }

    fun clearBarcodeResults() {
        val currentResults = _scanResultsState.value
        Log.d(
            TAG,
            "BatchResultsClear: groupedItems=${currentResults.size} " +
                    "totalQuantity=${currentResults.sumOf { it.quantity }}"
        )
        capturedBarcodeRepository.clearCapturedBarcodes()
        barcodeScanSessionManager.resetSessionState()
        Log.d(TAG, "BatchResultsClear: repository and feedback session reset")
    }
}
