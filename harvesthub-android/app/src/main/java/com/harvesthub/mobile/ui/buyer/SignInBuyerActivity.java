package com.harvesthub.mobile.ui.buyer;

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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.LoginRequest;
import com.harvesthub.mobile.model.LoginResponse;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Page 1.1: HarvestHub - Welcome Buyer / Sign In.
 * The transporter login (TransporterLoginActivity) reuses this screen with its own wording, role and
 * destination, so both sign-in screens behave the same: mobile number or email + password, the shared
 * forgot-password flow, and "Register as ..." opening the shared Create Account form for that role.
 */
public class SignInBuyerActivity extends AppCompatActivity {

    private EditText etIdentifier;
    private EditText etPassword;
    private ImageView ivTogglePassword;
    private Button btnSignIn;

    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in_buyer);

        // Clean white status bar with dark icons
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(Color.WHITE);
        }

        etIdentifier = findViewById(R.id.etIdentifier);
        etPassword = findViewById(R.id.etPassword);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        applyRoleTexts();

        ivTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            etPassword.setTransformationMethod(isPasswordVisible
                    ? HideReturnsTransformationMethod.getInstance() : PasswordTransformationMethod.getInstance());
            ivTogglePassword.setImageResource(isPasswordVisible ? R.drawable.ic_visibility_eye_off : R.drawable.ic_visibility_eye);
            etPassword.setSelection(etPassword.getText().length());
        });
        btnSignIn.setOnClickListener(v -> handleSignIn());
        findViewById(R.id.tvForgotPassword).setOnClickListener(v -> openForgotPassword());
        findViewById(R.id.tvRegisterBuyer).setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateAccountActivity.class);
            intent.putExtra("TARGET_ROLE", getExpectedRole());
            startActivity(intent);
        });
    }

    // ---------------------------------------------------------------- per-role hooks

    protected String getExpectedRole() {
        return "BUYER";
    }

    /** Wording for this role (buyer = the layout as designed). */
    protected void applyRoleTexts() {
    }

    protected Class<?> homeScreen() {
        return BuyerDashboardActivity.class;
    }

    protected final void setText(int id, CharSequence text) {
        View v = findViewById(id);
        if (v instanceof TextView) ((TextView) v).setText(text);
    }

    private String roleName() {
        return getExpectedRole().charAt(0) + getExpectedRole().substring(1).toLowerCase();
    }

    // ---------------------------------------------------------------- sign in

    private void handleSignIn() {
        String identifier = etIdentifier.getText().toString().trim();
        String password = etPassword.getText().toString();

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
        // Accounts store mobiles as 07XXXXXXXX: accept "077 123 4567", "+94 77 123 4567" too.
        String mobile = com.harvesthub.mobile.utils.AuthInputUtils.normaliseMobile(identifier);
        String login = identifier.contains("@") ? identifier.toLowerCase(java.util.Locale.US) : (mobile != null ? mobile : identifier);

        setBusy(true);
        ApiClient.getApiService().login(new LoginRequest(login, password)).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                setBusy(false);
                LoginResponse body = response.body();
                if (response.code() == 429) {
                    toast("Too many failed attempts. Try again in 15 minutes.");
                    return;
                }
                if (!response.isSuccessful() || body == null || body.getUser() == null) {
                    toast("Incorrect mobile number/email or password.");
                    return;
                }
                if (!getExpectedRole().equalsIgnoreCase(body.getUser().getRole())) {
                    toast("This is not a " + roleName().toLowerCase() + " account. Go back and choose your account type.");
                    return;
                }
                SessionManager session = new SessionManager(SignInBuyerActivity.this);
                session.saveAuthToken(body.getToken());
                session.saveUser(body.getUser());
                session.setLogin(true, getExpectedRole());
                session.setStaySignedIn(true);
                Intent intent = new Intent(SignInBuyerActivity.this, homeScreen());
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                setBusy(false);
                toast("Unable to reach HarvestHub. Check your connection and try again.");
            }
        });
    }

    private void setBusy(boolean busy) {
        btnSignIn.setEnabled(!busy);
        btnSignIn.setText(busy ? "Signing In..." : "Sign In as " + roleName());
    }

    /** Real reset flow (6-digit code by email), shared by all roles; returns to this sign-in screen. */
    private void openForgotPassword() {
        Intent forgot = new Intent(this, com.harvesthub.mobile.ui.farmer.ForgotPasswordActivity.class);
        forgot.putExtra(com.harvesthub.mobile.ui.farmer.ForgotPasswordActivity.EXTRA_LOGIN_SCREEN, getClass().getName());
        startActivity(forgot);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
