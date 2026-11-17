package com.harvesthub.mobile.ui.buyer;

import androidx.annotation.NonNull;

import com.harvesthub.mobile.model.FarmLocation;
import com.harvesthub.mobile.model.RegisterRequest;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.ui.common.PinLocationActivity;
import com.harvesthub.mobile.utils.AuthInputUtils;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Buyer delivery point / transporter vehicle address. Same screen and process as the farmer's
 * Pin Farm Location ({@link PinLocationActivity}); only the wording and the save call differ.
 * The pin is required (decision 15 Nov) because transport distance and price are calculated from it.
 * <ul>
 *   <li>EXTRA_REGISTER (from Create Account): the account is created here.</li>
 *   <li>Without it: the signed-in user changes their saved point (Profile / Settings).</li>
 *   <li>EXTRA_TRANSPORTER: vehicle base used for dispatch instead of a delivery point.</li>
 * </ul>
 */
public class SetDeliveryAddressActivity extends PinLocationActivity {

    public static final String EXTRA_REGISTER = "registerRequest";
    /** Transporter mode: vehicle base for dispatch. */
    public static final String EXTRA_TRANSPORTER = "transporterBase";
    /** Transporter sign-up: licence photo (file Uri) uploaded right after the account is created. */
    public static final String EXTRA_LICENCE_URI = "licenceUri";

    private RegisterRequest registerRequest;
    private boolean transporter;

    @Override
    protected boolean prepare() {
        registerRequest = getIntent().getParcelableExtra(EXTRA_REGISTER);
        transporter = getIntent().getBooleanExtra(EXTRA_TRANSPORTER, false);
        return true;
    }

    @Override
    protected String screenTitle() {
        return transporter ? "Set Vehicle Address" : "Set Delivery Address";
    }

    @Override
    protected String screenSubtitle() {
        return transporter
                ? "Register the exact GPS coordinates where your vehicle is based"
                : "Register the exact GPS coordinates where your orders should be delivered";
    }

    @Override
    protected String hintText() {
        return transporter
                ? "Drag the map to position the pin exactly where your vehicle is usually parked. Nearby jobs are matched to it."
                : "Drag the map to position the pin exactly over your shop, warehouse or delivery gate. Transport cost is calculated from it.";
    }

    @Override
    protected String confirmLabel() {
        return registerRequest != null ? "Confirm Coordinates" : "Save Location";
    }

    @Override
    protected void onConfirm(double lat, double lng, String address) {
        if (registerRequest != null) register(lat, lng, address);
        else update(lat, lng, address);
    }

    // ---------------------------------------------------------------- new account

    private void register(double lat, double lng, String address) {
        registerRequest.setFarmLocation(new FarmLocation(address, lat, lng));
        setBusy(true, "Registering...");
        ApiClient.getApiService().registerFarmer(registerRequest).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Response<okhttp3.ResponseBody> r) {
                if (!r.isSuccessful()) {
                    setBusy(false, null);
                    showError(AuthInputUtils.errorMessage(r, "Registration failed (" + r.code() + ")"));
                    return;
                }
                // Straight into the new user's home screen (transporter: upload the licence photo first).
                setBusy(true, "Signing you in...");
                android.net.Uri licence = getIntent().getParcelableExtra(EXTRA_LICENCE_URI);
                com.harvesthub.mobile.ui.common.RoleHome.signInAfterRegistration(SetDeliveryAddressActivity.this,
                        registerRequest.getEmail(), registerRequest.getPassword(), transporter ? "TRANSPORTER" : "BUYER",
                        transporter && licence != null
                                ? goHome -> com.harvesthub.mobile.ui.transporter.TransporterSettingsActivity.LicenceUpload.upload(
                                        SetDeliveryAddressActivity.this, licence, goHome)
                                : null);
            }

            @Override
            public void onFailure(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Throwable t) {
                setBusy(false, null);
                showError("No connection to the server. Check your internet and try again.");
            }
        });
    }

    // ---------------------------------------------------------------- change saved point

    private void update(double lat, double lng, String address) {
        Map<String, Object> body = new HashMap<>();
        body.put("latitude", lat);
        body.put("longitude", lng);
        body.put("address", address);
        setBusy(true, "Saving...");
        if (transporter) {
            ApiClient.getApiService().updateVehicle(body).enqueue(new Callback<com.harvesthub.mobile.model.User>() {
                @Override
                public void onResponse(@NonNull Call<com.harvesthub.mobile.model.User> call, @NonNull Response<com.harvesthub.mobile.model.User> r) {
                    setBusy(false, null);
                    if (!r.isSuccessful()) {
                        showError(AuthInputUtils.errorMessage(r, "Could not save the location."));
                        return;
                    }
                    toast("Vehicle address saved.");
                    setResult(RESULT_OK);
                    finish();
                }

                @Override
                public void onFailure(@NonNull Call<com.harvesthub.mobile.model.User> call, @NonNull Throwable t) {
                    setBusy(false, null);
                    showError("No connection to the server. Check your internet and try again.");
                }
            });
            return;
        }
        ApiClient.getApiService().setDeliveryLocation(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Object>> call, @NonNull Response<Map<String, Object>> r) {
                setBusy(false, null);
                if (!r.isSuccessful()) {
                    showError(AuthInputUtils.errorMessage(r, "Could not save the location."));
                    return;
                }
                toast("Delivery point saved.");
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Object>> call, @NonNull Throwable t) {
                setBusy(false, null);
                showError("No connection to the server. Check your internet and try again.");
            }
        });
    }
}
