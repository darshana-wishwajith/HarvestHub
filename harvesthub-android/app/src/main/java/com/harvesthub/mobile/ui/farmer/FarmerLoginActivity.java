package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.LoginRequest;
import com.harvesthub.mobile.model.LoginResponse;
import com.harvesthub.mobile.model.User;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;
import com.harvesthub.mobile.utils.PasswordUtils;
import com.harvesthub.mobile.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FarmerLoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private ImageView ivPasswordToggle;
    private TextView tvForgotPassword, tvRegister;
    private Button btnSignIn;
    private com.google.android.material.card.MaterialCardView cvError;
    private TextView tvErrorMessage;
    private android.view.View ivCloseError;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farmer_login);

        initViews();
        setupListeners();
    }

    private void initViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        ivPasswordToggle = findViewById(R.id.ivPasswordToggle);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvRegister = findViewById(R.id.tvRegister);
        btnSignIn = findViewById(R.id.btnSignIn);
        cvError = findViewById(R.id.cvError);
        tvErrorMessage = findViewById(R.id.tvErrorMessage);
        ivCloseError = findViewById(R.id.ivCloseError);
    }

    private void setupListeners() {
        ivPasswordToggle.setOnClickListener(v -> togglePasswordVisibility());

        tvForgotPassword.setOnClickListener(v -> {
            // Navigate to Forgot Password screen
            Intent intent = new Intent(this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        btnSignIn.setOnClickListener(v -> performLogin());

        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(this, RegisterActivity.class);
            startActivity(intent);
        });

        ivCloseError.setOnClickListener(v -> cvError.setVisibility(android.view.View.GONE));
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            ivPasswordToggle.setImageResource(R.drawable.ic_visibility);
        } else {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            ivPasswordToggle.setImageResource(R.drawable.ic_visibility_off);
        }
        isPasswordVisible = !isPasswordVisible;
        etPassword.setSelection(etPassword.getText().length());
    }

    private void performLogin() {
        String emailOrMobile = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(emailOrMobile)) {
            etEmail.setError("Please enter your email or mobile number");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Please enter your password");
            etPassword.requestFocus();
            return;
        }

        // Hash the password before sending to the server
        String hashedPassword = PasswordUtils.hashPassword(password);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        LoginRequest loginRequest = new LoginRequest(emailOrMobile, hashedPassword);

        cvError.setVisibility(android.view.View.GONE);
        btnSignIn.setEnabled(false);
        btnSignIn.setText("Signing In...");

        apiService.login(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnSignIn.setEnabled(true);
                btnSignIn.setText(R.string.sign_in_as_farmer);
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    SessionManager sessionManager = new SessionManager(FarmerLoginActivity.this);
                    
                    User user = loginResponse.getUser();
                    if (user != null && (user.getRole() == null || user.getRole().isEmpty())) {
                        user.setRole("FARMER");
                    }
                    
                    sessionManager.saveAuthToken(loginResponse.getToken());
                    sessionManager.saveUser(user);
                    sessionManager.setLogin(true, "FARMER");

                    Toast.makeText(FarmerLoginActivity.this, "Login Successful!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(FarmerLoginActivity.this, com.harvesthub.mobile.MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    String detailedErrorMessage = "Login Failed";
                    try (okhttp3.ResponseBody errorBody = response.errorBody()) {
                        if (errorBody != null) {
                            String bodyString = errorBody.string();
                            if (bodyString != null && !bodyString.trim().isEmpty()) {
                                detailedErrorMessage = formatError(bodyString);
                            } else {
                                detailedErrorMessage = "Login Failed: " + response.code() + " " + response.message();
                            }
                        } else {
                            detailedErrorMessage = "Login Failed: " + response.code() + " " + response.message();
                        }
                    } catch (Exception e) {
                        android.util.Log.e("LoginError", "Error parsing error body", e);
                        detailedErrorMessage = "Login Failed: " + response.code();
                    }
                    tvErrorMessage.setText(detailedErrorMessage);
                    cvError.setVisibility(android.view.View.VISIBLE);
                    android.util.Log.e("LoginError", "HTTP Code: " + response.code() + ", Error: " + detailedErrorMessage);
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnSignIn.setEnabled(true);
                btnSignIn.setText(R.string.sign_in_as_farmer);
                String errorMessage = "Network error: " + t.getMessage();
                tvErrorMessage.setText(errorMessage);
                cvError.setVisibility(android.view.View.VISIBLE);
                android.util.Log.e("LoginError", errorMessage, t);
            }
        });
    }

    private String formatError(String errorBody) {
        try {
            if (errorBody.trim().startsWith("{")) {
                org.json.JSONObject jsonObject = new org.json.JSONObject(errorBody);
                StringBuilder sb = new StringBuilder();
                java.util.Iterator<String> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String message = jsonObject.optString(key);
                    if (sb.length() > 0) sb.append("\n");
                    sb.append("• ").append(message);
                }
                return sb.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return errorBody;
    }
}
