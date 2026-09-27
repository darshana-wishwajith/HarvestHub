package com.harvesthub.mobile.ui.farmer;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.databinding.ActivityCameraCaptureBinding;
import com.harvesthub.mobile.utils.BottomNavHelper;
import com.harvesthub.mobile.utils.NotificationPanelHelper;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraCaptureActivity extends AppCompatActivity {

    private static final String TAG = "CameraCaptureActivity";
    private static final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = new String[]{Manifest.permission.CAMERA};

    private ActivityCameraCaptureBinding binding;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private boolean isLocationLocked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCameraCaptureBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupNavigation();
        setupTabs();
        setupClickListeners();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void setupNavigation() {
        // Setup Top Navigation
        View btnNotification = binding.topNavBar.btnNotification;
        btnNotification.setOnClickListener(v -> NotificationPanelHelper.showNotificationPanel(this, v));

        // Setup Bottom Navigation
        BottomNavHelper.setup(this, binding.bottomNavBar.getRoot(), BottomNavHelper.Tab.LISTINGS);
    }

    private void setupClickListeners() {
        binding.btnCapture.setOnClickListener(v -> {
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).withEndAction(() -> {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                takePhoto();
            }).start();
        });

        binding.btnFlip.setOnClickListener(v -> {
            lensFacing = (lensFacing == CameraSelector.LENS_FACING_BACK) ? 
                    CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
            startCamera();
        });

        binding.btnLock.setOnClickListener(v -> {
            isLocationLocked = !isLocationLocked;
            binding.btnLock.setSelected(isLocationLocked);
            binding.btnLock.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(this, isLocationLocked ? R.color.accent_green : R.color.white)));
            Toast.makeText(this, isLocationLocked ? "Farm Location Locked" : "Location Unlocked", Toast.LENGTH_SHORT).show();
        });
    }

    private void setupTabs() {
        binding.tabWideAngle.setOnClickListener(v -> updateMode("Step 1: Capture Wide Angle", "Show the entire cultivated area", binding.tabWideAngle));
        binding.tabCloseUp.setOnClickListener(v -> updateMode("Step 2: Capture Close Up", "Show details of the crop", binding.tabCloseUp));
        binding.tabVideo.setOnClickListener(v -> updateMode("Step 3: Record Video", "Capture a 10-second overview", binding.tabVideo));
    }

    private void updateMode(String title, String subtitle, TextView activeTab) {
        binding.tvStepTitle.setText(title);
        binding.tvStepSubtitle.setText(subtitle);

        // Reset tabs
        binding.tabWideAngle.setTextColor(Color.WHITE);
        binding.tabCloseUp.setTextColor(Color.WHITE);
        binding.tabVideo.setTextColor(Color.WHITE);

        // Set active
        activeTab.setTextColor(ContextCompat.getColor(this, R.color.accent_green));
        
        // Add subtle animation to text change
        binding.headerLayout.setAlpha(0f);
        binding.headerLayout.animate().alpha(1f).setDuration(300).start();
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        String name = new SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/HarvestHub");
        }

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions
                .Builder(getContentResolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                .build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Toast.makeText(getBaseContext(), "Photo saved successfully", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                    }
                });
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(lensFacing)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
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
                Toast.makeText(this, "Permissions not granted.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
