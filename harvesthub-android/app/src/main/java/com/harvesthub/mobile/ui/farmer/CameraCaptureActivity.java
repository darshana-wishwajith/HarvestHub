package com.harvesthub.mobile.ui.farmer;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.harvesthub.mobile.R;

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

    private PreviewView viewFinder;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;

    private TextView tvStepTitle, tvStepSubtitle;
    private TextView tabWideAngle, tabCloseUp, tabVideo;
    private ImageButton btnLock, btnFlip;
    
    private int lensFacing = CameraSelector.LENS_FACING_BACK;
    private boolean isLocationLocked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_capture);

        // Initialize Views
        viewFinder = findViewById(R.id.viewFinder);
        tvStepTitle = findViewById(R.id.tvStepTitle);
        tvStepSubtitle = findViewById(R.id.tvStepSubtitle);
        tabWideAngle = findViewById(R.id.tabWideAngle);
        tabCloseUp = findViewById(R.id.tabCloseUp);
        tabVideo = findViewById(R.id.tabVideo);
        btnLock = findViewById(R.id.btnLock);
        btnFlip = findViewById(R.id.btnFlip);
        ImageButton btnCapture = findViewById(R.id.btnCapture);

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        btnLock.setOnClickListener(v -> {
            isLocationLocked = !isLocationLocked;
            btnLock.setSelected(isLocationLocked);
            Toast.makeText(this, isLocationLocked ? "Location Locked" : "Location Unlocked", Toast.LENGTH_SHORT).show();
        });

        setupTabs();

        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void setupTabs() {
        tabWideAngle.setOnClickListener(v -> updateMode("Step 1: Capture Wide Angle", "Show the entire cultivated area", tabWideAngle));
        tabCloseUp.setOnClickListener(v -> updateMode("Step 2: Capture Close Up", "Show details of the crop", tabCloseUp));
        tabVideo.setOnClickListener(v -> updateMode("Step 3: Record Video", "Capture a 10-second overview", tabVideo));
    }

    private void updateMode(String title, String subtitle, TextView activeTab) {
        tvStepTitle.setText(title);
        tvStepSubtitle.setText(subtitle);

        // Reset tab styles
        tabWideAngle.setTextColor(ContextCompat.getColor(this, R.color.text_gray));
        tabWideAngle.setTypeface(null, android.graphics.Typeface.NORMAL);
        tabCloseUp.setTextColor(ContextCompat.getColor(this, R.color.text_gray));
        tabCloseUp.setTypeface(null, android.graphics.Typeface.NORMAL);
        tabVideo.setTextColor(ContextCompat.getColor(this, R.color.text_gray));
        tabVideo.setTypeface(null, android.graphics.Typeface.NORMAL);

        // Set active tab style
        activeTab.setTextColor(ContextCompat.getColor(this, R.color.accent_green_tabs));
        activeTab.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        String name = new SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis());
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/HarvestHub-Image");
        }

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions
                .Builder(getContentResolver(), MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                .build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        String msg = "Photo capture succeeded: " + outputFileResults.getSavedUri();
                        Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, msg);
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
                preview.setSurfaceProvider(viewFinder.getSurfaceProvider());

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
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
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
