package com.harvesthub.desktop.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class FmtTest {

    @Test
    void moneyAndQuantitiesNeverInventValues() {
        assertEquals("Rs. 75,000", Fmt.money(75000.0));
        assertEquals("Rs. 1,234.50", Fmt.money(1234.5));
        assertEquals("- Rs. 12,540", Fmt.money(-12540.0));
        assertEquals(Fmt.NONE, Fmt.money(null));
        assertEquals("450 kg", Fmt.kg(450.0));
        assertEquals("Rs. 120 / kg", Fmt.perKg(120.0));
        assertEquals("850 m", Fmt.metres(850.0));
        assertEquals("1.5 km", Fmt.metres(1500.0));
        assertEquals(Fmt.NONE, Fmt.kg(null));
    }

    @Test
    void datesFromEveryServerFormat() {
        assertEquals("14 Nov 2026", Fmt.date("2026-11-14T21:42:01.6289887"));
        assertEquals("15 Nov 2026, 8:05 AM", Fmt.dateTime("2026-11-15 08:05:00"));
        assertEquals("15 Nov 2026", Fmt.date("2026-11-15"));
        assertEquals(Fmt.NONE, Fmt.date("not a date"));
        assertEquals(Fmt.NONE, Fmt.date(null));
    }

    @Test
    void relativeTimes() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 15, 18, 0);
        assertEquals("just now", Fmt.ago("2026-11-15T17:59:40", now));
        assertEquals("12 min ago", Fmt.ago("2026-11-15T17:48:00", now));
        assertEquals("3 h ago", Fmt.ago("2026-11-15T15:00:00", now));
        assertEquals("yesterday, 4:10 PM", Fmt.ago("2026-11-14T16:10:00", now));
        assertEquals("4 days ago", Fmt.ago("2026-11-11T09:00:00", now));
        assertEquals("1 Nov 2026", Fmt.ago("2026-11-01T09:00:00", now));
    }

    @Test
    void labelsForRolesStatusesAndDecisions() {
        assertEquals("Agricultural Officer", Fmt.role("OFFICER"));
        assertEquals("Agricultural Officer", Fmt.role("ADMIN"), "no admin role is shown");
        assertEquals("Transporter", Fmt.role("TRANSPORTER"));
        assertEquals("Rejected on delivery – returning", Fmt.orderStatus("RETURN_JOURNEY"));
        assertEquals("Buyer checking loading photos", Fmt.orderStatus("AWAITING_BUYER_LOADING_APPROVAL"));
        assertEquals("Some new status", Fmt.orderStatus("SOME_NEW_STATUS"));
        assertEquals("Retake requested", Fmt.decision("RETAKE"));
        assertEquals("KP", Fmt.initials("Kamal  Perera"));
        assertEquals("NI", Fmt.initials("Nimal"));
    }

    @Test
    void jsonAccessorsHandleMissingAndNull() throws Exception {
        var n = new ObjectMapper().readTree("{\"a\":\"x\",\"b\":null,\"c\":\"\",\"q\":450,\"s\":\"450\",\"f\":true}");
        assertEquals("x", Fmt.text(n, "a"));
        assertNull(Fmt.text(n, "b"));
        assertNull(Fmt.text(n, "c"));
        assertNull(Fmt.text(n, "missing"));
        assertEquals(450.0, Fmt.num(n, "q"));
        assertNull(Fmt.num(n, "s"), "a number sent as text is not guessed");
        assertTrue(Fmt.bool(n, "f"));
        assertEquals(0, Fmt.count(n, "missing"));
    }
}
