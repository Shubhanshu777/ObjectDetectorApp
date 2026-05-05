package com.rishi.objectdetector;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DetectionOverlayView extends View {

    private List<Detection> results = new ArrayList<>();
    private int imageWidth = 1;
    private int imageHeight = 1;

    private final Paint boxPaint;
    private final Paint textBackgroundPaint;
    private final Paint textPaint;
    private final Paint shadowPaint;

    private static final Map<String, Integer> LABEL_COLORS = new HashMap<>();

    static {
        LABEL_COLORS.put("pen", Color.rgb(255, 87, 34));       // Deep Orange
        LABEL_COLORS.put("paper", Color.rgb(156, 39, 176));    // Purple
        LABEL_COLORS.put("computer", Color.rgb(33, 150, 243)); // Blue
        LABEL_COLORS.put("table", Color.rgb(121, 85, 72));     // Brown
        LABEL_COLORS.put("chair", Color.rgb(255, 193, 7));     // Amber
        LABEL_COLORS.put("person", Color.rgb(76, 175, 80));    // Green
        LABEL_COLORS.put("laptop", Color.rgb(0, 188, 212));    // Cyan
        LABEL_COLORS.put("mouse", Color.rgb(233, 30, 99));     // Pink
        LABEL_COLORS.put("keyboard", Color.rgb(103, 58, 183)); // Deep Purple
    }

    public DetectionOverlayView(Context context) {
        this(context, null);
    }

    public DetectionOverlayView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DetectionOverlayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        boxPaint = new Paint();
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(6f);
        boxPaint.setAntiAlias(true);
        boxPaint.setStrokeCap(Paint.Cap.ROUND);
        boxPaint.setStrokeJoin(Paint.Join.ROUND);

        textBackgroundPaint = new Paint();
        textBackgroundPaint.setStyle(Paint.Style.FILL);
        textBackgroundPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(42f);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setAntiAlias(true);

        shadowPaint = new Paint();
        shadowPaint.setStyle(Paint.Style.STROKE);
        shadowPaint.setStrokeWidth(10f);
        shadowPaint.setColor(Color.argb(80, 0, 0, 0));
        shadowPaint.setAntiAlias(true);
    }

    public void setResults(@NonNull List<Detection> detectionResults, int imageHeight, int imageWidth) {
        this.results = new ArrayList<>(detectionResults);
        this.imageHeight = Math.max(1, imageHeight);
        this.imageWidth = Math.max(1, imageWidth);
        postInvalidate();
    }

    public void clear() {
        this.results.clear();
        postInvalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (results.isEmpty()) {
            return;
        }

        float scaleX = (float) getWidth() / imageWidth;
        float scaleY = (float) getHeight() / imageHeight;

        for (Detection detection : results) {
            drawDetection(canvas, detection, scaleX, scaleY);
        }
    }

    private void drawDetection(Canvas canvas, Detection detection, float scaleX, float scaleY) {
        RectF boundingBox = detection.getBoundingBox();
        String label = detection.getLabel();
        float confidence = detection.getConfidence();

        // Scale bounding box to view coordinates
        float left = boundingBox.left * scaleX;
        float top = boundingBox.top * scaleY;
        float right = boundingBox.right * scaleX;
        float bottom = boundingBox.bottom * scaleY;

        // Clamp to view bounds
        left = Math.max(0, Math.min(left, getWidth()));
        top = Math.max(0, Math.min(top, getHeight()));
        right = Math.max(0, Math.min(right, getWidth()));
        bottom = Math.max(0, Math.min(bottom, getHeight()));

        // Get color for this label
        Integer colorInt = LABEL_COLORS.get(label);
        int color = (colorInt != null) ? colorInt : Color.GREEN;

        // Draw shadow for depth
        canvas.drawRect(left, top, right, bottom, shadowPaint);

        // Draw bounding box
        boxPaint.setColor(color);
        canvas.drawRect(left, top, right, bottom, boxPaint);

        // Draw corner accents
        drawCornerAccents(canvas, left, top, right, bottom, color);

        // Prepare label text
        String labelText = String.format("%s %.0f%%", capitalize(label), confidence * 100);

        // Measure text
        Rect textBounds = new Rect();
        textPaint.getTextBounds(labelText, 0, labelText.length(), textBounds);

        float padding = 10f;
        float textWidth = textBounds.width() + padding * 2;
        float textHeight = textBounds.height() + padding * 2;

        // Position label above box, or below if no room
        float labelTop;
        float labelBottom;
        if (top - textHeight > 0) {
            labelTop = top - textHeight;
            labelBottom = top;
        } else {
            labelTop = top;
            labelBottom = top + textHeight;
        }

        // Draw label background
        textBackgroundPaint.setColor(color);
        RectF labelRect = new RectF(left, labelTop, left + textWidth, labelBottom);
        canvas.drawRoundRect(labelRect, 8f, 8f, textBackgroundPaint);

        // Draw label text
        float textX = left + padding;
        float textY = labelBottom - padding - (textBounds.height() - textBounds.bottom) / 2f;
        canvas.drawText(labelText, textX, textY, textPaint);
    }

    private void drawCornerAccents(Canvas canvas, float left, float top, float right, float bottom, int color) {
        Paint accentPaint = new Paint();
        accentPaint.setColor(color);
        accentPaint.setStyle(Paint.Style.STROKE);
        accentPaint.setStrokeWidth(10f);
        accentPaint.setStrokeCap(Paint.Cap.ROUND);

        float cornerLength = 25f;

        // Top-left corner
        canvas.drawLine(left, top, left + cornerLength, top, accentPaint);
        canvas.drawLine(left, top, left, top + cornerLength, accentPaint);

        // Top-right corner
        canvas.drawLine(right - cornerLength, top, right, top, accentPaint);
        canvas.drawLine(right, top, right, top + cornerLength, accentPaint);

        // Bottom-left corner
        canvas.drawLine(left, bottom - cornerLength, left, bottom, accentPaint);
        canvas.drawLine(left, bottom, left + cornerLength, bottom, accentPaint);

        // Bottom-right corner
        canvas.drawLine(right - cornerLength, bottom, right, bottom, accentPaint);
        canvas.drawLine(right, bottom - cornerLength, right, bottom, accentPaint);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
