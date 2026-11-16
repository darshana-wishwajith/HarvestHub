package com.harvesthub.mobile.ui.buyer;

import com.harvesthub.mobile.R;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Page 1.2: HarvestHub - Create Account
 * Register to Next Generation Agricultural MarketPlace
 */
public class CreateAccountActivity extends AppCompatActivity {

    private EditText etFullName;
    private EditText etMobile;
    private EditText etNic;
    private ImageView ivNicInfo;
    private EditText etCreatePassword;
    private ImageView ivToggleCreatePassword;
    private EditText etConfirmPassword;
    private ImageView ivToggleConfirmPassword;
    private LinearLayout btnContinueLocation;
    private TextView tvSignInLink;

    private boolean isCreatePasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        // White status bar with dark icons
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(Color.WHITE);
        }

        initViews();
        setupListeners();
    }

    private void initViews() {
        etFullName = findViewById(R.id.etFullName);
        etMobile = findViewById(R.id.etMobile);
        etNic = findViewById(R.id.etNic);
        ivNicInfo = findViewById(R.id.ivNicInfo);
        etCreatePassword = findViewById(R.id.etCreatePassword);
        ivToggleCreatePassword = findViewById(R.id.ivToggleCreatePassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword);
        btnContinueLocation = findViewById(R.id.btnContinueLocation);
        tvSignInLink = findViewById(R.id.tvSignInLink);
    }

    private void setupListeners() {
        // NIC Info Click
        ivNicInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(CreateAccountActivity.this)
                        .setTitle("National Identity Card (NIC)")
                        .setMessage("Please enter your valid Sri Lankan NIC number:\nâ€¢ 12-digit format (e.g., 199012345678)\nâ€¢ 9-digit format with V/X (e.g., 901234567V)\n\nThis is strictly required for verified agricultural escrow contracts.")
                        .setPositiveButton("Understood", null)
                        .show();
            }
        });

        // Toggle Create Password
        ivToggleCreatePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isCreatePasswordVisible) {
                    etCreatePassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    ivToggleCreatePassword.setImageResource(R.drawable.ic_visibility_eye);
                    isCreatePasswordVisible = false;
                } else {
                    etCreatePassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    ivToggleCreatePassword.setImageResource(R.drawable.ic_visibility_eye_off);
                    isCreatePasswordVisible = true;
                }
                etCreatePassword.setSelection(etCreatePassword.getText().length());
            }
        });

        // Toggle Confirm Password
        ivToggleConfirmPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConfirmPasswordVisible) {
                    etConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    ivToggleConfirmPassword.setImageResource(R.drawable.ic_visibility_eye);
                    isConfirmPasswordVisible = false;
                } else {
                    etConfirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    ivToggleConfirmPassword.setImageResource(R.drawable.ic_visibility_eye_off);
                    isConfirmPasswordVisible = true;
                }
                etConfirmPassword.setSelection(etConfirmPassword.getText().length());
            }
        });

        // Continue to Location Setup Button
        btnContinueLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleContinue();
            }
        });

        // Already have an account? Sign In link
        tvSignInLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CreateAccountActivity.this, SignInBuyerActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void handleContinue() {
        EditText etEmail = findViewById(R.id.etEmail);
        String fullName = etFullName.getText().toString().trim();
        String mobile = com.harvesthub.mobile.utils.AuthInputUtils.normaliseMobile(etMobile.getText().toString());
        String email = etEmail.getText().toString().trim().toLowerCase(java.util.Locale.US);
        String nic = etNic.getText().toString().trim().toUpperCase(java.util.Locale.US);
        String password = etCreatePassword.getText().toString();
        String confirmPassword = etConfirmPassword.getText().toString();

        if (fullName.length() < 3 || !fullName.matches("^[A-Za-z][A-Za-z .']*$")) {
            etFullName.setError("Use letters, spaces and dots only (min 3 characters)");
            etFullName.requestFocus();
            return;
        }
        if (!mobile.matches("^07[0-9]{8}$")) {
            etMobile.setError("Enter a 10-digit number starting with 07");
            etMobile.requestFocus();
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Enter a valid email address");
            etEmail.requestFocus();
            return;
        }
        if (!nic.matches("^([0-9]{9}[VX]|[0-9]{12})$")) {
            etNic.setError("Enter a valid NIC (123456789V or 200012345678)");
            etNic.requestFocus();
            return;
        }
        if (password.length() < 8 || !password.matches(".*[A-Za-z].*") || !password.matches(".*[0-9].*")) {
            etCreatePassword.setError("At least 8 characters with letters and numbers");
            etCreatePassword.requestFocus();
            return;
        }
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        com.harvesthub.mobile.model.RegisterRequest request =
                new com.harvesthub.mobile.model.RegisterRequest(fullName, mobile, nic, email, password, targetRole());
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("email", email);
        body.put("mobileNumber", mobile);
        body.put("nic", nic);
        btnContinueLocation.setEnabled(false);
        com.harvesthub.mobile.network.ApiClient.getApiService().checkAvailability(body)
                .enqueue(new retrofit2.Callback<java.util.Map<String, Boolean>>() {
                    @Override
                    public void onResponse(retrofit2.Call<java.util.Map<String, Boolean>> call,
                                           retrofit2.Response<java.util.Map<String, Boolean>> response) {
                        btnContinueLocation.setEnabled(true);
                        java.util.Map<String, Boolean> r = response.body();
                        if (!response.isSuccessful() || r == null) {
                            Toast.makeText(CreateAccountActivity.this, "Could not verify your details. Please try again.",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                        boolean ok = true;
                        if (Boolean.FALSE.equals(r.get("mobileNumber"))) { etMobile.setError("This mobile number is already registered"); ok = false; }
                        if (Boolean.FALSE.equals(r.get("email"))) { etEmail.setError("This email is already registered"); ok = false; }
                        if (Boolean.FALSE.equals(r.get("nic"))) { etNic.setError("This NIC is already registered"); ok = false; }
                        if (!ok) return;
                        // Step 2: required delivery pin; the account is created there (decision 15 Nov)
                        // transporters: vehicle profile first (Figma transporter "Vehicle Profile"), then the vehicle address
                        Intent intent = new Intent(CreateAccountActivity.this, "TRANSPORTER".equals(targetRole())
                                ? com.harvesthub.mobile.ui.transporter.TransporterVehicleProfileActivity.class : SetDeliveryAddressActivity.class);
                        intent.putExtra(SetDeliveryAddressActivity.EXTRA_REGISTER, request);
                        startActivity(intent);
                    }

                    @Override
                    public void onFailure(retrofit2.Call<java.util.Map<String, Boolean>> call, Throwable t) {
                        btnContinueLocation.setEnabled(true);
                        Toast.makeText(CreateAccountActivity.this, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** BUYER by default; the transporter login opens this screen with TARGET_ROLE=TRANSPORTER. */
    private String targetRole() {
        return "TRANSPORTER".equals(getIntent().getStringExtra("TARGET_ROLE")) ? "TRANSPORTER" : "BUYER";
    }
}
