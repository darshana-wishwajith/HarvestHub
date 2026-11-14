package com.harvesthub.mobile.ui.transporter;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.ForgotPasswordRequest;
import com.harvesthub.mobile.model.LoginRequest;
import com.harvesthub.mobile.model.LoginResponse;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.ui.buyer.CreateAccountActivity;
import com.harvesthub.mobile.utils.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Transporter login. Figma: HarvestHub-UI, frame "1.1 Login" (node 623:2819).
 * Signs in with a Sri Lankan mobile number (+94) or, after "Use Email instead", an email address.
 */
public class TransporterLoginActivity extends AppCompatActivity {

    private EditText etIdentifier;
    private EditText etPassword;
    private ImageView ivTogglePassword;
    private ImageView ivIdentifierIcon;
    private View layoutCountryPrefix;
    private TextView tvIdentifierLabel;
    private TextView tvUseEmail;
    private CheckBox cbStaySignedIn;
    private Button btnSignIn;

    private boolean isPasswordVisible = false;
    private boolean emailMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transporter_login);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.tl_background));
        }

        initViews();
        setupListeners();
    }

    private void initViews() {
        etIdentifier = findViewById(R.id.etIdentifier);
        etPassword = findViewById(R.id.etPassword);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        ivIdentifierIcon = findViewById(R.id.ivIdentifierIcon);
        layoutCountryPrefix = findViewById(R.id.layoutCountryPrefix);
        tvIdentifierLabel = findViewById(R.id.tvIdentifierLabel);
        tvUseEmail = findViewById(R.id.tvUseEmail);
        cbStaySignedIn = findViewById(R.id.cbStaySignedIn);
        btnSignIn = findViewById(R.id.btnSignIn);
    }

    private void setupListeners() {
        ivTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            etPassword.setTransformationMethod(isPasswordVisible
                    ? HideReturnsTransformationMethod.getInstance()
                    : PasswordTransformationMethod.getInstance());
            ivTogglePassword.setImageResource(isPasswordVisible
                    ? R.drawable.ic_visibility_eye_off
                    : R.drawable.ic_tl_eye);
            etPassword.setSelection(etPassword.getText().length());
        });

        tvUseEmail.setOnClickListener(v -> setEmailMode(!emailMode));

        btnSignIn.setOnClickListener(v -> handleSignIn());

        findViewById(R.id.tvForgotPassword).setOnClickListener(v -> showForgotPasswordDialog());

        findViewById(R.id.tvRegisterTransporter).setOnClickListener(v -> {
            Intent intent = new Intent(this, CreateAccountActivity.class);
            intent.putExtra("TARGET_ROLE", "TRANSPORTER");
            startActivity(intent);
        });
    }

    /** Switches the first field between "+94 mobile" and "email". */
    private void setEmailMode(boolean email) {
        emailMode = email;
        etIdentifier.setText("");
        etIdentifier.setError(null);
        tvIdentifierLabel.setText(email ? R.string.tl_label_email : R.string.tl_label_mobile);
        tvUseEmail.setText(email ? R.string.tl_use_mobile : R.string.tl_use_email);
        etIdentifier.setHint(email ? R.string.tl_hint_email : R.string.tl_hint_mobile);
        etIdentifier.setInputType(email
                ? InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                : InputType.TYPE_CLASS_PHONE);
        layoutCountryPrefix.setVisibility(email ? View.GONE : View.VISIBLE);
        ivIdentifierIcon.setVisibility(email ? View.GONE : View.VISIBLE);
        etIdentifier.requestFocus();
    }

    /**
     * Accounts store mobiles as 07XXXXXXXX. Accepts "077 123 4567", "771234567", "+94771234567" or "94771234567".
     * Returns null if it is not a valid Sri Lankan mobile number.
     */
    static String normaliseMobile(String raw) {
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("94") && digits.length() == 11) {
            digits = "0" + digits.substring(2);
        } else if (digits.length() == 9 && digits.startsWith("7")) {
            digits = "0" + digits;
        }
        return digits.matches("07\\d{8}") ? digits : null;
    }

    private void handleSignIn() {
        String input = etIdentifier.getText().toString().trim();
        String password = etPassword.getText().toString();

        String identifier;
        if (emailMode) {
            if (!Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
                etIdentifier.setError("Enter a valid email address");
                etIdentifier.requestFocus();
                return;
            }
            identifier = input.toLowerCase();
        } else {
            identifier = normaliseMobile(input);
            if (identifier == null) {
                etIdentifier.setError("Enter a valid mobile number, e.g. 077 123 4567");
                etIdentifier.requestFocus();
                return;
            }
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Enter your PIN or password");
            etPassword.requestFocus();
            return;
        }

        btnSignIn.setEnabled(false);

        ApiClient.getApiService().login(new LoginRequest(identifier, password))
                .enqueue(new Callback<LoginResponse>() {
                    @Override
                    public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                        btnSignIn.setEnabled(true);
                        LoginResponse loginResponse = response.body();

                        if (response.code() == 429) {
                            toast("Too many failed attempts. Try again in 15 minutes.");
                            return;
                        }
                        if (!response.isSuccessful() || loginResponse == null || loginResponse.getUser() == null) {
                            toast("Incorrect mobile/email or password.");
                            return;
                        }

                        String userRole = loginResponse.getUser().getRole();
                        if (!"TRANSPORTER".equalsIgnoreCase(userRole)) {
                            toast("This is not a transporter account. Go back and choose your account type.");
                            return;
                        }

                        SessionManager sessionManager = new SessionManager(TransporterLoginActivity.this);
                        sessionManager.saveAuthToken(loginResponse.getToken());
                        sessionManager.saveUser(loginResponse.getUser());
                        sessionManager.setLogin(true, "TRANSPORTER");
                        sessionManager.setStaySignedIn(cbStaySignedIn.isChecked());

                        Intent intent = new Intent(TransporterLoginActivity.this, DeliveryDashboardActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailure(Call<LoginResponse> call, Throwable throwable) {
                        btnSignIn.setEnabled(true);
                        toast("Can't reach the server. Check your internet connection.");
                    }
                });
    }

    private void showForgotPasswordDialog() {
        String identifier = etIdentifier.getText().toString().trim();
        if (!emailMode || !Patterns.EMAIL_ADDRESS.matcher(identifier).matches()) {
            new AlertDialog.Builder(this)
                    .setTitle("Reset PIN / password")
                    .setMessage("Reset codes are sent by email. Tap \"Use Email instead\", enter your registered email, then tap \"Forgot PIN?\" again.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        ApiClient.getApiService().forgotPassword(new ForgotPasswordRequest(identifier))
                .enqueue(new Callback<okhttp3.ResponseBody>() {
                    @Override
                    public void onResponse(Call<okhttp3.ResponseBody> call, Response<okhttp3.ResponseBody> response) {
                        new AlertDialog.Builder(TransporterLoginActivity.this)
                                .setTitle("Check your email")
                                .setMessage("If " + identifier + " is registered, a reset code has been sent to it.")
                                .setPositiveButton("OK", null)
                                .show();
                    }

                    @Override
                    public void onFailure(Call<okhttp3.ResponseBody> call, Throwable t) {
                        toast("Can't reach the server. Check your internet connection.");
                    }
                });
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
