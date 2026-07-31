package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.RegisterRequest;

import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilFullName, tilMobileNumber, tilEmail, tilNic, tilPassword, tilConfirmPassword;
    private TextInputEditText etFullName, etMobileNumber, etEmail, etNic, etPassword, etConfirmPassword;
    private TextView tvAppName, tvSignIn;
    private Button btnContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initViews();
        setupSpannableTexts();

        tilEmail.setEndIconOnClickListener(v -> showEmailInfoDialog());
        tilNic.setEndIconOnClickListener(v -> showNicInfoDialog());
        btnContinue.setOnClickListener(v -> validateAndContinue());
    }

    private void initViews() {
        tilFullName = findViewById(R.id.tilFullName);
        tilMobileNumber = findViewById(R.id.tilMobileNumber);
        tilEmail = findViewById(R.id.tilEmail);
        tilNic = findViewById(R.id.tilNic);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        etFullName = findViewById(R.id.etFullName);
        etMobileNumber = findViewById(R.id.etMobileNumber);
        etEmail = findViewById(R.id.etEmail);
        etNic = findViewById(R.id.etNic);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        tvAppName = findViewById(R.id.tvAppName);
        tvSignIn = findViewById(R.id.tvSignIn);
        btnContinue = findViewById(R.id.btnContinue);
    }

    private void setupSpannableTexts() {
        String appName = "HarvestHub";
        SpannableString ssAppName = new SpannableString(appName);
        ssAppName.setSpan(new ForegroundColorSpan(Color.BLACK), 0, 7, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssAppName.setSpan(new ForegroundColorSpan(Color.parseColor("#1B4332")), 7, 10, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvAppName.setText(ssAppName);

        String signInText = "Already have an account? Sign In";
        SpannableString ssSignIn = new SpannableString(signInText);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                startActivity(new Intent(RegisterActivity.this, FarmerLoginActivity.class));
            }
        };
        int start = signInText.indexOf("Sign In");
        ssSignIn.setSpan(clickableSpan, start, signInText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssSignIn.setSpan(new ForegroundColorSpan(Color.parseColor("#1B4332")), start, signInText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssSignIn.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, signInText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvSignIn.setText(ssSignIn);
        tvSignIn.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
    }

    private void showNicInfoDialog() {
        new AlertDialog.Builder(this)
                .setTitle("NIC Format Info")
                .setMessage("Valid NIC formats:\n\n" +
                        "Old: 9 digits followed by 'V' or 'X' (e.g., 123456789V)\n" +
                        "New: 12 digits (e.g., 200012345678)")
                .setPositiveButton("Got it", null)
                .show();
    }

    private void showEmailInfoDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Email Format Info")
                .setMessage("Please enter a valid email address.\n\n" +
                        "Example: example@domain.com\n" +
                        "It must contain an '@' symbol and a domain (e.g., .com, .org).")
                .setPositiveButton("Got it", null)
                .show();
    }

    private void validateAndContinue() {
        String fullName = etFullName.getText().toString().trim();
        String mobile = etMobileNumber.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String nic = etNic.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        tilFullName.setError(null);
        tilMobileNumber.setError(null);
        tilEmail.setError(null);
        tilNic.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        boolean isValid = true;

        if (fullName.isEmpty()) {
            tilFullName.setError("Full Name is required");
            isValid = false;
        }
        if (!Pattern.matches("07[0-9]{8}", mobile)) {
            tilMobileNumber.setError("Enter a valid 10-digit mobile number");
            isValid = false;
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address");
            isValid = false;
        }
        if (!Pattern.matches("^[0-9]{9}[vVxX]$|^[0-9]{12}$", nic)) {
            tilNic.setError("Enter a valid NIC");
            isValid = false;
        }
        if (password.length() < 8) {
            tilPassword.setError("Password must be at least 8 characters");
            isValid = false;
        }
        if (!confirmPassword.equals(password)) {
            tilConfirmPassword.setError("Passwords do not match");
            isValid = false;
        }

        if (isValid) {
            String hashedPassword = com.harvesthub.mobile.utils.PasswordUtils.hashPassword(password);
            RegisterRequest registerRequest = new RegisterRequest(
                    fullName, mobile, nic, email, hashedPassword, "FARMER"
            );
            Intent intent = new Intent(this, LocationSetupActivity.class);
            intent.putExtra("registerRequest", registerRequest);
            startActivity(intent);
        }
    }
}
