package com.harvesthub.mobile.ui.buyer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.RegisterRequest;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.utils.AuthInputUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Page 1.2: HarvestHub - Create Account (buyer; also used by transporters with TARGET_ROLE=TRANSPORTER).
 * Validation matches farmer registration: every field is checked at once and each message is shown
 * under its own field; messages clear as soon as the field is edited. Email / mobile / NIC are then
 * checked against the server before moving to step 2.
 */
public class CreateAccountActivity extends AppCompatActivity {

    private TextInputLayout tilFullName, tilEmail, tilMobile, tilNic, tilCreatePassword, tilConfirmPassword;
    private TextInputEditText etFullName, etEmail, etMobile, etNic, etCreatePassword, etConfirmPassword;
    private LinearLayout btnContinueLocation;
    private TextView tvContinueLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        // White status bar with dark icons
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(Color.WHITE);
        }

        tilFullName = findViewById(R.id.tilFullName);
        tilEmail = findViewById(R.id.tilEmail);
        tilMobile = findViewById(R.id.tilMobile);
        tilNic = findViewById(R.id.tilNic);
        tilCreatePassword = findViewById(R.id.tilCreatePassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etFullName = findViewById(R.id.etFullName);
        etEmail = findViewById(R.id.etEmail);
        etMobile = findViewById(R.id.etMobile);
        etNic = findViewById(R.id.etNic);
        etCreatePassword = findViewById(R.id.etCreatePassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnContinueLocation = findViewById(R.id.btnContinueLocation);
        tvContinueLabel = (TextView) btnContinueLocation.getChildAt(0);

        tilNic.setEndIconOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("National Identity Card (NIC)")
                .setMessage("Please enter your valid Sri Lankan NIC number:\n"
                        + "• 12-digit format (e.g., 199012345678)\n"
                        + "• 9 digits with V/X (e.g., 901234567V)\n\n"
                        + "This is required for verified agricultural escrow contracts.")
                .setPositiveButton("Understood", null)
                .show());

        clearErrorOnEdit(etFullName, tilFullName);
        clearErrorOnEdit(etEmail, tilEmail);
        clearErrorOnEdit(etMobile, tilMobile);
        clearErrorOnEdit(etNic, tilNic);
        clearErrorOnEdit(etCreatePassword, tilCreatePassword);
        clearErrorOnEdit(etConfirmPassword, tilConfirmPassword);

        btnContinueLocation.setOnClickListener(v -> validateAndContinue());
        // Back to the sign-in screen this form was opened from (buyer or transporter)
        findViewById(R.id.tvSignInLink).setOnClickListener(v -> finish());
    }

    /** Clears a field's error as soon as the user edits it. */
    private static void clearErrorOnEdit(TextInputEditText et, TextInputLayout til) {
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence c, int a, int b, int d) { }
            @Override public void onTextChanged(CharSequence c, int a, int b, int d) { til.setError(null); }
            @Override public void afterTextChanged(Editable e) { }
        });
    }

    private static String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString();
    }

    private void validateAndContinue() {
        String fullName = text(etFullName).trim().replaceAll("\\s+", " ");
        String rawMobile = text(etMobile).trim();
        String mobile = AuthInputUtils.normaliseMobile(rawMobile);
        String email = text(etEmail).trim().toLowerCase(Locale.US);
        String nic = text(etNic).trim().toUpperCase(Locale.US);
        String password = text(etCreatePassword);            // sent as typed; never trimmed
        String confirmPassword = text(etConfirmPassword);

        TextInputLayout firstError = null;

        if (fullName.isEmpty()) {
            firstError = fail(tilFullName, "Full name is required", firstError);
        } else if (fullName.length() < 3 || !fullName.matches("^[A-Za-z][A-Za-z .']*$")) {
            firstError = fail(tilFullName, "Use letters, spaces and dots only (min 3 characters)", firstError);
        }
        if (email.isEmpty()) {
            firstError = fail(tilEmail, "Email is required", firstError);
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            firstError = fail(tilEmail, "Enter a valid email address", firstError);
        }
        if (rawMobile.isEmpty()) {
            firstError = fail(tilMobile, "Mobile number is required", firstError);
        } else if (mobile == null) {
            firstError = fail(tilMobile, "Enter a 10-digit number starting with 07", firstError);
        }
        if (nic.isEmpty()) {
            firstError = fail(tilNic, "NIC is required", firstError);
        } else if (!AuthInputUtils.isValidNic(nic)) {
            firstError = fail(tilNic, "Enter a valid NIC (123456789V or 200012345678)", firstError);
        }
        if (password.isEmpty()) {
            firstError = fail(tilCreatePassword, "Password is required", firstError);
        } else if (!AuthInputUtils.isStrongEnough(password)) {
            firstError = fail(tilCreatePassword, "At least 8 characters with letters and numbers", firstError);
        }
        if (confirmPassword.isEmpty()) {
            firstError = fail(tilConfirmPassword, "Please confirm your password", firstError);
        } else if (!confirmPassword.equals(password)) {
            firstError = fail(tilConfirmPassword, "Passwords do not match", firstError);
        }

        if (firstError != null) {
            firstError.requestFocus();
            return;
        }
        checkAvailabilityThenContinue(new RegisterRequest(fullName, mobile, nic, email, password, targetRole()));
    }

    /** Shows {@code message} under the field; returns the first field with an error (for focus). */
    private static TextInputLayout fail(TextInputLayout til, String message, TextInputLayout firstError) {
        til.setError(message);
        return firstError != null ? firstError : til;
    }

    /** Asks the server whether email / mobile / NIC are already registered before moving to step 2. */
    private void checkAvailabilityThenContinue(RegisterRequest request) {
        Map<String, String> body = new HashMap<>();
        body.put("email", request.getEmail());
        body.put("mobileNumber", request.getMobileNumber());
        body.put("nic", request.getNic());
        setBusy(true);
        ApiClient.getApiService().checkAvailability(body).enqueue(new Callback<Map<String, Boolean>>() {
            @Override
            public void onResponse(@NonNull Call<Map<String, Boolean>> call, @NonNull Response<Map<String, Boolean>> response) {
                setBusy(false);
                Map<String, Boolean> r = response.body();
                if (!response.isSuccessful() || r == null) {
                    Toast.makeText(CreateAccountActivity.this,
                            "Could not verify your details (" + response.code() + "). Please try again.", Toast.LENGTH_LONG).show();
                    return;
                }
                TextInputLayout firstError = null;
                if (Boolean.FALSE.equals(r.get("email"))) firstError = fail(tilEmail, "This email is already registered", null);
                if (Boolean.FALSE.equals(r.get("mobileNumber"))) firstError = fail(tilMobile, "This mobile number is already registered", firstError);
                if (Boolean.FALSE.equals(r.get("nic"))) firstError = fail(tilNic, "This NIC is already registered", firstError);
                if (firstError != null) {
                    firstError.requestFocus();
                    return;
                }
                // Step 2: required delivery pin; the account is created there (decision 15 Nov)
                // transporters: vehicle profile first (Figma transporter "Vehicle Profile"), then the vehicle address
                Intent intent = new Intent(CreateAccountActivity.this, "TRANSPORTER".equals(targetRole())
                        ? com.harvesthub.mobile.ui.transporter.TransporterVehicleProfileActivity.class : SetDeliveryAddressActivity.class);
                intent.putExtra(SetDeliveryAddressActivity.EXTRA_REGISTER, request);
                startActivity(intent);
            }

            @Override
            public void onFailure(@NonNull Call<Map<String, Boolean>> call, @NonNull Throwable t) {
                setBusy(false);
                Toast.makeText(CreateAccountActivity.this, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setBusy(boolean busy) {
        btnContinueLocation.setEnabled(!busy);
        btnContinueLocation.setAlpha(busy ? 0.7f : 1f);
        if (tvContinueLabel != null) tvContinueLabel.setText(busy ? "Checking details..." : "Continue to Location Setup");
    }

    /** BUYER by default; the transporter login opens this screen with TARGET_ROLE=TRANSPORTER. */
    private String targetRole() {
        return "TRANSPORTER".equals(getIntent().getStringExtra("TARGET_ROLE")) ? "TRANSPORTER" : "BUYER";
    }
}
