package com.harvesthub.mobile.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.MainActivity;

public class BottomNavHelper {

    public enum Tab {
        HOME, MARKET, LISTINGS, WALLET, SETTINGS
    }

    public static void setup(Activity activity, View navBarView, Tab activeTab) {
        Context context = activity.getApplicationContext();

        // 1. Initialize Views
        View navHome = navBarView.findViewById(R.id.nav_home);
        View navMarket = navBarView.findViewById(R.id.nav_market);
        View navListings = navBarView.findViewById(R.id.nav_listings);
        View navWallet = navBarView.findViewById(R.id.nav_wallet);
        View navSettings = navBarView.findViewById(R.id.nav_settings);

        // 2. Set Active Visuals
        updateTabVisuals(navBarView, activeTab, context);

        // 3. Set Click Listeners
        navHome.setOnClickListener(v -> navigate(activity, Tab.HOME));
        navMarket.setOnClickListener(v -> navigate(activity, Tab.MARKET));
        navListings.setOnClickListener(v -> navigate(activity, Tab.LISTINGS));
        navWallet.setOnClickListener(v -> navigate(activity, Tab.WALLET));
        navSettings.setOnClickListener(v -> navigate(activity, Tab.SETTINGS));
    }

    private static void updateTabVisuals(View navBarView, Tab activeTab, Context context) {
        resetAllTabs(navBarView, context);

        int pillId, iconId, labelId, activeIconRes;
        switch (activeTab) {
            case MARKET:
                pillId = R.id.pill_market; iconId = R.id.icon_market; labelId = R.id.label_market;
                activeIconRes = R.drawable.ic_market_icon_bottom_bar;
                break;
            case LISTINGS:
                pillId = R.id.pill_listings; iconId = R.id.icon_listings; labelId = R.id.label_listings;
                activeIconRes = R.drawable.ic_listings_icon_bottom_bar;
                break;
            case WALLET:
                pillId = R.id.pill_wallet; iconId = R.id.icon_wallet; labelId = R.id.label_wallet;
                activeIconRes = R.drawable.ic_wallet_bottom_bar;
                break;
            case SETTINGS:
                pillId = R.id.pill_settings; iconId = R.id.icon_settings; labelId = R.id.label_settings;
                activeIconRes = R.drawable.ic_settings_bottom_bar;
                break;
            case HOME:
            default:
                pillId = R.id.pill_home; iconId = R.id.icon_home; labelId = R.id.label_home;
                activeIconRes = R.drawable.ic_home_bottom_bar;
                break;
        }

        View pill = navBarView.findViewById(pillId);
        ImageView icon = navBarView.findViewById(iconId);
        TextView label = navBarView.findViewById(labelId);

        pill.setVisibility(View.VISIBLE);
        pill.setBackgroundResource(R.drawable.bg_nav_item_active);
        icon.setImageResource(activeIconRes);
        icon.setColorFilter(ContextCompat.getColor(context, R.color.nav_active_text));
        label.setTextColor(ContextCompat.getColor(context, R.color.nav_active_text));
    }

    private static void resetAllTabs(View navBarView, Context context) {
        int[] pills = {R.id.pill_home, R.id.pill_market, R.id.pill_listings, R.id.pill_wallet, R.id.pill_settings};
        int[] icons = {R.id.icon_home, R.id.icon_market, R.id.icon_listings, R.id.icon_wallet, R.id.icon_settings};
        int[] labels = {R.id.label_home, R.id.label_market, R.id.label_listings, R.id.label_wallet, R.id.label_settings};
        int[] defaultIconRes = {
            R.drawable.ic_home_bottom_bar,
            R.drawable.ic_market_icon_bottom_bar,
            R.drawable.ic_listings_icon_bottom_bar,
            R.drawable.ic_wallet_bottom_bar,
            R.drawable.ic_settings_bottom_bar
        };

        int inactiveColor = ContextCompat.getColor(context, R.color.nav_inactive_text);

        for (int i = 0; i < pills.length; i++) {
            View pill = navBarView.findViewById(pills[i]);
            if (pill != null) {
                pill.setVisibility(View.VISIBLE);
                pill.setBackground(null);
            }
            ImageView icon = navBarView.findViewById(icons[i]);
            if (icon != null) {
                icon.setImageResource(defaultIconRes[i]);
                icon.setColorFilter(inactiveColor);
            }
            TextView label = navBarView.findViewById(labels[i]);
            if (label != null) {
                label.setTextColor(inactiveColor);
            }
        }
    }

    private static void navigate(Activity activity, Tab targetTab) {
        MainActivity.Tab mainTab;
        switch (targetTab) {
            case MARKET: mainTab = MainActivity.Tab.MARKET; break;
            case LISTINGS: mainTab = MainActivity.Tab.LISTINGS; break;
            case WALLET: mainTab = MainActivity.Tab.WALLET; break;
            case SETTINGS: mainTab = MainActivity.Tab.SETTINGS; break;
            default: mainTab = MainActivity.Tab.HOME; break;
        }

        Intent intent = new Intent(activity, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_INITIAL_TAB, mainTab.name());
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        activity.startActivity(intent);
    }
}
