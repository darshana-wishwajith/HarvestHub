package com.harvesthub.mobile.ui.farmer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.harvesthub.mobile.MainActivity;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.databinding.ActivityCameraCaptureBinding;
import com.harvesthub.mobile.model.Listing;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.utils.AuthInputUtils;
import com.harvesthub.mobile.utils.BottomNavHelper;
import com.harvesthub.mobile.utils.NotificationPanelHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
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
 * Figma 13 "Smart Guided Camera" (workflow 1.3). Live capture only (no gallery):
 * step 1 wide-angle photo, step 2 close-up photo, step 3 a 10-second video. Each file is uploaded
 * right away; with all three the farmer continues to the listing details (or the retake is re-submitted).
 * GPS comes from the location lock that opened this screen.
 */
public class CameraCaptureActivity extends AppCompatActivity {

    private static final String TAG = "CameraCapture";
    private static final int REQUEST_CODE_PERMISSIONS = 10;

    private ActivityCameraCaptureBinding binding;
    private ImageCapture imageCapture;
    private int step = 0;                       // 0 wide, 1 close-up, 2 video
    private final String[] uploaded = new String[3];
    private double lat, lng;
    private String retakeListingId;
    private boolean busy;

    private final ActivityResultLauncher<Intent> recordVideo = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                Uri uri = result.getData() != null ? result.getData().getData() : null;
                if (result.getResultCode() == RESULT_OK && uri != null) {
                    uploadUri(uri, "video/mp4", 2);
                } else {
                    Toast.makeText(this, "Video not recorded. Tap the button to record 10 seconds.", Toast.LENGTH_SHORT).show();
                }
            });

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
        binding.btnFlip.setVisibility(View.GONE);   // back camera only: the crop, not the farmer
        binding.btnLock.setSelected(true);           // location already locked
        binding.btnLock.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                ContextCompat.getColor(this, R.color.accent_green)));
        binding.btnLock.setOnClickListener(v ->
                Toast.makeText(this, String.format(Locale.US, "Location locked: %.5f, %.5f", lat, lng), Toast.LENGTH_SHORT).show());
        binding.btnCapture.setOnClickListener(v -> capture());
        // tabs only show progress; steps are done in order
        binding.tabWideAngle.setOnClickListener(null);
        binding.tabCloseUp.setOnClickListener(null);
        binding.tabVideo.setOnClickListener(null);
        showStep();

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_PERMISSIONS);
        }
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
                    : (uploaded[i] != null ? Color.parseColor("#A7F3D0") : Color.WHITE));
        }
    }

    private void capture() {
        if (busy) return;
        if (step == 2) {
            Intent i = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            i.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 10);
            i.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0); // small file (server limit 25 MB)
            try {
                recordVideo.launch(i);
            } catch (Exception e) {
                Toast.makeText(this, "No camera app available to record video.", Toast.LENGTH_LONG).show();
            }
            return;
        }
        if (imageCapture == null) return;
        File file = new File(getCacheDir(), "capture_" + step + "_" + System.currentTimeMillis() + ".jpg");
        busy = true;
        imageCapture.takePicture(new ImageCapture.OutputFileOptions.Builder(file).build(), ContextCompat.getMainExecutor(this),
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

    private void uploadUri(Uri uri, String type, int forStep) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            File f = new File(getCacheDir(), "capture_video_" + System.currentTimeMillis() + ".mp4");
            try (OutputStream out = new FileOutputStream(f)) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while (in != null && (n = in.read(buf)) > 0) out.write(buf, 0, n);
            }
            upload(f, type, forStep);
        } catch (Exception e) {
            Toast.makeText(this, "Could not read the video, record it again.", Toast.LENGTH_LONG).show();
        }
    }

    private void upload(File file, String type, int forStep) {
        busy = true;
        binding.tvStepSubtitle.setText("Uploading…");
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", file.getName(),
                RequestBody.create(MediaType.parse(type), file));
        ApiClient.getApiService().uploadMedia(part).enqueue(new Callback<Map<String, String>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, String>> call, @NonNull Response<Map<String, String>> response) {
                busy = false;
                file.delete();
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
                } else {
                    finishCapture();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, String>> call, @NonNull Throwable t) {
                busy = false;
                Toast.makeText(CameraCaptureActivity.this, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
                showStep();
            }
        });
    }

    private void finishCapture() {
        ArrayList<String> media = new ArrayList<>();
        for (String u : uploaded) media.add(u);
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
                        openListings(null);
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
        Bundle extras = new Bundle();
        extras.putStringArrayList(MainActivity.EXTRA_CAPTURED_MEDIA, media);
        extras.putDouble(LocationVerificationActivity.EXTRA_LAT, lat);
        extras.putDouble(LocationVerificationActivity.EXTRA_LNG, lng);
        extras.putString(CreateListingFragment.ARG_CAPTURED_AT, capturedAt);
        extras.putString(CreateListingFragment.ARG_DEVICE, Build.MANUFACTURER + " " + Build.MODEL);
        String crop = getIntent().getStringExtra(LocationVerificationActivity.EXTRA_CROP);
        if (crop != null) extras.putString(LocationVerificationActivity.EXTRA_CROP, crop);
        openListings(extras);
    }

    private void openListings(Bundle extras) {
        Intent i = new Intent(this, MainActivity.class);
        i.putExtra(MainActivity.EXTRA_INITIAL_TAB, MainActivity.Tab.LISTINGS.name());
        if (extras != null) i.putExtras(extras);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());
                imageCapture = new ImageCapture.Builder().setTargetRotation(binding.viewFinder.getDisplay() != null
                        ? binding.viewFinder.getDisplay().getRotation() : 0).build();
                provider.unbindAll();
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture);
            } catch (Exception e) {
                Log.e(TAG, "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
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
