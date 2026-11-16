package com.harvesthub.desktop.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiClientTest {

    @Test
    void queryStringSkipsBlankValuesAndEncodes() {
        assertEquals("/api/queue", ApiClient.query("/api/queue", "tab", null, "search", " "));
        assertEquals("/api/queue?tab=all&search=Big+Onions", ApiClient.query("/api/queue", "tab", "all", "search", "Big Onions"));
        assertEquals("/api/review-history?from=2026-11-01&to=2026-11-15",
                ApiClient.query("/api/review-history", "from", "2026-11-01", "crop", null, "to", "2026-11-15"));
    }

    @Test
    void absoluteUrlsForUploads() {
        String base = ApiClient.getBaseUrl();
        assertEquals(base + "/uploads/a.jpg", ApiClient.absolute("/uploads/a.jpg"));
        assertEquals(base + "/uploads/a.jpg", ApiClient.absolute("uploads/a.jpg"));
        assertEquals("https://x.lk/a.jpg", ApiClient.absolute("https://x.lk/a.jpg"));
        assertNull(ApiClient.absolute(null));
        assertEquals("https://harvesthubapp.tech", ApiClient.cleanUrl(" https://harvesthubapp.tech// "));
    }

    @Test
    void serverErrorMessagesArePassedThroughReadably() {
        assertEquals("This listing is already being reviewed by Nadeesha.",
                ApiClient.errorMessage(409, "{\"error\":\"This listing is already being reviewed by Nadeesha.\"}"));
        assertEquals("Invalid credentials", ApiClient.errorMessage(401, "{\"error\":\"Invalid credentials\"}"));
        assertEquals("Your session has ended. Please sign in again.", ApiClient.errorMessage(401, ""));
        assertTrue(ApiClient.errorMessage(502, "<html>Bad gateway</html>").contains("HTTP 502"));
        assertEquals("Not found. It may have been removed.", ApiClient.errorMessage(404, "{\"message\":\"Request failed\"}"));
    }

    @Test
    void onlyOfficersMayUseThePortal() {
        assertTrue(Session.mayUsePortal("OFFICER"));
        assertTrue(Session.mayUsePortal("officer"));
        assertFalse(Session.mayUsePortal("FARMER"));
        assertFalse(Session.mayUsePortal("BUYER"));
        assertFalse(Session.mayUsePortal("TRANSPORTER"));
        assertFalse(Session.mayUsePortal(null));
    }
}
