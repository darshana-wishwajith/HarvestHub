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
import com.harvesthub.mobile.ui.buyer.MyDemandRequestsActivity;
import com.harvesthub.mobile.ui.buyer.SupplyOffersActivity;
import com.harvesthub.mobile.ui.buyer.BuyerOrdersActivity;

public final class BuyerNavigationHelper {
    public enum Tab { HOME, ORDERS, DEMANDS, OFFERS }

    private BuyerNavigationHelper() { }

    public static void setup(Activity activity, View root, Tab activeTab) {
        setActiveState(root, activeTab);
        root.findViewById(R.id.buyerNavHome).setOnClickListener(v -> navigate(activity, BuyerDashboardActivity.class));
        root.findViewById(R.id.buyerNavOrders).setOnClickListener(v -> navigate(activity, BuyerOrdersActivity.class));
        root.findViewById(R.id.buyerNavDemands).setOnClickListener(v -> navigate(activity, MyDemandRequestsActivity.class));
        root.findViewById(R.id.buyerNavOffers).setOnClickListener(v -> navigate(activity, SupplyOffersActivity.class));
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

    private static void navigate(Activity activity, Class<? extends Activity> destination) {
        if (activity.getClass().equals(destination)) return;
        Intent intent = new Intent(activity, destination);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        activity.startActivity(intent);
    }

    private static void setActiveState(View root, Tab activeTab) {
        int[] pills = {R.id.buyerPillHome, R.id.buyerPillOrders, R.id.buyerPillDemands, R.id.buyerPillOffers};
        int[] icons = {R.id.buyerIconHome, R.id.buyerIconOrders, R.id.buyerIconDemands, R.id.buyerIconOffers};
        int[] labels = {R.id.buyerLabelHome, R.id.buyerLabelOrders, R.id.buyerLabelDemands, R.id.buyerLabelOffers};
        int activeIndex = activeTab.ordinal();
        for (int index = 0; index < pills.length; index++) {
            View pill = root.findViewById(pills[index]);
            ImageView icon = root.findViewById(icons[index]);
            TextView label = root.findViewById(labels[index]);
            boolean active = index == activeIndex;
            pill.setBackgroundResource(active ? R.drawable.bg_nav_item_active : android.R.color.transparent);
            int color = Color.parseColor(active ? "#FFFFFF" : "#414844");
            icon.setColorFilter(color);
            label.setTextColor(color);
        }
    }
}