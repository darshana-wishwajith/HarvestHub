package com.harvesthub.mobile.ui.buyer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.utils.SessionManager;

public class BuyerDashboardActivity extends AppCompatActivity {

    public static final String EXTRA_INITIAL_TAB = "extra_initial_tab";
    public static final String EXTRA_AD_TITLE = "extra_ad_title";

    public enum Tab {
        HOME, ORDERS, DEMANDS, OFFERS
    }

    private Tab currentTab = null;
    private View buyerTopNav;
    private View buyerBottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer_dashboard);

        buyerTopNav = findViewById(R.id.buyer_top_nav_bar);
        buyerBottomNav = findViewById(R.id.buyer_bottom_nav_bar);

        setupTopNav();
        setupBottomNav();

        Tab initialTab = Tab.HOME;
        String initialAdTitle = null;
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(EXTRA_INITIAL_TAB)) {
                try {
                    initialTab = Tab.valueOf(intent.getStringExtra(EXTRA_INITIAL_TAB));
                } catch (Exception ignored) { }
            }
            if (intent.hasExtra(EXTRA_AD_TITLE)) {
                initialAdTitle = intent.getStringExtra(EXTRA_AD_TITLE);
            }
        }

        selectTab(initialTab, initialAdTitle);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (intent != null) {
            Tab targetTab = Tab.HOME;
            String adTitle = null;
            if (intent.hasExtra(EXTRA_INITIAL_TAB)) {
                try {
                    targetTab = Tab.valueOf(intent.getStringExtra(EXTRA_INITIAL_TAB));
                } catch (Exception ignored) { }
            }
            if (intent.hasExtra(EXTRA_AD_TITLE)) {
                adTitle = intent.getStringExtra(EXTRA_AD_TITLE);
            }
            selectTab(targetTab, adTitle);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateTopNavUserGreeting();
    }

    private void setupTopNav() {
        if (buyerTopNav == null) return;
        updateTopNavUserGreeting();

        View notifications = buyerTopNav.findViewById(R.id.btnBuyerNotifications);
        if (notifications != null) {
            notifications.setOnClickListener(v -> {
                SessionManager session = new SessionManager(this);
                String name = (session.getUser() != null && session.getUser().getFullName() != null)
                        ? session.getUser().getFullName() : "Buyer";
                Toast.makeText(this, "Notifications active for " + name, Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void updateTopNavUserGreeting() {
        if (buyerTopNav == null) return;
        SessionManager session = new SessionManager(this);
        TextView tvSubtitle = buyerTopNav.findViewById(R.id.tvBuyerNavSubtitle);
        if (tvSubtitle != null) {
            String name = (session.getUser() != null && session.getUser().getFullName() != null && !session.getUser().getFullName().isEmpty())
                    ? session.getUser().getFullName() : "Perera Supermarket";
            tvSubtitle.setText("Hi, " + name);
        }
    }

    private void setupBottomNav() {
        if (buyerBottomNav == null) return;

        View navHome = buyerBottomNav.findViewById(R.id.buyerNavHome);
        View navOrders = buyerBottomNav.findViewById(R.id.buyerNavOrders);
        View navDemands = buyerBottomNav.findViewById(R.id.buyerNavDemands);
        View navOffers = buyerBottomNav.findViewById(R.id.buyerNavOffers);

        if (navHome != null) navHome.setOnClickListener(v -> selectTab(Tab.HOME));
        if (navOrders != null) navOrders.setOnClickListener(v -> selectTab(Tab.ORDERS));
        if (navDemands != null) navDemands.setOnClickListener(v -> selectTab(Tab.DEMANDS));
        if (navOffers != null) navOffers.setOnClickListener(v -> selectTab(Tab.OFFERS));
    }

    public void selectTab(Tab tab) {
        selectTab(tab, null);
    }

    public void selectTab(Tab tab, String customAdTitle) {
        if (tab == null) tab = Tab.HOME;
        if (currentTab == tab && customAdTitle == null) return;

        Fragment fragment = createFragmentForTab(tab, customAdTitle);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.buyer_fragment_container, fragment)
                .commit();

        currentTab = tab;
        updateBottomNavVisuals(tab);
    }

    private Fragment createFragmentForTab(Tab tab, String customAdTitle) {
        switch (tab) {
            case ORDERS:
                return new BuyerOrdersFragment();
            case DEMANDS:
                return new BuyerDemandsFragment();
            case OFFERS:
                return BuyerOffersFragment.newInstance(customAdTitle);
            case HOME:
            default:
                return new BuyerMarketplaceFragment();
        }
    }

    private void updateBottomNavVisuals(Tab activeTab) {
        if (buyerBottomNav == null) return;

        int[] pills = {R.id.buyerPillHome, R.id.buyerPillOrders, R.id.buyerPillDemands, R.id.buyerPillOffers};
        int[] icons = {R.id.buyerIconHome, R.id.buyerIconOrders, R.id.buyerIconDemands, R.id.buyerIconOffers};
        int[] labels = {R.id.buyerLabelHome, R.id.buyerLabelOrders, R.id.buyerLabelDemands, R.id.buyerLabelOffers};
        int activeIndex = activeTab.ordinal();

        for (int index = 0; index < pills.length; index++) {
            View pill = buyerBottomNav.findViewById(pills[index]);
            ImageView icon = buyerBottomNav.findViewById(icons[index]);
            TextView label = buyerBottomNav.findViewById(labels[index]);

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

    @Override
    public void onBackPressed() {
        if (currentTab != null && currentTab != Tab.HOME) {
            selectTab(Tab.HOME);
        } else {
            super.onBackPressed();
        }
    }
}
