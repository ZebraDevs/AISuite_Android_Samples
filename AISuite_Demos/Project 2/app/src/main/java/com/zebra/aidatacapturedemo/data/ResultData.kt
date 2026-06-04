package com.zebra.aidatacapturedemo.data

import android.graphics.Rect

/**
 * ResultData class used to store OCR-Barcode Find results
 * @param boundingBox: Rect
 * @param text: String
 */
class ResultData(var boundingBox: Rect, var text: String)