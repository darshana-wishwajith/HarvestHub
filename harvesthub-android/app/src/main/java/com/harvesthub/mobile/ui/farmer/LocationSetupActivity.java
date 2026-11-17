package com.harvesthub.mobile.ui.farmer;

import androidx.annotation.NonNull;

import com.harvesthub.mobile.model.FarmLocation;
import com.harvesthub.mobile.model.RegisterRequest;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.ui.common.PinLocationActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Step 2 of farmer registration: pin the farm (shared {@link PinLocationActivity} screen, same as buyer and
 * transporter). On confirm the account and farm location are sent to the server in one register call.
 */
public class LocationSetupActivity extends PinLocationActivity {

    private RegisterRequest registerRequest;

    @Override
    protected boolean prepare() {
        registerRequest = getIntent().getParcelableExtra("registerRequest");
        return registerRequest != null;   // opened without step 1 - go back to the form
    }

    @Override
    protected String screenTitle() {
        return "Pin Farm Location";
    }

    @Override
    protected String screenSubtitle() {
        return "Register the exact GPS coordinates of your physical farm";
    }

    @Override
    protected String hintText() {
        return "Drag the map to position the pin exactly over your main farm facility or central field.";
    }

    @Override
    protected void onConfirm(double lat, double lng, String address) {
        registerRequest.setFarmLocation(new FarmLocation(address, lat, lng));
        setBusy(true, "Registering...");
        ApiClient.getApiService().registerFarmer(registerRequest).enqueue(new Callback<okhttp3.ResponseBody>() {
            @Override
            public void onResponse(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Response<okhttp3.ResponseBody> response) {
                if (response.isSuccessful()) {   // straight into the farmer home screen
                    setBusy(true, "Signing you in...");
                    com.harvesthub.mobile.ui.common.RoleHome.signInAfterRegistration(LocationSetupActivity.this,
                            registerRequest.getEmail(), registerRequest.getPassword(), "FARMER", null);
                    return;
                }
                setBusy(false, null);
                String message = "Registration failed (" + response.code() + ")";
                try (okhttp3.ResponseBody err = response.errorBody()) {
                    if (err != null) message = formatError(err.string(), message);
                } catch (Exception ignored) {
                }
                showError(message);
            }

            @Override
            public void onFailure(@NonNull Call<okhttp3.ResponseBody> call, @NonNull Throwable t) {
                setBusy(false, null);
                showError("No connection to the server. Check your internet and try again.");
            }
        });
    }
}
