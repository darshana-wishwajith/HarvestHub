package com.harvesthub.mobile.ui.farmer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
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
import androidx.camera.video.FallbackStrategy;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.harvesthub.mobile.MainActivity;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.databinding.ActivityCameraCaptureBinding;
import com.harvesthub.mobile.model.Listing;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.utils.AuthInputUtils;
import com.harvesthub.mobile.utils.BottomNavHelper;
import com.harvesthub.mobile.utils.NotificationPanelHelper;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * "Smart Guided Camera" screen. Live capture only (no gallery), all inside the app:
 * step 1 wide-angle photo, step 2 close-up photo, step 3 a 10-second video (stops by itself). Each file is
 * uploaded right away; after the third the farmer goes straight to the Listing Details form, which shows the
 * captured media (or, for a retake, the new media is re-submitted). GPS comes from the location lock.
 */
public class CameraCaptureActivity extends AppCompatActivity {

    private static final String TAG = "CameraCapture";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final int VIDEO_SECONDS = 10;
    /** Local copies of the captured files, shown as thumbnails on the details form. */
    public static final String EXTRA_LOCAL_MEDIA = "localMedia";

    private ActivityCameraCaptureBinding binding;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture;
    private VideoCapture<Recorder> videoCapture;
    private Recording recording;
    private CountDownTimer recordTimer;
    private int lensFacing = CameraSelector.LENS_FACING_BACK;

    private int step = 0;                       // 0 wide, 1 close-up, 2 video
    private final String[] uploaded = new String[3];
    private final String[] localFiles = new String[3];
    private double lat, lng;
    private String retakeListingId;
    private boolean busy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCameraCaptureBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        lat = getIntent().getDoubleExtra(LocationVerificationActivity.EXTRA_LAT, Double.NaN);
        lng = getIntent().getDoubleExtra(LocationVerificationActivity.EXTRA_LNG, Double.NaN);
        retakeListingId = getIntent().getStringExtra(LocationVerificationActivity.EXTRA_RETAKE_LISTING_ID);
        if (Double.isNaN(lat) || Double.isNaN(lng)) {
            // never capture without the location lock
            startActivity(new Intent(this, LocationVerificationActivity.class).putExtras(getIntent()));
            finish();
            return;
        }

        binding.topNavBar.btnNotification.setOnClickListener(v -> NotificationPanelHelper.showNotificationPanel(this, v));
        BottomNavHelper.setup(this, binding.bottomNavBar.getRoot(), BottomNavHelper.Tab.LISTINGS);

        // Location lock: already locked by the previous screen (green lock)
        binding.btnLock.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accent_green)));
        binding.btnLock.setOnClickListener(v ->
                Toast.makeText(this, String.format(Locale.US, "Location locked at your farm: %.5f, %.5f", lat, lng), Toast.LENGTH_SHORT).show());
        binding.btnFlip.setOnClickListener(v -> {
            if (busy) return;
            lensFacing = lensFacing == CameraSelector.LENS_FACING_BACK ? CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
            bindUseCases();
        });
        binding.btnCapture.setOnClickListener(v -> capture());
        showStep();

        if (allPermissionsGranted()) startCamera();
        else ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_PERMISSIONS);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (recordTimer != null) recordTimer.cancel();
        if (recording != null) recording.close();
    }

    private void showStep() {
        String[] titles = {"Step 1: Capture Wide Angle", "Step 2: Capture Close Up", "Step 3: Record Video"};
        String[] subs = {"Show the entire cultivated area", "Show a freshly harvested sample: size, texture, quality",
                "10 seconds: field condition + 360° view of the sample"};
        TextView[] tabs = {binding.tabWideAngle, binding.tabCloseUp, binding.tabVideo};
        binding.tvStepTitle.setText(titles[step]);
        binding.tvStepSubtitle.setText(subs[step]);
        for (int i = 0; i < tabs.length; i++) {
            tabs[i].setTextColor(i == step ? ContextCompat.getColor(this, R.color.accent_green)
                    : (uploaded[i] != null ? Color.parseColor("#A7F3D0") : Color.parseColor("#D1D5DB")));
        }
        setShutterColor(step == 2 ? "#DC2626" : "#1B4332");
    }

    private void setShutterColor(String color) {
        if (binding.btnCapture.getBackground() instanceof LayerDrawable) {
            android.graphics.drawable.Drawable core = ((LayerDrawable) binding.btnCapture.getBackground().mutate())
                    .findDrawableByLayerId(R.id.shutterCore);
            if (core instanceof GradientDrawable) ((GradientDrawable) core).setColor(Color.parseColor(color));
        }
    }

    // ---------------------------------------------------------------- capture

    private void capture() {
        if (busy) return;
        if (step == 2) {
            if (recording == null) startRecording();
            return;
        }
        if (imageCapture == null) return;
        File file = new File(getCacheDir(), "capture_" + step + "_" + System.currentTimeMillis() + ".jpg");
        busy = true;
        ImageCapture.Metadata meta = new ImageCapture.Metadata();
        meta.setLocation(farmLocation());   // geotag: GPS written into the photo's EXIF
        imageCapture.takePicture(new ImageCapture.OutputFileOptions.Builder(file).setMetadata(meta).build(), ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults r) {
                        busy = false;
                        upload(file, "image/jpeg", step);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        busy = false;
                        Log.e(TAG, "Photo capture failed", e);
                        Toast.makeText(CameraCaptureActivity.this, "Photo failed, try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** Records exactly {@value #VIDEO_SECONDS} seconds in the app, then uploads it. */
    private void startRecording() {
        if (videoCapture == null) return;
        File file = new File(getCacheDir(), "capture_video_" + System.currentTimeMillis() + ".mp4");
        busy = true;
        binding.tvRecording.setVisibility(View.VISIBLE);
        binding.tvRecording.setText("● REC " + VIDEO_SECONDS + "s");
        try {
            recording = videoCapture.getOutput()
                    .prepareRecording(this, new FileOutputOptions.Builder(file).setLocation(farmLocation()).build())   // geotagged; no audio
                    .start(ContextCompat.getMainExecutor(this), event -> {
                        if (!(event instanceof VideoRecordEvent.Finalize)) return;
                        VideoRecordEvent.Finalize fin = (VideoRecordEvent.Finalize) event;
                        recording = null;
                        if (recordTimer != null) recordTimer.cancel();
                        binding.tvRecording.setVisibility(View.GONE);
                        busy = false;
                        if (fin.hasError() && fin.getError() != VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED || !file.exists() || file.length() == 0) {
                            Log.e(TAG, "Recording failed: " + fin.getError(), fin.getCause());
                            Toast.makeText(this, "Video could not be recorded. Tap the button to try again.", Toast.LENGTH_LONG).show();
                            return;
                        }
                        upload(file, "video/mp4", 2);
                    });
        } catch (Exception e) {
            busy = false;
            binding.tvRecording.setVisibility(View.GONE);
            Toast.makeText(this, "Video recording is not available on this phone.", Toast.LENGTH_LONG).show();
            return;
        }
        recordTimer = new CountDownTimer(VIDEO_SECONDS * 1000L, 1000) {
            @Override
            public void onTick(long left) {
                binding.tvRecording.setText(String.format(Locale.US, "● REC %ds", (left + 999) / 1000));
            }

            @Override
            public void onFinish() {
                if (recording != null) recording.stop();
            }
        }.start();
    }

    private void upload(File file, String type, int forStep) {
        busy = true;
        localFiles[forStep] = file.getAbsolutePath();
        binding.uploadOverlay.setVisibility(View.VISIBLE);
        binding.tvUploading.setText(String.format(Locale.US, "Uploading %s (%d of 3)…", forStep == 2 ? "video" : "photo", forStep + 1));
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", file.getName(),
                RequestBody.create(MediaType.parse(type), file));
        ApiClient.getApiService().uploadMedia(part).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                busy = false;
                binding.uploadOverlay.setVisibility(View.GONE);
                if (!response.isSuccessful() || response.body() == null || response.body().get("url") == null) {
                    Toast.makeText(CameraCaptureActivity.this, AuthInputUtils.errorMessage(response, "Upload failed, capture again."),
                            Toast.LENGTH_LONG).show();
                    showStep();
                    return;
                }
                uploaded[forStep] = response.body().get("url");
                if (forStep < 2) {
                    step = forStep + 1;
                    showStep();
                    if (step == 2) bindUseCases();   // switch the camera to video
                } else {
                    finishCapture();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                busy = false;
                binding.uploadOverlay.setVisibility(View.GONE);
                Toast.makeText(CameraCaptureActivity.this, "Upload failed: check your internet and capture again.", Toast.LENGTH_LONG).show();
                showStep();
            }
        });
    }

    private void finishCapture() {
        ArrayList<String> media = new ArrayList<>();
        ArrayList<String> local = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            media.add(uploaded[i]);
            local.add(localFiles[i]);
        }
        String capturedAt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(new Date());
        if (retakeListingId != null) {
            Listing capture = new Listing();
            capture.setGpsLatitude(lat);
            capture.setGpsLongitude(lng);
            capture.setMediaUrls(media);
            capture.setDeviceModel(Build.MANUFACTURER + " " + Build.MODEL);
            capture.setCapturedAt(capturedAt);
            ApiClient.getApiService().retakeListing(retakeListingId, capture).enqueue(new Callback<Listing>() {
                @Override
                public void onResponse(@NonNull Call<Listing> call, @NonNull Response<Listing> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(CameraCaptureActivity.this, "New photos sent for verification.", Toast.LENGTH_LONG).show();
                        openMain(null);
                    } else {
                        Toast.makeText(CameraCaptureActivity.this, AuthInputUtils.errorMessage(response, "Could not re-submit."),
                                Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<Listing> call, @NonNull Throwable t) {
                    Toast.makeText(CameraCaptureActivity.this, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }
        // New listing: straight to the Listing Details form with the captured media.
        Bundle extras = new Bundle();
        extras.putStringArrayList(MainActivity.EXTRA_CAPTURED_MEDIA, media);
        extras.putStringArrayList(EXTRA_LOCAL_MEDIA, local);
        extras.putDouble(LocationVerificationActivity.EXTRA_LAT, lat);
        extras.putDouble(LocationVerificationActivity.EXTRA_LNG, lng);
        extras.putString(CreateListingFragment.ARG_CAPTURED_AT, capturedAt);
        extras.putString(CreateListingFragment.ARG_DEVICE, Build.MANUFACTURER + " " + Build.MODEL);
        String crop = getIntent().getStringExtra(LocationVerificationActivity.EXTRA_CROP);
        if (crop != null) extras.putString(LocationVerificationActivity.EXTRA_CROP, crop);
        openMain(extras);
    }

    private void openMain(Bundle extras) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(MainActivity.EXTRA_INITIAL_TAB, MainActivity.Tab.LISTINGS.name());
        if (extras != null) i.putExtras(extras);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    // ---------------------------------------------------------------- camera

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                cameraProvider = future.get();
                bindUseCases();
            } catch (Exception e) {
                Log.e(TAG, "Camera start failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /** Photo steps bind preview + photo; the video step binds preview + video (works on every phone). */
    private void bindUseCases() {
        if (cameraProvider == null) return;
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());
        CameraSelector selector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();
        try {
            cameraProvider.unbindAll();
            if (step == 2) {
                Recorder recorder = new Recorder.Builder()   // small file: the server limit is 25 MB
                        .setQualitySelector(QualitySelector.from(Quality.SD, FallbackStrategy.higherQualityOrLowerThan(Quality.SD)))
                        .build();
                videoCapture = VideoCapture.withOutput(recorder);
                cameraProvider.bindToLifecycle(this, selector, preview, videoCapture);
            } else {
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(binding.viewFinder.getDisplay() != null ? binding.viewFinder.getDisplay().getRotation() : 0)
                        .build();
                cameraProvider.bindToLifecycle(this, selector, preview, imageCapture);
            }
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
            if (lensFacing == CameraSelector.LENS_FACING_FRONT) {   // no front camera: back to the main one
                lensFacing = CameraSelector.LENS_FACING_BACK;
                bindUseCases();
            }
        }
    }

    /** The locked capture position (from the location-lock screen), used to geotag every photo and the video. */
    private android.location.Location farmLocation() {
        android.location.Location l = new android.location.Location("harvesthub-lock");
        l.setLatitude(lat);
        l.setLongitude(lng);
        l.setTime(System.currentTimeMillis());
        return l;
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
