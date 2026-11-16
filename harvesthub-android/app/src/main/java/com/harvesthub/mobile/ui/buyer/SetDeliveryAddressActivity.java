package com.harvesthub.mobile.ui.buyer;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.FarmLocation;
import com.harvesthub.mobile.model.RegisterRequest;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.utils.AuthInputUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Figma buyer 1.2 "Delivery Address Configuration Map". The delivery pin is required (decision 15 Nov) because
 * transport distance and price are calculated from it.
 * With EXTRA_REGISTER (from 1.2 Register) the buyer account is created here; without it the signed-in buyer
 * changes their saved delivery point (Profile).
 */
public class SetDeliveryAddressActivity extends AppCompatActivity {

    public static final String EXTRA_REGISTER = "registerRequest";
    /** Transporter mode: Figma transporter "Set Vehicle Address" (vehicle base for dispatch). */
    public static final String EXTRA_TRANSPORTER = "transporterBase";
    /** Transporter sign-up: licence photo (file Uri) uploaded right after the account is created. */
    public static final String EXTRA_LICENCE_URI = "licenceUri";
    private boolean transporter;
    private static final LatLng SRI_LANKA = new LatLng(7.8731, 80.7718);
    private static final int REQ_LOCATION = 41;

    private GoogleMap map;
    private RegisterRequest registerRequest;
    private String currentAddress;
    private final ExecutorService geo = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private int geocodeGen;
    private boolean busy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_delivery_address);
        registerRequest = getIntent().getParcelableExtra(EXTRA_REGISTER);
        transporter = getIntent().getBooleanExtra(EXTRA_TRANSPORTER, false);
        if (transporter) {
            text(R.id.tvStepLabel, registerRequest != null ? "STEP 3 OF 3" : "CHANGE VEHICLE BASE");
            text(R.id.tvSaveAddressLabel, registerRequest != null ? "Save Vehicle Address & Create Account" : "Save Vehicle Address");
            android.view.View title = findViewById(R.id.tvDeliveryTitle);
            if (title instanceof TextView) ((TextView) title).setText("Set Vehicle Address");
            text(R.id.tvConfirmTitle, "Confirm the Location");
        } else {
            text(R.id.tvStepLabel, registerRequest != null ? "STEP 2 OF 2" : "CHANGE DELIVERY POINT");
            text(R.id.tvSaveAddressLabel, registerRequest != null ? "Save & Create Account" : "Save Delivery Address");
        }

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnRecenter).setOnClickListener(v -> locateMe());
        findViewById(R.id.btnEditAddress).setOnClickListener(v -> editLabel());
        findViewById(R.id.layoutAddressBox).setOnClickListener(v -> editLabel());
        findViewById(R.id.btnSaveAddress).setOnClickListener(v -> save());

        SupportMapFragment f = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapDelivery);
        if (f != null) f.getMapAsync(m -> {
            map = m;
            map.getUiSettings().setZoomControlsEnabled(true);
            map.getUiSettings().setMyLocationButtonEnabled(false);
            map.getUiSettings().setMapToolbarEnabled(false);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(SRI_LANKA, 7f));
            map.setOnCameraMoveStartedListener(r -> text(R.id.tvDeliveryAddress, "Locating address…"));
            map.setOnCameraIdleListener(this::reverseGeocode);
            locateMe();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        geo.shutdownNow();
    }

    private void locateMe() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION);
            return;
        }
        if (map == null) return;
        try {
            map.setMyLocationEnabled(true);
            LocationServices.getFusedLocationProviderClient(this).getLastLocation().addOnSuccessListener(this, loc -> {
                if (loc != null && map != null) {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(loc.getLatitude(), loc.getLongitude()), 16f));
                }
            });
        } catch (SecurityException ignored) { }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_LOCATION && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) locateMe();
    }

    private void reverseGeocode() {
        if (map == null) return;
        LatLng c = map.getCameraPosition().target;
        int gen = ++geocodeGen;
        String coords = String.format(Locale.US, "%.5f, %.5f", c.latitude, c.longitude);
        geo.execute(() -> {
            String line = null;
            try {
                List<Address> list = new Geocoder(this, Locale.getDefault()).getFromLocation(c.latitude, c.longitude, 1);
                if (list != null && !list.isEmpty()) line = list.get(0).getAddressLine(0);
            } catch (Exception ignored) { }
            String address = line;
            main.post(() -> {
                if (gen != geocodeGen || isFinishing()) return;
                currentAddress = address;
                text(R.id.tvDeliveryAddress, address != null ? address : coords);
            });
        });
    }

    /** Lets the buyer name the place (e.g. "Pettah warehouse, gate 2"); the pin stays where it is. */
    private void editLabel() {
        EditText in = new EditText(this);
        in.setText(currentAddress == null ? "" : currentAddress);
        in.setHint("Warehouse / shop name and street");
        new AlertDialog.Builder(this).setTitle("Delivery address label").setView(in)
                .setPositiveButton("Update", (d, w) -> {
                    String s = in.getText().toString().trim();
                    if (!s.isEmpty()) {
                        currentAddress = s;
                        text(R.id.tvDeliveryAddress, s);
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    private void save() {
        if (busy || map == null) return;
        LatLng c = map.getCameraPosition().target;
        if (c.latitude < 5.8 || c.latitude > 10.0 || c.longitude < 79.4 || c.longitude > 82.1) {
            Toast.makeText(this, "Place the pin on your delivery point in Sri Lanka.", Toast.LENGTH_LONG).show();
            return;
        }
        if (map.getCameraPosition().zoom < 12f) {
            Toast.makeText(this, "Zoom in and place the pin exactly on your delivery point.", Toast.LENGTH_LONG).show();
            return;
        }
        String address = currentAddress != null ? currentAddress : String.format(Locale.US, "%.5f, %.5f", c.latitude, c.longitude);
        busy = true;
        if (registerRequest != null) register(address, c);
        else update(address, c);
    }

    private void register(String address, LatLng c) {
        registerRequest.setFarmLocation(new FarmLocation(address, c.latitude, c.longitude));
        ApiClient.getApiService().registerFarmer(registerRequest).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Response<okhttp3.ResponseBody> r) {
                busy = false;
                if (!r.isSuccessful()) {
                    Toast.makeText(SetDeliveryAddressActivity.this, AuthInputUtils.errorMessage(r, "Registration failed."), Toast.LENGTH_LONG).show();
                    return;
                }
                if (transporter) {
                    signInTransporter();
                    return;
                }
                Toast.makeText(SetDeliveryAddressActivity.this, "Account created. Please sign in.", Toast.LENGTH_LONG).show();
                Intent i = new Intent(SetDeliveryAddressActivity.this, BuyerLoginActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            }

            @Override
            public void onFailure(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Throwable t) {
                busy = false;
                Toast.makeText(SetDeliveryAddressActivity.this, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** New driver: sign in with the typed credentials, upload the licence photo, open the dashboard. */
    private void signInTransporter() {
        ApiClient.getApiService().login(new com.harvesthub.mobile.model.LoginRequest(registerRequest.getEmail(), registerRequest.getPassword()))
                .enqueue(new Callback<com.harvesthub.mobile.model.LoginResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<com.harvesthub.mobile.model.LoginResponse> call,
                                           @NonNull Response<com.harvesthub.mobile.model.LoginResponse> r) {
                        if (!r.isSuccessful() || r.body() == null || r.body().getUser() == null) {
                            Toast.makeText(SetDeliveryAddressActivity.this, "Account created. Please sign in.", Toast.LENGTH_LONG).show();
                            openAndClear(com.harvesthub.mobile.ui.transporter.TransporterLoginActivity.class);
                            return;
                        }
                        com.harvesthub.mobile.utils.SessionManager s = new com.harvesthub.mobile.utils.SessionManager(SetDeliveryAddressActivity.this);
                        s.saveAuthToken(r.body().getToken());
                        s.saveUser(r.body().getUser());
                        s.setLogin(true, "TRANSPORTER");
                        android.net.Uri licence = getIntent().getParcelableExtra(EXTRA_LICENCE_URI);
                        Runnable home = () -> openAndClear(com.harvesthub.mobile.ui.transporter.DeliveryDashboardActivity.class);
                        Toast.makeText(SetDeliveryAddressActivity.this, "Welcome to HarvestHub! Your vehicle is registered.", Toast.LENGTH_LONG).show();
                        if (licence != null) com.harvesthub.mobile.ui.transporter.TransporterSettingsActivity.LicenceUpload.upload(SetDeliveryAddressActivity.this, licence, home);
                        else home.run();
                    }

                    @Override
                    public void onFailure(@NonNull Call<com.harvesthub.mobile.model.LoginResponse> call, @NonNull Throwable t) {
                        openAndClear(com.harvesthub.mobile.ui.transporter.TransporterLoginActivity.class);
                    }
                });
    }

    private void openAndClear(Class<?> target) {
        Intent i = new Intent(this, target);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }

    private void update(String address, LatLng c) {
        Map<String, Object> body = new HashMap<>();
        body.put("latitude", c.latitude);
        body.put("longitude", c.longitude);
        body.put("address", address);
        if (transporter) {
            ApiClient.getApiService().updateVehicle(body).enqueue(new Callback<com.harvesthub.mobile.model.User>() {
            @Override
            public void onResponse(@NonNull Call<com.harvesthub.mobile.model.User> call, @NonNull Response<com.harvesthub.mobile.model.User> r) {
                busy = false;
                Toast.makeText(SetDeliveryAddressActivity.this, r.isSuccessful() ? "Vehicle address saved."
                        : AuthInputUtils.errorMessage(r, "Could not save the location."), Toast.LENGTH_LONG).show();
                if (r.isSuccessful()) finish();
            }

            @Override
            public void onFailure(@NonNull Call<com.harvesthub.mobile.model.User> call, @NonNull Throwable t) {
                busy = false;
                Toast.makeText(SetDeliveryAddressActivity.this, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
            }
        });
            return;
        }
        ApiClient.getApiService().setDeliveryLocation(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> r) {
                busy = false;
                if (!r.isSuccessful()) {
                    Toast.makeText(SetDeliveryAddressActivity.this, AuthInputUtils.errorMessage(r, "Could not save the location."), Toast.LENGTH_LONG).show();
                    return;
                }
                Toast.makeText(SetDeliveryAddressActivity.this, "Delivery point saved.", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                busy = false;
                Toast.makeText(SetDeliveryAddressActivity.this, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void text(int id, String s) {
        TextView v = findViewById(id);
        if (v != null) v.setText(s);
    }
}
