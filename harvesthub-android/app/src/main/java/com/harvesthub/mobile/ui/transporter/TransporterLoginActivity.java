package com.harvesthub.mobile.ui.transporter;

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

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.ForgotPasswordRequest;
import com.harvesthub.mobile.model.LoginRequest;
import com.harvesthub.mobile.model.LoginResponse;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.ui.buyer.CreateAccountActivity;
import com.harvesthub.mobile.utils.SessionManager;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Transporter Login Activity
 * Dedicated authentication portal for agricultural freight haulers and logistics fleet.
 */
public class TransporterLoginActivity extends AppCompatActivity {

    private EditText etIdentifier;
    private EditText etPassword;
    private ImageView ivTogglePassword;
    private Button btnSignIn;
    private TextView tvForgotPassword;
    private TextView tvRegisterTransporter;

    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transporter_login);

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
        tvRegisterTransporter = findViewById(R.id.tvRegisterTransporter);
    }

    private void setupListeners() {
        ivTogglePassword.setOnClickListener(v -> {
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
        });

        btnSignIn.setOnClickListener(v -> handleSignIn());

        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        if (tvRegisterTransporter != null) {
            tvRegisterTransporter.setOnClickListener(v -> {
                Intent intent = new Intent(TransporterLoginActivity.this, CreateAccountActivity.class);
                intent.putExtra("TARGET_ROLE", "TRANSPORTER");
                startActivity(intent);
            });
        }
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

        btnSignIn.setEnabled(false);
        Toast.makeText(this, "Signing in to Transporter Portal...", Toast.LENGTH_SHORT).show();

        ApiClient.getApiService().login(new LoginRequest(identifier, password))
                .enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                        btnSignIn.setEnabled(true);
                        LoginResponse loginResponse = response.body();

                        if (!response.isSuccessful() || loginResponse == null || loginResponse.getUser() == null) {
                            Toast.makeText(TransporterLoginActivity.this,
                                    "Invalid credentials. Please verify your mobile or password.",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        String userRole = loginResponse.getUser().getRole();
                        if (userRole == null || !"TRANSPORTER".equalsIgnoreCase(userRole)) {
                            Toast.makeText(TransporterLoginActivity.this,
                                    "This account is registered as " + userRole + ". Please use the Transporter portal with a transporter account.",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        SessionManager sessionManager = new SessionManager(TransporterLoginActivity.this);
                        sessionManager.saveAuthToken(loginResponse.getToken());
                        sessionManager.saveUser(loginResponse.getUser());
                        sessionManager.setLogin(true, "TRANSPORTER");

                        Intent intent = new Intent(TransporterLoginActivity.this, DeliveryDashboardActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailure(Call<LoginResponse> call, Throwable throwable) {
                        btnSignIn.setEnabled(true);
                        Toast.makeText(TransporterLoginActivity.this,
                                "Network connection failed. Please check your internet connection.",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showForgotPasswordDialog() {
        String identifier = etIdentifier.getText().toString().trim();
        if (TextUtils.isEmpty(identifier) || !identifier.contains("@")) {
            new AlertDialog.Builder(this)
                    .setTitle("Reset Password")
                    .setMessage("Please enter your registered email address in the field above to receive a password reset token.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        Toast.makeText(this, "Dispatching reset email...", Toast.LENGTH_SHORT).show();
        ApiClient.getApiService().forgotPassword(new ForgotPasswordRequest(identifier))
                .enqueue(new Callback<okhttp3.ResponseBody>() {
                    @Override
                    public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                        new AlertDialog.Builder(TransporterLoginActivity.this)
                                .setTitle("Password Reset Dispatched")
                                .setMessage("A secure password reset token has been sent to " + identifier + ". Please check your inbox.")
                                .setPositiveButton("OK", null)
                                .show();
                    }

                    @Override
                    public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                        Toast.makeText(TransporterLoginActivity.this, "Failed to send reset email. Check connection.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}