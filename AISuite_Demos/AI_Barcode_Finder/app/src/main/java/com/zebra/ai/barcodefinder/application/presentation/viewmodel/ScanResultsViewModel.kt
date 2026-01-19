package com.zebra.ai.barcodefinder.application.presentation.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zebra.ai.barcodefinder.application.data.services.SystemFeedbackService
import com.zebra.ai.barcodefinder.application.data.source.repository.ActionableBarcodeRepository
import com.zebra.ai.barcodefinder.application.domain.enums.ActionState
import com.zebra.ai.barcodefinder.application.domain.enums.ActionType
import com.zebra.ai.barcodefinder.application.domain.enums.BarcodeUserDataKeys
import com.zebra.ai.barcodefinder.application.domain.model.ScanResult
import com.zebra.ai.barcodefinder.application.domain.model.ScanStatus
import com.zebra.ai.barcodefinder.application.domain.services.feedback.BarcodeScanSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * View Model for managing confirmed actionable barcode data.
 */
class ScanResultsViewModel(application: Application): AndroidViewModel(application) {
    private val TAG = "ScanResultsViewModel"

    private val actionableBarcodeJsonRepository = ActionableBarcodeRepository.getInstance(application)

    private val _scanResultsState = MutableStateFlow<List<ScanResult>>(emptyList())
    val scanResultsState = _scanResultsState.asStateFlow()

    val feedbackEngine = SystemFeedbackService(application)

    private val barcodeScanSessionManager = BarcodeScanSessionManager.getInstance(feedbackEngine)

    init {
        observeActionCompletedBarcodes()
    }

    /**
     * Retrieves action completed(confirmed) barcode list and convert them into ScanResult objects
     */
    fun observeActionCompletedBarcodes() {
        Log.d(TAG, "Retrieving the list of action completed barcodes")
        viewModelScope.launch {
            actionableBarcodeJsonRepository.actionCompletedBarcodes.collect { actionCompletedBarcodeList ->
                val scanResultsList = mutableListOf<ScanResult>()
                actionCompletedBarcodeList.forEach { actionCompletedBarcode ->
                    val icon = actionCompletedBarcode.getIconForState(ActionState.STATE_ACTION_NOT_COMPLETED)
                    val status = when (actionCompletedBarcode.actionType) {
                        ActionType.TYPE_RECALL -> ScanStatus.RecallConfirmed(icon)
                        ActionType.TYPE_CONFIRM_PICKUP -> ScanStatus.PickupConfirmed(icon)
                        ActionType.TYPE_QUANTITY_PICKUP -> {
                            val replenish =
                                actionCompletedBarcode.getUserDataValue(BarcodeUserDataKeys.REPLENISH_STOCK)
                                    ?.toBoolean() ?: false
                            val pickedQuantity =
                                actionCompletedBarcode.getUserDataValue(BarcodeUserDataKeys.PICKED_QUANTITY)
                                    ?.toIntOrNull() ?: 0
                            val quantity = actionCompletedBarcode.quantity
                            ScanStatus.QuantityPicked(quantity, pickedQuantity, replenish, icon)
                        }

                        else -> ScanStatus.NoActionNeeded(icon)
                    }
                    scanResultsList.add(
                        ScanResult(
                            productName = actionCompletedBarcode.productName,
                            barcode = actionCompletedBarcode.barcodeData,
                            status = status,
                            additionalInfo = actionCompletedBarcode.getUserDataValue(
                                BarcodeUserDataKeys.RESULT
                            )
                        )
                    )
                }

                // emit the updated list of scanResults
                _scanResultsState.value = scanResultsList
            }
        }
    }

    /**
     * Clear out the action completed barcode list
     */
    fun clearBarcodeResults() {
        actionableBarcodeJsonRepository.clearActionCompletedBarcodes()
        barcodeScanSessionManager.resetSessionState()
    }
}