// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodefinder.domain.model

import android.graphics.Bitmap
import com.zebra.ai.barcodefinder.domain.enums.ActionState
import com.zebra.ai.barcodefinder.domain.enums.ActionType

/**
 * Represents a barcode item with associated dialog, icon, and action state.
 * This class models a barcode item with properties defining its interaction
 * type, visual representation, and current action state.
 */
data class ActionableBarcode(
    val barcodeData: String,
    var productName: String,
    var actionType: ActionType,
    var actionState: ActionState,
    private var quantityValue: Int = 0,
    var isConfirmed: Boolean = false
) {
    companion object {
        private const val TAG = "ActionableBarcode"
    }

    private val userData: MutableMap<String, String> = mutableMapOf()
    private val actionStateIcons: MutableMap<ActionState, Bitmap> = mutableMapOf()

    /**
     * Gets the quantity as an integer.
     */
    val quantity: Int
        get() = quantityValue

    /**
     * Sets the quantity for this barcode.
     */
    fun setQuantity(quantity: Int) {
        this.quantityValue = quantity
    }

    /**
     * Gets the icon associated with the current action state.
     */
    fun getActiveIcon(): Bitmap? {
        return actionStateIcons[actionState]
    }

    /**
     * Gets the icon for a specific action state.
     */
    fun getIconForState(state: ActionState): Bitmap? {
        return actionStateIcons[state]
    }

    /**
     * Sets the icons for different action states.
     */
    fun setActionStateIcons(newActionStateIcons: Map<ActionState, Bitmap>) {
        actionStateIcons.clear()
        actionStateIcons.putAll(newActionStateIcons)
    }

    /**
     * Sets a user data value for the given key.
     */
    fun setUserData(key: String, value: String) {
        userData[key] = value
    }

    /**
     * Gets a user data value for the given key.
     */
    fun getUserDataValue(key: String): String? {
        return userData[key]
    }


}
