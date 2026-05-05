package com.rishi.objectdetector;

import android.graphics.RectF;

import androidx.annotation.NonNull;

public class Detection {
    private final RectF boundingBox;
    private final String label;
    private final float confidence;

    public Detection(@NonNull RectF boundingBox, @NonNull String label, float confidence) {
        this.boundingBox = boundingBox;
        this.label = label;
        this.confidence = confidence;
    }

    @NonNull
    public RectF getBoundingBox() {
        return boundingBox;
    }

    @NonNull
    public String getLabel() {
        return label;
    }

    public float getConfidence() {
        return confidence;
    }

    @NonNull
    @Override
    public String toString() {
        return "Detection{" +
                "label='" + label + '\'' +
                ", confidence=" + confidence +
                ", box=" + boundingBox +
                '}';
    }
}
