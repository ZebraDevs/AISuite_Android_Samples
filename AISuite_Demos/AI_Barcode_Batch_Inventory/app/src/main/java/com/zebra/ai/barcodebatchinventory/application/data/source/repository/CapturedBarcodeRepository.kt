// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.

package com.zebra.ai.barcodebatchinventory.application.data.source.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.zebra.ai.barcodebatchinventory.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Stores barcode values captured during batch inventory scan sessions.
 *
 * Duplicate entries are intentionally preserved so the results screen can aggregate
 * repeated captures as quantities.
 */
class CapturedBarcodeRepository private constructor(context: Context) {

    companion object {
        private const val TAG = "CapturedBarcodeRepo"

        @Volatile
        private var INSTANCE: CapturedBarcodeRepository? = null

        fun getInstance(context: Context): CapturedBarcodeRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CapturedBarcodeRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val lock = Any()
    private val capturedBarcodeList = mutableListOf<String>()
    private val _capturedBarcodes = MutableStateFlow<List<String>>(emptyList())

    val capturedBarcodes: StateFlow<List<String>> = _capturedBarcodes.asStateFlow()

    private val completedIcon: Bitmap? =
        BitmapFactory.decodeResource(context.resources, R.drawable.complete_action_marker)
    private val undecodedIcon: Bitmap? =
        BitmapFactory.decodeResource(context.resources, R.drawable.no_decode_marker)

    init {
        Log.d(TAG, "Initialized batch-only captured barcode repository")
    }

    fun addCapturedBarcode(barcodeData: String) {
        val normalized = barcodeData.trim()
        if (normalized.isEmpty()) {
            Log.d(TAG, "Ignored blank barcode capture")
            return
        }

        synchronized(lock) {
            capturedBarcodeList.add(0, normalized)
            _capturedBarcodes.value = capturedBarcodeList.toList()
            Log.d(
                TAG,
                "Captured barcode added. totalCaptured=${capturedBarcodeList.size} " +
                        "uniqueCaptured=${HashSet(capturedBarcodeList).size}"
            )
        }
    }

    fun getCapturedBarcodes(): List<String> {
        return synchronized(lock) {
            capturedBarcodeList.toList()
        }
    }

    fun getCapturedBarcodeSet(): HashSet<String> {
        return synchronized(lock) {
            HashSet(capturedBarcodeList)
        }
    }

    fun clearCapturedBarcodes() {
        synchronized(lock) {
            val previousTotal = capturedBarcodeList.size
            val previousUnique = HashSet(capturedBarcodeList).size
            capturedBarcodeList.clear()
            _capturedBarcodes.value = emptyList()
            Log.d(
                TAG,
                "Captured barcodes cleared. previousTotal=$previousTotal previousUnique=$previousUnique"
            )
        }
    }

    fun getCompletedIcon(): Bitmap? = completedIcon

    fun getUndecodedIcon(): Bitmap? = undecodedIcon
}
