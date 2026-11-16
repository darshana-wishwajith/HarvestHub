package com.harvesthub.desktop.util;

import java.util.prefs.Preferences;

/** "Remember me": stores only the officer's email/mobile on this Windows account. Passwords are never stored. */
public final class Prefs {

    private static final String KEY = "rememberedLogin";

    private Prefs() {
    }

    private static Preferences node() {
        return Preferences.userRoot().node("lk/harvesthub/officer-portal");
    }

    public static String rememberedLogin() {
        try {
            return node().get(KEY, "");
        } catch (Exception e) {
            return "";
        }
    }

    public static void rememberLogin(String login) {
        try {
            if (login == null || login.isBlank()) node().remove(KEY);
            else node().put(KEY, login.trim());
        } catch (Exception ignored) {
            // preferences unavailable – remember me simply does nothing
        }
    }
}
