package com.harvesthub.mobile.ui.transporter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class TransporterLoginMobileTest {

    @Test
    public void acceptsCommonSriLankanFormats() {
        assertEquals("0771234567", TransporterLoginActivity.normaliseMobile("077 123 4567"));
        assertEquals("0771234567", TransporterLoginActivity.normaliseMobile("0771234567"));
        assertEquals("0771234567", TransporterLoginActivity.normaliseMobile("771234567"));
        assertEquals("0771234567", TransporterLoginActivity.normaliseMobile("+94 77 123 4567"));
        assertEquals("0771234567", TransporterLoginActivity.normaliseMobile("94771234567"));
    }

    @Test
    public void rejectsInvalidNumbers() {
        assertNull(TransporterLoginActivity.normaliseMobile(""));
        assertNull(TransporterLoginActivity.normaliseMobile("12345"));
        assertNull(TransporterLoginActivity.normaliseMobile("0112345678")); // landline
        assertNull(TransporterLoginActivity.normaliseMobile("07712345678")); // too long
    }
}
