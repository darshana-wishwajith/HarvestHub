package com.harvesthub.mobile.ui.buyer;

import com.harvesthub.mobile.R;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class TransactionLedgerActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvSecuredAmount;
    private TextView tvTxnRef;
    private TextView tvSystemStatus;
    private ImageView iconStepSpinner;
    private TextView tvStep3Title;
    private TextView tvStep3Subtitle;
    private Button btnReturnToOrders;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_ledger);

        initViews();
        loadIntentData();
        setupListeners();
        startSpinnerAnimation();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvSecuredAmount = findViewById(R.id.tvSecuredAmount);
        tvTxnRef = findViewById(R.id.tvTxnRef);
        tvSystemStatus = findViewById(R.id.tvSystemStatus);
        iconStepSpinner = findViewById(R.id.iconStepSpinner);
        tvStep3Title = findViewById(R.id.tvStep3Title);
        tvStep3Subtitle = findViewById(R.id.tvStep3Subtitle);
        btnReturnToOrders = findViewById(R.id.btnReturnToOrders);
    }

    private void loadIntentData() {
        String amount = getIntent().getStringExtra("secured_amount");
        String txnRef = getIntent().getStringExtra("txn_ref");
        String status = getIntent().getStringExtra("system_status");

        if (amount != null && !amount.isEmpty()) {
            tvSecuredAmount.setText(amount);
        }
        if (txnRef != null && !txnRef.isEmpty()) {
            tvTxnRef.setText(txnRef.startsWith("Txn") ? txnRef : "Txn #" + txnRef);
        }
        if (status != null && !status.isEmpty()) {
            tvSystemStatus.setText(status);
        }
    }

    private void setupListeners() {
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        btnReturnToOrders.setOnClickListener(v -> {
            Intent intent = new Intent(this, TransitTrackingActivity.class);
            intent.putExtra("ORDER_ID", getIntent().getStringExtra("order_id"));
            startActivity(intent);
            finish();
        });
    }

    private void startSpinnerAnimation() {
        if (iconStepSpinner != null) {
            RotateAnimation rotate = new RotateAnimation(
                    0, 360,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF, 0.5f
            );
            rotate.setDuration(1200);
            rotate.setRepeatCount(Animation.INFINITE);
            rotate.setInterpolator(new LinearInterpolator());
            iconStepSpinner.startAnimation(rotate);
        }
    }
}

