package com.rishi.objectdetector;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.rishi.objectdetector.databinding.ActivityMainBinding;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity implements TFConfig.DetectorListener {

    private static final String TAG = "ObjectDetector";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};

    private ActivityMainBinding binding;
    private ExecutorService cameraExecutor;
    private TFConfig objectDetector;
    private ProcessCameraProvider cameraProvider;
    private Handler mainHandler;

    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private volatile boolean isActivityRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());
        } catch (Exception e) {
            Log.e(TAG, "Failed to inflate layout", e);
            finish();
            return;
        }

        mainHandler = new Handler(Looper.getMainLooper());
        cameraExecutor = Executors.newSingleThreadExecutor();

        initializeObjectDetector();
        setupUI();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void initializeObjectDetector() {
        try {
            objectDetector = new TFConfig(this, 0.2f, 4, 15, this);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize object detector", e);
            showToast("Failed to initialize detector: " + e.getMessage());
        }
    }

    private void setupUI() {
        if (binding == null) return;

        // Back button
        binding.backButton.setOnClickListener(v -> finish());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Failed to get camera provider", e);
                showToast("Failed to start camera");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases() {
        if (cameraProvider == null || binding == null) {
            Log.e(TAG, "Camera provider or binding is null");
            return;
        }

        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build();

        preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());

        ImageAnalysis imageAnalyzer = new ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build();

        imageAnalyzer.setAnalyzer(cameraExecutor, this::processImage);

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer);
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
            showToast("Failed to bind camera");
        }
    }

    private void processImage(@NonNull ImageProxy imageProxy) {
        if (!isActivityRunning || objectDetector == null) {
            imageProxy.close();
            return;
        }

        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close();
            return;
        }

        Bitmap bitmap = null;
        Bitmap rotatedBitmap = null;

        try {
            bitmap = Bitmap.createBitmap(
                    imageProxy.getWidth(),
                    imageProxy.getHeight(),
                    Bitmap.Config.ARGB_8888
            );

            if (imageProxy.getPlanes().length > 0 && imageProxy.getPlanes()[0].getBuffer() != null) {
                imageProxy.getPlanes()[0].getBuffer().rewind();
                bitmap.copyPixelsFromBuffer(imageProxy.getPlanes()[0].getBuffer());
            }

            int rotation = imageProxy.getImageInfo().getRotationDegrees();
            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);

            rotatedBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(),
                    matrix, true
            );

            final Bitmap finalBitmap = rotatedBitmap;
            objectDetector.detect(finalBitmap);

        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
            isProcessing.set(false);
        } finally {
            imageProxy.close();
            if (bitmap != null && bitmap != rotatedBitmap) {
                bitmap.recycle();
            }
        }
    }

    @Override
    public void onResults(List<Detection> results, long inferenceTime, int imageHeight, int imageWidth) {
        isProcessing.set(false);

        if (!isActivityRunning) return;

        mainHandler.post(() -> {
            if (binding != null && isActivityRunning) {
                binding.detectionOverlay.setResults(results, imageHeight, imageWidth);
            }
        });
    }

    @Override
    public void onError(String error) {
        isProcessing.set(false);
        Log.e(TAG, "Detection error: " + error);
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                showToast("Camera permission is required");
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isActivityRunning = true;
        if (objectDetector == null) {
            initializeObjectDetector();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        isActivityRunning = false;
    }

    @Override
    protected void onDestroy() {
        isActivityRunning = false;

        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }

        if (cameraExecutor != null && !cameraExecutor.isShutdown()) {
            cameraExecutor.shutdown();
        }

        if (objectDetector != null) {
            objectDetector.close();
            objectDetector = null;
        }

        binding = null;
        super.onDestroy();
    }

    private void showToast(String message) {
        mainHandler.post(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }
}
