package com.zebra.aidatacapturedemo.data

import android.graphics.Bitmap
import android.graphics.Point
import android.util.Log
import com.zebra.ai.vision.detector.BBox
import com.zebra.ai.vision.detector.Recognizer.Recognition

/**
 * ProductData class used to store product recognition data
 * @param point: Point
 * @param text: String
 * @param bBox: BBox
 * @param crop: Bitmap
 */
class ProductData(var point: Point, var text: String, var bBox: BBox, var crop : Bitmap) {}

/**
 * toProductData function used to convert input bitmap, products and recognitions to product data
 * @param inputBitmap: Bitmap
 * @param products: Array<BBox>
 * @param recognitions: Array<Recognition>
 * @return MutableList<ProductData>
 */
fun toProductData(inputBitmap:Bitmap, products: Array<BBox>, recognitions: Array<Recognition>): MutableList<ProductData> {
    val ProductData = mutableListOf<ProductData>()
    for (i in products.indices) {
        if((products[i].xmin.toInt() + (products[i].xmax - products[i].xmin).toInt() < inputBitmap.width) &&
            (products[i].ymin.toInt() + (products[i].ymax - products[i].ymin).toInt() < inputBitmap.height)) {
            if (recognitions[i].similarity.first() > 0.80) {
                ProductData += ProductData(
                    Point(products[i].xmin.toInt(), products[i].ymin.toInt()),
                    recognitions[i].sku.first(),
                    products[i],
                    Bitmap.createBitmap(
                        inputBitmap,
                        products[i].xmin.toInt(),
                        products[i].ymin.toInt(),
                        (products[i].xmax - products[i].xmin).toInt(),
                        (products[i].ymax - products[i].ymin).toInt()
                    )
                )
            } else {
                ProductData += ProductData(
                    Point(products[i].xmin.toInt(), products[i].ymin.toInt()),
                    "",
                    products[i],
                    Bitmap.createBitmap(
                        inputBitmap,
                        products[i].xmin.toInt(),
                        products[i].ymin.toInt(),
                        (products[i].xmax - products[i].xmin).toInt(),
                        (products[i].ymax - products[i].ymin).toInt()
                    )
                )
            }
        }
        else {
            Log.i("ProductData", "Product BBox out of bounds")
        }
    }
    return ProductData
}