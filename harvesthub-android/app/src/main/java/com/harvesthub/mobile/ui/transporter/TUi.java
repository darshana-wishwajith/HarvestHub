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
    public static final int GREY_BG = Color.parseColor("#F3F0EE");
    public static final int MINT = Color.parseColor("#B9F2C4");
    public static final int MINT_INK = Color.parseColor("#14532D");
    public static final int PAGE = Color.parseColor("#FDF8F6");
    public static final int BLUE_BG = Color.parseColor("#EEF4FF");

    private static Typeface boldFace, semiFace;

    /** Plus Jakarta Sans Bold (Typeface.DEFAULT_BOLD would fall back to Roboto). */
    public static Typeface bold(Context c) {
        if (boldFace == null) {
            try {
                boldFace = androidx.core.content.res.ResourcesCompat.getFont(c, R.font.plus_jakarta_sans_bold);
            } catch (Exception ignored) { }
            if (boldFace == null) boldFace = Typeface.DEFAULT_BOLD;
        }
        return boldFace;
    }

    private static Typeface regularFace;

    public static Typeface regular(Context c) {
        if (regularFace == null) {
            try {
                regularFace = androidx.core.content.res.ResourcesCompat.getFont(c, R.font.plus_jakarta_sans);
            } catch (Exception ignored) { }
            if (regularFace == null) regularFace = Typeface.DEFAULT;
        }
        return regularFace;
    }

    public static Typeface semi(Context c) {
        if (semiFace == null) {
            try {
                semiFace = androidx.core.content.res.ResourcesCompat.getFont(c, R.font.plus_jakarta_sans_semibold);
            } catch (Exception ignored) { }
            if (semiFace == null) semiFace = bold(c);
        }
        return semiFace;
    }

    public static GradientDrawable round(int color, float radiusPx) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radiusPx);
        return d;
    }

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
        card.setRadius(dp(c, 16));
        boolean plain = bg == Color.WHITE && stroke == LINE;
        card.setCardElevation(plain ? dp(c, 1.5f) : 0);
        card.setStrokeColor(stroke);
        card.setStrokeWidth(plain ? 0 : dp(c, 1));
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
        if (bold) t.setTypeface(bold(c));
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
        l.setTypeface(strong ? bold(c) : semi(c));
        r.addView(l, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView v = new TextView(c);
        v.setText(value);
        v.setTextSize(strong ? 17 : 14);
        v.setTextColor(valueColor);
        v.setTypeface(bold(c));
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
        t.setTypeface(bold(c));
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
        icon.setTypeface(bold(c));
        icon.setTextColor(state == 2 ? ACCENT : state == 1 ? DARK : MUTED);
        icon.setWidth(dp(c, 28));
        r.addView(icon);
        TextView l = new TextView(c);
        l.setText(label);
        l.setTextSize(15);
        l.setTextColor(state == 0 ? MUTED : INK);
        if (state == 1) l.setTypeface(bold(c));
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
        return button(parent, s, Color.parseColor("#E9E7E5"), INK, l);
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
        b.setTypeface(bold(c));
        b.setLetterSpacing(0f);
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
            t.setTypeface(bold(c));
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
        d.setColor(on ? DARK : Color.parseColor("#ECEAE8"));
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

    // ---------------------------------------------------------------- Figma building blocks

    /** Horizontal, vertically centred row. */
    public static LinearLayout hrow(ViewGroup parent, int topMarginDp) {
        Context c = parent.getContext();
        LinearLayout r = new LinearLayout(c);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, topMarginDp);
        parent.addView(r, lp);
        return r;
    }

    /** Vertical column inside a row; weight 0 = wrap. */
    public static LinearLayout col(LinearLayout row, float weight) {
        LinearLayout v = new LinearLayout(row.getContext());
        v.setOrientation(LinearLayout.VERTICAL);
        row.addView(v, weight > 0 ? new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight)
                : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return v;
    }

    /** Text that wraps its own width (for rows). */
    public static TextView label(ViewGroup parent, CharSequence s, float sp, int color, boolean bold) {
        TextView t = text(parent, s, sp, color, bold);
        t.getLayoutParams().width = ViewGroup.LayoutParams.WRAP_CONTENT;
        return t;
    }

    /** Text that takes the remaining width of a row. */
    public static TextView fill(LinearLayout row, CharSequence s, float sp, int color, boolean bold) {
        TextView t = text(row, s, sp, color, bold);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return t;
    }

    /** Tinted inner box (Figma grey panels). */
    public static LinearLayout box(ViewGroup parent, int bg, int topMarginDp) {
        Context c = parent.getContext();
        LinearLayout b = new LinearLayout(c);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setBackground(round(bg, dp(c, 12)));
        int p = dp(c, 12);
        b.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, topMarginDp);
        parent.addView(b, lp);
        return b;
    }

    public static ImageView icon(ViewGroup parent, int res, int sizeDp, int tint) {
        Context c = parent.getContext();
        ImageView iv = new ImageView(c);
        iv.setImageResource(res);
        if (tint != 0) iv.setColorFilter(tint);
        parent.addView(iv, new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
        return iv;
    }

    /** Icon on a rounded-square (or round) coloured tile. */
    public static android.widget.FrameLayout iconTile(ViewGroup parent, int res, int sizeDp, int bg, int tint, boolean circle) {
        Context c = parent.getContext();
        android.widget.FrameLayout f = new android.widget.FrameLayout(c);
        f.setBackground(round(bg, dp(c, circle ? sizeDp : 12)));
        ImageView iv = new ImageView(c);
        iv.setImageResource(res);
        if (tint != 0) iv.setColorFilter(tint);
        int in = dp(c, sizeDp * 0.5f);
        f.addView(iv, new android.widget.FrameLayout.LayoutParams(in, in, Gravity.CENTER));
        parent.addView(f, new LinearLayout.LayoutParams(dp(c, sizeDp), dp(c, sizeDp)));
        return f;
    }

    /** Pill without top margin, for use inside rows. */
    public static TextView tag(ViewGroup parent, CharSequence s, int bg, int fg) {
        TextView t = pill(parent, s, bg, fg);
        ((LinearLayout.LayoutParams) t.getLayoutParams()).topMargin = 0;
        t.setTextSize(11);
        return t;
    }

    /** Card heading with an optional pill on the right; returns the pill (or null). */
    public static TextView header(ViewGroup parent, CharSequence title, @Nullable CharSequence pillText, int pillBg, int pillFg) {
        LinearLayout r = hrow(parent, 0);
        fill(r, title, 17, DARK, true);
        return pillText == null ? null : tag(r, pillText, pillBg, pillFg);
    }

    /** Heading with an icon in front (Figma "Manifest", "Pre-Loading Sequence"...). */
    public static TextView iconHeader(ViewGroup parent, int iconRes, CharSequence title, @Nullable CharSequence pillText, int pillBg, int pillFg) {
        Context c = parent.getContext();
        LinearLayout r = hrow(parent, 0);
        icon(r, iconRes, 20, DARK);
        TextView t = fill(r, title, 18, DARK, true);
        ((LinearLayout.LayoutParams) t.getLayoutParams()).leftMargin = dp(c, 8);
        return pillText == null ? null : tag(r, pillText, pillBg, pillFg);
    }

    /** Section heading outside cards ("Current Job", "Recent Payouts") with an optional right label. */
    public static TextView sectionTitle(ViewGroup parent, CharSequence title, @Nullable CharSequence right) {
        LinearLayout r = hrow(parent, 20);
        fill(r, title, 17, INK, true);
        return right == null ? null : label(r, right, 13, DARK, true);
    }

    /** Row of equal grey tiles; add tiles with {@link #tile}. */
    public static LinearLayout tiles(ViewGroup parent) {
        return hrow(parent, 10);
    }

    /** One grey tile: small label, bold value, optional sub line, optional icon. */
    public static LinearLayout tile(LinearLayout row, int iconRes, CharSequence labelText, CharSequence value, @Nullable CharSequence sub) {
        Context c = row.getContext();
        LinearLayout t = new LinearLayout(c);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setBackground(round(GREY_BG, dp(c, 12)));
        int p = dp(c, 12);
        t.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        if (row.getChildCount() > 0) lp.leftMargin = dp(c, 8);
        row.addView(t, lp);
        if (iconRes != 0) {
            android.widget.FrameLayout f = iconTile(t, iconRes, 32, Color.WHITE, INK, true);
            ((LinearLayout.LayoutParams) f.getLayoutParams()).rightMargin = dp(c, 8);
        }
        LinearLayout v = col(t, 1f);
        TextView l = text(v, labelText, 11, MUTED, false);
        l.setTypeface(semi(c));
        text(v, value, 15, INK, true);
        if (sub != null) text(v, sub, 11, MUTED, false);
        return t;
    }

    /** Stops joined by a line: each stop is {LABEL, title, sub}. First dots dark, last dot green. */
    public static void timeline(ViewGroup parent, String[]... stops) {
        Context c = parent.getContext();
        for (int i = 0; i < stops.length; i++) {
            boolean last = i == stops.length - 1;
            LinearLayout r = new LinearLayout(c);
            r.setOrientation(LinearLayout.HORIZONTAL);
            parent.addView(r, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            LinearLayout rail = new LinearLayout(c);
            rail.setOrientation(LinearLayout.VERTICAL);
            rail.setGravity(Gravity.CENTER_HORIZONTAL);
            r.addView(rail, new LinearLayout.LayoutParams(dp(c, 22), ViewGroup.LayoutParams.MATCH_PARENT));
            View dot = new View(c);
            dot.setBackground(round(last ? Color.parseColor("#15803D") : DARK, dp(c, 10)));
            LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(dp(c, 11), dp(c, 11));
            dl.topMargin = dp(c, 3);
            rail.addView(dot, dl);
            if (!last) {
                View line = new View(c);
                line.setBackgroundColor(Color.parseColor("#C9C5C2"));
                rail.addView(line, new LinearLayout.LayoutParams(dp(c, 2), 0, 1f));
            }
            LinearLayout v = col(r, 1f);
            v.setPadding(dp(c, 6), 0, 0, dp(c, last ? 0 : 12));
            String[] s = stops[i];
            TextView cap = text(v, s[0].toUpperCase(Locale.US), 11, last ? Color.parseColor("#15803D") : MUTED, true);
            cap.setLetterSpacing(0.04f);
            text(v, s[1] == null || s[1].isEmpty() ? "—" : s[1], 15, INK, true);
            if (s.length > 2 && s[2] != null && !s[2].isEmpty()) text(v, s[2], 12, MUTED, false);
        }
    }

    /** White check row inside a sequence box: icon, label, status pill. state 2 done, 1 current, 0 pending. */
    public static LinearLayout checkRow(ViewGroup parent, CharSequence labelText, int state, @Nullable CharSequence pillText) {
        Context c = parent.getContext();
        LinearLayout r = hrow(parent, 8);
        r.setBackground(round(Color.WHITE, dp(c, 12)));
        int p = dp(c, 12);
        r.setPadding(p, p, p, p);
        if (state == 2) icon(r, R.drawable.ic_check_circle_green, 20, Color.parseColor("#15803D"));
        else iconTile(r, state == 1 ? R.drawable.ic_camera_add : R.drawable.ic_clock, state == 1 ? 34 : 22,
                state == 1 ? DARK : GREY_BG, state == 1 ? Color.WHITE : MUTED, false);
        TextView l = fill(r, labelText, 14, state == 0 ? MUTED : state == 1 ? DARK : INK, state == 1);
        ((LinearLayout.LayoutParams) l.getLayoutParams()).leftMargin = dp(c, 10);
        if (pillText != null) tag(r, pillText, state == 2 ? MINT : state == 1 ? RED_BG : GREY_BG, state == 2 ? MINT_INK : state == 1 ? RED : MUTED);
        return r;
    }

    /** Numbered instruction card (Figma "Mandatory Driver Actions"); returns the text column for extra buttons. */
    public static LinearLayout numStep(ViewGroup parent, int number, CharSequence title, CharSequence bodyText, boolean alert) {
        Context c = parent.getContext();
        LinearLayout r = new LinearLayout(c);
        r.setBackground(round(Color.WHITE, dp(c, 12)));
        int p = dp(c, 12);
        r.setPadding(p, p, p, p);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(c, 8);
        parent.addView(r, lp);
        TextView n = new TextView(c);
        n.setText(String.valueOf(number));
        n.setGravity(Gravity.CENTER);
        n.setTextSize(12);
        n.setTypeface(bold(c));
        n.setTextColor(alert ? RED : MINT_INK);
        n.setBackground(round(alert ? RED_BG : MINT, dp(c, 20)));
        r.addView(n, new LinearLayout.LayoutParams(dp(c, 26), dp(c, 26)));
        LinearLayout v = col(r, 1f);
        v.setPadding(dp(c, 10), 0, 0, 0);
        text(v, title, 15, alert ? RED : INK, true);
        text(v, bodyText, 13, MUTED, false);
        return v;
    }

    /** Coloured notice with a round icon, title and body (Figma "Smart Contract Notice", "Escrow ... Protected"). */
    public static LinearLayout notice(ViewGroup parent, int iconRes, int bg, int iconBg, CharSequence title, CharSequence bodyText) {
        Context c = parent.getContext();
        LinearLayout card = card(parent, bg, bg);
        LinearLayout r = hrow(card, 0);
        r.setGravity(Gravity.TOP);
        iconTile(r, iconRes, 38, iconBg, Color.WHITE, true);
        LinearLayout v = col(r, 1f);
        v.setPadding(dp(c, 12), 0, 0, 0);
        text(v, title, 15, bg == RED_BG ? RED : DARK, true);
        TextView b = text(v, bodyText, 13, bg == RED_BG ? RED : Color.parseColor("#374151"), false);
        ((LinearLayout.LayoutParams) b.getLayoutParams()).topMargin = dp(c, 2);
        return v;
    }

    /** Button with a leading icon. */
    public static MaterialButton iconButton(ViewGroup parent, CharSequence s, int iconRes, int bg, int fg, View.OnClickListener l) {
        MaterialButton b = button(parent, s, bg, fg, l);
        b.setIconResource(iconRes);
        b.setIconTint(ColorStateList.valueOf(fg));
        b.setIconGravity(MaterialButton.ICON_GRAVITY_TEXT_START);
        b.setIconPadding(dp(parent.getContext(), 8));
        b.setIconSize(dp(parent.getContext(), 20));
        return b;
    }

    /** Small inline button (inside cards). */
    public static MaterialButton smallButton(ViewGroup parent, CharSequence s, int iconRes, int bg, int fg, View.OnClickListener l) {
        Context c = parent.getContext();
        MaterialButton b = iconRes == 0 ? button(parent, s, bg, fg, l) : iconButton(parent, s, iconRes, bg, fg, l);
        b.setTextSize(13);
        b.setCornerRadius(dp(c, 10));
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(dp(c, 14), 0, dp(c, 14), 0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(c, 38));
        lp.topMargin = dp(c, 8);
        b.setLayoutParams(lp);
        return b;
    }

    /** Centred text link under the main button. */
    public static TextView link(ViewGroup parent, CharSequence s, int color, View.OnClickListener l) {
        Context c = parent.getContext();
        TextView t = text(parent, s, 13, color, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(c, 12), 0, dp(c, 6));
        t.setPaintFlags(t.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
        t.setOnClickListener(l);
        return t;
    }

    /** Thin progress bar, 0–100. */
    public static android.widget.ProgressBar progress(ViewGroup parent, int percent, int topMarginDp) {
        Context c = parent.getContext();
        android.widget.ProgressBar bar = new android.widget.ProgressBar(c, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(Math.max(0, Math.min(100, percent)));
        bar.setProgressTintList(ColorStateList.valueOf(DARK));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#D6D3D1")));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 8));
        lp.topMargin = dp(c, topMarginDp);
        parent.addView(bar, lp);
        return bar;
    }

    public static View divider(ViewGroup parent, int vMarginDp) {
        Context c = parent.getContext();
        View v = new View(c);
        v.setBackgroundColor(Color.parseColor("#E7E5E4"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(c, 1));
        lp.topMargin = dp(c, vMarginDp);
        lp.bottomMargin = dp(c, vMarginDp);
        parent.addView(v, lp);
        return v;
    }

    /** Remote photo with rounded corners. */
    public static ImageView thumb(ViewGroup parent, @Nullable String path, int wDp, int hDp) {
        Context c = parent.getContext();
        ImageView iv = new ImageView(c);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setBackground(round(Color.parseColor("#E5E7EB"), dp(c, 10)));
        iv.setClipToOutline(true);
        parent.addView(iv, new LinearLayout.LayoutParams(dp(c, wDp), dp(c, hDp)));
        if (path != null && !path.isEmpty()) RemoteImage.load(iv, BaseBuyerOrderActivity.mediaUrl(path));
        return iv;
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
