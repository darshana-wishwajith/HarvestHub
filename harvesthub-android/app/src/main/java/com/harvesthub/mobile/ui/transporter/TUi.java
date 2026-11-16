package com.harvesthub.mobile.ui.transporter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.harvesthub.mobile.R;
import com.harvesthub.mobile.ui.buyer.BaseBuyerOrderActivity;
import com.harvesthub.mobile.utils.RemoteImage;

import java.util.List;
import java.util.Locale;

/**
 * Small view kit for the transporter screens (Figma page 612:58): white rounded cards, label/value rows,
 * pills, step rows, photo strips and brand buttons (#1B4332 / #4CAF50 / #E8F5EA). Screens put real data into
 * these blocks inside the shared {@code transporter_scaffold} layout.
 */
public final class TUi {

    public static final int DARK = Color.parseColor("#1B4332");
    public static final int ACCENT = Color.parseColor("#4CAF50");
    public static final int SURFACE = Color.parseColor("#E8F5EA");
    public static final int INK = Color.parseColor("#111827");
    public static final int MUTED = Color.parseColor("#64748B");
    public static final int LINE = Color.parseColor("#E2E8F0");
    public static final int RED = Color.parseColor("#B91C1C");
    public static final int RED_BG = Color.parseColor("#FDE2E2");
    public static final int AMBER = Color.parseColor("#92400E");
    public static final int AMBER_BG = Color.parseColor("#FEF3C7");
    public static final int GREY_BG = Color.parseColor("#F1F5F9");

    private TUi() {}

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics()));
    }

    public static String rs(double v) {
        return BaseBuyerOrderActivity.rs(v);
    }

    public static String signedRs(double v) {
        return (v >= 0 ? "+" : "-") + BaseBuyerOrderActivity.rs(Math.abs(v));
    }

    public static String km(double v) {
        return String.format(Locale.US, v >= 100 ? "%.0f km" : "%.1f km", v);
    }

    /** Rounded white card; returns its inner vertical container. */
    public static LinearLayout card(ViewGroup parent) {
        return card(parent, Color.WHITE, LINE);
    }

    public static LinearLayout card(ViewGroup parent, int bg, int stroke) {
        Context c = parent.getContext();
        MaterialCardView card = new MaterialCardView(c);
        card.setCardBackgroundColor(bg);
        card.setRadius(dp(c, 18));
        card.setCardElevation(0);
        card.setStrokeColor(stroke);
        card.setStrokeWidth(dp(c, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, 14);
        parent.addView(card, lp);
        LinearLayout inner = new LinearLayout(c);
        inner.setOrientation(LinearLayout.VERTICAL);
        int p = dp(c, 16);
        inner.setPadding(p, p, p, p);
        card.addView(inner);
        return inner;
    }

    public static TextView text(ViewGroup parent, CharSequence s, float sp, int color, boolean bold) {
        Context c = parent.getContext();
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        parent.addView(t, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return t;
    }

    public static TextView title(ViewGroup parent, CharSequence s) {
        return text(parent, s, 18, DARK, true);
    }

    public static TextView body(ViewGroup parent, CharSequence s) {
        TextView t = text(parent, s, 14, MUTED, false);
        ((LinearLayout.LayoutParams) t.getLayoutParams()).topMargin = dp(parent.getContext(), 4);
        return t;
    }

    /** Small all-caps section label. */
    public static TextView caption(ViewGroup parent, CharSequence s) {
        TextView t = text(parent, s.toString().toUpperCase(Locale.US), 11, MUTED, true);
        t.setLetterSpacing(0.06f);
        ((LinearLayout.LayoutParams) t.getLayoutParams()).topMargin = dp(parent.getContext(), 10);
        return t;
    }

    /** Big money / number line. */
    public static TextView big(ViewGroup parent, CharSequence s, int color) {
        TextView t = text(parent, s, 30, color, true);
        ((LinearLayout.LayoutParams) t.getLayoutParams()).topMargin = dp(parent.getContext(), 2);
        return t;
    }

    /** "label ........ value" row. */
    public static TextView row(ViewGroup parent, CharSequence label, CharSequence value) {
        return row(parent, label, value, INK, false);
    }

    public static TextView row(ViewGroup parent, CharSequence label, CharSequence value, int valueColor, boolean strong) {
        Context c = parent.getContext();
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0, dp(c, 6), 0, dp(c, 6));
        TextView l = new TextView(c);
        l.setText(label);
        l.setTextSize(14);
        l.setTextColor(strong ? INK : MUTED);
        if (strong) l.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        r.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView v = new TextView(c);
        v.setText(value);
        v.setTextSize(strong ? 17 : 14);
        v.setTextColor(valueColor);
        v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setGravity(Gravity.END);
        r.addView(v, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        parent.addView(r);
        return v;
    }

    /** Rounded pill (badge). */
    public static TextView pill(ViewGroup parent, CharSequence s, int bg, int fg) {
        Context c = parent.getContext();
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(12);
        t.setTextColor(fg);
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setPadding(dp(c, 10), dp(c, 4), dp(c, 10), dp(c, 4));
        GradientDrawable d = new GradientDrawable();
        d.setColor(bg);
        d.setCornerRadius(dp(c, 50));
        t.setBackground(d);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, 6);
        parent.addView(t, lp);
        return t;
    }

    /** Coloured banner card (status). */
    public static LinearLayout banner(ViewGroup parent, int bg, int fg, CharSequence title, @Nullable CharSequence sub) {
        LinearLayout b = card(parent, bg, bg);
        text(b, title, 17, fg, true);
        if (sub != null) {
            TextView s = text(b, sub, 13, fg, false);
            ((LinearLayout.LayoutParams) s.getLayoutParams()).topMargin = dp(parent.getContext(), 4);
        }
        return b;
    }

    /** Pickup ● → drop-off ● block. */
    public static void route(ViewGroup parent, String fromLabel, String from, String toLabel, String to) {
        LinearLayout box = new LinearLayout(parent.getContext());
        box.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable d = new GradientDrawable();
        d.setColor(GREY_BG);
        d.setCornerRadius(dp(parent.getContext(), 12));
        box.setBackground(d);
        int p = dp(parent.getContext(), 12);
        box.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(parent.getContext(), 10);
        parent.addView(box, lp);
        text(box, "●  " + fromLabel.toUpperCase(Locale.US), 11, MUTED, true);
        text(box, from == null || from.isEmpty() ? "—" : from, 15, INK, true);
        TextView t = text(box, "●  " + toLabel.toUpperCase(Locale.US), 11, MUTED, true);
        ((LinearLayout.LayoutParams) t.getLayoutParams()).topMargin = dp(parent.getContext(), 8);
        text(box, to == null || to.isEmpty() ? "—" : to, 15, INK, true);
    }

    /** Checklist step: ✓ done / ● current / ○ pending, with an optional pill on the right. */
    public static void step(ViewGroup parent, String label, int state, @Nullable String pillText) {
        Context c = parent.getContext();
        LinearLayout r = new LinearLayout(c);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0, dp(c, 8), 0, dp(c, 8));
        TextView icon = new TextView(c);
        icon.setText(state == 2 ? "✓" : state == 1 ? "●" : "○");
        icon.setTextSize(16);
        icon.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        icon.setTextColor(state == 2 ? ACCENT : state == 1 ? DARK : MUTED);
        icon.setWidth(dp(c, 28));
        r.addView(icon);
        TextView l = new TextView(c);
        l.setText(label);
        l.setTextSize(15);
        l.setTextColor(state == 0 ? MUTED : INK);
        if (state == 1) l.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        r.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        parent.addView(r);
        if (pillText != null) {
            TextView p = pill(r, pillText, state == 2 ? SURFACE : state == 1 ? RED_BG : GREY_BG, state == 2 ? DARK : state == 1 ? RED : MUTED);
            ((LinearLayout.LayoutParams) p.getLayoutParams()).topMargin = 0;
        }
    }

    /** Horizontal strip of remote photos (stored media paths). */
    public static void photos(ViewGroup parent, @Nullable List<String> paths) {
        Context c = parent.getContext();
        if (paths == null || paths.isEmpty()) {
            body(parent, "No photos.");
            return;
        }
        HorizontalScrollView hs = new HorizontalScrollView(c);
        LinearLayout strip = new LinearLayout(c);
        hs.addView(strip);
        for (String p : paths) {
            ImageView iv = new ImageView(c);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setBackgroundColor(GREY_BG);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(c, 150), dp(c, 110));
            lp.rightMargin = dp(c, 8);
            strip.addView(iv, lp);
            RemoteImage.load(iv, BaseBuyerOrderActivity.mediaUrl(p));
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, 10);
        parent.addView(hs, lp);
    }

    public static MaterialButton primary(ViewGroup parent, CharSequence s, View.OnClickListener l) {
        return button(parent, s, DARK, Color.WHITE, l);
    }

    public static MaterialButton secondary(ViewGroup parent, CharSequence s, View.OnClickListener l) {
        return button(parent, s, Color.parseColor("#E5E7EB"), INK, l);
    }

    public static MaterialButton danger(ViewGroup parent, CharSequence s, View.OnClickListener l) {
        return button(parent, s, RED_BG, RED, l);
    }

    public static MaterialButton button(ViewGroup parent, CharSequence s, int bg, int fg, View.OnClickListener l) {
        Context c = parent.getContext();
        MaterialButton b = new MaterialButton(c);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextSize(16);
        b.setTextColor(fg);
        b.setBackgroundTintList(ColorStateList.valueOf(bg));
        b.setCornerRadius(dp(c, 14));
        b.setInsetTop(0);
        b.setInsetBottom(0);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 54));
        lp.topMargin = dp(c, 10);
        parent.addView(b, lp);
        return b;
    }

    /** Toggle chips; selected ones are dark. Returns the chips in order. */
    public static java.util.List<TextView> chips(ViewGroup parent, String[] labels, boolean multi, @Nullable Runnable onChange) {
        Context c = parent.getContext();
        HorizontalScrollView hs = new HorizontalScrollView(c);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(c);
        hs.addView(row);
        java.util.List<TextView> out = new java.util.ArrayList<>();
        for (String s : labels) {
            TextView t = new TextView(c);
            t.setText(s);
            t.setTextSize(13);
            t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            t.setPadding(dp(c, 14), dp(c, 8), dp(c, 14), dp(c, 8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(c, 8);
            row.addView(t, lp);
            out.add(t);
            setChip(t, false);
            t.setOnClickListener(v -> {
                boolean on = !Boolean.TRUE.equals(v.getTag());
                if (!multi) for (TextView o : out) setChip(o, false);
                setChip((TextView) v, multi ? on : true);
                if (onChange != null) onChange.run();
            });
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, 10);
        parent.addView(hs, lp);
        return out;
    }

    public static void setChip(TextView t, boolean on) {
        t.setTag(on);
        GradientDrawable d = new GradientDrawable();
        d.setColor(on ? DARK : Color.parseColor("#EEEEEE"));
        d.setCornerRadius(dp(t.getContext(), 50));
        t.setBackground(d);
        t.setTextColor(on ? Color.WHITE : INK);
        String s = t.getText().toString().replace("✓ ", "");
        t.setText(on ? "✓ " + s : s);
    }

    public static boolean isOn(TextView chip) {
        return Boolean.TRUE.equals(chip.getTag());
    }

    public static String chipText(TextView chip) {
        return chip.getText().toString().replace("✓ ", "");
    }

    public static EditText input(ViewGroup parent, String hint, boolean multiline) {
        Context c = parent.getContext();
        EditText e = new EditText(c);
        e.setHint(hint);
        e.setTextSize(15);
        GradientDrawable d = new GradientDrawable();
        d.setColor(GREY_BG);
        d.setCornerRadius(dp(c, 12));
        e.setBackground(d);
        int p = dp(c, 12);
        e.setPadding(p, p, p, p);
        if (multiline) {
            e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            e.setMinLines(3);
            e.setGravity(Gravity.TOP | Gravity.START);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, 8);
        parent.addView(e, lp);
        return e;
    }

    // ---------------------------------------------------------------- scaffold

    public enum Tab { HOME, JOBS, HISTORY, SETTINGS }

    /**
     * Sets up {@code transporter_scaffold}: title, back arrow (null tab) or logo header, and the bottom bar.
     * Returns the scrolling content container.
     */
    public static LinearLayout scaffold(Activity a, String title, @Nullable Tab tab) {
        a.setContentView(R.layout.transporter_scaffold);
        TextView t = a.findViewById(R.id.tvTpTitle);
        t.setText(title);
        View back = a.findViewById(R.id.btnBack);
        back.setVisibility(tab == null ? View.VISIBLE : View.GONE);
        back.setOnClickListener(v -> a.finish());
        a.findViewById(R.id.ivTpLogo).setVisibility(tab == null ? View.GONE : View.VISIBLE);
        View bell = a.findViewById(R.id.btnTpBell);
        bell.setOnClickListener(v -> a.startActivity(new Intent(a, com.harvesthub.mobile.ui.farmer.NotificationsActivity.class)));
        bindNav(a, tab == null ? Tab.JOBS : tab);
        return a.findViewById(R.id.tpContent);
    }

    /** Fixed bottom area above the nav bar (main action buttons). */
    public static LinearLayout actions(Activity a) {
        LinearLayout l = a.findViewById(R.id.tpActions);
        l.setVisibility(View.VISIBLE);
        return l;
    }

    private static void bindNav(Activity a, Tab current) {
        int[] ids = {R.id.navTpHome, R.id.navTpJobs, R.id.navTpHistory, R.id.navTpSettings};
        Class<?>[] targets = {DeliveryDashboardActivity.class, TransporterJobsActivity.class, JobHistoryActivity.class,
                TransporterSettingsActivity.class};
        for (int i = 0; i < ids.length; i++) {
            TextView item = a.findViewById(ids[i]);
            boolean on = current.ordinal() == i;
            GradientDrawable d = new GradientDrawable();
            d.setColor(on ? DARK : Color.TRANSPARENT);
            d.setCornerRadius(dp(a, 14));
            item.setBackground(d);
            item.setTextColor(on ? Color.WHITE : INK);
            if (item.getCompoundDrawablesRelative()[1] != null) {
                item.getCompoundDrawablesRelative()[1].mutate().setTint(on ? Color.WHITE : INK);
            }
            Class<?> target = targets[i];
            item.setOnClickListener(v -> {
                if (a.getClass() == target) return;
                Intent intent = new Intent(a, target);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                a.startActivity(intent);
                a.overridePendingTransition(0, 0);
            });
        }
    }
}
