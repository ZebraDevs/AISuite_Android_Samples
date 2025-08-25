package com.zebra.aidatacapturedemo.model

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Point
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.google.gson.Gson
import com.zebra.ai.vision.detector.BBox
import com.zebra.aidatacapturedemo.data.BarcodeSettings
import com.zebra.aidatacapturedemo.data.OcrFindSettings
import com.zebra.aidatacapturedemo.data.ProductData
import com.zebra.aidatacapturedemo.data.ProductRecognitionSettings
import com.zebra.aidatacapturedemo.data.RetailShelfSettings
import com.zebra.aidatacapturedemo.data.TextOcrSettings
import com.zebra.aidatacapturedemo.data.UsecaseState
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.OutputStream
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.ArrayDeque


/**
 * FileUtils class to provide utility functions related to filesystem
 */
class FileUtils(cacheDir: String, context : Context) {
    init {
        mCacheDir = cacheDir
        mContext = context
        barcodeSettingsFile = File(mCacheDir, "barcode_settings.json")
        ocrTextSettingsFile = File(mCacheDir, "ocr_text_settings.json")
        retailShelfSettingsFile = File(mCacheDir, "retailshelf_settings.json")
        ocrFindSettingsFile = File(mCacheDir, "ocrfind_settings.json")
        productRecogntionSettingsFile= File(mCacheDir, "product_recognition_settings.json")
        settingsFiles.put(UsecaseState.Barcode.value, barcodeSettingsFile)
        settingsFiles.put(UsecaseState.OCR.value, ocrTextSettingsFile)
        settingsFiles.put(UsecaseState.OCRFind.value, ocrFindSettingsFile)
        settingsFiles.put(UsecaseState.Retail.value, retailShelfSettingsFile)
        settingsFiles.put(UsecaseState.Product.value, productRecogntionSettingsFile)

    }

    companion object {
        lateinit var mCacheDir: String
        lateinit var mContext : Context
        lateinit var mSavedTimeStamp : String
        var databaseFile: String = "products.db"
        private val gson = Gson()
        private lateinit var barcodeSettingsFile: File
        private lateinit var ocrTextSettingsFile: File
        private lateinit var retailShelfSettingsFile: File
        private lateinit var ocrFindSettingsFile: File
        private lateinit var productRecogntionSettingsFile: File

        var settingsFiles : MutableMap<String, File> = mutableMapOf()

        fun loadBarcodeSettings(): BarcodeSettings {

            return if (settingsFiles.getValue(UsecaseState.Barcode.value).exists()) {
                try {
                    val json = settingsFiles.getValue(UsecaseState.Barcode.value).readText()
                    gson.fromJson(json, BarcodeSettings::class.java) ?: BarcodeSettings()
                } catch (_: Exception) {
                    BarcodeSettings()
                }
            } else {
                BarcodeSettings()
            }
        }

        fun saveBarcodeSettings(settings: BarcodeSettings) {
            try {
                FileWriter(settingsFiles.getValue(UsecaseState.Barcode.value)).use { writer ->
                    gson.toJson(settings, writer)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        fun loadOCRSettings(): TextOcrSettings {
            return if (settingsFiles.getValue(UsecaseState.OCR.value).exists()) {
                try {
                    val json = settingsFiles.getValue(UsecaseState.OCR.value).readText()
                    gson.fromJson(json, TextOcrSettings::class.java) ?: TextOcrSettings()
                } catch (_: Exception) {
                    TextOcrSettings()
                }
            } else {
                TextOcrSettings()
            }
        }

        fun saveOCRSettings(settings: TextOcrSettings) {
            try {
                FileWriter(settingsFiles.getValue(UsecaseState.OCR.value)).use { writer ->
                    gson.toJson(settings, writer)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun loadAdvancedOCRSettings(): OcrFindSettings {
            return if (settingsFiles.getValue(UsecaseState.OCRFind.value).exists()) {
                try {
                    val json = settingsFiles.getValue(UsecaseState.OCRFind.value).readText()
                    gson.fromJson(json, OcrFindSettings::class.java) ?: OcrFindSettings()
                } catch (_: Exception) {
                    OcrFindSettings()
                }
            } else {
                OcrFindSettings()
            }
        }

        fun saveAdvancedOCRSettings(settings: OcrFindSettings) {
            try {
                FileWriter(settingsFiles.getValue(UsecaseState.OCRFind.value)).use { writer ->
                    gson.toJson(settings, writer)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun loadRetailShelfSettings(): RetailShelfSettings {
            return if (settingsFiles.getValue(UsecaseState.Retail.value).exists()) {
                try {
                    val json = settingsFiles.getValue(UsecaseState.Retail.value).readText()
                    gson.fromJson(json, RetailShelfSettings::class.java) ?: RetailShelfSettings()
                } catch (_: Exception) {
                    RetailShelfSettings()
                }
            } else {
                RetailShelfSettings()
            }
        }

        fun saveRetailShelfSettings(settings: RetailShelfSettings) {
            try {
                FileWriter(settingsFiles.getValue(UsecaseState.Retail.value)).use { writer ->
                    gson.toJson(settings, writer)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun loadProductRecognitionSettings(): ProductRecognitionSettings {
            return if (settingsFiles.getValue(UsecaseState.Product.value).exists()) {
                try {
                    val json = settingsFiles.getValue(UsecaseState.Product.value).readText()
                    gson.fromJson(json, ProductRecognitionSettings::class.java) ?: ProductRecognitionSettings()
                } catch (_: Exception) {
                    ProductRecognitionSettings()
                }
            } else {
                ProductRecognitionSettings()
            }
        }

        fun saveProductRecognitionSettings(settings: ProductRecognitionSettings) {
            try {
                FileWriter(settingsFiles.getValue(UsecaseState.Product.value)).use { writer ->
                    gson.toJson(settings, writer)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun getTimeStamp(): String {
            return return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS"))
        }

        /**
         * This function is used to create a timestamped folder in the external files directory to
         * save product images
         */
        fun getTimeStampedFolderName() : String {
            mSavedTimeStamp = getTimeStamp()
            val timestampFolder = File(mContext.getExternalFilesDir(null), mSavedTimeStamp)
            if (!timestampFolder.exists()) {
                timestampFolder.mkdir()
            }
            return mSavedTimeStamp
        }

        /**
         * This function is used to save the bitmap image in the Pictures folder
         */
        fun saveBitmap(bmp: Bitmap,
                      subFolderName: String?,
                      filename: String?) {
            var imageOutStream: OutputStream
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues()
                values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/" +subFolderName);

                val uri = mContext.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                imageOutStream = uri?.let { mContext.getContentResolver().openOutputStream(it) }!!
            } else {
                val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString(), subFolderName)
                if (!folder.exists()) {
                    folder.mkdir()
                }
                val file = File(folder, filename)
                imageOutStream =  FileOutputStream(file);
            }
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, imageOutStream);
            imageOutStream.flush();
            imageOutStream.close();
        }


        /**
         * This function is used to delete the products.db file from the cache directory
         */
        fun deleteProductDBFile() {
            val path = Paths.get(mCacheDir, databaseFile).toString()
            val file = File(path)
            file.delete()
        }

        /**
         * This function is used to save the product database file (products.db) in Downloads folder
         */
        fun saveProductDBFile() {
            val productDBFile = File(mCacheDir, databaseFile)
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "products.db")
                put(MediaStore.MediaColumns.MIME_TYPE, "*/*")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            // Query for the file
            val cursor: Cursor? = mContext.getContentResolver().query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, null, null, null, null)
            var fileUri: Uri? = null
            // If file found
            if (cursor != null && cursor.count > 0) {
                // Get URI
                while (cursor.moveToNext()) {
                    val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    if (nameIndex > -1) {
                        val displayName = cursor.getString(nameIndex)
                        if (displayName == "products.db") {
                            val idIndex = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                            if (idIndex > -1) {
                                val id = cursor.getLong(idIndex)
                                fileUri = ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
                                break
                            }
                        }
                    }
                }
                cursor.close()
            } else {
                // insert new file otherwise
                val resolver = mContext.contentResolver
                fileUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            }
            saveFile(productDBFile.toUri(), fileUri)
        }

        /**
         * This function is used to reads product crops in Downloads folder
         */
        fun readProductCrops(uri: Uri) : List<ProductData>{

            // Creates a remembered mutable state list to store paths of images.
            val rootDirectoryFile = DocumentFile.fromTreeUri(mContext, uri)
            val directories = ArrayDeque(listOf(rootDirectoryFile))
            val imageFileUris = mutableListOf<Uri>()
            val listOfProductData = mutableListOf<ProductData>()

            // Loop through all of the subdirectories, starting with the root
            while (directories.isNotEmpty()) {
                val currentDirectory = directories.removeFirst()

                // List all of the files in the current directory
                val files = currentDirectory?.listFiles()
                if (files != null) {
                    for (file in files) {
                        if (file.isDirectory) {
                            // Add subdirectories to the list to search through
                            directories.add(file)
                        } else if (file.type?.startsWith("image/") == true) {
                            // Add Uri of the image file to the list
                            imageFileUris += file.uri
                            mContext.contentResolver.openInputStream(file.uri).use { input ->
                                if(input != null) {
                                    val bitmap = BitmapFactory.decodeStream(input)
                                    file.parentFile?.name?.let {
                                        listOfProductData += ProductData(Point(0, 0), it, BBox(), bitmap)
                                    }
                                    input.close()
                                }
                            }
                        }
                    }
                }
            }
            return listOfProductData
        }

        /**
         * This function is used to write and save the file
         */
        @RequiresApi(Build.VERSION_CODES.Q)
        fun saveFile(srcUri: Uri, destUri: Uri?) {
            if (destUri != null) {
                mContext.contentResolver.openInputStream(srcUri).use { input ->
                    if(input != null) {
                        mContext.contentResolver.openOutputStream(destUri).use { output ->
                            input.copyTo(output!!, DEFAULT_BUFFER_SIZE)
                        }
                    }
                }
            }
        }
    }
}
