package com.harvesthub.mobile.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Calendar;

public class TimeTextTest {

    private static Calendar now() {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(2026, Calendar.NOVEMBER, 17, 9, 30);
        return c;
    }

    @Test
    public void chatTimesReadNaturally() {
        assertEquals("05:03", TimeText.chat("2026-11-17T05:03:22", now()));
        assertEquals("05:03", TimeText.chat("2026-11-17T05:03:22.123456789", now()));   // older server values
        assertEquals("Yesterday 18:20", TimeText.chat("2026-11-16T18:20:00", now()));
        assertEquals("14 Nov, 07:05", TimeText.chat("2026-11-14T07:05:00", now()));
        assertEquals("30 Dec 2025, 23:59", TimeText.chat("2025-12-30T23:59:00", now()));
        assertEquals("", TimeText.chat(null, now()));
    }
}
