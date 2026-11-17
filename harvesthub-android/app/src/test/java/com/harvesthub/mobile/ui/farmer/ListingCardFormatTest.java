package com.harvesthub.mobile.ui.farmer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Listing card texts: real created date, "updated ... ago" and the short place name. */
public class ListingCardFormatTest {

    private static String isoMinutesAgo(long minutes) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("Asia/Colombo"));   // server times are Sri Lanka local time
        return f.format(new Date(System.currentTimeMillis() - minutes * 60_000L));
    }

    @Test
    public void updatedShowsRealElapsedTime() {
        assertEquals("Just now", ListingAdapter.formatAgo(isoMinutesAgo(0)));
        assertEquals("15m ago", ListingAdapter.formatAgo(isoMinutesAgo(15)));
        assertEquals("3h ago", ListingAdapter.formatAgo(isoMinutesAgo(3 * 60 + 5)));
        assertEquals("Yesterday", ListingAdapter.formatAgo(isoMinutesAgo(26 * 60)));
        assertEquals("—", ListingAdapter.formatAgo(null));
    }

    @Test
    public void createdDateIsTheRealDate() {
        assertEquals("Nov 16, 2025", ListingAdapter.formatDate("2025-11-16T22:27:43"));
        assertEquals("—", ListingAdapter.formatDate(""));
    }

    @Test
    public void placeIsTheTownOfTheFarmAddress() {
        assertEquals("Mihintale", ListingAdapter.shortPlace("Senate Building, Mihintale 50300, Sri Lanka"));
        assertEquals("Mihintale", ListingAdapter.shortPlace("9G42+JP5, Mihintale, Sri Lanka"));
        assertEquals("Dambulla", ListingAdapter.shortPlace("Dambulla"));
        assertEquals("GPS pinned", ListingAdapter.shortPlace("Lat 8.35523, Lng 80.50242"));
        assertEquals("Sri Lanka", ListingAdapter.shortPlace(null));
    }
}
