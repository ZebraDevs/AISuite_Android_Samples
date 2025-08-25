// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zebra.ai.barcodefinder.R
import com.zebra.ai.barcodefinder.common.enums.ActionState
import com.zebra.ai.barcodefinder.common.enums.ActionType
import com.zebra.ai.barcodefinder.data.model.ActionableBarcode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileReader
import java.io.FileWriter

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
class ActionableBarcodeRepository private constructor(private val context: Context) {

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

    // Map of icons for each action type
    private val _actionTypeIcons = mutableMapOf<ActionType, Bitmap>()

    // JSON serialization and configuration file name
    private val gson = Gson()
    private val CONFIGURATIONS_FILE_NAME = "barcode_configurations.json"

    // Observable state for configured barcodes
    val liveConfiguredActionableBarcodes: MutableStateFlow<List<ActionableBarcode>> =
        MutableStateFlow(emptyList())

    /**
     * Initializes icons and loads barcode configurations from JSON.
     * Populates tracking and configuration maps/lists for use in the app.
     */
    init {
        initIcons()
        // Initialize barcodeMap and configuredBarcodeMap from JSON configurations
        val configurations = loadConfigurationsFromJson()
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
                    barcode.actionType
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
        val configurations = loadConfigurationsFromJson()
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
                    barcode.actionType
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
     * Returns a copy of all configured actionable barcodes.
     */
    fun getConfiguredBarcodes(): List<ActionableBarcode> {
        return _configuredActionableBarcodeList.toList()
    }

    /**
     * Returns the current live list of configured actionable barcodes.
     */
    fun getLiveConfiguredBarcodes(): List<ActionableBarcode> {
        return liveConfiguredActionableBarcodes.value
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
                actionableBarcode.actionType
            )
        )

        _configuredActionableBarcodeList.add(0, newBarcode)
        _configuredActionableBarcodeMap[newBarcode.barcodeData] = newBarcode

        liveConfiguredActionableBarcodes.value = _configuredActionableBarcodeList.toList()
    }

    /**
     * Clears all configured actionable barcodes and updates observable state.
     */
    fun clearConfiguredActionableBarcodes() {
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
        saveConfigurationsToJson(_configuredActionableBarcodeList)
    }

    /**
     * Returns a barcode from the live configured list by barcode data, or null if not found.
     */
    fun getLiveConfiguredActionableBarcodeFromConfigList(barcodeData: String): ActionableBarcode? {
        return liveConfiguredActionableBarcodes.value.find { it.barcodeData == barcodeData }
    }

    /**
     * Gets the icon bitmap for a specific action type.
     */
    fun getIconForActionType(actionType: ActionType): Bitmap? {
        return _actionTypeIcons[actionType]
    }

    /**
     * Returns a map of action state icons for a given action state and action type.
     */
    fun getActionStateIconsWithDefaults(
        actionState: ActionState,
        actionType: ActionType
    ): Map<ActionState, Bitmap> {
        val actionStateIcons = mutableMapOf<ActionState, Bitmap>()

        _actionTypeIcons[ActionType.TYPE_ACTION_COMPLETE]?.let {
            actionStateIcons[ActionState.STATE_ACTION_COMPLETED] = it
        }
        _actionTypeIcons[actionType]?.let {
            actionStateIcons[actionState] = it
        }

        return actionStateIcons
    }

    /**
     * Returns the background color for a given action type for UI overlays.
     */
    fun getBackgroundColorForActionType(actionType: ActionType): androidx.compose.ui.graphics.Color {
        return when (actionType) {
            ActionType.TYPE_RECALL -> androidx.compose.ui.graphics.Color.Red.copy(alpha = 0.8f)
            ActionType.TYPE_QUANTITY_PICKUP -> androidx.compose.ui.graphics.Color(0xFFFFA500)
                .copy(alpha = 0.8f) // Orange
            ActionType.TYPE_CONFIRM_PICKUP -> androidx.compose.ui.graphics.Color.Green.copy(alpha = 0.8f)
            ActionType.TYPE_ACTION_COMPLETE -> androidx.compose.ui.graphics.Color.Blue.copy(alpha = 0.8f)
            ActionType.TYPE_NO_ACTION -> androidx.compose.ui.graphics.Color(0xFF40A535)
                .copy(alpha = 0.7f)

            ActionType.TYPE_NONE -> androidx.compose.ui.graphics.Color(0xFFF8D249)
                .copy(alpha = 0.8f)
        }
    }

    /**
     * Loads barcode configurations from JSON file.
     */
    private fun loadConfigurationsFromJson(): List<ActionableBarcode> {
        return try {
            val jsonFile = File(context.getExternalFilesDir(""), CONFIGURATIONS_FILE_NAME)
            if (jsonFile.exists()) {
                FileReader(jsonFile).use { reader ->
                    val type = object : TypeToken<List<ActionableBarcode>>() {}.type
                    gson.fromJson<List<ActionableBarcode>>(reader, type) ?: emptyList()
                }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Saves barcode configurations to JSON file.
     */
    private fun saveConfigurationsToJson(configurations: List<ActionableBarcode>) {
        try {
            val jsonFile = File(context.getExternalFilesDir(""), CONFIGURATIONS_FILE_NAME)
            FileWriter(jsonFile).use { writer ->
                gson.toJson(configurations, writer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Returns a default actionable barcode for a given barcode data string.
     */
    fun getActionableForData(barcodeData: String): ActionableBarcode {
        return ActionableBarcode(
            barcodeData = barcodeData,
            productName = "",
            actionType = ActionType.TYPE_NO_ACTION,
            actionState = ActionState.STATE_ACTION_NOT_COMPLETED
        ).apply {
            setActionStateIcons(getActionStateIconsWithDefaults(actionState, actionType))
        }
    }

    /**
     * Returns an empty barcode with TYPE_NONE action type for unrecognized barcodes.
     */
    fun getEmptyBarcode(): ActionableBarcode {
        return ActionableBarcode(
            barcodeData = "",
            productName = "",
            actionType = ActionType.TYPE_NONE,
            actionState = ActionState.STATE_ACTION_NOT_COMPLETED
        ).apply {
            setQuantity(0)
            setActionStateIcons(getActionStateIconsWithDefaults(actionState, actionType))
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
     * Initializes the action type icons map from drawable resources.
     */
    private fun initIcons() {
        ActionType.entries.forEach { actionType ->
            val iconRes = getStatusIconRes(actionType)
            val icon = BitmapFactory.decodeResource(context.resources, iconRes)
            _actionTypeIcons[actionType] = icon
        }
    }
}
