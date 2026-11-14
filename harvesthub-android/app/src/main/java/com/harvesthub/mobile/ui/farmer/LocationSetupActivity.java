package com.harvesthub.mobile.ui.farmer;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.FarmLocation;
import com.harvesthub.mobile.model.RegisterRequest;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Step 2 of farmer registration: pin the farm on a Google Map.
 * The pin is fixed in the centre of the map; the farmer either jumps to their GPS position,
 * searches a place name, or drags the map by hand. On confirm the account and location are
 * sent to the server in one register call.
 */
public class LocationSetupActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int REQ_LOCATION_PERMISSION = 1001;
    private static final int REQ_LOCATION_SETTINGS = 1002;
    /** Centre of Sri Lanka - shown until we have a real position. */
    private static final LatLng SRI_LANKA = new LatLng(7.8731, 80.7718);

    private GoogleMap map;
    private FusedLocationProviderClient fusedLocationClient;
    private RegisterRequest registerRequest;

    private Button btnConfirm;
    private MaterialCardView cvError;
    private TextView tvErrorMessage, tvSelectedAddress;
    private TextInputEditText etSearch;

    private final ExecutorService geoExecutor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    /** Address of the point under the pin (reverse-geocoded); null until resolved. */
    private String currentAddress;
    private int geocodeGeneration = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_setup);

        btnConfirm = findViewById(R.id.btnConfirm);
        cvError = findViewById(R.id.cvError);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        tvSelectedAddress = findViewById(R.id.tvSelectedAddress);
        etSearch = findViewById(R.id.etSearch);

        registerRequest = getIntent().getParcelableExtra("registerRequest");
        if (registerRequest == null) {          // opened without step 1 - go back to the form
            finish();
            return;
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCloseMap).setOnClickListener(v -> finish());
        findViewById(R.id.btnMyLocation).setOnClickListener(v -> locateMe());
        findViewById(R.id.ivCloseError).setOnClickListener(v -> cvError.setVisibility(View.GONE));
        btnConfirm.setOnClickListener(v -> register());

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                searchPlace();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        geoExecutor.shutdownNow();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setMyLocationButtonEnabled(false);   // we have our own button
        map.getUiSettings().setZoomControlsEnabled(true);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);               // satellite + labels: easier to find a field
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(SRI_LANKA, 7f));

        map.setOnCameraMoveStartedListener(reason -> {
            currentAddress = null;
            tvSelectedAddress.setText("Locating address...");
        });
        map.setOnCameraIdleListener(this::reverseGeocodeCenter);

        locateMe();
    }

    // ---------------------------------------------------------------- current location

    /** Permission -> location settings (GPS on) -> fresh high-accuracy fix -> move camera. */
    private void locateMe() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    REQ_LOCATION_PERMISSION);
            return;
        }
        LocationSettingsRequest settings = new LocationSettingsRequest.Builder()
                .addLocationRequest(new com.google.android.gms.location.LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY, 5000).build())
                .setAlwaysShow(true)
                .build();
        LocationServices.getSettingsClient(this).checkLocationSettings(settings)
                .addOnSuccessListener(this, r -> fetchCurrentLocation())
                .addOnFailureListener(this, e -> {
                    if (e instanceof ResolvableApiException) {
                        try {   // shows the system "Turn on location?" dialog
                            ((ResolvableApiException) e).startResolutionForResult(this, REQ_LOCATION_SETTINGS);
                        } catch (IntentSender.SendIntentException ignored) {
                            toast("Please turn on Location in your phone settings.");
                        }
                    } else {
                        toast("Please turn on Location in your phone settings.");
                    }
                });
    }

    private void fetchCurrentLocation() {
        if (!hasLocationPermission() || map == null) return;
        try {
            map.setMyLocationEnabled(true);    // blue dot
            tvSelectedAddress.setText("Getting your current location...");
            CurrentLocationRequest req = new CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setMaxUpdateAgeMillis(30_000)
                    .setDurationMillis(15_000)
                    .build();
            fusedLocationClient.getCurrentLocation(req, null)
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(location.getLatitude(), location.getLongitude()), 17f));
                        } else {
                            fallbackToLastLocation();
                        }
                    })
                    .addOnFailureListener(this, e -> fallbackToLastLocation());
        } catch (SecurityException ignored) {
            // permission revoked meanwhile
        }
    }

    private void fallbackToLastLocation() {
        try {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(location.getLatitude(), location.getLongitude()), 17f));
                } else {
                    toast("Could not get your GPS position. Search or drag the map to your farm.");
                    reverseGeocodeCenter();
                }
            });
        } catch (SecurityException ignored) {
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_LOCATION_PERMISSION) return;
        if (hasLocationPermission()) {
            locateMe();
        } else {
            toast("Location permission denied. Search or drag the map to your farm instead.");
            reverseGeocodeCenter();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_LOCATION_SETTINGS) {
            if (resultCode == RESULT_OK) fetchCurrentLocation();
            else toast("Location is off. Search or drag the map to your farm instead.");
        }
    }

    // ---------------------------------------------------------------- search & address

    /** Manual option: find a village / town by name and move the pin there. */
    private void searchPlace() {
        String query = etSearch.getText() == null ? "" : etSearch.getText().toString().trim();
        if (query.isEmpty() || map == null) return;
        hideKeyboard();
        if (!Geocoder.isPresent()) {
            toast("Search is not available on this phone. Drag the map instead.");
            return;
        }
        geoExecutor.execute(() -> {
            List<Address> results = null;
            try {
                // Bias results to Sri Lanka.
                results = new Geocoder(this, Locale.getDefault())
                        .getFromLocationName(query + ", Sri Lanka", 1, 5.7, 79.3, 10.1, 82.1);
            } catch (Exception ignored) {
            }
            List<Address> found = results;
            main.post(() -> {
                if (isFinishing()) return;
                if (found == null || found.isEmpty()) {
                    toast("No place found for \"" + query + "\"");
                } else {
                    Address a = found.get(0);
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(a.getLatitude(), a.getLongitude()), 15f));
                }
            });
        });
    }

    /** Shows the address under the pin. Runs after every map move. */
    private void reverseGeocodeCenter() {
        if (map == null) return;
        LatLng c = map.getCameraPosition().target;
        int gen = ++geocodeGeneration;
        String coords = String.format(Locale.US, "%.5f, %.5f", c.latitude, c.longitude);
        if (!Geocoder.isPresent()) {
            currentAddress = null;
            tvSelectedAddress.setText(coords);
            return;
        }
        geoExecutor.execute(() -> {
            String line = null;
            try {
                List<Address> list = new Geocoder(this, Locale.getDefault()).getFromLocation(c.latitude, c.longitude, 1);
                if (list != null && !list.isEmpty()) line = list.get(0).getAddressLine(0);
            } catch (Exception ignored) {
            }
            String address = line;
            main.post(() -> {
                if (gen != geocodeGeneration || isFinishing()) return;   // a newer move happened
                currentAddress = address;
                tvSelectedAddress.setText(address != null ? address + "\n" + coords : coords);
            });
        });
    }

    // ---------------------------------------------------------------- register

    private void register() {
        if (map == null) return;
        LatLng center = map.getCameraPosition().target;
        if (center.latitude < 5.7 || center.latitude > 10.1 || center.longitude < 79.3 || center.longitude > 82.1) {
            showError("Please place the pin on your farm in Sri Lanka.");
            return;
        }
        if (map.getCameraPosition().zoom < 13f) {
            showError("Zoom in closer and place the pin exactly on your farm.");
            return;
        }
        String address = currentAddress != null ? currentAddress
                : String.format(Locale.US, "Lat %.5f, Lng %.5f", center.latitude, center.longitude);
        registerRequest.setFarmLocation(new FarmLocation(address, center.latitude, center.longitude));

        cvError.setVisibility(View.GONE);
        btnConfirm.setEnabled(false);
        btnConfirm.setText("Registering...");

        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.registerFarmer(registerRequest).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Response<okhttp3.ResponseBody> response) {
                btnConfirm.setEnabled(true);
                btnConfirm.setText("Confirm Coordinates");
                if (response.isSuccessful()) {
                    toast("Registration successful! Please sign in.");
                    Intent intent = new Intent(LocationSetupActivity.this, FarmerLoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                    return;
                }
                String message = "Registration failed (" + response.code() + ")";
                try (okhttp3.ResponseBody err = response.errorBody()) {
                    if (err != null) message = formatError(err.string(), message);
                } catch (Exception ignored) {
                }
                showError(message);
            }

            @Override
            public void onFailure(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Throwable t) {
                btnConfirm.setEnabled(true);
                btnConfirm.setText("Confirm Coordinates");
                showError("No connection to the server. Check your internet and try again.");
            }
        });
    }

    /** Server errors are {"error": "..."} or {"field": "message", ...} (validation). */
    private static String formatError(String body, String fallback) {
        try {
            if (body != null && body.trim().startsWith("{")) {
                org.json.JSONObject json = new org.json.JSONObject(body);
                if (json.has("error")) return json.optString("error");
                StringBuilder sb = new StringBuilder();
                java.util.Iterator<String> keys = json.keys();
                while (keys.hasNext()) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append("• ").append(json.optString(keys.next()));
                }
                if (sb.length() > 0) return sb.toString();
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    private void showError(String message) {
        tvErrorMessage.setText(message);
        cvError.setVisibility(View.VISIBLE);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null) imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }
}
