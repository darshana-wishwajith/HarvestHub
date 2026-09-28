package com.example.project_part_2;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/**
 * AccountSuspendedActivity
 * Screen displayed when a transporter account is suspended/locked due to a debt balance.
 */
public class AccountSuspendedActivity extends AppCompatActivity {

    private ImageView btnClose;
    private LinearLayout layoutChannelBank;
    private LinearLayout layoutChannelCard;
    private LinearLayout layoutChannelGateway;

    private ImageView ivBankIconContainer;
    private ImageView ivCardIconContainer;
    private ImageView ivGatewayIconContainer;

    private ImageView rbChannelBank;
    private ImageView rbChannelCard;
    private ImageView rbChannelGateway;

    private LinearLayout btnPayDebt;
    private TextView tvDisputeSupport;

    // Selected settlement channel: 0 = Bank Transfer, 1 = Debit Card, 2 = Direct Gateway
    private int selectedChannelIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_suspended);

        // Configure light status bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window window = getWindow();
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            window.setStatusBarColor(Color.parseColor("#F8F9FA"));
        }

        initViews();
        setupListeners();
        selectPaymentChannel(0);

        android.widget.ScrollView scrollView = findViewById(R.id.scrollView);
        if (scrollView != null) {
            scrollView.setFocusableInTouchMode(true);
            scrollView.requestFocus();
            scrollView.post(() -> scrollView.scrollTo(0, 0));
        }
    }

    private void initViews() {
        btnClose = findViewById(R.id.btnClose);

        layoutChannelBank = findViewById(R.id.layoutChannelBank);
        layoutChannelCard = findViewById(R.id.layoutChannelCard);
        layoutChannelGateway = findViewById(R.id.layoutChannelGateway);

        ivBankIconContainer = findViewById(R.id.ivBankIconContainer);
        ivCardIconContainer = findViewById(R.id.ivCardIconContainer);
        ivGatewayIconContainer = findViewById(R.id.ivGatewayIconContainer);

        rbChannelBank = findViewById(R.id.rbChannelBank);
        rbChannelCard = findViewById(R.id.rbChannelCard);
        rbChannelGateway = findViewById(R.id.rbChannelGateway);

        btnPayDebt = findViewById(R.id.btnPayDebt);
        tvDisputeSupport = findViewById(R.id.tvDisputeSupport);
    }

    private void setupListeners() {
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }

        if (layoutChannelBank != null) {
            layoutChannelBank.setOnClickListener(v -> selectPaymentChannel(0));
        }

        if (layoutChannelCard != null) {
            layoutChannelCard.setOnClickListener(v -> selectPaymentChannel(1));
        }

        if (layoutChannelGateway != null) {
            layoutChannelGateway.setOnClickListener(v -> selectPaymentChannel(2));
        }

        if (btnPayDebt != null) {
            btnPayDebt.setOnClickListener(v -> processDebtSettlement());
        }

        if (tvDisputeSupport != null) {
            tvDisputeSupport.setOnClickListener(v -> openSupportDisputeDialog());
        }
    }

    /**
     * Updates UI state based on selected payment channel
     */
    private void selectPaymentChannel(int index) {
        this.selectedChannelIndex = index;

        // Reset all to unselected state
        layoutChannelBank.setBackgroundResource(R.drawable.bg_channel_unselected);
        ivBankIconContainer.setBackgroundResource(R.drawable.bg_channel_icon_circle_default);
        rbChannelBank.setImageResource(R.drawable.ic_radio_unchecked);

        layoutChannelCard.setBackgroundResource(R.drawable.bg_channel_unselected);
        ivCardIconContainer.setBackgroundResource(R.drawable.bg_channel_icon_circle_default);
        rbChannelCard.setImageResource(R.drawable.ic_radio_unchecked);

        layoutChannelGateway.setBackgroundResource(R.drawable.bg_channel_unselected);
        ivGatewayIconContainer.setBackgroundResource(R.drawable.bg_channel_icon_circle_default);
        rbChannelGateway.setImageResource(R.drawable.ic_radio_unchecked);

        // Apply selected state
        switch (index) {
            case 0:
                layoutChannelBank.setBackgroundResource(R.drawable.bg_channel_selected);
                ivBankIconContainer.setBackgroundResource(R.drawable.bg_channel_icon_circle);
                rbChannelBank.setImageResource(R.drawable.ic_radio_checked);
                break;
            case 1:
                layoutChannelCard.setBackgroundResource(R.drawable.bg_channel_selected);
                ivCardIconContainer.setBackgroundResource(R.drawable.bg_channel_icon_circle);
                rbChannelCard.setImageResource(R.drawable.ic_radio_checked);
                break;
            case 2:
                layoutChannelGateway.setBackgroundResource(R.drawable.bg_channel_selected);
                ivGatewayIconContainer.setBackgroundResource(R.drawable.bg_channel_icon_circle);
                rbChannelGateway.setImageResource(R.drawable.ic_radio_checked);
                break;
        }
    }

    private void processDebtSettlement() {
        String channelName;
        switch (selectedChannelIndex) {
            case 0:
                channelName = "Instant Bank Transfer (CEFT/SLIPS)";
                break;
            case 1:
                channelName = "Linked Debit Card (Visa •••• 3091)";
                break;
            case 2:
                channelName = "Direct Gateway (LankaQR / Vishwa / Genie)";
                break;
            default:
                channelName = "Selected Channel";
                break;
        }

        new AlertDialog.Builder(this)
                .setTitle("Confirm Settlement")
                .setMessage("Initiate payment of Rs. 8,500 via " + channelName + " to unlock driver account?")
                .setPositiveButton("Pay Now", (dialog, which) -> {
                    Toast.makeText(this, "Settlement initiated. Your account will reactivate upon confirmation.", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openSupportDisputeDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Carrier Support Dispute")
                .setMessage("Transit Damage Penalty Claim (Order #810) - Rs. 6,500\n\nWould you like to open a dispute ticket with Carrier Support?")
                .setPositiveButton("Contact Support", (dialog, which) -> {
                    Toast.makeText(this, "Connecting to Carrier Dispute Support line...", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
    }
}
