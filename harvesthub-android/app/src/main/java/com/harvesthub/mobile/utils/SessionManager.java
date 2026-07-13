package com.harvesthub.mobile.utils;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.google.gson.Gson;
import com.harvesthub.mobile.model.User;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class SessionManager {
    private static final String PREF_NAME = "HarvestHubSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ROLE = "userRole";
    private static final String KEY_AUTH_TOKEN = "authToken";
    private static final String KEY_USER_DATA = "userData";
    
    private SharedPreferences pref;
    private final Gson gson;

    public SessionManager(Context context) {
        gson = new Gson();
        try {
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            pref = EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            e.printStackTrace();
            pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }
    }

    public void setLogin(boolean isLoggedIn, String role) {
        pref.edit()
            .putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
            .putString(KEY_USER_ROLE, role)
            .commit(); // Use commit for immediate effect before navigation
    }

    public void saveAuthToken(String token) {
        pref.edit()
            .putString(KEY_AUTH_TOKEN, token)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply();
    }

    public String getAuthToken() {
        return pref.getString(KEY_AUTH_TOKEN, null);
    }

    public void saveUser(User user) {
        String userJson = gson.toJson(user);
        pref.edit()
            .putString(KEY_USER_DATA, userJson)
            .putString(KEY_USER_ROLE, user.getRole())
            .apply();
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
        pref.edit().clear().commit();
    }
}
