package com.harvesthub.mobile;

import android.app.Application;

import com.harvesthub.mobile.network.ApiClient;

/** Initialises app-wide singletons (authenticated API client). */
public class HarvestHubApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ApiClient.init(this);
    }
}
