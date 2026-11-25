package com.harvesthub.mobile.ui.buyer;

import android.content.Intent;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.button.MaterialButton;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.model.WalletSummaryResponse;
import com.harvesthub.mobile.utils.AuthInputUtils;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * "Simulated Payment Gateway" screen. Pays the advance (AWAITING_PAYMENT) or, with
 * EXTRA_STAGE = BALANCE, the remaining 80% when accepting the delivery (ARRIVED_AT_DESTINATION).
 * The buyer chooses a simulated card (no real money) or the mock wallet balance.
 */
public class SecureCheckoutActivity extends BaseBuyerOrderActivity {

    public static final String EXTRA_STAGE = "PAY_STAGE";
    public static final String STAGE_BALANCE = "BALANCE";

    private boolean balanceStage;
    private double amount;
    private double walletBalance = -1;
    private boolean busy;

    @Override
    protected int layout() {
        return R.layout.activity_secure_checkout;
    }

    @Override
    protected boolean followStatus() {
        return false; // we decide below; paying moves the order on
    }

    @Override
    protected void onResume() {
        balanceStage = STAGE_BALANCE.equals(getIntent().getStringExtra(EXTRA_STAGE));
        super.onResume();
        loadWallet();
        RadioGroup group = findViewById(R.id.rgPaymentMethod);
        group.setOnCheckedChangeListener((g, id) -> updateMethodUi());
        findViewById(R.id.btnTopUpWallet).setOnClickListener(v -> BuyerWalletActivity.showTopUp(this, this::loadWallet));
        View help = findViewById(R.id.btnCvvHelp);
        if (help != null) help.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("CVV")
                .setMessage("The 3-digit code on the back of the card. This is a simulated checkout: no real card is charged.")
                .setPositiveButton("OK", null).show());
    }

    @Override
    protected void bind(@NonNull Order o) {
        if (busy) return;
        String expected = balanceStage ? "ARRIVED_AT_DESTINATION" : "AWAITING_PAYMENT";
        if (!expected.equals(o.getStatus())) { // already paid / moved on elsewhere
            handOver(o);
            return;
        }
        Order.Payment p = o.getPayment();
        if (p == null) {
            Toast.makeText(this, "This order has no payment quote yet.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        amount = balanceStage ? p.balanceDue : p.advanceDue;
        setText(R.id.tvHeaderTitle, balanceStage ? "Pay Balance" : "Secure Checkout");
        setText(R.id.tvContractPill, "Order " + ref(o));
        setText(R.id.tvAdvanceAmount, rs(amount));
        if (balanceStage) {
            setText(R.id.pillCropCost, "Crop balance (80%): " + rs(p.balanceDue));
            show(R.id.pillLogisticsCost, false);
            show(R.id.pillEscrowFee, false);
        } else {
            setText(R.id.pillCropCost, "Crop (20%): " + rs(p.advanceCropPortion));
            setText(R.id.pillLogisticsCost, "Logistics: " + rs(p.transportFee));
            setText(R.id.pillEscrowFee, "Fee (3%): " + rs(p.buyerFee));
        }
        setText(R.id.tvBatchDescription, String.format(Locale.US, "%s %s • %s • Order %s",
                kg(o.getQuantity()), o.getCropName(), o.getFarmerName(), ref(o)));
        updateMethodUi();
    }

    private boolean walletSelected() {
        return ((RadioButton) findViewById(R.id.rbMethodWallet)).isChecked();
    }

    private void loadWallet() {
        String me = session.getUserId();
        if (me == null) return;
        api.getWalletSummary(me).enqueue(new Callback<WalletSummaryResponse>() {
            @Override
            public void onResponse(@NonNull Call<WalletSummaryResponse> call, @NonNull Response<WalletSummaryResponse> r) {
                if (r.isSuccessful() && r.body() != null) {
                    walletBalance = r.body().getBalance();
                    ((RadioButton) findViewById(R.id.rbMethodWallet)).setText("HarvestHub Wallet (" + rs(walletBalance) + " available)");
                    updateMethodUi();
                }
            }

            @Override
            public void onFailure(@NonNull Call<WalletSummaryResponse> call, @NonNull Throwable t) { }
        });
    }

    private void updateMethodUi() {
        boolean wallet = walletSelected();
        show(R.id.cardPaymentForm, !wallet);
        TextView hint = findViewById(R.id.tvWalletHint);
        boolean shortOfFunds = wallet && walletBalance >= 0 && walletBalance + 0.005 < amount;
        hint.setVisibility(wallet ? View.VISIBLE : View.GONE);
        if (wallet) {
            hint.setText(walletBalance < 0 ? "Checking your balance…"
                    : shortOfFunds ? "Not enough balance. You need " + rs(amount - walletBalance) + " more."
                    : "The amount is moved from your wallet into escrow.");
            hint.setTextColor(shortOfFunds ? 0xFFB91C1C : 0xFF64748B);
        }
        show(R.id.btnTopUpWallet, wallet);
        MaterialButton pay = findViewById(R.id.btnPayNow);
        pay.setText(amount > 0 ? "Pay " + rs(amount) : "Pay");
        pay.setEnabled(!busy && amount > 0 && !shortOfFunds);
        pay.setOnClickListener(v -> submit());
    }

    private void submit() {
        String method = walletSelected() ? "WALLET" : "CARD";
        if ("CARD".equals(method) && !cardValid()) return;
        busy = true;
        MaterialButton pay = findViewById(R.id.btnPayNow);
        pay.setEnabled(false);
        pay.setText("Processing…");
        Callback<Order> done = new Callback<Order>() {
            @Override
            public void onResponse(@NonNull Call<Order> call, @NonNull Response<Order> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    busy = false;
                    Toast.makeText(SecureCheckoutActivity.this,
                            AuthInputUtils.errorMessage(response, "Payment could not be recorded."), Toast.LENGTH_LONG).show();
                    loadWallet();
                    load();
                    return;
                }
                Order updated = response.body(); // busy stays true: the refresh must not open a screen behind the dialog
                if (isFinishing() || isDestroyed()) return;
                new AlertDialog.Builder(SecureCheckoutActivity.this)
                        .setTitle("Payment successful")
                        .setMessage(rs(amount) + (balanceStage
                                ? " balance paid into escrow. Confirm \"Goods Received\" to release the funds."
                                : " is locked in HarvestHub escrow. A vehicle is being assigned now."))
                        .setCancelable(false)
                        .setPositiveButton("Continue", (d, w) -> {
                            startActivity(BuyerOrderRouter.intentFor(SecureCheckoutActivity.this, updated, session.getUserId()));
                            finish();
                        })
                        .show();
            }

            @Override
            public void onFailure(@NonNull Call<Order> call, @NonNull Throwable t) {
                busy = false;
                updateMethodUi();
                Toast.makeText(SecureCheckoutActivity.this, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
            }
        };
        if (balanceStage) {
            Map<String, String> body = new HashMap<>();
            body.put("event", "ACCEPT_DELIVERY");
            body.put("method", method);
            api.sendOrderEvent(orderId, body).enqueue(done);
        } else {
            Map<String, Object> body = new HashMap<>();
            body.put("amount", amount);
            body.put("method", method);
            api.recordAdvancePayment(orderId, body).enqueue(done);
        }
    }

    /** Format checks only — the gateway is simulated. */
    private boolean cardValid() {
        EditText number = findViewById(R.id.etCardNumber);
        EditText expiry = findViewById(R.id.etExpiry);
        EditText cvv = findViewById(R.id.etCvv);
        EditText name = findViewById(R.id.etCardholderName);
        String digits = number.getText().toString().replaceAll("\\s", "");
        if (!digits.matches("\\d{13,19}") || !luhn(digits)) {
            number.setError("Enter a valid card number");
            number.requestFocus();
            return false;
        }
        String exp = expiry.getText().toString().trim();
        if (!exp.matches("(0[1-9]|1[0-2])/\\d{2}") || expired(exp)) {
            expiry.setError("MM/YY, not expired");
            expiry.requestFocus();
            return false;
        }
        if (!cvv.getText().toString().trim().matches("\\d{3,4}")) {
            cvv.setError("3 or 4 digits");
            cvv.requestFocus();
            return false;
        }
        if (name.getText().toString().trim().length() < 2) {
            name.setError("Enter the name on the card");
            name.requestFocus();
            return false;
        }
        return true;
    }

    static boolean luhn(String digits) {
        int sum = 0;
        boolean dbl = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (dbl) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
            dbl = !dbl;
        }
        return sum % 10 == 0;
    }

    static boolean expired(String mmYy) {
        int month = Integer.parseInt(mmYy.substring(0, 2));
        int year = 2000 + Integer.parseInt(mmYy.substring(3));
        Calendar now = Calendar.getInstance();
        int cy = now.get(Calendar.YEAR), cm = now.get(Calendar.MONTH) + 1;
        return year < cy || (year == cy && month < cm);
    }

    /** Opens the balance checkout from 6.1. */
    static Intent balanceIntent(android.content.Context ctx, String orderId) {
        Intent i = new Intent(ctx, SecureCheckoutActivity.class);
        i.putExtra(BuyerOrderRouter.EXTRA_ORDER_ID, orderId);
        i.putExtra(EXTRA_STAGE, STAGE_BALANCE);
        return i;
    }
}
