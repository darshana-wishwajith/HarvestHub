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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Page 1.3.4: Create New Password
 * Secure password reset and immediate authentication login
 */
public class CreateNewPasswordActivity extends AppCompatActivity {

    private ImageView btnBack;
    private EditText etNewPassword;
    private ImageView ivToggleNewPassword;
    private EditText etConfirmPassword;
    private ImageView ivToggleConfirmPassword;
    private LinearLayout btnSaveLogin;

    private boolean isNewPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_create_new_password);

        // White status bar with dark icons
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(Color.WHITE);
        }

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        etNewPassword = findViewById(R.id.etNewPassword);
        ivToggleNewPassword = findViewById(R.id.ivToggleNewPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword);
        btnSaveLogin = findViewById(R.id.btnSaveLogin);
    }

    private void setupListeners() {
        // Back Button
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Toggle New Password Visibility
        ivToggleNewPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isNewPasswordVisible) {
                    etNewPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    ivToggleNewPassword.setImageResource(R.drawable.ic_visibility_eye_off);
                    isNewPasswordVisible = false;
                } else {
                    etNewPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    ivToggleNewPassword.setImageResource(R.drawable.ic_visibility_eye);
                    isNewPasswordVisible = true;
                }
                etNewPassword.setSelection(etNewPassword.getText().length());
            }
        });

        // Toggle Confirm Password Visibility
        ivToggleConfirmPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConfirmPasswordVisible) {
                    etConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    ivToggleConfirmPassword.setImageResource(R.drawable.ic_visibility_eye_off);
                    isConfirmPasswordVisible = false;
                } else {
                    etConfirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    ivToggleConfirmPassword.setImageResource(R.drawable.ic_visibility_eye);
                    isConfirmPasswordVisible = true;
                }
                etConfirmPassword.setSelection(etConfirmPassword.getText().length());
            }
        });

        // Save & Login Button
        btnSaveLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSaveAndLogin();
            }
        });
    }

    private void handleSaveAndLogin() {
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(newPass)) {
            etNewPassword.setError("Please enter your new password");
            etNewPassword.requestFocus();
            return;
        }

        if (newPass.length() < 8) {
            etNewPassword.setError("Password must be at least 8 characters");
            etNewPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPass)) {
            etConfirmPassword.setError("Please confirm your new password");
            etConfirmPassword.requestFocus();
            return;
        }

        if (!newPass.equals(confirmPass)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        Toast.makeText(this, "âœ“ Password successfully changed! Logging into HarvestHub...", Toast.LENGTH_LONG).show();

        // Navigate to Sign In (Page 1.1) or Marketplace (Page 2.1)
        Intent intent = new Intent(CreateNewPasswordActivity.this, SignInBuyerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}

