package com.zebra.ai.barcodefinder.data.source.storage

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zebra.ai.barcodefinder.domain.model.ActionableBarcode
import java.io.File

/**
 * Storage manager for barcode configurations.
 */
class BarcodeStorage(context: Context) : JsonStorage<List<ActionableBarcode>>(Gson()) {

    private val barcodesFile: File = File(
        context.getExternalFilesDir(null),
        "barcode_configurations.json"
    )

    override fun getFile(): File = barcodesFile

    /**
     * Loads the list of barcodes.
     *
     * @return A list of ActionableBarcode, or an empty list if not found.
     */
    fun loadBarcodes(): List<ActionableBarcode> {
        return loadData(object : TypeToken<List<ActionableBarcode>>() {}) ?: emptyList()
    }

    /**
     * Saves the list of barcodes.
     *
     * @param barcodes The list of barcodes to save.
     */
    fun saveBarcodes(barcodes: List<ActionableBarcode>) {
        saveData(barcodes)
    }
}