package com.zebra.ai.barcodefinder.data.source.storage

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/**
 * A generic class for handling JSON-based file storage.
 *
 * @param T The type of the object to be stored.
 */
abstract class JsonStorage<T>(private val gson: Gson) {

    /**
     * Provides the file where the data will be stored.
     * Subclasses must implement this to specify the file location.
     */
    abstract fun getFile(): File

    /**
     * Loads the data from the JSON file.
     *
     * @param typeToken The type token for deserialization.
     * @return The loaded data, or null if an error occurs or the file doesn't exist.
     */
    fun loadData(typeToken: TypeToken<T>): T? {
        val file = getFile()
        if (!file.exists()) return null

        return try {
            FileReader(file).use { reader ->
                gson.fromJson<T>(reader, typeToken.type)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Saves the data to the JSON file.
     *
     * @param data The data to save.
     */
    fun saveData(data: T) {
        val file = getFile()
        try {
            FileWriter(file).use { writer ->
                gson.toJson(data, writer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}