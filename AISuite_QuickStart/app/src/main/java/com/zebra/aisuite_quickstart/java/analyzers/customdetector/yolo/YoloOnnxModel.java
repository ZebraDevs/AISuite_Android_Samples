// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.analyzers.customdetector.yolo;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.mlkit.vision.common.InputImage;
import com.zebra.ai.vision.entity.DetectionEntity;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * YOLOv8n general object detector — 80 COCO classes (ONNX Runtime).
 *
 * Input : [1, 3, 640, 640] float32 CHW, RGB normalised to [0, 1].
 * Output: [1, 84, 8400] — cx/cy/w/h + 80 class scores per prediction.
 *
 * OrtEnvironment is a JVM singleton — do NOT close it here; only the session is released.
 */
public class YoloOnnxModel implements AutoCloseable {

    private static final String TAG             = "YoloOnnxModel";
    private static final String MODEL_FILE      = "models/yolov8n-onnx/yolov8n.onnx";
    private static final int    INPUT_SIZE      = 640;
    private static final float  CONF_THRESHOLD  = 0.40f;
    private static final float  IOU_THRESHOLD   = 0.45f;
    private static final int    NUM_PREDICTIONS = 8400;
    private static final int    BOX_DIMS        = 4;
    private static final int    NUM_CLASSES     = 80;

    private final OrtEnvironment env;
    private final OrtSession     session;

    public YoloOnnxModel(Context context) throws IOException, OrtException {
        Log.d(TAG, "Loading ONNX session from assets: " + MODEL_FILE);
        env = OrtEnvironment.getEnvironment();
        android.content.res.AssetFileDescriptor fd = context.getAssets().openFd(MODEL_FILE);
        MappedByteBuffer buffer = new FileInputStream(fd.getFileDescriptor())
                .getChannel()
                .map(FileChannel.MapMode.READ_ONLY, fd.getStartOffset(), fd.getDeclaredLength());
        session = env.createSession(buffer);
        Log.i(TAG, "ONNX session ready — inputs=" + session.getInputNames()
                + " outputs=" + session.getOutputNames());
    }

    public Task<List<DetectionEntity>> process(
            InputImage inputImage) {
        try {
            List<DetectionEntity> result =
                    detect(inputImage.getBitmapInternal()).stream()
                            .map(rect -> new DetectionEntity(rect, new ArrayList<>()))
                            .collect(Collectors.toList());
            return Tasks.forResult(result);
        } catch (OrtException e) {
            return Tasks.forException(e);
        }
    }

    public List<Rect> detect(Bitmap bitmap) throws OrtException {
        float[] inputFloats = bitmapToInputFloats(bitmap);
        long[] shape        = {1L, 3L, INPUT_SIZE, INPUT_SIZE};
        String inputName    = session.getInputNames().iterator().next();

        OnnxTensor input = OnnxTensor.createTensor(env, FloatBuffer.wrap(inputFloats), shape);
        try {
            Map<String, OnnxTensor> inputMap = new HashMap<>();
            inputMap.put(inputName, input);
            try (OrtSession.Result results = session.run(inputMap)) {
                OnnxTensor outTensor = (OnnxTensor) results.get(0);
                FloatBuffer buf = outTensor.getFloatBuffer();
                float[] raw = new float[buf.remaining()];
                buf.get(raw);
                return decodeYolov8(raw, bitmap.getWidth(), bitmap.getHeight());
            }
        } finally {
            input.close();
        }
    }

    // YOLOv8n output: [1, 84, 8400] row-major — first index = feature, second = prediction
    // Channels 0-3: cx, cy, w, h in 640-px model space. Channels 4-83: 80 COCO class scores.
    private List<Rect> decodeYolov8(float[] raw, int bitmapW, int bitmapH) {
        List<float[]> boxes  = new ArrayList<>();
        List<Float>   scores = new ArrayList<>();

        for (int p = 0; p < NUM_PREDICTIONS; p++) {
            float maxScore = 0f;
            for (int c = 0; c < NUM_CLASSES; c++) {
                float s = raw[(BOX_DIMS + c) * NUM_PREDICTIONS + p];
                if (s > maxScore) maxScore = s;
            }
            if (maxScore < CONF_THRESHOLD) continue;

            float cx = raw[0 * NUM_PREDICTIONS + p];
            float cy = raw[1 * NUM_PREDICTIONS + p];
            float w  = raw[2 * NUM_PREDICTIONS + p];
            float h  = raw[3 * NUM_PREDICTIONS + p];
            boxes.add(new float[]{cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f});
            scores.add(maxScore);
        }

        if (boxes.isEmpty()) return new ArrayList<>();

        float[] scoresArr = new float[scores.size()];
        for (int i = 0; i < scores.size(); i++) scoresArr[i] = scores.get(i);

        List<Integer> kept = NmsUtils.nms(boxes, scoresArr, IOU_THRESHOLD);
        List<Rect> results = new ArrayList<>(kept.size());
        for (int idx : kept) {
            float[] b = boxes.get(idx);
            results.add(new Rect(
                    Math.round(b[0] / INPUT_SIZE * bitmapW),
                    Math.round(b[1] / INPUT_SIZE * bitmapH),
                    Math.round(b[2] / INPUT_SIZE * bitmapW),
                    Math.round(b[3] / INPUT_SIZE * bitmapH)));
        }
        return results;
    }

    private float[] bitmapToInputFloats(Bitmap bitmap) {
        Bitmap software = bitmap.getConfig() == Bitmap.Config.HARDWARE
                ? bitmap.copy(Bitmap.Config.ARGB_8888, false) : bitmap;
        Bitmap scaled = Bitmap.createScaledBitmap(software, INPUT_SIZE, INPUT_SIZE, true);
        int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];
        scaled.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE);

        int n = INPUT_SIZE * INPUT_SIZE;
        float[] floats = new float[3 * n];
        for (int i = 0; i < n; i++) {
            floats[i]         = (pixels[i] >> 16 & 0xFF) / 255f;
            floats[n + i]     = (pixels[i] >>  8 & 0xFF) / 255f;
            floats[2 * n + i] = (pixels[i]        & 0xFF) / 255f;
        }
        return floats;
    }

    @Override
    public void close() throws OrtException {
        session.close();
        // Do NOT close OrtEnvironment — it is a JVM-wide singleton
    }
}
