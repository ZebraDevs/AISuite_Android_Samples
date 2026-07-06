// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.analyzers.customdetector.yolo

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import java.io.FileInputStream
import java.nio.FloatBuffer
import java.nio.channels.FileChannel

/**
 * YOLOv8n general object detector — 80 COCO classes (ONNX Runtime).
 *
 * Input : [1, 3, 640, 640] float32 CHW, RGB normalised to [0, 1].
 * Output: [1, 84, 8400] — cx/cy/w/h + 80 class scores per prediction.
 *
 * Implements AutoCloseable so the ONNX session is released when
 * CustomDetector.close() cascades to this.
 */
class YoloOnnxModel(context: Context) : AutoCloseable {

    companion object {
        private const val MODEL_FILE      = "models/yolov8n-onnx/yolov8n.onnx"
        private const val INPUT_SIZE      = 640
        private const val CONF_THRESHOLD  = 0.3f
        private const val IOU_THRESHOLD   = 0.45f
        private const val TAG = "YoloOnnxModel"
    }

    private val env    : OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    init {
        Log.d(TAG, "Loading ONNX session from assets: $MODEL_FILE")
        val fd     = context.assets.openFd(MODEL_FILE)
        val buffer = FileInputStream(fd.fileDescriptor).channel.map(
            FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength
        )
        session = env.createSession(buffer)
        Log.i(TAG, "ONNX session ready — inputs=${session.inputNames} outputs=${session.outputNames}")
    }

    fun detect(bitmap: Bitmap): List<Rect> {
        val inputFloats = bitmapToInputFloats(bitmap)
        val shape       = longArrayOf(1L, 3L, INPUT_SIZE.toLong(), INPUT_SIZE.toLong())
        val inputName   = session.inputNames.iterator().next()

        val raw = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputFloats), shape).use { input ->
            session.run(mapOf(inputName to input)).use { results ->
                val outTensor = results[0] as OnnxTensor
                val buf = outTensor.floatBuffer
                FloatArray(buf.remaining()).also { buf.get(it) }
            }
        }

        return decodeYolov8(raw, bitmap.width, bitmap.height)
    }

    private fun bitmapToInputFloats(bitmap: Bitmap): FloatArray {
        val software = if (bitmap.config == Bitmap.Config.HARDWARE)
            bitmap.copy(Bitmap.Config.ARGB_8888, false) else bitmap
        val scaled = Bitmap.createScaledBitmap(software, INPUT_SIZE, INPUT_SIZE, true)
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaled.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        val n = INPUT_SIZE * INPUT_SIZE
        val floats = FloatArray(3 * n)
        for (i in pixels.indices) {
            floats[i]         = (pixels[i] shr 16 and 0xFF) / 255f
            floats[n + i]     = (pixels[i] shr  8 and 0xFF) / 255f
            floats[2 * n + i] = (pixels[i]        and 0xFF) / 255f
        }
        return floats
    }

    // YOLOv8n output: [1, 84, 8400] row-major — first index = feature, second = prediction.
    // Channels 0-3: cx, cy, w, h in 640-px model space. Channels 4-83: 80 COCO class scores.
    private fun decodeYolov8(raw: FloatArray, bitmapW: Int, bitmapH: Int): List<Rect> {
        val numPreds  = 8400
        val numValues = 84

        val boxes  = mutableListOf<FloatArray>()
        val scores = mutableListOf<Float>()

        for (p in 0 until numPreds) {
            val cx = raw[0 * numPreds + p]
            val cy = raw[1 * numPreds + p]
            val w  = raw[2 * numPreds + p]
            val h  = raw[3 * numPreds + p]

            var maxScore = 0f
            for (c in 4 until numValues) {
                val s = raw[c * numPreds + p]
                if (s > maxScore) maxScore = s
            }
            if (maxScore < CONF_THRESHOLD) continue

            boxes  += floatArrayOf(
                (cx - w / 2f) * bitmapW / 640f,
                (cy - h / 2f) * bitmapH / 640f,
                (cx + w / 2f) * bitmapW / 640f,
                (cy + h / 2f) * bitmapH / 640f
            )
            scores += maxScore
        }

        return nms(boxes, scores.toFloatArray(), IOU_THRESHOLD)
            .map { i -> boxes[i].let { Rect(it[0].toInt(), it[1].toInt(), it[2].toInt(), it[3].toInt()) } }
    }

    // OrtEnvironment is a singleton — closing it would affect any other ONNX session.
    override fun close() = session.close()
}
