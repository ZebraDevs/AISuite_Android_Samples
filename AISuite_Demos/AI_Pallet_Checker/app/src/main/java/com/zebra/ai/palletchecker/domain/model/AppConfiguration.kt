package com.zebra.ai.palletchecker.domain.model

import com.google.gson.Gson

/**
 * AppConfiguration holds all configuration options for AI Pallet Checker.
 * This includes model input size, camera resolution, processor type, and barcode symbology settings.
 * Used to persist, compare, and apply user or system configuration throughout the app.
 */
data class AppConfig(
    val expectedBoxes: Int = 10,
    val listOfConfig: List<BarcodeConfig> = createDefaultList()
) {
    /**
     * Compares this AppSettings instance to another for equality of all fields.
     * Useful for detecting changes in configuration.
     */
    fun isEquals(other: AppConfig): Boolean {
        val gson = Gson()
        val oth = gson.toJson(other)
        val current = gson.toJson(this)
        return oth.equals(current)
    }
}

data class BarcodeConfig(
    val type: String,
    val isSelected: Boolean = false,
    val regex: String,
    val symbology: Int? = null,
    @Transient val isExpanded: Boolean = false
)

fun createDefaultList(): List<BarcodeConfig> {
    val arr = arrayListOf<BarcodeConfig>()
    arr.add(BarcodeConfig(regex = "", type = FIELD_TYPE.PRODUCT_SKU.name, isSelected = false))
    arr.add(BarcodeConfig(regex = "", type = FIELD_TYPE.QTY.name, isSelected = false))
    return arr
}


enum class FIELD_TYPE(val title:String, val startWith: String) {
    PRODUCT_SKU("Product SKU","S00"),
    QTY("Quantity ","P00"),
}