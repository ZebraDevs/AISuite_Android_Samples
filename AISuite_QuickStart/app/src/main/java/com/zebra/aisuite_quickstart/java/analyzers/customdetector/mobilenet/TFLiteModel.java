// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.analyzers.customdetector.mobilenet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.zebra.ai.vision.entity.DetectionEntity;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public class TFLiteModel implements AutoCloseable {

    private static final String TAG        = "TFLiteModel";
    private static final String MODEL_FILE = "models/mobilenet_ssd.tflite";
    private static final float  CONF_THRESHOLD = 0.5f;

    private final Interpreter interpreter;
    private final int maxDetections;
    private final int inputSize;
    private final int boxesIdx;
    private final int scoresIdx;
    private final int numDetIdx;

    // ── Model Loading ────────────────────────────────────────────────────────

    public TFLiteModel(Context context) throws IOException {
        interpreter   = new Interpreter(FileUtil.loadMappedFile(context, MODEL_FILE));
        inputSize     = interpreter.getInputTensor(0).shape()[1];
        int[] idx     = resolveOutputIndices();
        boxesIdx      = idx[0];
        scoresIdx     = idx[1];
        numDetIdx     = idx[2];
        maxDetections = idx[3];
        Log.i(TAG, "Model ready — inputSize=" + inputSize + " maxDetections=" + maxDetections);
    }

    private int[] resolveOutputIndices() {
        int foundBoxes = 0, foundScores = 2, foundNumDet = 3, foundMaxDet = 10;
        int numOutputs = interpreter.getOutputTensorCount();
        for (int i = 0; i < numOutputs; i++) {
            int[]  sh = interpreter.getOutputTensor(i).shape();
            String nm = interpreter.getOutputTensor(i).name();
            Log.i(TAG, "  output[" + i + "] name=" + nm + " shape=" + Arrays.toString(sh)
                    + " dtype=" + interpreter.getOutputTensor(i).dataType());
            if (sh.length == 3 && sh[sh.length - 1] == 4) { foundBoxes = i; foundMaxDet = sh[sh.length - 2]; }
            if (sh.length == 1) foundNumDet = i;
        }
        foundScores = (foundBoxes == 2) ? 0 : 2;
        return new int[]{ foundBoxes, foundScores, foundNumDet, foundMaxDet };
    }

    // ── Inference ────────────────────────────────────────────────────────────

    public Task<List<DetectionEntity>> process(InputImage inputImage) {
        Bitmap bitmap = inputImage.getBitmapInternal();
        List<DetectionEntity> result = detect(bitmap).stream()
                .map(rect -> new DetectionEntity(rect, new ArrayList<>()))
                .collect(java.util.stream.Collectors.toList());
        return Tasks.forResult(result);
    }

    public List<Rect> detect(Bitmap bitmap) {
        ByteBuffer input = toInputBuffer(bitmap);
        int bw = bitmap.getWidth();
        int bh = bitmap.getHeight();

        float[][][] boxes    = new float[1][maxDetections][4];
        float[][]   classes  = new float[1][maxDetections];
        float[][]   scores   = new float[1][maxDetections];
        float[]     numDet   = new float[1];

        Map<Integer, Object> outputs = new HashMap<>();
        outputs.put(boxesIdx,  boxes);
        outputs.put(1,         classes);
        outputs.put(scoresIdx, scores);
        outputs.put(numDetIdx, numDet);

        interpreter.runForMultipleInputsOutputs(new Object[]{input}, outputs);

        int n = Math.min((int) numDet[0], maxDetections);
        List<Rect> results = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            float conf = scores[0][i];
            if (conf < CONF_THRESHOLD) continue;
            float ymin = boxes[0][i][0], xmin = boxes[0][i][1];
            float ymax = boxes[0][i][2], xmax = boxes[0][i][3];
            results.add(new Rect(
                    Math.round(xmin * bw), Math.round(ymin * bh),
                    Math.round(xmax * bw), Math.round(ymax * bh)));
        }
        if (n > 0) {
            Log.v(TAG, "  MobileNet SSD detections kept=" + results.size() + "/" + n
                    + " top_score=" + scores[0][0]);
        }
        return results;
    }

    // ── Pre-processing ───────────────────────────────────────────────────────

    private ByteBuffer toInputBuffer(Bitmap bitmap) {
        Bitmap sw = bitmap.getConfig() == Bitmap.Config.HARDWARE
                ? bitmap.copy(Bitmap.Config.ARGB_8888, false) : bitmap;
        Bitmap scaled = Bitmap.createScaledBitmap(sw, inputSize, inputSize, true);
        int[] pixels = new int[inputSize * inputSize];
        scaled.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize);

        ByteBuffer bufOut = ByteBuffer.allocateDirect(inputSize * inputSize * 3);
        bufOut.order(ByteOrder.nativeOrder());
        for (int px : pixels) {
            bufOut.put((byte) ((px >> 16) & 0xFF));
            bufOut.put((byte) ((px >>  8) & 0xFF));
            bufOut.put((byte) (px & 0xFF));
        }
        bufOut.rewind();
        return bufOut;
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void close() {
        interpreter.close();
    }
}
