package com.harvesthub.mobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.harvesthub.mobile.ui.farmer.HomeFragment;
import com.harvesthub.mobile.ui.farmer.MarketFragment;
import com.harvesthub.mobile.ui.farmer.ListingsFragment;
import com.harvesthub.mobile.ui.farmer.WalletFragment;
import com.harvesthub.mobile.ui.farmer.SettingsFragment;
import com.harvesthub.mobile.ui.farmer.NotificationsActivity;
import com.harvesthub.mobile.util.NotificationPanelHelper;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.network.ApiService;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_INITIAL_TAB = "extra_initial_tab";
    
    public enum Tab { HOME, MARKET, LISTINGS, WALLET, SETTINGS }
    private Tab currentTab;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        apiService = ApiClient.getClient().create(ApiService.class);
        setupTopNav();
        setupBottomNav();

        if (savedInstanceState == null) {
            Tab initialTab = Tab.HOME;
            if (getIntent().hasExtra(EXTRA_INITIAL_TAB)) {
                try {
                    initialTab = Tab.valueOf(getIntent().getStringExtra(EXTRA_INITIAL_TAB));
                } catch (Exception ignored) {
                }
            }
            loadFragment(initialTab, createFragmentForTab(initialTab));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent.hasExtra(EXTRA_INITIAL_TAB)) {
            try {
                Tab targetTab = Tab.valueOf(intent.getStringExtra(EXTRA_INITIAL_TAB));
                loadFragment(targetTab, createFragmentForTab(targetTab));
            } catch (Exception ignored) {}
        }
    }

    private void setupTopNav() {
        View topNav = findViewById(R.id.top_nav_bar);
        if (topNav != null) {
            View btnNotification = topNav.findViewById(R.id.btnNotification);
            if (btnNotification != null) {
                btnNotification.setOnClickListener(v -> NotificationPanelHelper.showNotificationPanel(this, v));
            }
        }
    }

    private void setupBottomNav() {
        findViewById(R.id.nav_home).setOnClickListener(v -> loadFragment(Tab.HOME, new HomeFragment()));
        findViewById(R.id.nav_market).setOnClickListener(v -> loadFragment(Tab.MARKET, new MarketFragment()));
        findViewById(R.id.nav_listings).setOnClickListener(v -> loadFragment(Tab.LISTINGS, new ListingsFragment()));
        findViewById(R.id.nav_wallet).setOnClickListener(v -> loadFragment(Tab.WALLET, new WalletFragment()));
        findViewById(R.id.nav_settings).setOnClickListener(v -> loadFragment(Tab.SETTINGS, new SettingsFragment()));
    }

    private void loadFragment(Tab tab, Fragment fragment) {
        if (currentTab == tab) return;

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();

        currentTab = tab;
        updateBottomNavVisuals(tab);
    }

    private Fragment createFragmentForTab(Tab tab) {
        switch (tab) {
            case MARKET: return new MarketFragment();
            case LISTINGS: return new ListingsFragment();
            case WALLET: return new WalletFragment();
            case SETTINGS: return new SettingsFragment();
            case HOME:
            default: return new HomeFragment();
        }
    }

    private void updateBottomNavVisuals(Tab activeTab) {
        resetAllTabs();

        int pillId, iconId, labelId, activeIconRes;
        switch (activeTab) {
            case MARKET:
                pillId = R.id.pill_market; iconId = R.id.icon_market; labelId = R.id.label_market;
                activeIconRes = R.drawable.ic_market;
                break;
            case LISTINGS:
                pillId = R.id.pill_listings; iconId = R.id.icon_listings; labelId = R.id.label_listings;
                activeIconRes = R.drawable.ic_list;
                break;
            case WALLET:
                pillId = R.id.pill_wallet; iconId = R.id.icon_wallet; labelId = R.id.label_wallet;
                activeIconRes = R.drawable.ic_wallet;
                break;
            case SETTINGS:
                pillId = R.id.pill_settings; iconId = R.id.icon_settings; labelId = R.id.label_settings;
                activeIconRes = R.drawable.ic_settings;
                break;
            case HOME:
            default:
                pillId = R.id.pill_home; iconId = R.id.icon_home; labelId = R.id.label_home;
                activeIconRes = R.drawable.ic_home;
                break;
        }

        LinearLayout pill = findViewById(pillId);
        ImageView icon = findViewById(iconId);
        TextView label = findViewById(labelId);

        pill.setBackgroundResource(R.drawable.bg_nav_item_active);
        icon.setImageResource(activeIconRes);
        icon.setColorFilter(ContextCompat.getColor(this, R.color.nav_active_text));
        label.setTextColor(ContextCompat.getColor(this, R.color.nav_active_text));
    }

    private void resetAllTabs() {
        int[] pills = {R.id.pill_home, R.id.pill_market, R.id.pill_listings, R.id.pill_wallet, R.id.pill_settings};
        int[] icons = {R.id.icon_home, R.id.icon_market, R.id.icon_listings, R.id.icon_wallet, R.id.icon_settings};
        int[] labels = {R.id.label_home, R.id.label_market, R.id.label_listings, R.id.label_wallet, R.id.label_settings};

        for (int i = 0; i < pills.length; i++) {
            findViewById(pills[i]).setBackground(null);
            ImageView icon = findViewById(icons[i]);
            icon.setColorFilter(ContextCompat.getColor(this, R.color.nav_inactive_text));
            TextView label = findViewById(labels[i]);
            label.setTextColor(ContextCompat.getColor(this, R.color.nav_inactive_text));
        }
    }
}