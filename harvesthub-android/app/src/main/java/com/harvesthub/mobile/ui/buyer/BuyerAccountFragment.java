package com.harvesthub.mobile.ui.buyer;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.harvesthub.mobile.R;
import com.harvesthub.mobile.model.BuyerDashboard;
import com.harvesthub.mobile.model.User;
import com.harvesthub.mobile.network.ApiClient;
import com.harvesthub.mobile.ui.farmer.AccountDialogs;
import com.harvesthub.mobile.ui.transporter.TUi;
import com.harvesthub.mobile.utils.SessionManager;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Buyer "Account" tab: identity + trust, delivery point (required for
 * orders), wallet and escrow, notification preferences, password, help and sign out. Same structure as the
 * transporter Settings screen.
 */
public class BuyerAccountFragment extends Fragment {

    private LinearLayout content;
    private SessionManager session;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ScrollView sv = new ScrollView(requireContext());
        sv.setBackgroundColor(TUi.PAGE);
        sv.setFillViewport(true);
        content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        int p = dp(16);
        content.setPadding(p, dp(8), p, dp(24));
        sv.addView(content);
        return sv;
    }

    @Override
    public void onResume() {
        super.onResume();
        session = new SessionManager(requireContext());
        render(null);
        String me = session.getUserId();
        if (me == null) return;
        ApiClient.getApiService().getBuyerDashboard(me).enqueue(new Callback<BuyerDashboard>() {
            @Override
            public void onResponse(@NonNull Call<BuyerDashboard> call, @NonNull Response<BuyerDashboard> r) {
                if (isAdded() && r.isSuccessful() && r.body() != null) render(r.body());
            }

            @Override
            public void onFailure(@NonNull Call<BuyerDashboard> call, @NonNull Throwable t) { }
        });
    }

    private void render(@Nullable BuyerDashboard d) {
        content.removeAllViews();
        User u = session.getUser();
        String name = u == null || u.getFullName() == null || u.getFullName().isEmpty() ? "Buyer" : u.getFullName();

        LinearLayout p = TUi.card(content);
        LinearLayout pr = TUi.hrow(p, 0);
        TextView av = new TextView(requireContext());
        av.setText(initials(name));
        av.setGravity(Gravity.CENTER);
        av.setTextSize(22);
        av.setTypeface(TUi.bold(requireContext()));
        av.setTextColor(Color.WHITE);
        av.setBackground(TUi.round(TUi.DARK, dp(40)));
        pr.addView(av, new LinearLayout.LayoutParams(dp(64), dp(64)));
        LinearLayout pt = TUi.col(pr, 1f);
        pt.setPadding(dp(14), 0, 0, 0);
        TextView nm = TUi.text(pt, name, 20, TUi.INK, true);
        nm.setMaxLines(1);
        nm.setEllipsize(TextUtils.TruncateAt.END);
        TUi.pill(pt, "✓ Verified Buyer", TUi.MINT, TUi.MINT_INK).setTextSize(11);
        if (u != null) {
            TUi.text(pt, (u.getEmail() == null ? "" : u.getEmail()) + (u.getMobileNumber() == null ? "" : "\n" + u.getMobileNumber()),
                    12, TUi.MUTED, false).setPadding(0, dp(6), 0, 0);
        }
        LinearLayout stats = TUi.box(p, TUi.GREY_BG, 14);
        LinearLayout sr = TUi.hrow(stats, 0);
        stat(sr, "Trust", d == null ? "—" : d.trustPercent + "%");
        stat(sr, "Rating", d == null || d.ratingAverage == null ? "—" : String.format(Locale.US, "★ %.1f", d.ratingAverage));
        stat(sr, "Active Orders", d == null ? "—" : String.valueOf(d.activeOrders));

        section("Delivery & payments");
        LinearLayout c1 = TUi.card(content);
        boolean hasLoc = d != null && d.hasDeliveryLocation;
        setting(c1, R.drawable.ic_location_pin, "Delivery Point", d == null ? "Loading…"
                        : hasLoc ? (d.deliveryAddress == null ? "Set" : d.deliveryAddress) : "Not set – required for orders",
                d != null && !hasLoc ? "Required" : null, x -> startActivity(new Intent(requireContext(), SetDeliveryAddressActivity.class)));
        TUi.divider(c1, 10);
        setting(c1, R.drawable.ic_wallet, "Wallet & Escrow", d == null ? "Loading…"
                        : "Balance " + BaseBuyerOrderActivity.rs(d.walletBalance) + " • " + BaseBuyerOrderActivity.rs(d.escrowHeld) + " in escrow",
                null, x -> startActivity(new Intent(requireContext(), BuyerWalletActivity.class)));

        section("Account");
        LinearLayout c2 = TUi.card(content);
        AppCompatActivity a = (AppCompatActivity) requireActivity();
        setting(c2, R.drawable.ic_bell, "Notification Preferences", "Offers, payments and delivery updates", null,
                x -> AccountDialogs.notificationPreferences(a));
        TUi.divider(c2, 10);
        setting(c2, R.drawable.ic_lock_outline, "Password", "Change your password", null, x -> AccountDialogs.changePassword(a));
        TUi.divider(c2, 10);
        setting(c2, R.drawable.ic_help_outline, "Help & Support", "How escrow, delivery and refunds work", null, x -> AccountDialogs.help(a));

        com.google.android.material.button.MaterialButton out = TUi.iconButton(content, "Sign Out", R.drawable.ic_logout, TUi.RED_BG, TUi.RED,
                x -> new AlertDialog.Builder(requireContext())
                        .setTitle("Sign out?")
                        .setPositiveButton("Sign out", (dd, w) -> {
                            session.logoutUser();
                            Intent i = new Intent(requireContext(), com.harvesthub.mobile.ui.common.RoleSelectionActivity.class);
                            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(i);
                            requireActivity().finish();
                        })
                        .setNegativeButton("Cancel", null).show());
        ((LinearLayout.LayoutParams) out.getLayoutParams()).topMargin = dp(24);
        TextView ver = TUi.text(content, "HarvestHub Mobile v" + com.harvesthub.mobile.BuildConfig.VERSION_NAME, 11, TUi.MUTED, false);
        ver.setGravity(Gravity.CENTER);
        ver.setPadding(0, dp(10), 0, 0);
    }

    private void stat(LinearLayout row, String label, String value) {
        LinearLayout c = TUi.col(row, 1f);
        TUi.text(c, label, 11, TUi.MUTED, true).setGravity(Gravity.CENTER);
        TUi.text(c, value, 15, TUi.INK, true).setGravity(Gravity.CENTER);
    }

    private void section(String title) {
        TextView t = TUi.text(content, title.toUpperCase(Locale.US), 11, TUi.INK, true);
        t.setLetterSpacing(0.05f);
        t.setPadding(0, dp(20), 0, 0);
    }

    private void setting(LinearLayout card, int iconRes, String label, String value, @Nullable String tagText, View.OnClickListener click) {
        LinearLayout r = TUi.hrow(card, 0);
        TUi.iconTile(r, iconRes, 40, TUi.GREY_BG, TUi.INK, false);
        LinearLayout t = TUi.col(r, 1f);
        t.setPadding(dp(12), 0, dp(8), 0);
        TUi.text(t, label, 12, TUi.MUTED, true);
        TextView v = TUi.text(t, value, 14, TUi.INK, true);
        v.setMaxLines(2);
        v.setEllipsize(TextUtils.TruncateAt.END);
        if (tagText != null) TUi.tag(r, tagText, TUi.RED_BG, TUi.RED);
        else TUi.icon(r, R.drawable.ic_chevron_right, 18, TUi.INK);
        r.setOnClickListener(click);
    }

    private static String initials(String name) {
        String[] p = name.trim().split("\\s+");
        return (p[0].substring(0, 1) + (p.length > 1 ? p[p.length - 1].substring(0, 1) : "")).toUpperCase(Locale.US);
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
