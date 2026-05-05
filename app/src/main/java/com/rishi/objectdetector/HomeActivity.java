package com.rishi.objectdetector;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AppCompatActivity;

import com.rishi.objectdetector.databinding.ActivityHomeBinding;

public class HomeActivity extends AppCompatActivity {

    private ActivityHomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupUI();
        startAnimations();
    }

    private void setupUI() {
        binding.cameraButton.setOnClickListener(v -> {
            // Add click animation
            v.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(() -> {
                        v.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(100)
                                .withEndAction(() -> {
                                    // Open camera activity
                                    Intent intent = new Intent(HomeActivity.this, MainActivity.class);
                                    startActivity(intent);
                                })
                                .start();
                    })
                    .start();
        });
    }

    private void startAnimations() {
        // Fade in animation for title
        binding.appTitle.setAlpha(0f);
        binding.appTitle.animate()
                .alpha(1f)
                .setDuration(800)
                .setStartDelay(200)
                .start();

        // Fade in animation for subtitle
        binding.appSubtitle.setAlpha(0f);
        binding.appSubtitle.animate()
                .alpha(1f)
                .setDuration(800)
                .setStartDelay(400)
                .start();

        // Scale animation for camera button
        binding.cameraButton.setScaleX(0f);
        binding.cameraButton.setScaleY(0f);
        binding.cameraButton.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setStartDelay(600)
                .start();

        // Fade in for labels list
        binding.labelsCard.setAlpha(0f);
        binding.labelsCard.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setStartDelay(800)
                .start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
