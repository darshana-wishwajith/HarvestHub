package com.harvesthub.mobile.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.harvesthub.mobile.model.User;

public class SessionManager {
    private static final String PREF_NAME = "HarvestHubSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ROLE = "userRole";
    private static final String KEY_AUTH_TOKEN = "authToken";
    private static final String KEY_USER_DATA = "userData";
    
    private final SharedPreferences pref;
    private final SharedPreferences.Editor editor;
    private final Gson gson;

    public SessionManager(Context context) {
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
        gson = new Gson();
    }

    public void setLogin(boolean isLoggedIn, String role) {
        editor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn);
        editor.putString(KEY_USER_ROLE, role);
        editor.apply();
    }

    public void saveAuthToken(String token) {
        editor.putString(KEY_AUTH_TOKEN, token);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    public String getAuthToken() {
        return pref.getString(KEY_AUTH_TOKEN, null);
    }

    public void saveUser(User user) {
        String userJson = gson.toJson(user);
        editor.putString(KEY_USER_DATA, userJson);
        editor.putString(KEY_USER_ROLE, user.getRole());
        editor.apply();
    }

    public User getUser() {
        String userJson = pref.getString(KEY_USER_DATA, null);
        if (userJson == null) return null;
        return gson.fromJson(userJson, User.class);
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
