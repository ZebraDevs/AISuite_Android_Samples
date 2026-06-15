package com.zebra.aidatacapturedemo.data

import android.graphics.Bitmap
import com.zebra.ai.vision.detector.BBox
import com.zebra.ai.vision.entity.LabelEntity
import com.zebra.ai.vision.entity.ProductEntity
import com.zebra.ai.vision.entity.ShelfEntity

/**
 * ModuleData.kt is a data class that encapsulates the results from the Product & Shelf Recognizer
 * module. It contains lists of ShelfEntity, LabelEntity, and ProductEntity objects, which represent
 * the detected shelves, labels, and products in the input image. This class serves as a structured
 * way to store and access the results from the recognition process, allowing for easy integration
 * with the UI and other components of the application.
 */
class ModuleData(var shelves: List<ShelfEntity>, var labelEntity: List<LabelEntity>, var productEntity: List<ProductEntity> )