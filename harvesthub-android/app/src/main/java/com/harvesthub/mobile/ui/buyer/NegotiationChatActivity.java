package com.harvesthub.mobile.ui.buyer;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.button.MaterialButton;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.ChatMessage;
import com.harvesthub.mobile.model.ConfirmDealRequest;
import com.harvesthub.mobile.model.GenericResponse;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.model.SendMessageRequest;
import com.harvesthub.mobile.utils.AuthInputUtils;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Figma buyer 3.2 "In-App Chat & Negotiation". Real chat for the order plus the binding contract card.
 * Changing quantity, price or pickup date is a counter-offer (the farmer must confirm again); when both sides
 * confirmed the same terms the order is accepted and the buyer goes to 4.1 (workflow 2.2 / 3.1).
 */
public class NegotiationChatActivity extends BaseBuyerOrderActivity {

    private double qty, price;
    private String pickup;
    private boolean termsEdited;
    private int shownMessages = -1;

    @Override
    protected int layout() {
        return R.layout.activity_negotiation_chat;
    }

    @Override
    protected boolean followStatus() {
        return false; // the chat stays usable during the whole order
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSamples();
        findViewById(R.id.btnSendMessage).setOnClickListener(v -> {
            EditText input = findViewById(R.id.etMessageInput);
            String text = input.getText().toString().trim();
            if (!text.isEmpty()) send(text, input);
        });
        findViewById(R.id.btnEditPrice).setOnClickListener(v -> editPrice());
        findViewById(R.id.btnEditPickupDate).setOnClickListener(v -> editDate());
        View qtyView = findViewById(R.id.tvCardQuantity);
        qtyView.setOnClickListener(v -> editQuantity());
    }

    /** The layout contains Figma sample bubbles; only real messages are shown. */
    private void hideSamples() {
        hideRow(findViewById(R.id.tvChatTimestamp));
        hideRow(findViewById(R.id.tvBuyerMessage));
        hideRow(findViewById(R.id.tvFarmerMessage));
    }

    private void hideRow(View v) {
        View column = findViewById(R.id.layoutDynamicMessages).getParent() instanceof View
                ? (View) findViewById(R.id.layoutDynamicMessages).getParent() : null;
        View cur = v;
        while (cur != null && cur.getParent() instanceof View && cur.getParent() != column) cur = (View) cur.getParent();
        if (cur != null && cur != column) cur.setVisibility(View.GONE);
    }

    @Override
    protected void bind(@NonNull Order o) {
        if (!termsEdited) {
            qty = o.getQuantity();
            price = o.getPricePerKg();
            pickup = o.getPickupDate();
        }
        setText(R.id.tvNegotiatingCrop, "Negotiating: " + kg(qty) + "\n" + o.getCropName());
        setText(R.id.tvTargetPickup, "Target Pickup: " + (pickup == null ? "not set" : pickup));
        setText(R.id.tvOrderRef, "Order Ref " + ref(o));
        setText(R.id.tvCardFarmerName, o.getFarmerName());
        setText(R.id.tvCardQuantity, kg(qty) + " ✎");
        setText(R.id.tvCardPrice, rs(price) + " / kg");
        setText(R.id.tvCardPickupDate, pickup == null ? "Choose date" : pickup);

        boolean negotiating = "NEGOTIATING".equals(o.getStatus());
        TextView badge = findViewById(R.id.tvStatusBadge);
        MaterialButton accept = findViewById(R.id.btnAcceptDeal);
        findViewById(R.id.btnEditPrice).setVisibility(negotiating ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnEditPickupDate).setVisibility(negotiating ? View.VISIBLE : View.GONE);
        findViewById(R.id.tvCardQuantity).setEnabled(negotiating);
        if (negotiating) {
            boolean waitingForFarmer = o.isBuyerConfirmed() && !o.isFarmerConfirmed() && !termsEdited;
            badge.setText(termsEdited ? "Status: Counter-offer (not sent)"
                    : waitingForFarmer ? "Status: Waiting for the farmer"
                    : o.isFarmerConfirmed() ? "Status: Farmer confirmed" : "Status: Negotiating");
            accept.setEnabled(!waitingForFarmer);
            accept.setText(termsEdited ? "Send Counter-Offer" : waitingForFarmer ? "Waiting for the farmer…" : "Accept Deal ✓");
            accept.setOnClickListener(v -> confirm(accept));
        } else {
            badge.setText("Status: " + statusLabel(o.getStatus()));
            badge.setBackgroundResource(R.drawable.bg_status_accepted);
            accept.setEnabled(true);
            accept.setText("CANCELLED".equals(o.getStatus()) ? "Order cancelled" : "Open Order");
            accept.setOnClickListener(v -> {
                if (!"CANCELLED".equals(o.getStatus())) {
                    startActivity(BuyerOrderRouter.intentFor(this, o, session.getUserId()));
                }
            });
        }
        loadMessages();
    }

    private static String statusLabel(String s) {
        if (s == null) return "";
        String t = s.replace('_', ' ').toLowerCase(Locale.US);
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    private void editPrice() {
        EditText in = new EditText(this);
        in.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        in.setText(String.format(Locale.US, "%.0f", price));
        new AlertDialog.Builder(this).setTitle("Counter-offer price (Rs. per kg)").setView(pad(in))
                .setPositiveButton("Update", (d, w) -> {
                    double v = parse(in.getText().toString());
                    if (v <= 0) return;
                    price = v;
                    termsEdited = true;
                    if (order != null) bind(order);
                }).setNegativeButton("Cancel", null).show();
    }

    private void editQuantity() {
        EditText in = new EditText(this);
        in.setInputType(InputType.TYPE_CLASS_NUMBER);
        in.setText(String.format(Locale.US, "%.0f", qty));
        new AlertDialog.Builder(this).setTitle("Quantity (kg)").setView(pad(in))
                .setPositiveButton("Update", (d, w) -> {
                    double v = parse(in.getText().toString());
                    if (v <= 0) return;
                    qty = v;
                    termsEdited = true;
                    if (order != null) bind(order);
                }).setNegativeButton("Cancel", null).show();
    }

    private void editDate() {
        Calendar c = Calendar.getInstance();
        DatePickerDialog dp = new DatePickerDialog(this, (view, y, m, d) -> {
            pickup = String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d);
            termsEdited = true;
            if (order != null) bind(order);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dp.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dp.show();
    }

    private View pad(View v) {
        LinearLayout box = new LinearLayout(this);
        int p = dp(20);
        box.setPadding(p, p / 2, p, 0);
        box.addView(v, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return box;
    }

    private void confirm(MaterialButton button) {
        if (pickup == null || pickup.isEmpty()) {
            Toast.makeText(this, "Choose a pickup date first.", Toast.LENGTH_SHORT).show();
            editDate();
            return;
        }
        String msg = String.format(Locale.US, "%s at %s/kg, pickup %s. %s", kg(qty), rs(price), pickup,
                termsEdited ? "The farmer must confirm these new terms." : "When both of you confirm, the order is locked and the advance is quoted.");
        new AlertDialog.Builder(this).setTitle(termsEdited ? "Send counter-offer?" : "Accept the deal?").setMessage(msg)
                .setPositiveButton("Confirm", (d, w) -> {
                    button.setEnabled(false);
                    api.confirmDeal(orderId, new ConfirmDealRequest(qty, price, pickup)).enqueue(new Callback<GenericResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<GenericResponse> call, @NonNull Response<GenericResponse> r) {
                            button.setEnabled(true);
                            if (!r.isSuccessful()) {
                                Toast.makeText(NegotiationChatActivity.this, AuthInputUtils.errorMessage(r, "Deal could not be confirmed."),
                                        Toast.LENGTH_LONG).show();
                                return;
                            }
                            termsEdited = false;
                            if (r.body() != null && r.body().getMessage() != null) {
                                Toast.makeText(NegotiationChatActivity.this, r.body().getMessage(), Toast.LENGTH_LONG).show();
                            }
                            reloadAndOpenSummary();
                        }

                        @Override
                        public void onFailure(@NonNull Call<GenericResponse> call, @NonNull Throwable t) {
                            button.setEnabled(true);
                            Toast.makeText(NegotiationChatActivity.this, R.string.fp_network_error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }).setNegativeButton("Cancel", null).show();
    }

    private void reloadAndOpenSummary() {
        api.getOrder(orderId).enqueue(new Callback<Order>() {
            @Override
            public void onResponse(@NonNull Call<Order> call, @NonNull Response<Order> r) {
                if (!r.isSuccessful() || r.body() == null) return;
                order = r.body();
                if ("AWAITING_PAYMENT".equals(order.getStatus())) {
                    Intent i = new Intent(NegotiationChatActivity.this, OrderSummaryActivity.class);
                    i.putExtra(BuyerOrderRouter.EXTRA_ORDER_ID, orderId);
                    startActivity(i);
                    finish();
                } else {
                    bind(order);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Order> call, @NonNull Throwable t) { }
        });
    }

    private void loadMessages() {
        api.getChatMessages(orderId).enqueue(new Callback<List<ChatMessage>>() {
            @Override
            public void onResponse(@NonNull Call<List<ChatMessage>> call, @NonNull Response<List<ChatMessage>> r) {
                if (!r.isSuccessful() || r.body() == null || r.body().size() == shownMessages) return;
                shownMessages = r.body().size();
                LinearLayout list = findViewById(R.id.layoutDynamicMessages);
                list.removeAllViews();
                for (ChatMessage m : r.body()) {
                    if (m.getText() != null) bubble(list, m.getText(), m.isSentByMe(), time(m.getTimestamp()));
                }
                NestedScrollView scroll = findViewById(R.id.scrollChatArea);
                scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
            }

            @Override
            public void onFailure(@NonNull Call<List<ChatMessage>> call, @NonNull Throwable t) { }
        });
    }

    private void send(String text, EditText input) {
        api.sendMessage(orderId, new SendMessageRequest(session.getUserId(), text)).enqueue(new Callback<ChatMessage>() {
            @Override
            public void onResponse(@NonNull Call<ChatMessage> call, @NonNull Response<ChatMessage> r) {
                if (r.isSuccessful()) {
                    input.setText("");
                    shownMessages = -1;
                    loadMessages();
                } else {
                    Toast.makeText(NegotiationChatActivity.this, "Message could not be sent.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<ChatMessage> call, @NonNull Throwable t) {
                Toast.makeText(NegotiationChatActivity.this, "Message could not be sent.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bubble(LinearLayout list, String text, boolean mine, String when) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = mine ? Gravity.END : Gravity.START;
        lp.topMargin = dp(12);
        if (mine) lp.leftMargin = dp(48); else lp.rightMargin = dp(48);
        box.setLayoutParams(lp);

        TextView msg = new TextView(this);
        msg.setBackgroundResource(mine ? R.drawable.bg_chat_buyer : R.drawable.bg_chat_farmer);
        msg.setText(text);
        msg.setTextColor(mine ? ContextCompat.getColor(this, R.color.white) : 0xFF111827);
        msg.setTextSize(14f);
        msg.setPadding(dp(16), dp(12), dp(16), dp(12));
        box.addView(msg);

        TextView t = new TextView(this);
        t.setText(mine ? when : (order != null && order.getFarmerName() != null ? order.getFarmerName() + " · " : "") + when);
        t.setTextColor(0xFF9CA3AF);
        t.setTextSize(11f);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.gravity = mine ? Gravity.END : Gravity.START;
        tp.topMargin = dp(4);
        t.setLayoutParams(tp);
        box.addView(t);
        list.addView(box);
    }

    private static double parse(String s) {
        try {
            return Double.parseDouble(s.replaceAll("[^0-9.]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
