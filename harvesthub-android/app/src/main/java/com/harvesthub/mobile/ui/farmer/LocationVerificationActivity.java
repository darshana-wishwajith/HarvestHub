package com.harvesthub.mobile.ui.farmer;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.harvesthub.mobile.R;

public class LocationVerificationActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final double TARGET_LATITUDE = 6.9271; // Example: Colombo
    private static final double TARGET_LONGITUDE = 79.8612;
    private static final float VERIFICATION_RADIUS_METERS = 200f;

    private ProgressBar progressBar;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_verification);

        progressBar = findViewById(R.id.locationProgressBar);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        startVerificationProcess();
    }

    private void startVerificationProcess() {
        // Animate progress bar from 0 to 100 over 3 seconds
        ObjectAnimator animation = ObjectAnimator.ofInt(progressBar, "progress", 0, 100);
        animation.setDuration(3000);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();

        // Delay the actual location check to match the animation
        new Handler(Looper.getMainLooper()).postDelayed(this::checkLocationPermission, 3000);
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                showPermissionRationale();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            getLastLocation();
        }
    }

    private void showPermissionRationale() {
        new AlertDialog.Builder(this)
                .setTitle("Location Permission Required")
                .setMessage("HarvestHub needs access to your location to verify that you are at the farm.")
                .setPositiveButton("OK", (dialog, which) -> ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE))
                .setNegativeButton("Cancel", (dialog, which) -> showErrorState("Permission denied. Cannot verify location."))
                .show();
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                verifyProximity(location);
            } else {
                showErrorState("Unable to retrieve GPS coordinates. Please try again.");
            }
        });
    }

    private void verifyProximity(Location currentLocation) {
        float[] results = new float[1];
        Location.distanceBetween(currentLocation.getLatitude(), currentLocation.getLongitude(),
                TARGET_LATITUDE, TARGET_LONGITUDE, results);

        float distanceInMeters = results[0];

        if (distanceInMeters <= VERIFICATION_RADIUS_METERS) {
            handleSuccess();
        } else {
            showErrorState("You are not at the registered farm location.");
        }
    }

    private void handleSuccess() {
        Toast.makeText(this, "Location Verified Successfully!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(this, AccountVerifiedActivity.class));
        finish();
    }

    private void showErrorState(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Verification Failed")
                .setMessage(message)
                .setPositiveButton("Retry", (dialog, which) -> startVerificationProcess())
                .setNegativeButton("Exit", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocation();
            } else {
                showErrorState("Location permission is required for verification.");
            }
        }
    }
}
