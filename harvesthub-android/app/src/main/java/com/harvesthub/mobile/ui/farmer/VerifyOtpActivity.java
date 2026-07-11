package com.harvesthub.mobile.ui.farmer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.UnderlineSpan;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.harvesthub.mobile.R;

public class VerifyOtpActivity extends AppCompatActivity {

    private EditText etOtp1, etOtp2, etOtp3, etOtp4;
    private TextView tvSubtitle, tvResend;
    private LinearLayout llErrorContainer;
    private Button btnVerify;
    private CountDownTimer countDownTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        initViews();
        setupOtpInputs();
        startResendTimer();

        btnVerify.setOnClickListener(v -> {
            Intent intent = new Intent(this, AccountVerifiedActivity.class);
            intent.putExtra("resetToken", "mock_token");
            startActivity(intent);
            finish();
        });
    }

    private void initViews() {
        etOtp1 = findViewById(R.id.etOtp1);
        etOtp2 = findViewById(R.id.etOtp2);
        etOtp3 = findViewById(R.id.etOtp3);
        etOtp4 = findViewById(R.id.etOtp4);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvResend = findViewById(R.id.tvResend);
        llErrorContainer = findViewById(R.id.llErrorContainer);
        btnVerify = findViewById(R.id.btnVerify);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupOtpInputs() {
        etOtp1.addTextChangedListener(new OtpTextWatcher(etOtp1, etOtp2));
        etOtp2.addTextChangedListener(new OtpTextWatcher(etOtp2, etOtp3));
        etOtp3.addTextChangedListener(new OtpTextWatcher(etOtp3, etOtp4));
        etOtp4.addTextChangedListener(new OtpTextWatcher(etOtp4, null));

        View.OnKeyListener backspaceListener = (v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN) {
                EditText current = (EditText) v;
                if (current.getText().length() == 0) {
                    if (current == etOtp2) etOtp1.requestFocus();
                    else if (current == etOtp3) etOtp2.requestFocus();
                    else if (current == etOtp4) etOtp3.requestFocus();
                }
            }
            return false;
        };
        etOtp2.setOnKeyListener(backspaceListener);
        etOtp3.setOnKeyListener(backspaceListener);
        etOtp4.setOnKeyListener(backspaceListener);
    }

    private void startResendTimer() {
        countDownTimer = new CountDownTimer(60000, 1000) {
            @SuppressLint("DefaultLocale")
            @Override
            public void onTick(long millisUntilFinished) {
                String fullText = String.format("Need a new code? Resend in 00:%02d", millisUntilFinished / 1000);
                SpannableString ss = new SpannableString(fullText);
                ss.setSpan(new ForegroundColorSpan(Color.parseColor("#9E9E9E")), 0, 18, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ss.setSpan(new ForegroundColorSpan(Color.parseColor("#2E7D32")), 18, fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ss.setSpan(new UnderlineSpan(), 18, fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                tvResend.setText(ss);
            }
            @Override
            public void onFinish() { setResendClickable(); }
        }.start();
    }

    private void setResendClickable() {
        String text = "Need a new code? Resend";
        SpannableString ss = new SpannableString(text);
        ss.setSpan(new ForegroundColorSpan(Color.parseColor("#9E9E9E")), 0, 18, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) { Toast.makeText(VerifyOtpActivity.this, "Resending...", Toast.LENGTH_SHORT).show(); startResendTimer(); }
            @Override
            public void updateDrawState(@NonNull TextPaint ds) { super.updateDrawState(ds); ds.setUnderlineText(true); ds.setColor(Color.parseColor("#2E7D32")); }
        };
        ss.setSpan(clickableSpan, 18, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvResend.setText(ss);
        tvResend.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private class OtpTextWatcher implements TextWatcher {
        private final View current, next;
        public OtpTextWatcher(View current, View next) { this.current = current; this.next = next; }
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override
        public void afterTextChanged(Editable s) { if (s.length() == 1 && next != null) next.requestFocus(); }
    }
}
