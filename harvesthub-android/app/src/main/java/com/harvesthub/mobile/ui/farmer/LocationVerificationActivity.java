package com.harvesthub.mobile.ui.farmer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.utils.AuthInputUtils;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * "Location-Lock Validation" screen: the farmer must be physically at the registered farm.
 * The device position is checked by the backend against the farm GPS saved at registration.
 * On success the guided camera opens (new listing, or a retake requested by an officer).
 */
public class LocationVerificationActivity extends AppCompatActivity {

    public static final String EXTRA_RETAKE_LISTING_ID = "retakeListingId";
    public static final String EXTRA_CROP = "crop";
    public static final String EXTRA_LAT = "lat";
    public static final String EXTRA_LNG = "lng";
    private static final int REQ_LOCATION = 41;

    private TextView tvVerifying, tvSubtext;
    private View progress;
    private FusedLocationProviderClient location;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_verification);
        tvVerifying = findViewById(R.id.tvVerifying);
        tvSubtext = findViewById(R.id.tvSubtext);
        progress = findViewById(R.id.locationProgressBar);
        location = LocationServices.getFusedLocationProviderClient(this);
        findViewById(R.id.centerCard).setOnClickListener(v -> start());
        start();
    }

    private void start() {
        showState("Verifying Location...", "Ensuring you are physically at the farm.", true);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LOCATION);
            return;
        }
        locate();
    }

    @SuppressLint("MissingPermission")
    private void locate() {
        location.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, new CancellationTokenSource().getToken())
                .addOnSuccessListener(loc -> {
                    if (loc == null) {
                        showState("Location unavailable", "Turn on GPS and tap here to try again.", false);
                    } else {
                        check(loc);
                    }
                })
                .addOnFailureListener(e -> showState("Location unavailable", "Turn on GPS and tap here to try again.", false));
    }

    private void check(Location loc) {
        ApiClient.getApiService().checkFarmLocation(loc.getLatitude(), loc.getLongitude()).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> response) {
                Map<String, Object> body = response.body();
                if (!response.isSuccessful() || body == null) {
                    showState("Cannot verify location", AuthInputUtils.errorMessage(response, "Tap here to try again."), false);
                    return;
                }
                if (Boolean.TRUE.equals(body.get("withinFarm"))) {
                    Intent i = new Intent(LocationVerificationActivity.this, CameraCaptureActivity.class);
                    i.putExtras(getIntent());
                    i.putExtra(EXTRA_LAT, loc.getLatitude());
                    i.putExtra(EXTRA_LNG, loc.getLongitude());
                    startActivity(i);
                    finish();
                } else {
                    Object m = body.get("distanceMeters");
                    long meters = m instanceof Number ? ((Number) m).longValue() : -1;
                    showState("Not at your farm", "You are " + meters + " m from your registered farm"
                            + (body.get("farmAddress") != null ? " (" + body.get("farmAddress") + ")" : "")
                            + ". Go to the farm and tap here to try again.", false);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                showState("No connection", "Check your internet and tap here to try again.", false);
            }
        });
    }

    private void showState(String title, String sub, boolean busy) {
        tvVerifying.setText(title);
        tvSubtext.setText(sub);
        progress.setVisibility(busy ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            locate();
        } else {
            Toast.makeText(this, "Location permission is required to create a listing.", Toast.LENGTH_LONG).show();
            showState("Location permission needed", "Allow location access and tap here to try again.", false);
        }
    }
}
