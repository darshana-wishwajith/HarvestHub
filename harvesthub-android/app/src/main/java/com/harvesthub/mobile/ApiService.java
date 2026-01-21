package com.harvesthub.mobile;

import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;

public interface ApiService {
    @GET("/api/users")
    Call<List<User>> getAllUsers();
}