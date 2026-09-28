package com.example.project_part_2;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Page 1.1: HarvestHub - Welcome Buyer / Sign In
 * Wholesale Agricultural Sourcing Platform
 */
public class SignInBuyerActivity extends AppCompatActivity {

    private EditText etIdentifier;
    private EditText etPassword;
    private ImageView ivTogglePassword;
    private Button btnSignIn;
    private TextView tvForgotPassword;
    private TextView tvRegisterBuyer;

    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in_buyer);

        // Set clean white status bar with dark icons
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(Color.WHITE);
        }

        initViews();
        setupListeners();
    }

    private void initViews() {
        etIdentifier = findViewById(R.id.etIdentifier);
        etPassword = findViewById(R.id.etPassword);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvRegisterBuyer = findViewById(R.id.tvRegisterBuyer);
    }

    private void setupListeners() {
        // Toggle password show/hide
        ivTogglePassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isPasswordVisible) {
                    etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    ivTogglePassword.setImageResource(R.drawable.ic_visibility_eye);
                    isPasswordVisible = false;
                } else {
                    etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    ivTogglePassword.setImageResource(R.drawable.ic_visibility_eye_off);
                    isPasswordVisible = true;
                }
                etPassword.setSelection(etPassword.getText().length());
            }
        });

        // Sign In button
        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleSignIn();
            }
        });

        // Forgot password
        tvForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showForgotPasswordDialog();
            }
        });

        // Register link
        tvRegisterBuyer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignInBuyerActivity.this, CreateAccountActivity.class);
                startActivity(intent);
            }
        });
    }

    private void handleSignIn() {
        String identifier = etIdentifier.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(identifier)) {
            etIdentifier.setError("Please enter your mobile number or email");
            etIdentifier.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Please enter your password");
            etPassword.requestFocus();
            return;
        }

        Toast.makeText(this, "Signing in to HarvestHub...", Toast.LENGTH_SHORT).show();

        // Navigate to Verified Harvests Marketplace (Page 2.1)
        Intent intent = new Intent(SignInBuyerActivity.this, VerifiedHarvestsActivity.class);
        startActivity(intent);
    }

    private void showForgotPasswordDialog() {
        String identifier = etIdentifier.getText().toString().trim();
        String message = TextUtils.isEmpty(identifier) 
                ? "Enter your mobile number or email above, and a secure password recovery code will be dispatched."
                : "A secure reset link has been dispatched to: " + identifier;

        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }
}
