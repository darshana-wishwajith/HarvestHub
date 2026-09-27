package com.harvesthub.mobile;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.harvesthub.mobile.databinding.ActivityMainBinding;
import com.harvesthub.mobile.databinding.BottomNavBarBinding;
import com.harvesthub.mobile.ui.farmer.HomeFragment;
import com.harvesthub.mobile.ui.farmer.MarketFragment;
import com.harvesthub.mobile.ui.farmer.ListingsFragment;
import com.harvesthub.mobile.ui.farmer.WalletFragment;
import com.harvesthub.mobile.ui.farmer.SettingsFragment;
import com.harvesthub.mobile.utils.NotificationPanelHelper;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_INITIAL_TAB = "extra_initial_tab";
    
    public enum Tab { HOME, MARKET, LISTINGS, WALLET, SETTINGS }
    private Tab currentTab;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupTopNav();
        setupBottomNav();

        if (savedInstanceState == null) {
            Tab initialTab = Tab.HOME;
            if (getIntent().hasExtra(EXTRA_INITIAL_TAB)) {
                try {
                    initialTab = Tab.valueOf(getIntent().getStringExtra(EXTRA_INITIAL_TAB));
                } catch (Exception ignored) {}
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
        if (binding.topNavBar.getRoot() != null) {
            View btnNotification = binding.topNavBar.getRoot().findViewById(R.id.btnNotification);
            if (btnNotification != null) {
                btnNotification.setOnClickListener(v -> NotificationPanelHelper.showNotificationPanel(this, v));
            }
        }
    }

    private void setupBottomNav() {
        BottomNavBarBinding nav = BottomNavBarBinding.bind(binding.bottomNavBar.getRoot());
        nav.navHome.setOnClickListener(v -> loadFragment(Tab.HOME, new HomeFragment()));
        nav.navMarket.setOnClickListener(v -> loadFragment(Tab.MARKET, new MarketFragment()));
        nav.navListings.setOnClickListener(v -> loadFragment(Tab.LISTINGS, new ListingsFragment()));
        nav.navWallet.setOnClickListener(v -> loadFragment(Tab.WALLET, new WalletFragment()));
        nav.navSettings.setOnClickListener(v -> loadFragment(Tab.SETTINGS, new SettingsFragment()));
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
        BottomNavBarBinding nav = BottomNavBarBinding.bind(binding.bottomNavBar.getRoot());
        resetAllTabs(nav);

        View pill;
        ImageView icon;
        TextView label;
        int activeIconRes;

        switch (activeTab) {
            case MARKET:
                pill = nav.pillMarket; icon = nav.iconMarket; label = nav.labelMarket;
                activeIconRes = R.drawable.ic_market_icon_bottom_bar;
                break;
            case LISTINGS:
                pill = nav.pillListings; icon = nav.iconListings; label = nav.labelListings;
                activeIconRes = R.drawable.ic_listings_icon_bottom_bar;
                break;
            case WALLET:
                pill = nav.pillWallet; icon = nav.iconWallet; label = nav.labelWallet;
                activeIconRes = R.drawable.ic_wallet_bottom_bar;
                break;
            case SETTINGS:
                pill = nav.pillSettings; icon = nav.iconSettings; label = nav.labelSettings;
                activeIconRes = R.drawable.ic_settings_bottom_bar;
                break;
            case HOME:
            default:
                pill = nav.pillHome; icon = nav.iconHome; label = nav.labelHome;
                activeIconRes = R.drawable.ic_home_bottom_bar;
                break;
        }

        pill.setVisibility(View.VISIBLE);
        icon.setImageResource(activeIconRes);
        icon.setColorFilter(ContextCompat.getColor(this, R.color.nav_active_text));
        label.setTextColor(ContextCompat.getColor(this, R.color.primary_green));
    }

    private void resetAllTabs(BottomNavBarBinding nav) {
        View[] pills = {nav.pillHome, nav.pillMarket, nav.pillListings, nav.pillWallet, nav.pillSettings};
        ImageView[] icons = {nav.iconHome, nav.iconMarket, nav.iconListings, nav.iconWallet, nav.iconSettings};
        TextView[] labels = {nav.labelHome, nav.labelMarket, nav.labelListings, nav.labelWallet, nav.labelSettings};

        int inactiveColor = ContextCompat.getColor(this, R.color.nav_inactive_text);

        for (int i = 0; i < pills.length; i++) {
            pills[i].setVisibility(View.INVISIBLE);
            icons[i].setColorFilter(inactiveColor);
            labels[i].setTextColor(inactiveColor);
        }
    }
}
