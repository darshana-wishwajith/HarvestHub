package com.harvesthub.mobile;

import android.app.Application;

import com.harvesthub.mobile.network.ApiClient;

/** Initialises app-wide singletons (authenticated API client). */
public class HarvestHubApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // The designs are light-only: without this, a phone in dark mode loads values-night (e.g. "white" = #121212)
        // and white text on green bubbles/buttons becomes invisible.
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        ApiClient.init(this);
    }
}
