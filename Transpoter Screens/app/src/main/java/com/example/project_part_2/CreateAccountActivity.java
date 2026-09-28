package com.example.project_part_2;

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
                        .setMessage("Please enter your valid Sri Lankan NIC number:\n• 12-digit format (e.g., 199012345678)\n• 9-digit format with V/X (e.g., 901234567V)\n\nThis is strictly required for verified agricultural escrow contracts.")
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
        String fullName = etFullName.getText().toString().trim();
        String mobile = etMobile.getText().toString().trim();
        String nic = etNic.getText().toString().trim();
        String password = etCreatePassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Please enter your full name");
            etFullName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(mobile)) {
            etMobile.setError("Please enter your mobile number");
            etMobile.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(nic)) {
            etNic.setError("Please enter your NIC number");
            etNic.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etCreatePassword.setError("Please create a password");
            etCreatePassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etCreatePassword.setError("Password must be at least 6 characters");
            etCreatePassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        Toast.makeText(this, "Account details validated! Proceeding to Location Setup...", Toast.LENGTH_SHORT).show();

        // Navigate to OTP Verification (Page 1.3.6)
        Intent intent = new Intent(CreateAccountActivity.this, VerificationCodeActivity.class);
        startActivity(intent);
    }
}
