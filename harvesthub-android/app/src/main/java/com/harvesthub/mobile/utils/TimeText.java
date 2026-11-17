package com.harvesthub.mobile.utils;

import java.util.Calendar;
import java.util.Locale;

/**
 * Message times as people read them. Server times are ISO local date-times in Sri Lanka time
 * ("2026-11-17T05:03:22", older messages may carry fractions of a second).
 */
public final class TimeText {

    private static final String[] MONTHS = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

    private TimeText() {
    }

    /** "05:03" today, "Yesterday 18:20", "16 Nov, 18:20", "16 Nov 2025, 18:20". Empty for a missing time. */
    public static String chat(String iso) {
        return chat(iso, Calendar.getInstance());
    }

    static String chat(String iso, Calendar now) {
        Calendar c = parse(iso);
        if (c == null) return "";
        String hm = String.format(Locale.US, "%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
        if (sameDay(c, now)) return hm;
        Calendar y = (Calendar) now.clone();
        y.add(Calendar.DAY_OF_YEAR, -1);
        if (sameDay(c, y)) return "Yesterday " + hm;
        String day = c.get(Calendar.DAY_OF_MONTH) + " " + MONTHS[c.get(Calendar.MONTH)];
        if (c.get(Calendar.YEAR) != now.get(Calendar.YEAR)) day += " " + c.get(Calendar.YEAR);
        return day + ", " + hm;
    }

    static Calendar parse(String iso) {
        if (iso == null || iso.length() < 16) return null;
        try {
            String s = iso.replace(' ', 'T');
            Calendar c = Calendar.getInstance();
            c.clear();
            c.set(Integer.parseInt(s.substring(0, 4)), Integer.parseInt(s.substring(5, 7)) - 1, Integer.parseInt(s.substring(8, 10)),
                    Integer.parseInt(s.substring(11, 13)), Integer.parseInt(s.substring(14, 16)));
            return c;
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean sameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }
}
