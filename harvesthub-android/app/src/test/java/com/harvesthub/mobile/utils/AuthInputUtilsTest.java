package com.harvesthub.mobile.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AuthInputUtilsTest {

    @Test
    public void identifierAcceptsEmailOrSriLankanMobile() {
        assertEquals("kamal@gmail.com", AuthInputUtils.normaliseIdentifier("  Kamal@Gmail.com "));
        assertEquals("0771234567", AuthInputUtils.normaliseIdentifier("077 123 4567"));
        assertEquals("0771234567", AuthInputUtils.normaliseIdentifier("+94 77 123 4567"));
        assertNull(AuthInputUtils.normaliseIdentifier("kamal@"));
        assertNull(AuthInputUtils.normaliseIdentifier("12345"));
        assertNull(AuthInputUtils.normaliseIdentifier(""));
    }

    @Test
    public void emailIsMaskedLikeTheDesign() {
        assertEquals("kama***@gmail.com", AuthInputUtils.maskEmail("kamal@gmail.com"));
        assertEquals("a***@x.lk", AuthInputUtils.maskEmail("ab@x.lk"));
    }

    @Test
    public void passwordRuleMatchesRegistration() {
        assertTrue(AuthInputUtils.isStrongEnough("harvest42"));
        assertFalse(AuthInputUtils.isStrongEnough("short1"));
        assertFalse(AuthInputUtils.isStrongEnough("lettersonly"));
        assertFalse(AuthInputUtils.isStrongEnough("12345678"));
    }
}
