// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.java.analyzers.customdetector.yolo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Non-Maximum Suppression utility used by YOLO post-processing.
 */
public final class NmsUtils {

    private NmsUtils() {}

    /**
     * Classic greedy NMS.
     *
     * @param boxes        axis-aligned bounding boxes; each is [x1, y1, x2, y2]
     * @param scores       confidence score per box (same index order as boxes)
     * @param iouThreshold boxes with IoU > threshold w.r.t. a kept box are suppressed
     * @return list of kept indices into boxes/scores (descending score order)
     */
    public static List<Integer> nms(List<float[]> boxes, float[] scores, float iouThreshold) {
        int n = boxes.size();
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        java.util.Arrays.sort(order, (a, b) -> Float.compare(scores[b], scores[a]));

        boolean[] suppressed = new boolean[n];
        List<Integer> kept = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            int idx = order[i];
            if (suppressed[idx]) continue;
            kept.add(idx);
            for (int j = i + 1; j < n; j++) {
                int other = order[j];
                if (!suppressed[other] && iou(boxes.get(idx), boxes.get(other)) > iouThreshold) {
                    suppressed[other] = true;
                }
            }
        }
        return kept;
    }

    private static float iou(float[] a, float[] b) {
        float interX1 = Math.max(a[0], b[0]);
        float interY1 = Math.max(a[1], b[1]);
        float interX2 = Math.min(a[2], b[2]);
        float interY2 = Math.min(a[3], b[3]);

        float interW = Math.max(0f, interX2 - interX1);
        float interH = Math.max(0f, interY2 - interY1);
        float inter  = interW * interH;
        if (inter == 0f) return 0f;

        float areaA = Math.max(0f, a[2] - a[0]) * Math.max(0f, a[3] - a[1]);
        float areaB = Math.max(0f, b[2] - b[0]) * Math.max(0f, b[3] - b[1]);
        return inter / (areaA + areaB - inter);
    }
}
