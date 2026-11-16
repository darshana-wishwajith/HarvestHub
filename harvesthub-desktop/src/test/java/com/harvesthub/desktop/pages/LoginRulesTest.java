package com.harvesthub.desktop.pages;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Client-side checks mirror the server rules so officers see the problem before a request is sent. */
class LoginRulesTest {

    @Test
    void passwordRulesMatchTheServer() {
        assertEquals("Use at least 8 characters.", LoginView.passwordProblem("abc12", "abc12"));
        assertEquals("Use both letters and numbers.", LoginView.passwordProblem("abcdefgh", "abcdefgh"));
        assertEquals("Use both letters and numbers.", LoginView.passwordProblem("12345678", "12345678"));
        assertEquals("The two passwords do not match.", LoginView.passwordProblem("harvest2026", "harvest2027"));
        assertNull(LoginView.passwordProblem("harvest2026", "harvest2026"));
        assertEquals("Use at least 8 characters.", LoginView.passwordProblem(null, null));
    }

    @Test
    void verificationCodeIsSixDigits() {
        assertTrue(LoginView.CODE.matcher("042913").matches());
        assertFalse(LoginView.CODE.matcher("42913").matches());
        assertFalse(LoginView.CODE.matcher("04291a").matches());
    }

    @Test
    void gradeDescriptionsComeFromTheProposal() {
        assertTrue(ReviewPage.GRADES.get("A").startsWith("Export / supermarket"));
        assertTrue(ReviewPage.GRADES.get("B").startsWith("Standard retail"));
        assertTrue(ReviewPage.GRADES.get("C").startsWith("Processing / bulk"));
    }
}
