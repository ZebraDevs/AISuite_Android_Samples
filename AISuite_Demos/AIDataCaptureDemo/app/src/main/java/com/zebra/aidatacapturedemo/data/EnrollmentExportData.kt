package com.zebra.aidatacapturedemo.data

import android.graphics.Bitmap

/**
 * Artifact produced during enrollment for each SKU.
 * This stores the crop, numeric embedding, and model version info.
 */
data class EnrollmentExportArtifact(
    val sku: String,
    val cropBitmap: Bitmap,
    val embedding: List<FloatArray>,
    val modelVersionInfo: String
)

/**
 * Payload written to a per-SKU embedding file.
 */
data class EnrollmentEmbeddingFile(
    val sku: String,
    val modelVersionInfo: String,
    val embedding: List<FloatArray>
)

/**
 * CSV row data for export manifest.
 */
data class EnrollmentExportCsvRow(
    val sku: String,
    val imageFile: String,
    val embeddingFile: String,
    val productDbFile: String,
    val modelVersionInfo: String,
    val date: String,
    val time: String,
    val location: GeoLocation,
    val deviceInfo: DeviceInfo

)

/**
 * CSV row data for export manifest.
 */
data class DeviceInfo(
    val deviceManufacturer: String,
    val deviceModel: String,
    val deviceName: String,
    val androidVersion: String
)

/**
 * Geo Location captured at enrollment time
 */
data class GeoLocation(
    val latitude: String,
    val longitude: String
)
