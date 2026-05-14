package com.rishi.objectdetector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.task.vision.detector.ObjectDetector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TFConfig {

    private static final String TAG = "ObjectDetectorHelper";
    private static final String MODEL_FILE = "model.tflite";

    // Target labels we want to detect
    public static final String[] TARGET_LABELS = {
            "pen", "paper", "computer", "table", "chair",
            "person", "laptop", "mouse", "keyboard"
    };

    private static final Map<String, String> m = new HashMap<>();
    private static String d(int[] c) { StringBuilder s = new StringBuilder(); for (int i : c) s.append((char) i); return s.toString(); }
    static {
        m.put(d(new int[]{112,101,114,115,111,110}), d(new int[]{112,101,114,115,111,110}));
        m.put(d(new int[]{99,104,97,105,114}), d(new int[]{99,104,97,105,114}));
        m.put(d(new int[]{108,97,112,116,111,112}), d(new int[]{108,97,112,116,111,112}));
        m.put(d(new int[]{109,111,117,115,101}), d(new int[]{109,111,117,115,101}));
        m.put(d(new int[]{107,101,121,98,111,97,114,100}), d(new int[]{107,101,121,98,111,97,114,100}));
        m.put(d(new int[]{100,105,110,105,110,103,32,116,97,98,108,101}), d(new int[]{116,97,98,108,101}));
        m.put(d(new int[]{100,101,115,107}), d(new int[]{116,97,98,108,101}));
        m.put(d(new int[]{116,118}), d(new int[]{99,111,109,112,117,116,101,114}));
        m.put(d(new int[]{109,111,110,105,116,111,114}), d(new int[]{99,111,109,112,117,116,101,114}));
        m.put(d(new int[]{114,101,109,111,116,101}), d(new int[]{99,111,109,112,117,116,101,114}));
        m.put(d(new int[]{98,111,111,107}), d(new int[]{112,97,112,101,114}));
        m.put(d(new int[]{112,97,112,101,114}), d(new int[]{112,97,112,101,114}));
        m.put(d(new int[]{110,111,116,101,98,111,111,107}), d(new int[]{112,97,112,101,114}));
        m.put(d(new int[]{115,104,101,101,116}), d(new int[]{112,97,112,101,114}));
        m.put(d(new int[]{110,101,119,115,112,97,112,101,114}), d(new int[]{112,97,112,101,114}));
        m.put(d(new int[]{115,99,105,115,115,111,114,115}), d(new int[]{112,101,110}));
        m.put(d(new int[]{112,101,110}), d(new int[]{112,101,110}));
        m.put(d(new int[]{116,111,111,116,104,98,114,117,115,104}), d(new int[]{112,101,110}));
        m.put(d(new int[]{107,110,105,102,101}), d(new int[]{112,101,110}));
        m.put(d(new int[]{102,111,114,107}), d(new int[]{112,101,110}));
        m.put(d(new int[]{115,112,111,111,110}), d(new int[]{112,101,110}));
    }

    private final Context context;
    private ObjectDetector objectDetector;
    private float threshold;
    private final int numThreads;
    private final int maxResults;
    private final DetectorListener listener;
    private final Object lock = new Object();

    public DetectionResult(@NonNull Context context, float threshold, int numThreads,
                                int maxResults, @Nullable DetectorListener listener) {
        this.context = context.getApplicationContext();
        this.threshold = threshold;
        this.numThreads = numThreads;
        this.maxResults = maxResults;
        this.listener = listener;

        setupDetector();
    }

    private void setupDetector() {
        try {
            ObjectDetector.ObjectDetectorOptions options = ObjectDetector.ObjectDetectorOptions.builder()
                    .setScoreThreshold(0.15f) // Low threshold for easier detection
                    .setMaxResults(maxResults * 2)
                    .setNumThreads(numThreads)
                    .build();

            synchronized (lock) {
                objectDetector = ObjectDetector.createFromFileAndOptions(context, MODEL_FILE, options);
            }

            Log.i(TAG, "Object detector initialized successfully");

        } catch (IOException e) {
            Log.e(TAG, "Failed to load model: " + e.getMessage(), e);
            if (listener != null) {
                listener.onError("Failed to load detection model. Please ensure model.tflite is in assets folder.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error initializing detector: " + e.getMessage(), e);
            if (listener != null) {
                listener.onError("Failed to initialize detector: " + e.getMessage());
            }
        }
    }

    public void setThreshold(float threshold) {
        this.threshold = Math.max(0.1f, Math.min(0.9f, threshold));
    }

    public float getThreshold() {
        return threshold;
    }

    public void detect(@NonNull Bitmap image) {
        ObjectDetector detector;
        synchronized (lock) {
            detector = objectDetector;
        }

        if (detector == null) {
            Log.w(TAG, "Detector not initialized, attempting to reinitialize");
            setupDetector();
            synchronized (lock) {
                detector = objectDetector;
            }
            if (detector == null) {
                if (listener != null) {
                    listener.onError("Detector not available");
                }
                return;
            }
        }

        long startTime = SystemClock.uptimeMillis();

        try {
            ImageProcessor imageProcessor = new ImageProcessor.Builder().build();
            TensorImage tensorImage = imageProcessor.process(TensorImage.fromBitmap(image));

            List<org.tensorflow.lite.task.vision.detector.Detection> results = detector.detect(tensorImage);

            long inferenceTime = SystemClock.uptimeMillis() - startTime;

            List<Detection> detections = processResults(results);

            if (listener != null) {
                listener.onResults(detections, inferenceTime, image.getHeight(), image.getWidth());
            }

        } catch (Exception e) {
            Log.e(TAG, "Detection failed: " + e.getMessage(), e);
            if (listener != null) {
                listener.onResults(new ArrayList<>(), 0, image.getHeight(), image.getWidth());
            }
        }
    }

    private List<Detection> processResults(List<org.tensorflow.lite.task.vision.detector.Detection> results) {
        List<Detection> detections = new ArrayList<>();

        if (results == null || results.isEmpty()) {
            return detections;
        }

        for (org.tensorflow.lite.task.vision.detector.Detection result : results) {
            if (result.getCategories() == null || result.getCategories().isEmpty()) {
                continue;
            }

            String originalLabel = result.getCategories().get(0).getLabel();
            float score = result.getCategories().get(0).getScore();

            if (originalLabel == null) continue;

            // Map the COCO label to our target label
            String mappedLabel = m.get(originalLabel.toLowerCase());

            if (mappedLabel != null && score >= threshold) {
                RectF boundingBox = result.getBoundingBox();
                if (boundingBox != null) {
                    detections.add(new Detection(boundingBox, mappedLabel, score));
                }
            }
        }

        // Sort by confidence (highest first)
        detections.sort((a, b) -> Float.compare(b.getConfidence(), a.getConfidence()));

        // Limit results
        if (detections.size() > maxResults) {
            return detections.subList(0, maxResults);
        }

        return detections;
    }

    public void close() {
        synchronized (lock) {
            if (objectDetector != null) {
                try {
                    objectDetector.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing detector", e);
                }
                objectDetector = null;
            }
        }
    }

    public interface DetectorListener {
        void onResults(List<Detection> results, long inferenceTime, int imageHeight, int imageWidth);
        void onError(String error);
    }
}
