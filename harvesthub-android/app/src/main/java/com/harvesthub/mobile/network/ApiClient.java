package com.harvesthub.mobile.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.harvesthub.mobile.utils.Constants;
import com.harvesthub.mobile.utils.SessionManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ApiClient {
    private static Retrofit retrofit = null;
    private static ApiService apiService = null;

    public static ApiService getApiService() {
        if (apiService == null) {
            apiService = getClient().create(ApiService.class);
        }
        return apiService;
    }

    public static Retrofit getClient() {
        return getClient(null);
    }

    public static Retrofit getClient(final SessionManager sessionManager) {
        if (retrofit == null || sessionManager != null) {
            Gson gson = new GsonBuilder()
                    .setLenient()
                    .create();

            OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

            if (sessionManager != null) {
                httpClient.addInterceptor(chain -> {
                    Request original = chain.request();
                    String token = sessionManager.getAuthToken();

                    if (token != null && !token.isEmpty()) {
                        Request request = original.newBuilder()
                                .header("Authorization", "Bearer " + token)
                                .method(original.method(), original.body())
                                .build();
                        return chain.proceed(request);
                    }
                    return chain.proceed(original);
                });
            }

            Retrofit newRetrofit = new Retrofit.Builder()
                    .baseUrl(Constants.BASE_URL)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(httpClient.build())
                    .build();

            if (sessionManager == null) {
                retrofit = newRetrofit;
            }
            return newRetrofit;
        }
        return retrofit;
    }
}
