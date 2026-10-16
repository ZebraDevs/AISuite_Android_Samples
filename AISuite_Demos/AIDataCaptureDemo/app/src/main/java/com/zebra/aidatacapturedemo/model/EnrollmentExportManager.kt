package com.zebra.aidatacapturedemo.model

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.zebra.aidatacapturedemo.data.DeviceInfo
import com.zebra.aidatacapturedemo.data.EnrollmentEmbeddingFile
import com.zebra.aidatacapturedemo.data.EnrollmentExportArtifact
import com.zebra.aidatacapturedemo.data.EnrollmentExportCsvRow
import com.zebra.aidatacapturedemo.data.GeoLocation
import java.io.OutputStreamWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class EnrollmentExportManager(private val context: Context) {

    companion object {
        private const val ROOT_FOLDER = "AIDataCaptureEnrollment"

    }

    private val gson = Gson()

    /**
     * Exports:
     * - cropped images
     * - embeddings (json files)
     * - csv manifest
     *
     * Returns relative folder under Downloads, e.g.:
     * AIDataCaptureEnrollment/
     */
    fun exportEnrollmentSession(artifacts: List<EnrollmentExportArtifact>): String? {
        if (artifacts.isEmpty()) return null

        clearAndPrepare()

        // Deduplicate: keep only the first artifact for each unique SKU
        val uniqueArtifacts = artifacts.distinctBy { it.sku }

        if (uniqueArtifacts.isEmpty()) return null

        val now = LocalDateTime.now()
        val date = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val time = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"))

        val downloadsRoot = Environment.DIRECTORY_DOWNLOADS
        val rootRelativePath = "$downloadsRoot/$ROOT_FOLDER"
        val imagesRelativePath = "$rootRelativePath/images"
        val embeddingsRelativePath = "$rootRelativePath/embeddings"

        val (lat, lon) = getLastKnownLatLon()
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val model = Build.MODEL.orEmpty()
        val device = Build.DEVICE.orEmpty()
        val androidVersion = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"

        val rows = mutableListOf<EnrollmentExportCsvRow>()

        uniqueArtifacts.forEachIndexed { index, item ->
            val safeSku = sanitize(item.sku.ifBlank { "unknown_sku" })
            val uniqueBase = "${safeSku}_${index + 1}"

            val imageFile = "${uniqueBase}_crop.jpg"
            val embeddingFile = "${uniqueBase}_embedding.json"
            val productDbFile = FileUtils.databaseFile

            val savedImage = saveBitmapToDownloads(
                bitmap = item.cropBitmap,
                fileName = imageFile,
                relativePath = imagesRelativePath
            )

            val savedEmbedding = saveTextToDownloads(
                text = gson.toJson(
                    EnrollmentEmbeddingFile(
                        sku = item.sku,
                        modelVersionInfo = item.modelVersionInfo,
                        embedding = item.embedding
                    )
                ),
                fileName = embeddingFile,
                mimeType = "application/json",
                relativePath = embeddingsRelativePath
            )

            if (savedImage && savedEmbedding) {
                rows += EnrollmentExportCsvRow(
                    sku = item.sku,
                    imageFile = "images/$imageFile",
                    embeddingFile = "embeddings/$embeddingFile",
                    productDbFile = productDbFile,
                    modelVersionInfo = item.modelVersionInfo,
                    date = date,
                    time = time,
                    location = GeoLocation(
                        lat,
                        lon
                    ),
                    deviceInfo = DeviceInfo(
                        manufacturer,
                        model,
                        device,
                        androidVersion
                    ),
                )
            }
        }

        if (rows.isEmpty()) {
            return null
        }

        val csvContent = buildCsv(rows)
        val csvSaved = saveTextToDownloads(
            text = csvContent,
            fileName = "enrollment_manifest.csv",
            mimeType = "text/csv",
            relativePath = rootRelativePath
        )

        return if (csvSaved) ROOT_FOLDER else null
    }

    private fun saveBitmapToDownloads(
        bitmap: Bitmap,
        fileName: String,
        relativePath: String
    ): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return false

        return try {
            val success = context.contentResolver.openOutputStream(uri)?.use { out ->
                val ok = bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                out.flush()
                ok
            } ?: false

            if (success && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val updateValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                context.contentResolver.update(uri, updateValues, null, null)
            }
            if (success) scanUri(uri, "image/jpeg")
            success
        } catch (_: Exception) {
            false
        }
    }

    private fun saveTextToDownloads(
        text: String,
        fileName: String,
        mimeType: String,
        relativePath: String
    ): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return false

        return try {
            val success = context.contentResolver.openOutputStream(uri)?.use { out ->
                OutputStreamWriter(out).use { writer ->
                    writer.write(text)
                    writer.flush()
                }
            } != null

            if (success && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val updateValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                context.contentResolver.update(uri, updateValues, null, null)
            }
            if (success) scanUri(uri, mimeType)
            success
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Force MediaStore to acknowledge the file immediately so it is visible
     * in file managers without needing the app to lose focus.
     */
    private fun scanUri(uri: Uri, mimeType: String) {
        try {
            val filePath = getFilePathFromUri(uri)
            if (filePath != null) {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(filePath),
                    arrayOf(mimeType),
                    null
                )
            }
        } catch (_: Exception) {
            // scanning is best-effort; don't fail the save
        }
    }

    private fun getFilePathFromUri(uri: Uri): String? {
        val projection = arrayOf(MediaStore.MediaColumns.DATA)
        context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                return cursor.getString(columnIndex)
            }
        }
        return null
    }

    fun clearAndPrepare() {
        val base = "${Environment.DIRECTORY_DOWNLOADS}/$ROOT_FOLDER/"
        val targets = arrayOf(
            base,
            "${base}images/",
            "${base}embeddings/"
        )

        targets.forEach { relativePath ->
            deleteDownloadsByRelativePath(relativePath)
        }
    }

    private fun deleteDownloadsByRelativePath(relativePath: String) {
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ?"
        val selectionArgs = arrayOf(relativePath)

        val resolver = context.contentResolver
        resolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                runCatching { resolver.delete(uri, null, null) }
            }
        }
    }

    private fun buildCsv(rows: List<EnrollmentExportCsvRow>): String {
        val header = listOf(
            "sku",
            "image_file",
            "embedding_file",
            "product_db_file",
            "model_version_info",
            "date",
            "time",
            "latitude",
            "longitude",
            "device_manufacturer",
            "device_model",
            "device_name",
            "android_version"
        ).joinToString(",")

        val body = rows.joinToString("\n") { row ->
            listOf(
                row.sku,
                row.imageFile,
                row.embeddingFile,
                row.productDbFile,
                row.modelVersionInfo,
                row.date,
                row.time,
                row.location.latitude,
                row.location.longitude,
                row.deviceInfo.deviceManufacturer,
                row.deviceInfo.deviceModel,
                row.deviceInfo.deviceName,
                row.deviceInfo.androidVersion
            ).joinToString(",") { escapeCsv(it) }
        }

        return "$header\n$body\n"
    }

    private fun escapeCsv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    private fun sanitize(input: String): String {
        return input.replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }

    private fun getLastKnownLatLon(): Pair<String, String> {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            return "" to ""
        }

        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return "" to ""

        val providers = runCatching { locationManager.getProviders(true) }.getOrDefault(emptyList())
        val latest: Location? = providers
            .mapNotNull { provider ->
                runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
            }
            .maxByOrNull { it.time }

        return if (latest != null) {
            latest.latitude.toString() to latest.longitude.toString()
        } else {
            "" to ""
        }
    }
}