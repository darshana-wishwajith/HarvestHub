package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.R;

public class FarmerLoginActivity extends AppCompatActivity {

    private EditText etOtp1, etOtp2, etOtp3, etOtp4;
    private LinearLayout llErrorBanner;
    private TextView tvTimer;
    private Button btnVerify;
    private ImageView ivBackArrow;

    private CountDownTimer countDownTimer;
    private final long TIMER_DURATION = 60000; // 60 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        initViews();
        setupOtpInputs();
        startTimer();

        ivBackArrow.setOnClickListener(v -> finish());

        btnVerify.setOnClickListener(v -> {
            String otp = etOtp1.getText().toString() + etOtp2.getText().toString() +
                    etOtp3.getText().toString() + etOtp4.getText().toString();

            if (otp.length() == 4) {
                verifyOtpCode(otp);
            } else {
                showError();
            }
        });
    }

    private void initViews() {
        etOtp1 = findViewById(R.id.etOtp1);
        etOtp2 = findViewById(R.id.etOtp2);
        etOtp3 = findViewById(R.id.etOtp3);
        etOtp4 = findViewById(R.id.etOtp4);
        llErrorBanner = findViewById(R.id.llErrorBanner);
        tvTimer = findViewById(R.id.tvTimer);
        btnVerify = findViewById(R.id.btnVerify);
        ivBackArrow = findViewById(R.id.ivBackArrow);
    }

    private void setupOtpInputs() {
        EditText[] editTexts = { etOtp1, etOtp2, etOtp3, etOtp4 };

        for (int i = 0; i < editTexts.length; i++) {
            final int currentIndex = i;
            EditText currentEditText = editTexts[currentIndex];

            currentEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    hideError();
                    if (s.length() == 1 && currentIndex < editTexts.length - 1) {
                        editTexts[currentIndex + 1].requestFocus();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });

            currentEditText.setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (currentEditText.getText().toString().isEmpty() && currentIndex > 0) {
                        editTexts[currentIndex - 1].requestFocus();
                        editTexts[currentIndex - 1].setText("");
                        return true;
                    }
                }
                return false;
            });
        }
    }

    private void startTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(TIMER_DURATION, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                String timeString = String.format("%02d:%02d", seconds / 60, seconds % 60);
                String text = "Need a new code? Resend in " + timeString;

                SpannableString spannableString = new SpannableString(text);
                int startIndex = text.indexOf("Resend in");
                spannableString.setSpan(new ForegroundColorSpan(Color.parseColor("#4CAF50")),
                        startIndex, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                tvTimer.setText(spannableString);
                tvTimer.setMovementMethod(null);
            }

            @Override
            public void onFinish() {
                String text = "Need a new code? Resend Code";
                SpannableString spannableString = new SpannableString(text);

                ClickableSpan clickableSpan = new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        startTimer();
                        // TODO: Trigger API call to resend OTP
                        Toast.makeText(FarmerLoginActivity.this, "OTP Resent", Toast.LENGTH_SHORT).show();
                    }
                };

                int startIndex = text.indexOf("Resend Code");
                spannableString.setSpan(clickableSpan, startIndex, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannableString.setSpan(new ForegroundColorSpan(Color.parseColor("#1B4332")),
                        startIndex, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                tvTimer.setText(spannableString);
                tvTimer.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }.start();
    }

    private void verifyOtpCode(String otp) {
        // Placeholder for Retrofit API call
        // ApiClient.getClient().create(ApiService.class).verifyOtp(otp).enqueue(new
        // Callback<Response>() { ... });

        // Simulate behavior
        if ("1234".equals(otp)) {
            // Success
            hideError();
            // Intent intent = new Intent(FarmerLoginActivity.this,
            // CreateNewPasswordActivity.class);
            // startActivity(intent);
            // finish();
            Toast.makeText(this, "Success! Navigating to CreateNewPasswordActivity...", Toast.LENGTH_SHORT).show();
        } else {
            // Failure
            showError();
        }
    }

    private void showError() {
        llErrorBanner.setVisibility(View.VISIBLE);
        etOtp1.setBackgroundResource(R.drawable.bg_otp_input); // Optionally change to a red border drawable here
        etOtp2.setBackgroundResource(R.drawable.bg_otp_input);
        etOtp3.setBackgroundResource(R.drawable.bg_otp_input);
        etOtp4.setBackgroundResource(R.drawable.bg_otp_input);
    }

    private void hideError() {
        if (llErrorBanner.getVisibility() == View.VISIBLE) {
            llErrorBanner.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
