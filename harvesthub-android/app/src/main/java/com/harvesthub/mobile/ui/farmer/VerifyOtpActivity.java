package com.harvesthub.mobile.ui.farmer;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.ForgotPasswordRequest;
import com.harvesthub.mobile.model.VerifyResetCodeRequest;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.utils.AuthInputUtils;

import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Forgot password, step 2: enter the 6-digit code (Figma frames 4, 5 = success, 6 = error).
 * The code is checked with POST api/auth/verify-reset-code; on success it is passed on to
 * {@link AccountVerifiedActivity} and used once more by the final reset call.
 */
public class VerifyOtpActivity extends AppCompatActivity {

    public static final String EXTRA_CODE = "code";
    private static final long RESEND_WAIT_MS = 60_000;

    private final EditText[] boxes = new EditText[6];
    private TextView tvResend;
    private TextView tvError;
    private View llErrorContainer;
    private MaterialButton btnVerify;
    private CountDownTimer resendTimer;
    private String identifier;
    private boolean busy = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_otp);

        identifier = getIntent().getStringExtra(ForgotPasswordActivity.EXTRA_IDENTIFIER);
        if (identifier == null) { // opened without going through step 1
            finish();
            return;
        }

        int[] ids = {R.id.etOtp1, R.id.etOtp2, R.id.etOtp3, R.id.etOtp4, R.id.etOtp5, R.id.etOtp6};
        for (int i = 0; i < ids.length; i++) boxes[i] = findViewById(ids[i]);
        tvResend = findViewById(R.id.tvResend);
        tvError = findViewById(R.id.tvError);
        llErrorContainer = findViewById(R.id.llErrorContainer);
        btnVerify = findViewById(R.id.btnVerify);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        btnVerify.setOnClickListener(v -> verify());

        showSubtitle();
        setupBoxes();
        startResendTimer();
        boxes[0].requestFocus();
    }

    private void showSubtitle() {
        TextView tvSubtitle = findViewById(R.id.tvSubtitle);
        String target = AuthInputUtils.isEmail(identifier) ? AuthInputUtils.maskEmail(identifier) : identifier;
        String full = getString(AuthInputUtils.isEmail(identifier)
                ? R.string.fp_code_sent_to : R.string.fp_code_sent_generic, target);
        SpannableString s = new SpannableString(full);
        int start = full.indexOf(target);
        if (start >= 0) {
            s.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.fp_body_strong)),
                    start, start + target.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            s.setSpan(new StyleSpan(Typeface.BOLD), start, start + target.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        tvSubtitle.setText(s);
    }

    private void setupBoxes() {
        for (int i = 0; i < boxes.length; i++) {
            final int index = i;
            boxes[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
                @Override
                public void afterTextChanged(Editable s) {
                    clearState();
                    if (s.length() == 1 && index < boxes.length - 1) boxes[index + 1].requestFocus();
                    if (index == boxes.length - 1 && s.length() == 1 && codeIsComplete()) verify();
                }
            });
            boxes[i].setOnKeyListener((v, keyCode, event) -> {
                if (keyCode == KeyEvent.KEYCODE_DEL && event.getAction() == KeyEvent.ACTION_DOWN
                        && boxes[index].getText().length() == 0 && index > 0) {
                    boxes[index - 1].setText("");
                    boxes[index - 1].requestFocus();
                    return true;
                }
                return false;
            });
        }
        // Pasting the whole code into the first box: maxLength is 1, so catch it before the filter.
        boxes[0].setFilters(new android.text.InputFilter[]{(source, start, end, dest, dStart, dEnd) -> {
            String digits = source.subSequence(start, end).toString().replaceAll("\\D", "");
            if (digits.length() > 1) {
                fillCode(digits);
                return "";
            }
            return digits.length() == 1 ? digits : (end - start == 0 ? null : "");
        }, new android.text.InputFilter.LengthFilter(1)});
    }

    private void fillCode(String digits) {
        new Handler(Looper.getMainLooper()).post(() -> {
            for (int i = 0; i < boxes.length; i++) {
                boxes[i].setText(i < digits.length() ? String.valueOf(digits.charAt(i)) : "");
            }
            boxes[Math.min(digits.length(), boxes.length) - 1].requestFocus();
        });
    }

    private boolean codeIsComplete() {
        return getCode().length() == boxes.length;
    }

    private String getCode() {
        StringBuilder sb = new StringBuilder();
        for (EditText box : boxes) sb.append(box.getText().toString().trim());
        return sb.toString();
    }

    private void verify() {
        if (busy) return;
        if (!codeIsComplete()) {
            showError(getString(R.string.fp_err_code_incomplete), false);
            return;
        }
        String code = getCode();
        setBusy(true);
        ApiClient.getApiService().verifyResetCode(new VerifyResetCodeRequest(identifier, code))
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        setBusy(false);
                        if (response.isSuccessful()) {
                            showSuccessThenContinue(code);
                        } else {
                            showError(AuthInputUtils.errorMessage(response,
                                    getString(R.string.fp_err_code_invalid)), true);
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                        setBusy(false);
                        Toast.makeText(VerifyOtpActivity.this, R.string.fp_network_error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    /** Frame 5: green boxes, then move on. */
    private void showSuccessThenContinue(String code) {
        llErrorContainer.setVisibility(View.GONE);
        for (EditText box : boxes) {
            box.setBackgroundResource(R.drawable.bg_fp_otp_success);
            box.setEnabled(false);
        }
        btnVerify.setEnabled(false);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (isFinishing()) return;
            Intent intent = new Intent(this, AccountVerifiedActivity.class);
            intent.putExtra(ForgotPasswordActivity.EXTRA_IDENTIFIER, identifier);
            intent.putExtra(EXTRA_CODE, code);
            startActivity(intent);
            finish();
        }, 600);
    }

    /** Frame 6: red boxes (when the server rejected the code) and the error banner. */
    private void showError(String message, boolean markBoxes) {
        tvError.setText(message);
        llErrorContainer.setVisibility(View.VISIBLE);
        if (markBoxes) {
            for (EditText box : boxes) box.setBackgroundResource(R.drawable.bg_fp_otp_error);
        }
    }

    private void clearState() {
        if (llErrorContainer.getVisibility() == View.VISIBLE) {
            llErrorContainer.setVisibility(View.GONE);
            for (EditText box : boxes) box.setBackgroundResource(R.drawable.bg_fp_otp_box);
        }
    }

    private void setBusy(boolean b) {
        busy = b;
        btnVerify.setEnabled(!b);
        btnVerify.setText(b ? R.string.fp_verifying : R.string.fp_verify_code);
    }

    // ----- Resend -----

    private void startResendTimer() {
        if (resendTimer != null) resendTimer.cancel();
        tvResend.setMovementMethod(null);
        resendTimer = new CountDownTimer(RESEND_WAIT_MS, 1000) {
            @Override
            public void onTick(long msLeft) {
                long s = (msLeft + 999) / 1000;
                String countdown = getString(R.string.fp_resend_in,
                        String.format(Locale.US, "%02d:%02d", s / 60, s % 60));
                SpannableStringBuilder sb = new SpannableStringBuilder(getString(R.string.fp_resend_prefix));
                int start = sb.length();
                sb.append(countdown);
                sb.setSpan(new ForegroundColorSpan(ContextCompat.getColor(VerifyOtpActivity.this, R.color.fp_link)),
                        start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                tvResend.setText(sb);
            }

            @Override
            public void onFinish() {
                showResendLink();
            }
        }.start();
    }

    private void showResendLink() {
        SpannableStringBuilder sb = new SpannableStringBuilder(getString(R.string.fp_resend_prefix));
        int start = sb.length();
        sb.append(getString(R.string.fp_resend));
        sb.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                resendCode();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                ds.setColor(ContextCompat.getColor(VerifyOtpActivity.this, R.color.fp_link));
                ds.setUnderlineText(true);
            }
        }, start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        tvResend.setText(sb);
        tvResend.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void resendCode() {
        startResendTimer();
        ApiClient.getApiService().forgotPassword(new ForgotPasswordRequest(identifier))
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        for (EditText box : boxes) box.setText("");
                        clearState();
                        boxes[0].requestFocus();
                        Toast.makeText(VerifyOtpActivity.this, R.string.fp_resent, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                        Toast.makeText(VerifyOtpActivity.this, R.string.fp_network_error, Toast.LENGTH_LONG).show();
                        showResendLink();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        if (resendTimer != null) resendTimer.cancel();
        super.onDestroy();
    }
}
