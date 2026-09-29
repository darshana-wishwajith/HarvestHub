package com.harvesthub.mobile.utils;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.ui.buyer.BuyerDashboardActivity;

public final class BuyerNavigationHelper {
    public enum Tab { HOME, ORDERS, DEMANDS, OFFERS }

    private BuyerNavigationHelper() { }

    public static void setup(Activity activity, View root, Tab activeTab) {
        if (root == null) return;
        setActiveState(root, activeTab);

        View navHome = root.findViewById(R.id.buyerNavHome);
        View navOrders = root.findViewById(R.id.buyerNavOrders);
        View navDemands = root.findViewById(R.id.buyerNavDemands);
        View navOffers = root.findViewById(R.id.buyerNavOffers);

        if (navHome != null) navHome.setOnClickListener(v -> navigate(activity, BuyerDashboardActivity.Tab.HOME));
        if (navOrders != null) navOrders.setOnClickListener(v -> navigate(activity, BuyerDashboardActivity.Tab.ORDERS));
        if (navDemands != null) navDemands.setOnClickListener(v -> navigate(activity, BuyerDashboardActivity.Tab.DEMANDS));
        if (navOffers != null) navOffers.setOnClickListener(v -> navigate(activity, BuyerDashboardActivity.Tab.OFFERS));
    }

    public static void setupNotifications(Activity activity, View root) {
        if (root == null) return;
        SessionManager session = new SessionManager(activity);
        TextView tvSubtitle = root.findViewById(R.id.tvBuyerNavSubtitle);
        if (tvSubtitle != null) {
            String name = (session.getUser() != null && session.getUser().getFullName() != null && !session.getUser().getFullName().isEmpty())
                    ? session.getUser().getFullName() : "Perera Supermarket";
            tvSubtitle.setText("Hi, " + name);
        }
        View notifications = root.findViewById(R.id.btnBuyerNotifications);
        if (notifications != null) {
            notifications.setOnClickListener(v -> Toast.makeText(activity,
                    "Notifications active for " + (session.getUser() != null && session.getUser().getFullName() != null ? session.getUser().getFullName() : "Buyer"), Toast.LENGTH_SHORT).show());
        }
    }

    private static void navigate(Activity activity, BuyerDashboardActivity.Tab targetTab) {
        if (activity instanceof BuyerDashboardActivity) {
            ((BuyerDashboardActivity) activity).selectTab(targetTab);
            return;
        }
        Intent intent = new Intent(activity, BuyerDashboardActivity.class);
        intent.putExtra(BuyerDashboardActivity.EXTRA_INITIAL_TAB, targetTab.name());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
    }

    public static void setActiveState(View root, Tab activeTab) {
        if (root == null || activeTab == null) return;
        int[] pills = {R.id.buyerPillHome, R.id.buyerPillOrders, R.id.buyerPillDemands, R.id.buyerPillOffers};
        int[] icons = {R.id.buyerIconHome, R.id.buyerIconOrders, R.id.buyerIconDemands, R.id.buyerIconOffers};
        int[] labels = {R.id.buyerLabelHome, R.id.buyerLabelOrders, R.id.buyerLabelDemands, R.id.buyerLabelOffers};
        int activeIndex = activeTab.ordinal();
        for (int index = 0; index < pills.length; index++) {
            View pill = root.findViewById(pills[index]);
            ImageView icon = root.findViewById(icons[index]);
            TextView label = root.findViewById(labels[index]);
            boolean active = (index == activeIndex);
            if (pill != null) {
                pill.setBackgroundResource(active ? R.drawable.bg_nav_item_active : android.R.color.transparent);
            }
            int color = Color.parseColor(active ? "#FFFFFF" : "#414844");
            if (icon != null) {
                icon.setColorFilter(color);
            }
            if (label != null) {
                label.setTextColor(color);
            }
        }
    }
}