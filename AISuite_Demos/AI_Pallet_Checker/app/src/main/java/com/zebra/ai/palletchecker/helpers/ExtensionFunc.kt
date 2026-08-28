package com.zebra.ai.palletchecker.helpers

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

fun Modifier.TouchEvent(onTouch: (Offset) -> Unit): Modifier {
    return this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {

                val event = awaitFirstDown()

                onTouch(event.position)
            }
        }
    }
}

fun ImageProxy.rotateBitmapIfNeeded(): Bitmap? {
    try {
        val bitmap = this.toBitmap()
        val rotationDegrees = this.imageInfo.rotationDegrees
        return rotateBitmap(bitmap, rotationDegrees)
    } catch (e: Exception) {
        return null
    }
}

private fun rotateBitmap(bitmap: Bitmap?, degrees: Int): Bitmap? {
    if (degrees == 0 || bitmap == null) return bitmap

    try {
        val matrix = Matrix()
        matrix.postRotate(degrees.toFloat())
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.getWidth(),
            bitmap.getHeight(),
            matrix,
            true
        )
    } catch (e: Exception) {
        return bitmap
    }
}

@Composable
fun Dp.toPx(): Float {
    return with(LocalDensity.current) { this@toPx.toPx() }
}
