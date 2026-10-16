package com.zebra.aidatacapturedemo.model

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class DiagnosticExporter {
    private val TAG = "DiagnosticExporter"

    fun export(context: Context, framesDir: File): Boolean {
        val safeModel = Build.MODEL.orEmpty().replace(Regex("""[^\w.-]+"""), "_")
        val zipFileName = "aidc_diag_${safeModel}_${System.currentTimeMillis()}.zip"
        val details = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, zipFileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, details) ?: return false
        val outputStream = context.contentResolver.openOutputStream(uri) ?: run {
            context.contentResolver.delete(uri, null, null)
            return false
        }
        val zipped = ZipOutputStream(outputStream).use { zip ->
            var ok = true
            for (file in framesDir.walkTopDown().filter { it.isFile }) {
                try {
                    val relativePath = file.relativeTo(framesDir).path
                    zip.putNextEntry(ZipEntry(relativePath))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                } catch (e: Exception) {
                    Log.e(TAG, "add file to zip failed: ${e.message}")
                    ok = false
                    break
                }
            }
            ok
        }
        return if (zipped) {
            val update = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            context.contentResolver.update(uri, update, null, null)
            true
        } else {
            context.contentResolver.delete(uri, null, null)
            false
        }
    }
}