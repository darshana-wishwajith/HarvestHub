package com.harvesthub.mobile.ui.transporter;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
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

import com.google.android.gms.location.LocationServices;
import com.google.common.util.concurrent.ListenableFuture;
import com.harvesthub.mobile.R;

import java.io.File;
import java.util.Locale;

/**
 * "Smart Evidence Camera" screen: takes one pre-loading photo with CameraX and writes the GPS position
 * into the JPEG (EXIF) so the photo is geotagged. Returns the file path in {@link #EXTRA_PHOTO_PATH}.
 */
public class EvidenceCameraActivity extends AppCompatActivity {

    public static final String EXTRA_PHOTO_PATH = "photo_path";
    public static final String EXTRA_LABEL = "label";
    private static final int REQ = 91;

    private ImageCapture capture;
    private double[] gps;
    private boolean busy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transporter_activity_evidence_camera);
        findViewById(R.id.evClose).setOnClickListener(v -> finish());
        findViewById(R.id.evShutter).setOnClickListener(v -> take());
        String label = getIntent().getStringExtra(EXTRA_LABEL);
        if (label != null) ((TextView) findViewById(R.id.evLabel)).setText(label.toUpperCase(Locale.US) + "\nTIME + GPS STAMPED");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION}, REQ);
        } else {
            start();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) start();
        else {
            Toast.makeText(this, "Camera permission is needed for the pre-loading photos.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void start() {
        locate();
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                PreviewView view = findViewById(R.id.evPreview);
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(view.getSurfaceProvider());
                capture = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetResolution(new android.util.Size(1280, 960)).build();
                provider.unbindAll();
                provider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture);
            } catch (Exception e) {
                Toast.makeText(this, "Camera not available.", Toast.LENGTH_LONG).show();
                finish();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void locate() {
        TextView pill = findViewById(R.id.evGps);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            pill.setText("GPS permission off");
            return;
        }
        try {
            LocationServices.getFusedLocationProviderClient(this).getLastLocation().addOnSuccessListener(this, l -> {
                if (l == null) {
                    pill.setText("● Waiting for GPS");
                    return;
                }
                gps = new double[]{l.getLatitude(), l.getLongitude()};
                pill.setText(String.format(Locale.US, "● %.4f° N, %.4f° E • Live Evidence", gps[0], gps[1]));
            });
        } catch (SecurityException ignored) { }
    }

    private void take() {
        if (capture == null || busy) return;
        busy = true;
        File f = new File(getCacheDir(), "loading_" + System.currentTimeMillis() + ".jpg");
        capture.takePicture(new ImageCapture.OutputFileOptions.Builder(f).build(), ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults r) {
                        if (gps != null && android.os.Build.VERSION.SDK_INT >= 24) {
                            try {
                                android.media.ExifInterface exif = new android.media.ExifInterface(f.getAbsolutePath());
                                exif.setAttribute(android.media.ExifInterface.TAG_GPS_LATITUDE, dms(gps[0]));
                                exif.setAttribute(android.media.ExifInterface.TAG_GPS_LATITUDE_REF, gps[0] >= 0 ? "N" : "S");
                                exif.setAttribute(android.media.ExifInterface.TAG_GPS_LONGITUDE, dms(gps[1]));
                                exif.setAttribute(android.media.ExifInterface.TAG_GPS_LONGITUDE_REF, gps[1] >= 0 ? "E" : "W");
                                exif.saveAttributes();
                            } catch (Exception ignored) { }
                        }
                        setResult(RESULT_OK, new Intent().putExtra(EXTRA_PHOTO_PATH, f.getAbsolutePath()));
                        finish();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException e) {
                        busy = false;
                        Toast.makeText(EvidenceCameraActivity.this, "Could not take the photo. Try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** Decimal degrees → EXIF rational "d/1,m/1,s*1000/1000". */
    private static String dms(double v) {
        v = Math.abs(v);
        int d = (int) v;
        double mFull = (v - d) * 60;
        int m = (int) mFull;
        long s = Math.round((mFull - m) * 60 * 1000);
        return d + "/1," + m + "/1," + s + "/1000";
    }
}
