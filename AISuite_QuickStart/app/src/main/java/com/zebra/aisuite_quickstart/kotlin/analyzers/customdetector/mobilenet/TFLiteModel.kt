// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.kotlin.analyzers.customdetector.mobilenet

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * MobileNet SSD v1 COCO object detector (TFLite, quantized uint8).
 * Apache-2.0 licensed model from the TensorFlow Lite object detection examples.
 *
 * Input : [1, 300, 300, 3] uint8 NHWC, RGB values 0–255.
 * Outputs (TFLite_Detection_PostProcess — NMS is baked in):
 *   0 — locations  [1, max_det, 4]  float32  [ymin, xmin, ymax, xmax] normalised 0–1
 *   1 — classes    [1, max_det]     float32  class index (0-based into labelmap)
 *   2 — scores     [1, max_det]     float32  confidence (descending)
 *   3 — num_det    [1]              float32  number of valid detections
 * max_det is read from the model at runtime; boxes are mapped to bitmap pixel space on output.
 */
class TFLiteModel(context: Context) : AutoCloseable {

    companion object {
        private const val TAG            = "TFLiteModel"
        private const val MODEL_FILE     = "models/mobilenet_ssd.tflite"
        private const val CONF_THRESHOLD = 0.5f
    }

    private val interpreter: Interpreter
    private val inputSize: Int
    private val maxDetections: Int
    private val boxesIdx: Int
    private val scoresIdx: Int
    private val numDetIdx: Int

    init {
        Log.d(TAG, "Loading TFLite model from assets: $MODEL_FILE")
        interpreter = Interpreter(FileUtil.loadMappedFile(context, MODEL_FILE))

        // Resolve input size dynamically (typically 300 for MobileNet SSD)
        inputSize = interpreter.getInputTensor(0).shape()[1]

        var foundBoxes  = 0
        var foundScores = 2
        var foundNumDet = 3
        var foundMaxDet = 10

        repeat(interpreter.outputTensorCount) { i ->
            val sh  = interpreter.getOutputTensor(i).shape()
            val nm  = interpreter.getOutputTensor(i).name()
            Log.i(TAG, "  output[$i] name=$nm shape=${sh.contentToString()}" +
                    " dtype=${interpreter.getOutputTensor(i).dataType()}")
            if (sh.size == 3 && sh.last() == 4) { foundBoxes = i; foundMaxDet = sh[sh.size - 2] }
            if (sh.size == 1)                    { foundNumDet = i }
        }
        foundScores = if (foundBoxes == 2) 0 else 2

        boxesIdx      = foundBoxes
        scoresIdx     = foundScores
        numDetIdx     = foundNumDet
        maxDetections = foundMaxDet

        Log.i(TAG, "MobileNet SSD ready — inputSize=$inputSize maxDetections=$maxDetections" +
                " boxesIdx=$boxesIdx scoresIdx=$scoresIdx numDetIdx=$numDetIdx" +
                " inputDtype=${interpreter.getInputTensor(0).dataType()}")
    }

    fun detect(bitmap: Bitmap): List<Rect> {
        val input = toInputBuffer(bitmap)
        val bw = bitmap.width
        val bh = bitmap.height

        val boxes   = Array(1) { Array(maxDetections) { FloatArray(4) } }
        val classes = Array(1) { FloatArray(maxDetections) }
        val scores  = Array(1) { FloatArray(maxDetections) }
        val numDet  = FloatArray(1)

        val outputs: MutableMap<Int, Any> = mutableMapOf(
            boxesIdx  to boxes,
            1         to classes,
            scoresIdx to scores,
            numDetIdx to numDet
        )
        interpreter.runForMultipleInputsOutputs(arrayOf(input), outputs)

        val n = minOf(numDet[0].toInt(), maxDetections)
        val results = (0 until n)
            .filter { scores[0][it] >= CONF_THRESHOLD }
            .map { i ->
                val ymin = boxes[0][i][0]; val xmin = boxes[0][i][1]
                val ymax = boxes[0][i][2]; val xmax = boxes[0][i][3]
                Rect((xmin * bw).toInt(), (ymin * bh).toInt(),
                     (xmax * bw).toInt(), (ymax * bh).toInt())
            }
        if (n > 0) Log.v(TAG, "  MobileNet SSD detections kept=${results.size}/$n top_score=${scores[0][0]}")
        return results
    }

    private fun toInputBuffer(bitmap: Bitmap): ByteBuffer {
        val sw = if (bitmap.config == Bitmap.Config.HARDWARE)
            bitmap.copy(Bitmap.Config.ARGB_8888, false) else bitmap
        val scaled = Bitmap.createScaledBitmap(sw, inputSize, inputSize, true)
        val pixels = IntArray(inputSize * inputSize)
        scaled.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        val buf = ByteBuffer.allocateDirect(inputSize * inputSize * 3)
            .order(ByteOrder.nativeOrder())
        for (px in pixels) {
            buf.put(((px shr 16) and 0xFF).toByte())
            buf.put(((px shr  8) and 0xFF).toByte())
            buf.put((px and 0xFF).toByte())
        }
        buf.rewind()
        return buf
    }

    override fun close() = interpreter.close()
}
