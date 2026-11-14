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

        clearErrorOnEdit(etFullName, tilFullName);
        clearErrorOnEdit(etMobileNumber, tilMobileNumber);
        clearErrorOnEdit(etEmail, tilEmail);
        clearErrorOnEdit(etNic, tilNic);
        clearErrorOnEdit(etPassword, tilPassword);
        clearErrorOnEdit(etConfirmPassword, tilConfirmPassword);
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

    /** Clears a field's error as soon as the user edits it. */
    private void clearErrorOnEdit(TextInputEditText et, TextInputLayout til) {
        et.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence c, int a, int b, int d) { }
            @Override public void onTextChanged(CharSequence c, int a, int b, int d) { til.setError(null); }
            @Override public void afterTextChanged(android.text.Editable e) { }
        });
    }

    private static String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString();
    }

    /** Sri Lankan NIC: old 9 digits + V/X, new 12 digits. Day-of-year must be 1-366 (+500 for female). */
    static boolean isValidNic(String nic) {
        int day;
        if (nic.matches("^[0-9]{9}[VvXx]$")) {
            day = Integer.parseInt(nic.substring(2, 5));
        } else if (nic.matches("^[0-9]{12}$")) {
            int year = Integer.parseInt(nic.substring(0, 4));
            if (year < 1900 || year > java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - 16) return false;
            day = Integer.parseInt(nic.substring(4, 7));
        } else {
            return false;
        }
        if (day > 500) day -= 500;
        return day >= 1 && day <= 366;
    }

    private void validateAndContinue() {
        String fullName = text(etFullName).trim().replaceAll("\\s+", " ");
        String mobile = text(etMobileNumber).replaceAll("[\\s-]", "");
        String email = text(etEmail).trim().toLowerCase();
        String nic = text(etNic).trim().toUpperCase();
        String password = text(etPassword);            // sent as typed; never trimmed
        String confirmPassword = text(etConfirmPassword);

        TextInputLayout firstError = null;

        if (fullName.isEmpty()) {
            tilFullName.setError("Full name is required");
            firstError = tilFullName;
        } else if (fullName.length() < 3 || !fullName.matches("^[A-Za-z][A-Za-z .']*$")) {
            tilFullName.setError("Use letters, spaces and dots only (min 3 characters)");
            firstError = tilFullName;
        }
        if (mobile.isEmpty()) {
            tilMobileNumber.setError("Mobile number is required");
            if (firstError == null) firstError = tilMobileNumber;
        } else if (!mobile.matches("^07[0-9]{8}$")) {
            tilMobileNumber.setError("Enter a 10-digit number starting with 07");
            if (firstError == null) firstError = tilMobileNumber;
        }
        if (email.isEmpty()) {
            tilEmail.setError("Email is required");
            if (firstError == null) firstError = tilEmail;
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address");
            if (firstError == null) firstError = tilEmail;
        }
        if (nic.isEmpty()) {
            tilNic.setError("NIC is required");
            if (firstError == null) firstError = tilNic;
        } else if (!isValidNic(nic)) {
            tilNic.setError("Enter a valid NIC (123456789V or 200012345678)");
            if (firstError == null) firstError = tilNic;
        }
        if (password.isEmpty()) {
            tilPassword.setError("Password is required");
            if (firstError == null) firstError = tilPassword;
        } else if (password.length() < 8 || !password.matches(".*[A-Za-z].*") || !password.matches(".*[0-9].*")) {
            tilPassword.setError("At least 8 characters with letters and numbers");
            if (firstError == null) firstError = tilPassword;
        }
        if (confirmPassword.isEmpty()) {
            tilConfirmPassword.setError("Please confirm your password");
            if (firstError == null) firstError = tilConfirmPassword;
        } else if (!confirmPassword.equals(password)) {
            tilConfirmPassword.setError("Passwords do not match");
            if (firstError == null) firstError = tilConfirmPassword;
        }

        if (firstError != null) {
            firstError.requestFocus();
            return;
        }
        checkAvailabilityThenContinue(new RegisterRequest(fullName, mobile, nic, email, password, "FARMER"));
    }

    /** Asks the server whether email / mobile / NIC are already registered before moving to the map. */
    private void checkAvailabilityThenContinue(RegisterRequest request) {
        setBusy(true);
        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("email", request.getEmail());
        body.put("mobileNumber", request.getMobileNumber());
        body.put("nic", request.getNic());

        com.harvesthub.mobile.network.ApiClient.getClient()
                .create(com.harvesthub.mobile.network.ApiService.class)
                .checkAvailability(body)
                .enqueue(new retrofit2.Callback<java.util.Map<String, Boolean>>() {
                    @Override
                    public void onResponse(@NonNull retrofit2.Call<java.util.Map<String, Boolean>> call,
                                           @NonNull retrofit2.Response<java.util.Map<String, Boolean>> response) {
                        setBusy(false);
                        java.util.Map<String, Boolean> r = response.body();
                        if (!response.isSuccessful() || r == null) {
                            android.widget.Toast.makeText(RegisterActivity.this,
                                    "Could not verify your details (" + response.code() + "). Please try again.",
                                    android.widget.Toast.LENGTH_LONG).show();
                            return;
                        }
                        boolean ok = true;
                        if (Boolean.FALSE.equals(r.get("mobileNumber"))) {
                            tilMobileNumber.setError("This mobile number is already registered");
                            ok = false;
                        }
                        if (Boolean.FALSE.equals(r.get("email"))) {
                            tilEmail.setError("This email is already registered");
                            ok = false;
                        }
                        if (Boolean.FALSE.equals(r.get("nic"))) {
                            tilNic.setError("This NIC is already registered");
                            ok = false;
                        }
                        if (ok) {
                            Intent intent = new Intent(RegisterActivity.this, LocationSetupActivity.class);
                            intent.putExtra("registerRequest", request);
                            startActivity(intent);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull retrofit2.Call<java.util.Map<String, Boolean>> call, @NonNull Throwable t) {
                        setBusy(false);
                        android.widget.Toast.makeText(RegisterActivity.this,
                                "No connection to the server. Check your internet and try again.",
                                android.widget.Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setBusy(boolean busy) {
        btnContinue.setEnabled(!busy);
        btnContinue.setText(busy ? "Checking details..." : "Continue to Location Setup");
    }
}
