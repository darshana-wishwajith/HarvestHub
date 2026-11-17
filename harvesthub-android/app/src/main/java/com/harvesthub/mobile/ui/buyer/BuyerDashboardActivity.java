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

    /** MARKET has no bottom-nav item: it opens from Home ("View Market", Figma 564:330). */
    public enum Tab {
        HOME, ORDERS, DEMANDS, OFFERS, MARKET, ACCOUNT
    }

    private String offersAdId;

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
                Intent i = new Intent(this, com.harvesthub.mobile.ui.farmer.NotificationsActivity.class);
                i.putExtra(com.harvesthub.mobile.ui.farmer.NotificationsActivity.EXTRA_HIDE_NAV, true);
                startActivity(i);
            });
        }
    }

    private void updateTopNavUserGreeting() {
        if (buyerTopNav == null) return;
        SessionManager session = new SessionManager(this);
        TextView tvSubtitle = buyerTopNav.findViewById(R.id.tvBuyerNavSubtitle);
        if (tvSubtitle != null) {
            String name = (session.getUser() != null && session.getUser().getFullName() != null && !session.getUser().getFullName().isEmpty())
                    ? session.getUser().getFullName() : "Buyer";
            tvSubtitle.setVisibility(View.GONE); // greeting lives on the Home screen only (user request 18 Nov)
        }
    }

    private void setupBottomNav() {
        if (buyerBottomNav == null) return;
        int[] ids = {R.id.buyerNavHome, R.id.buyerNavMarket, R.id.buyerNavOrders, R.id.buyerNavDemands, R.id.buyerNavAccount};
        Tab[] tabs = {Tab.HOME, Tab.MARKET, Tab.ORDERS, Tab.DEMANDS, Tab.ACCOUNT};
        for (int i = 0; i < ids.length; i++) {
            final Tab t = tabs[i];
            View item = buyerBottomNav.findViewById(ids[i]);
            if (item != null) item.setOnClickListener(v -> selectTab(t));
        }
    }

    public void selectTab(Tab tab) {
        selectTab(tab, null);
    }

    /** 2.4 "View Offers" → 3.1 for one wanted ad. */
    public void showOffersFor(String adTitle, String adId) {
        offersAdId = adId;
        currentTab = null;
        selectTab(Tab.OFFERS, adTitle);
    }

    public void selectTab(Tab tab, String customAdTitle) {
        if (tab == null) tab = Tab.HOME;
        if (currentTab == tab && customAdTitle == null && offersAdId == null) return;

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
                Fragment f = BuyerOffersFragment.newInstance(customAdTitle, offersAdId);
                offersAdId = null;
                return f;
            case MARKET:
                return new BuyerMarketplaceFragment();
            case ACCOUNT:
                return new BuyerAccountFragment();
            case HOME:
            default:
                return new BuyerHomeFragment();
        }
    }

    private void updateBottomNavVisuals(Tab activeTab) {
        if (buyerBottomNav == null) return;
        int[] pills = {R.id.buyerPillHome, R.id.buyerPillMarket, R.id.buyerPillOrders, R.id.buyerPillDemands, R.id.buyerPillAccount};
        int[] icons = {R.id.buyerIconHome, R.id.buyerIconMarket, R.id.buyerIconOrders, R.id.buyerIconDemands, R.id.buyerIconAccount};
        int[] labels = {R.id.buyerLabelHome, R.id.buyerLabelMarket, R.id.buyerLabelOrders, R.id.buyerLabelDemands, R.id.buyerLabelAccount};
        // supply offers belong to a wanted ad, so they light up "Requests"
        int activeIndex;
        switch (activeTab) {
            case MARKET: activeIndex = 1; break;
            case ORDERS: activeIndex = 2; break;
            case DEMANDS:
            case OFFERS: activeIndex = 3; break;
            case ACCOUNT: activeIndex = 4; break;
            default: activeIndex = 0;
        }
        for (int index = 0; index < pills.length; index++) {
            View pill = buyerBottomNav.findViewById(pills[index]);
            ImageView icon = buyerBottomNav.findViewById(icons[index]);
            TextView label = buyerBottomNav.findViewById(labels[index]);
            boolean active = index == activeIndex;
            if (pill != null) {
                pill.setBackground(active ? com.harvesthub.mobile.ui.transporter.TUi.round(Color.parseColor("#1B4332"),
                        14 * getResources().getDisplayMetrics().density) : null);
            }
            int color = active ? Color.WHITE : Color.parseColor("#111827");
            if (icon != null) icon.setColorFilter(color);
            if (label != null) label.setTextColor(color);
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
