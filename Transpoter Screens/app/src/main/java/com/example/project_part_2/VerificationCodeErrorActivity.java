package com.example.project_part_2;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

/**
 * Page 1.3.5: Enter Verification Code - Invalid State
 * Displays error alert banner when incorrect or incomplete OTP is entered.
 */
public class VerificationCodeErrorActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvSubtitle;
    private EditText etOtp1, etOtp2, etOtp3, etOtp4;
    private LinearLayout layoutErrorBanner;
    private TextView tvResendTimer;
    private LinearLayout btnVerifyCode;

    private CountDownTimer countDownTimer;
    private boolean canResend = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification_code_error);

        // White status bar with dark icons
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            getWindow().setStatusBarColor(Color.WHITE);
        }

        initViews();
        setupFormattedSubtitle();
        setupOtpInputForwarding();
        startResendCountdown(58000); // 58 seconds
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        etOtp1 = findViewById(R.id.etOtp1);
        etOtp2 = findViewById(R.id.etOtp2);
        etOtp3 = findViewById(R.id.etOtp3);
        etOtp4 = findViewById(R.id.etOtp4);
        layoutErrorBanner = findViewById(R.id.layoutErrorBanner);
        tvResendTimer = findViewById(R.id.tvResendTimer);
        btnVerifyCode = findViewById(R.id.btnVerifyCode);
    }

    private void setupFormattedSubtitle() {
        String part1 = "We have sent a 4–digit verification code to\n";
        String email = "kama***@gmail.com";
        String part2 = ". Please enter it\nbelow.";
        String fullText = part1 + email + part2;

        SpannableString spannable = new SpannableString(fullText);
        int start = part1.length();
        int end = start + email.length();

        // Make email bold and darker
        spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new ForegroundColorSpan(Color.parseColor("#111827")), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvSubtitle.setText(spannable);
    }

    private void setupOtpInputForwarding() {
        etOtp1.addTextChangedListener(new GenericTextWatcher(etOtp1, etOtp2));
        etOtp2.addTextChangedListener(new GenericTextWatcher(etOtp2, etOtp3));
        etOtp3.addTextChangedListener(new GenericTextWatcher(etOtp3, etOtp4));
        etOtp4.addTextChangedListener(new GenericTextWatcher(etOtp4, null));

        etOtp2.setOnKeyListener(new GenericKeyEvent(etOtp2, etOtp1));
        etOtp3.setOnKeyListener(new GenericKeyEvent(etOtp3, etOtp2));
        etOtp4.setOnKeyListener(new GenericKeyEvent(etOtp4, etOtp3));
    }

    private void startResendCountdown(long millisInFuture) {
        canResend = false;
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        countDownTimer = new CountDownTimer(millisInFuture, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000);
                tvResendTimer.setText(String.format(Locale.getDefault(), "Resend in 00:%02d", seconds));
                tvResendTimer.setTextColor(Color.parseColor("#16A34A"));
            }

            @Override
            public void onFinish() {
                canResend = true;
                tvResendTimer.setText("Resend Code");
                tvResendTimer.setTextColor(Color.parseColor("#16A34A"));
            }
        }.start();
    }

    private void setupListeners() {
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        tvResendTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (canResend) {
                    Toast.makeText(VerificationCodeErrorActivity.this, "New code sent to kama***@gmail.com", Toast.LENGTH_SHORT).show();
                    layoutErrorBanner.setVisibility(View.GONE);
                    startResendCountdown(60000);
                } else {
                    Toast.makeText(VerificationCodeErrorActivity.this, "Please wait for timer to expire", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnVerifyCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleVerify();
            }
        });
    }

    private void handleVerify() {
        String code = etOtp1.getText().toString().trim() +
                etOtp2.getText().toString().trim() +
                etOtp3.getText().toString().trim() +
                etOtp4.getText().toString().trim();

        if (code.equals("8241")) {
            // Correct code entered! Navigate to valid state or next page
            Toast.makeText(this, "✓ Code 8241 Verified Successfully!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(VerificationCodeErrorActivity.this, VerificationCodeActivity.class);
            startActivity(intent);
            finish();
        } else {
            // Keep error visible
            layoutErrorBanner.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Invalid verification code. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private static class GenericTextWatcher implements TextWatcher {
        private final View currentView;
        private final View nextView;

        public GenericTextWatcher(View currentView, View nextView) {
            this.currentView = currentView;
            this.nextView = nextView;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void afterTextChanged(Editable s) {
            String text = s.toString();
            if (text.length() == 1 && nextView != null) {
                nextView.requestFocus();
            }
        }
    }

    private static class GenericKeyEvent implements View.OnKeyListener {
        private final EditText currentView;
        private final EditText previousView;

        public GenericKeyEvent(EditText currentView, EditText previousView) {
            this.currentView = currentView;
            this.previousView = previousView;
        }

        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                if (currentView.getText().toString().isEmpty() && previousView != null) {
                    previousView.requestFocus();
                    previousView.setSelection(previousView.getText().length());
                    return true;
                }
            }
            return false;
        }
    }
}
