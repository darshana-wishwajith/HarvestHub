package com.harvesthub.mobile.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "HarvestHubSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ROLE = "userRole";
    
    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    public void setLogin(boolean isLoggedIn, String role) {
        editor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn);
        editor.putString(KEY_USER_ROLE, role);
        editor.apply();
    }

    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getUserRole() {
        return pref.getString(KEY_USER_ROLE, "");
    }

    public void logoutUser() {
        editor.clear();
        editor.commit();
    }
}
