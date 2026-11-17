package com.harvesthub.mobile.ui.common;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.utils.MapPinHelper;
import com.harvesthub.mobile.utils.OsmMaps;

import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * One "pin a location" screen for every role (Figma "9. Farm Location Setup", 28:380), so farmer, buyer and
 * transporter see the same UI and process: OpenStreetMap map with a fixed centre pin and a my-location button,
 * a search box (place name, coordinates or a Google Maps link), a "Use copied coordinates" chip,
 * the address under the pin, "Check this pin in Google Maps" and a Confirm button.
 * Subclasses only give the wording and what Confirm does.
 */
public abstract class PinLocationActivity extends AppCompatActivity {

    private static final int REQ_LOCATION_PERMISSION = 1001;
    private static final int REQ_LOCATION_SETTINGS = 1002;
    /** Centre of Sri Lanka - shown until we have a real position. */
    private static final GeoPoint SRI_LANKA = new GeoPoint(7.8731, 80.7718);
    private static final double PIN_ZOOM = 17.0;
    private static final double MIN_CONFIRM_ZOOM = 15.0;

    private MapView map;
    private MyLocationNewOverlay myLocationOverlay;
    private FusedLocationProviderClient fusedLocationClient;

    private MaterialButton btnConfirm;
    private MaterialCardView cvError;
    private TextView tvErrorMessage, tvSelectedAddress, tvUseCopied;
    private TextInputEditText etSearch;

    protected final ExecutorService geoExecutor = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final Runnable geocodeAfterMove = this::reverseGeocodeCenter;
    /** Address of the point under the pin (reverse-geocoded); null until resolved. */
    private String currentAddress;
    private int geocodeGeneration = 0;
    private double[] copiedCoordinates;
    private boolean busy;

    // ---------------------------------------------------------------- what each role provides

    protected abstract String screenTitle();

    protected abstract String screenSubtitle();

    /** Grey hint under the address, e.g. "Drag the map to position the pin exactly over ...". */
    protected abstract String hintText();

    protected String confirmLabel() {
        return "Confirm Coordinates";
    }

    /** Return false when the screen cannot be used (e.g. missing registration data) - it then closes. */
    protected boolean prepare() {
        return true;
    }

    /** Confirm pressed with a valid pin in Sri Lanka. Call {@link #setBusy}/{@link #showError} as needed. */
    protected abstract void onConfirm(double lat, double lng, String address);

    // ---------------------------------------------------------------- lifecycle

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OsmMaps.init(this);   // before inflating the MapView
        setContentView(R.layout.activity_location_setup);
        if (!prepare()) {
            finish();
            return;
        }

        ((TextView) findViewById(R.id.tvTitle)).setText(screenTitle());
        ((TextView) findViewById(R.id.tvSubtitle)).setText(screenSubtitle());
        ((TextView) findViewById(R.id.tvHint)).setText(hintText());

        btnConfirm = findViewById(R.id.btnConfirm);
        btnConfirm.setText(confirmLabel());
        cvError = findViewById(R.id.cvError);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        tvSelectedAddress = findViewById(R.id.tvSelectedAddress);
        tvUseCopied = findViewById(R.id.tvUseCopied);
        etSearch = findViewById(R.id.etSearch);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        map = findViewById(R.id.map);
        OsmMaps.setUp(map);
        map.getController().setZoom(7.5);
        map.getController().setCenter(SRI_LANKA);
        map.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                onMapMoved();
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                onMapMoved();
                return false;
            }
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnMyLocation).setOnClickListener(v -> locateMe());
        findViewById(R.id.ivCloseError).setOnClickListener(v -> cvError.setVisibility(View.GONE));
        findViewById(R.id.btnCheckGoogle).setOnClickListener(v -> {
            GeoPoint c = center();
            MapPinHelper.openInGoogleMaps(this, c.getLatitude(), c.getLongitude());
        });
        tvUseCopied.setOnClickListener(v -> {
            if (copiedCoordinates != null) moveTo(copiedCoordinates[0], copiedCoordinates[1]);
            tvUseCopied.setVisibility(View.GONE);
        });
        btnConfirm.setOnClickListener(v -> confirm());

        ((TextInputLayout) findViewById(R.id.tilSearch)).setEndIconOnClickListener(v -> search());
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_NULL) {
                search();
                return true;
            }
            return false;
        });

        locateMe();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) map.onResume();
        if (myLocationOverlay != null) myLocationOverlay.enableMyLocation();
    }

    @Override
    protected void onPause() {
        if (map != null) map.onPause();
        if (myLocationOverlay != null) myLocationOverlay.disableMyLocation();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        main.removeCallbacksAndMessages(null);
        geoExecutor.shutdownNow();
        if (map != null) map.onDetach();
    }

    /** Coming back from Google Maps with copied coordinates: offer them as a one-tap chip. */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus || map == null) return;
        double[] p = MapPinHelper.copiedCoordinates(this);
        GeoPoint c = center();
        boolean alreadyThere = p != null && Math.abs(c.getLatitude() - p[0]) < 1e-5 && Math.abs(c.getLongitude() - p[1]) < 1e-5;
        copiedCoordinates = alreadyThere ? null : p;
        if (copiedCoordinates != null) {
            tvUseCopied.setText(String.format(Locale.US, "Use copied coordinates  %.5f, %.5f", p[0], p[1]));
            tvUseCopied.setVisibility(View.VISIBLE);
        } else {
            tvUseCopied.setVisibility(View.GONE);
        }
    }

    private GeoPoint center() {
        return (GeoPoint) map.getMapCenter();
    }

    private void onMapMoved() {
        currentAddress = null;
        main.removeCallbacks(geocodeAfterMove);
        main.postDelayed(geocodeAfterMove, 600);   // wait until the user stops dragging
    }

    private void moveTo(double lat, double lng) {
        map.getController().animateTo(new GeoPoint(lat, lng), PIN_ZOOM, 800L);
    }

    // ---------------------------------------------------------------- current location

    /** Permission -> location settings (GPS on) -> fresh high-accuracy fix -> move map. */
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
                            locationUnavailable("Please turn on Location in your phone settings.");
                        }
                    } else {
                        locationUnavailable("Please turn on Location in your phone settings.");
                    }
                });
    }

    private void fetchCurrentLocation() {
        if (!hasLocationPermission()) return;
        showMyLocationDot();
        tvSelectedAddress.setText("Getting your current location...");
        try {
            CurrentLocationRequest req = new CurrentLocationRequest.Builder()
                    .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                    .setMaxUpdateAgeMillis(30_000)
                    .setDurationMillis(15_000)
                    .build();
            fusedLocationClient.getCurrentLocation(req, null)
                    .addOnSuccessListener(this, location -> {
                        if (location != null) moveTo(location.getLatitude(), location.getLongitude());
                        else fallbackToLastLocation();
                    })
                    .addOnFailureListener(this, e -> fallbackToLastLocation());
        } catch (SecurityException e) {
            locationUnavailable(null);
        }
    }

    private void fallbackToLastLocation() {
        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) moveTo(location.getLatitude(), location.getLongitude());
                        else locationUnavailable("Could not get your GPS position. Search or drag the map to the place.");
                    })
                    .addOnFailureListener(this, e -> locationUnavailable(null));
        } catch (SecurityException e) {
            locationUnavailable(null);
        }
    }

    /** Never leave "Getting your current location..." on screen: fall back to the address under the pin. */
    private void locationUnavailable(@Nullable String message) {
        if (message != null) toast(message);
        reverseGeocodeCenter();
    }

    private void showMyLocationDot() {
        if (myLocationOverlay != null) return;
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), map);
        myLocationOverlay.setDrawAccuracyEnabled(true);
        android.graphics.Bitmap dot = OsmMaps.locationDot(this);
        myLocationOverlay.setDirectionArrow(dot, dot);
        myLocationOverlay.setPersonAnchor(0.5f, 0.5f);
        myLocationOverlay.setDirectionAnchor(0.5f, 0.5f);
        myLocationOverlay.enableMyLocation();
        map.getOverlays().add(myLocationOverlay);
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_LOCATION_PERMISSION) return;
        if (hasLocationPermission()) locateMe();
        else locationUnavailable("Location permission denied. Search or drag the map to the place instead.");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_LOCATION_SETTINGS) {
            if (resultCode == RESULT_OK) fetchCurrentLocation();
            else locationUnavailable("Location is off. Search or drag the map to the place instead.");
        }
    }

    // ---------------------------------------------------------------- search & address

    private void search() {
        String query = etSearch.getText() == null ? "" : etSearch.getText().toString().trim();
        if (query.isEmpty()) return;
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etSearch.getWindowToken(), 0);
        cvError.setVisibility(View.GONE);
        tvSelectedAddress.setText("Searching...");
        MapPinHelper.search(this, query, geoExecutor, new MapPinHelper.SearchResult() {
            @Override
            public void onPoint(double lat, double lng) {
                if (!isFinishing()) moveTo(lat, lng);
            }

            @Override
            public void onNotFound(String message) {
                if (isFinishing()) return;
                toast(message);
                reverseGeocodeCenter();
            }
        });
    }

    /** Shows the address under the pin. Runs after every map move. */
    private void reverseGeocodeCenter() {
        if (map == null) return;
        GeoPoint c = center();
        int gen = ++geocodeGeneration;
        String coords = String.format(Locale.US, "%.5f, %.5f", c.getLatitude(), c.getLongitude());
        if (!Geocoder.isPresent()) {
            currentAddress = null;
            tvSelectedAddress.setText(coords);
            return;
        }
        tvSelectedAddress.setText("Locating address...\n" + coords);
        geoExecutor.execute(() -> {
            String address = MapPinHelper.addressFor(this, c.getLatitude(), c.getLongitude());
            main.post(() -> {
                if (gen != geocodeGeneration || isFinishing()) return;   // a newer move happened
                currentAddress = address;
                tvSelectedAddress.setText(address != null ? address + "\n" + coords : coords);
            });
        });
    }

    // ---------------------------------------------------------------- confirm

    private void confirm() {
        if (busy || map == null) return;
        GeoPoint c = center();
        if (!MapPinHelper.insideSriLanka(c.getLatitude(), c.getLongitude())) {
            showError("Please place the pin on the right place in Sri Lanka.");
            return;
        }
        if (map.getZoomLevelDouble() < MIN_CONFIRM_ZOOM) {
            showError("Zoom in closer and place the pin exactly on the right place.");
            return;
        }
        String address = currentAddress != null ? currentAddress
                : String.format(Locale.US, "Lat %.5f, Lng %.5f", c.getLatitude(), c.getLongitude());
        cvError.setVisibility(View.GONE);
        onConfirm(c.getLatitude(), c.getLongitude(), address);
    }

    /** Disables Confirm while a request runs, showing {@code label} on it. */
    protected void setBusy(boolean isBusy, String label) {
        busy = isBusy;
        btnConfirm.setEnabled(!isBusy);
        btnConfirm.setText(isBusy ? label : confirmLabel());
    }

    protected void showError(String message) {
        tvErrorMessage.setText(message);
        cvError.setVisibility(View.VISIBLE);
    }

    protected void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /** Server errors are {"error": "..."} or {"field": "message", ...} (validation). */
    protected static String formatError(String body, String fallback) {
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

    protected void openAndClear(Class<?> target) {
        Intent i = new Intent(this, target);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}
