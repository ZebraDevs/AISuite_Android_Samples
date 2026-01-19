// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.application.data.source.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.Color
import com.zebra.ai.barcodefinder.R
import com.zebra.ai.barcodefinder.application.domain.enums.ActionState
import com.zebra.ai.barcodefinder.application.domain.enums.ActionType
import com.zebra.ai.barcodefinder.application.data.source.storage.ActionableBarcodeJsonStorage
import com.zebra.ai.barcodefinder.application.domain.model.ActionableBarcode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository for managing actionable barcodes and their states in the app.
 *
 * Responsibilities:
 * - Maintains lists and maps of actionable barcodes for tracking and configuration.
 * - Handles completed actions and provides observable state for UI updates.
 * - Manages barcode icons and background colors for UI overlays.
 * - Loads and saves barcode configurations to JSON for persistence.
 * - Provides utility methods for barcode lookup, configuration, and state management.
 *
 * Usage:
 * - Singleton pattern: use getInstance(context) to obtain the repository.
 * - Interacts with ViewModels and UI to provide barcode data and state.
 * - Note: JSON persistence is handled internally by this repository.
 */
class ActionableBarcodeRepository private constructor(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: ActionableBarcodeRepository? = null

        /**
         * Returns the singleton instance of the repository, initializing it if necessary.
         */
        fun getInstance(context: Context): ActionableBarcodeRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ActionableBarcodeRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    // Maps and lists for tracking actionable barcodes and their configuration states
    private val _actionableBarcodeMapForTracking = mutableMapOf<String, ActionableBarcode>()
    private val _configuredActionableBarcodeMap = mutableMapOf<String, ActionableBarcode>()
    private val _configuredActionableBarcodeList = mutableListOf<ActionableBarcode>()

    // List and observable state for completed actions
    private val _actionCompletedBarcodeList = mutableListOf<ActionableBarcode>()
    private val _actionCompletedBarcodes = MutableStateFlow<List<ActionableBarcode>>(emptyList())
    val actionCompletedBarcodes: StateFlow<List<ActionableBarcode>> =
        _actionCompletedBarcodes.asStateFlow()


    // Observable state for configured barcodes
    val liveConfiguredActionableBarcodes: MutableStateFlow<List<ActionableBarcode>> =
        MutableStateFlow(emptyList())
    private val actionTypeIcons = mutableMapOf<ActionType, Bitmap>()

    private val actionableBarcodeJsonStorage = ActionableBarcodeJsonStorage(context)
    /**
     * Initializes icons and loads barcode configurations from JSON.
     * Populates tracking and configuration maps/lists for use in the app.
     */
    init {
        initializeIcons(context)
        // Initialize barcodeMap and configuredBarcodeMap from JSON configurations
        val configurations = actionableBarcodeJsonStorage.loadBarcodes()
        _actionableBarcodeMapForTracking.clear()
        _configuredActionableBarcodeMap.clear()

        configurations.forEach { actionableBarcode ->
            val barcode = ActionableBarcode(
                barcodeData = actionableBarcode.barcodeData,
                productName = actionableBarcode.productName,
                actionType = actionableBarcode.actionType,
                actionState = ActionState.STATE_ACTION_NOT_COMPLETED,
                quantityValue = actionableBarcode.quantity
            )
            // Set actionStateIcons for each barcode
            barcode.setActionStateIcons(
                getActionStateIconsWithDefaults(
                    barcode.actionState,
                    barcode.actionType,
                )
            )
            _actionableBarcodeMapForTracking[barcode.barcodeData] = barcode
        }
        loadConfiguredActionableBarcodes()
    }

    /**
     * Returns an actionable barcode from the tracking list, or creates a default if not found.
     */
    fun getActionableBarcodeFromTrackList(barcodeData: String): ActionableBarcode {
        return _actionableBarcodeMapForTracking[barcodeData] ?: getActionableForData(barcodeData)
    }

    /**
     * Adds a barcode to the completed actions list and updates observable state.
     * Optionally attaches user data to the barcode.
     */
    fun addActionCompletedBarcode(barcode: ActionableBarcode, userData: Map<String, Any>? = null) {
        barcode.actionState = ActionState.STATE_ACTION_COMPLETED

        userData?.forEach { (key, value) -> barcode.setUserData(key, value.toString()) }

        // Remove if already exists and add to front
        _actionCompletedBarcodeList.removeAll { it.barcodeData == barcode.barcodeData }
        _actionCompletedBarcodeList.add(0, barcode)

        _actionCompletedBarcodes.value = _actionCompletedBarcodeList.toList()
    }

    /**
     * Returns a copy of the completed barcodes list.
     */
    fun getActionCompletedBarcodes(): List<ActionableBarcode> {
        return _actionCompletedBarcodeList.toList()
    }

    /**
     * Clears the completed barcodes list and resets their states.
     */
    fun clearActionCompletedBarcodes() {
        _actionCompletedBarcodeList.forEach { barcode ->
            barcode.actionState = ActionState.STATE_ACTION_NOT_COMPLETED
        }
        _actionCompletedBarcodeList.clear()
        _actionCompletedBarcodes.value = emptyList()
    }

    /**
     * Loads configured actionable barcodes from JSON and updates internal lists/maps and observable state.
     */
    fun loadConfiguredActionableBarcodes() {
        val configurations = actionableBarcodeJsonStorage.loadBarcodes()
        _configuredActionableBarcodeList.clear()
        _configuredActionableBarcodeMap.clear()
        configurations.forEach { actionableBarcode ->
            val barcode = ActionableBarcode(
                barcodeData = actionableBarcode.barcodeData,
                productName = actionableBarcode.productName,
                actionType = actionableBarcode.actionType,
                actionState = ActionState.STATE_ACTION_NOT_COMPLETED,
                quantityValue = actionableBarcode.quantity
            )
            // Set actionStateIcons for each barcode
            barcode.setActionStateIcons(
                getActionStateIconsWithDefaults(
                    barcode.actionState,
                    barcode.actionType,
                )
            )
            _configuredActionableBarcodeList.add(barcode)
            _configuredActionableBarcodeMap[barcode.barcodeData] = barcode
        }

        liveConfiguredActionableBarcodes.value = _configuredActionableBarcodeList.toList()
    }

    /**
     * Removes a barcode from the configuration list and updates observable state.
     */
    fun removeActionableBarcodeFromConfigList(actionableBarcode: ActionableBarcode) {
        _configuredActionableBarcodeMap.remove(actionableBarcode.barcodeData)
        _configuredActionableBarcodeList.removeAll { it.barcodeData == actionableBarcode.barcodeData }

        liveConfiguredActionableBarcodes.value = _configuredActionableBarcodeList.toList()
    }

    /**
     * Returns the current live list of configured actionable barcodes.
     */
    fun getLiveConfiguredBarcodes(): StateFlow<List<ActionableBarcode>> {
        return liveConfiguredActionableBarcodes.asStateFlow()
    }

    /**
     * Updates a barcode in the configuration list and updates observable state.
     */
    fun updateActionableBarcodeInConfigList(actionableBarcode: ActionableBarcode) {
        _configuredActionableBarcodeList.removeAll { it.barcodeData == actionableBarcode.barcodeData }

        // Ideally, we should update the existing barcode in place.
        val newBarcode = ActionableBarcode(
            barcodeData = actionableBarcode.barcodeData,
            productName = actionableBarcode.productName,
            actionType = actionableBarcode.actionType,
            actionState = ActionState.STATE_ACTION_NOT_COMPLETED,
            quantityValue = actionableBarcode.quantity
        )
        newBarcode.setActionStateIcons(
            getActionStateIconsWithDefaults(
                actionableBarcode.actionState,
                actionableBarcode.actionType,
            )
        )

        _configuredActionableBarcodeList.add(0, newBarcode)
        _configuredActionableBarcodeMap[newBarcode.barcodeData] = newBarcode

        liveConfiguredActionableBarcodes.value = _configuredActionableBarcodeList.toList()
    }

    /**
     * Clears all configured actionable barcodes and updates observable state.
     */
    fun clearAllConfiguredActionableBarcodes() {
        _configuredActionableBarcodeMap.clear()
        _configuredActionableBarcodeList.clear()

        liveConfiguredActionableBarcodes.value = emptyList()
    }

    /**
     * Applies the current configuration, resets completed actions, and persists to JSON.
     */
    fun applyConfigurations() {
        _actionCompletedBarcodeList.clear()
        _actionCompletedBarcodes.value = emptyList()
        _actionableBarcodeMapForTracking.clear()
        _actionableBarcodeMapForTracking.putAll(_configuredActionableBarcodeMap)
        actionableBarcodeJsonStorage.saveBarcodes( _configuredActionableBarcodeList)
    }

    /**
     * Returns a barcode from the live configured list by barcode data, or null if not found.
     */
    fun getLiveConfiguredActionableBarcodeFromConfigList(barcodeData: String): ActionableBarcode? {
        return liveConfiguredActionableBarcodes.value.find { it.barcodeData == barcodeData }
    }



    private fun initializeIcons(context: Context) {
        ActionType.entries.forEach { actionType ->
            val iconRes = getStatusIconRes(actionType)
            val icon = BitmapFactory.decodeResource(context.resources, iconRes)
            actionTypeIcons[actionType] = icon
        }
    }


    /**
     * Returns the drawable resource ID for a status icon for a given action type.
     */
    private fun getStatusIconRes(actionType: ActionType): Int {
        return when (actionType) {
            ActionType.TYPE_RECALL -> R.drawable.recall_marker
            ActionType.TYPE_CONFIRM_PICKUP -> R.drawable.confirm_pickup_marker
            ActionType.TYPE_QUANTITY_PICKUP -> R.drawable.quantity_marker
            ActionType.TYPE_ACTION_COMPLETE -> R.drawable.complete_action_marker
            ActionType.TYPE_NO_ACTION -> R.drawable.no_action_marker
            ActionType.TYPE_NONE -> R.drawable.no_decode_marker
        }
    }

    /**
     * Returns a map of action state icons for a given action state and action type.
     * The `_actionTypeIcons` map is passed as a parameter to keep the repository's state encapsulated.
     */
    private fun getActionStateIconsWithDefaults(
        actionState: ActionState,
        actionType: ActionType
    ): Map<ActionState, Bitmap> {
        val actionStateIcons = mutableMapOf<ActionState, Bitmap>()

        actionTypeIcons[ActionType.TYPE_ACTION_COMPLETE]?.let {
            actionStateIcons[ActionState.STATE_ACTION_COMPLETED] = it
        }
        actionTypeIcons[actionType]?.let {
            actionStateIcons[actionState] = it
        }

        return actionStateIcons
    }

    /**
     * Returns a default actionable barcode for a given barcode data string.
     * The `_actionTypeIcons` map is passed as a parameter to set the action state icons.
     */
    fun getActionableForData(
        barcodeData: String,
    ): ActionableBarcode {
        return ActionableBarcode(
            barcodeData = barcodeData,
            productName = "",
            actionType = ActionType.TYPE_NO_ACTION,
            actionState = ActionState.STATE_ACTION_NOT_COMPLETED
        ).apply {
            setActionStateIcons(
                getActionStateIconsWithDefaults(actionState, actionType)
            )
        }
    }

    /**
     * Returns an empty barcode with TYPE_NONE action type for unrecognized barcodes.
     * The `_actionTypeIcons` map is passed as a parameter to set the action state icons.
     */
    fun getEmptyBarcode(): ActionableBarcode {
        return ActionableBarcode(
            barcodeData = "",
            productName = "",
            actionType = ActionType.TYPE_NONE,
            actionState = ActionState.STATE_ACTION_NOT_COMPLETED
        ).apply {
            setQuantity(0)
            setActionStateIcons(
                getActionStateIconsWithDefaults(actionState, actionType)
            )
        }
    }

    /**
     * Gets the icon bitmap for a specific action type.
     */
    fun getIconForActionType(actionType: ActionType): Bitmap? {
        return actionTypeIcons[actionType]
    }


    /**
     * Returns the background color for a given action type for UI overlays.
     * This is a static method and can be called directly using the class name.
     */
    fun getBackgroundColorForActionType(actionType: ActionType): Color {
        return when (actionType) {
            ActionType.TYPE_RECALL -> Color.Red.copy(alpha = 0.8f)
            ActionType.TYPE_QUANTITY_PICKUP -> Color(0xFFFFA500).copy(alpha = 0.8f) // Orange
            ActionType.TYPE_CONFIRM_PICKUP -> Color.Green.copy(alpha = 0.8f)
            ActionType.TYPE_ACTION_COMPLETE -> Color.Blue.copy(alpha = 0.8f)
            ActionType.TYPE_NO_ACTION -> Color(0xFF40A535).copy(alpha = 0.7f)
            ActionType.TYPE_NONE -> Color(0xFFF8D249).copy(alpha = 0.8f)
        }
    }
}
