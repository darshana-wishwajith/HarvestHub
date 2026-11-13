package com.harvesthub.mobile.network;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.harvesthub.mobile.ui.common.RoleSelectionActivity;
import com.harvesthub.mobile.utils.Constants;
import com.harvesthub.mobile.utils.SessionManager;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

/**
 * Single shared Retrofit client. Every request carries the logged-in user's JWT; a 401 on an
 * authenticated request ends the session and returns to role selection.
 * All legacy overloads (getClient(), getClient(session), getApiService()) return this same client.
 */
public class ApiClient {

    private static volatile Retrofit retrofit;
    private static volatile ApiService apiService;
    private static Context appContext;
    private static SessionManager session;

    /** Called once from {@link com.harvesthub.mobile.HarvestHubApp#onCreate()}. */
    public static synchronized void init(Context context) {
        appContext = context.getApplicationContext();
        session = new SessionManager(appContext);
    }

    public static ApiService getApiService() {
        if (apiService == null) {
            synchronized (ApiClient.class) {
                if (apiService == null) {
                    apiService = getClient().create(ApiService.class);
                }
            }
        }
        return apiService;
    }

    /** @deprecated the session argument is ignored; the shared authenticated client is returned. */
    @Deprecated
    public static Retrofit getClient(final SessionManager ignored) {
        return getClient();
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            synchronized (ApiClient.class) {
                if (retrofit == null) {
                    retrofit = build();
                }
            }
        }
        return retrofit;
    }

    private static Retrofit build() {
        Gson gson = new GsonBuilder().setLenient().create();
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS) // media uploads
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    String token = session != null ? session.getAuthToken() : null;
                    boolean authed = token != null && !token.isEmpty();
                    Request request = authed
                            ? original.newBuilder().header("Authorization", "Bearer " + token).build()
                            : original;
                    Response response = chain.proceed(request);
                    if (response.code() == 401 && authed && !original.url().encodedPath().contains("/auth/")) {
                        onSessionExpired();
                    }
                    return response;
                })
                .build();
        return new Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(client)
                .build();
    }

    private static void onSessionExpired() {
        if (session == null || appContext == null) return;
        session.logoutUser();
        new Handler(Looper.getMainLooper()).post(() -> {
            Intent i = new Intent(appContext, RoleSelectionActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            i.putExtra("session_expired", true);
            appContext.startActivity(i);
        });
    }
}
