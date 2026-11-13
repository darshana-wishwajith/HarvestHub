package com.harvesthub.mobile.utils;

import com.harvesthub.mobile.BuildConfig;

@SuppressWarnings("unused")
public class Constants {
    /** Backend URL: release = production; debug = value of -PapiUrl (see app/build.gradle). */
    public static final String BASE_URL = BuildConfig.API_BASE_URL;
}
