package com.harvesthub.mobile.ui.farmer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.FarmLocation;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LocationSetupActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private RegisterRequest registerRequest;
    private Button btnConfirm;
    private com.google.android.material.card.MaterialCardView cvError;
    private android.widget.TextView tvErrorMessage;
    private View ivCloseError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_setup);

        btnConfirm = findViewById(R.id.btnConfirm);
        View btnBack = findViewById(R.id.btnBack);
        View btnCloseMap = findViewById(R.id.btnCloseMap);
        View btnMyLocation = findViewById(R.id.btnMyLocation);
        cvError = findViewById(R.id.cvError);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        ivCloseError = findViewById(R.id.ivCloseError);

        registerRequest = getIntent().getParcelableExtra("registerRequest");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnBack.setOnClickListener(v -> finish());
        btnCloseMap.setOnClickListener(v -> finish());
        btnMyLocation.setOnClickListener(v -> getDeviceLocation());
        btnConfirm.setOnClickListener(v -> saveLocation());
        ivCloseError.setOnClickListener(v -> cvError.setVisibility(View.GONE));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        updateLocationUI();
        getDeviceLocation();
    }

    private void updateLocationUI() {
        if (mMap == null) return;
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } catch (SecurityException e) { e.printStackTrace(); }
    }

    private void getDeviceLocation() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                    if (location != null) {
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
                    }
                });
            }
        } catch (SecurityException e) { e.printStackTrace(); }
    }

    private void saveLocation() {
        if (mMap == null || registerRequest == null) return;
        LatLng center = mMap.getCameraPosition().target;
        
        FarmLocation farmLocation = new FarmLocation("Address logic here", center.latitude, center.longitude);
        registerRequest.setFarmLocation(farmLocation);

        cvError.setVisibility(View.GONE);
        btnConfirm.setEnabled(false);
        btnConfirm.setText("Registering...");

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.registerFarmer(registerRequest).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                btnConfirm.setEnabled(true);
                btnConfirm.setText("Confirm Coordinates");

                if (response.isSuccessful()) {
                    Toast.makeText(LocationSetupActivity.this, "Registration Successful!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(LocationSetupActivity.this, FarmerLoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    String detailedErrorMessage = "Registration Failed";
                    try (okhttp3.ResponseBody errorBody = response.errorBody()) {
                        if (errorBody != null) {
                            String bodyString = errorBody.string();
                            if (bodyString != null && !bodyString.trim().isEmpty()) {
                                detailedErrorMessage = bodyString;
                            } else {
                                detailedErrorMessage = "Registration Failed: " + response.code() + " " + response.message();
                            }
                        } else {
                            detailedErrorMessage = "Registration Failed: " + response.code() + " " + response.message();
                        }
                    } catch (Exception e) {
                        android.util.Log.e("RegistrationError", "Error parsing error body", e);
                        detailedErrorMessage = "Registration Failed: " + response.code();
                    }
                    
                    tvErrorMessage.setText(detailedErrorMessage);
                    cvError.setVisibility(View.VISIBLE);
                    android.util.Log.e("RegistrationError", "HTTP Code: " + response.code() + ", Error: " + detailedErrorMessage);
                }
            }

            @Override
            public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                btnConfirm.setEnabled(true);
                btnConfirm.setText("Confirm Coordinates");
                String errorMessage = "Network Error: " + t.getMessage();
                tvErrorMessage.setText(errorMessage);
                cvError.setVisibility(View.VISIBLE);
                android.util.Log.e("RegistrationError", errorMessage, t);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateLocationUI();
                getDeviceLocation();
            }
        }
    }
}
