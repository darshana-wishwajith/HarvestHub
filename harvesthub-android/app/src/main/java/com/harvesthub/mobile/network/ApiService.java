package com.harvesthub.mobile.network;


import com.harvesthub.mobile.model.ChatMessage;
import com.harvesthub.mobile.model.ConfirmDealRequest;
import com.harvesthub.mobile.model.CropBatch;
import com.harvesthub.mobile.model.DashboardResponse;
import com.harvesthub.mobile.model.FarmerProfileResponse;
import com.harvesthub.mobile.model.FarmerSummaryResponse;
import com.harvesthub.mobile.model.ForgotPasswordRequest;
import com.harvesthub.mobile.model.GenericResponse;
import com.harvesthub.mobile.model.HistoryListing;
import com.harvesthub.mobile.model.ListingItem;
import com.harvesthub.mobile.model.LoginRequest;
import com.harvesthub.mobile.model.LoginResponse;
import com.harvesthub.mobile.model.MarketTrendResponse;
import com.harvesthub.mobile.model.NotificationItem;
import com.harvesthub.mobile.model.ResendOtpRequest;
import com.harvesthub.mobile.model.SendMessageRequest;
import com.harvesthub.mobile.model.SendOtpRequest;
import com.harvesthub.mobile.model.SupplyOfferRequest;
import com.harvesthub.mobile.model.Transaction;
import com.harvesthub.mobile.model.LocationRequest;
import com.harvesthub.mobile.model.RegisterRequest;
import com.harvesthub.mobile.model.ResetPasswordRequest;
import com.harvesthub.mobile.model.User;
import com.harvesthub.mobile.model.VerifyOtpRequest;
import com.harvesthub.mobile.model.VerifyOtpResponse;
import com.harvesthub.mobile.model.WalletSummaryResponse;
import com.harvesthub.mobile.model.WantedAd;
import com.harvesthub.mobile.model.WeatherResponse;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

import com.harvesthub.mobile.model.Listing;
import com.harvesthub.mobile.model.Order;
import com.harvesthub.mobile.model.MarketCenter;

public interface ApiService {
    @GET("/api/users")
    Call<List<User>> getAllUsers();

    @POST("api/auth/verify-otp")
    Call<VerifyOtpResponse> verifyOtp(@Body VerifyOtpRequest request);

    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/auth/send-otp")
    Call<GenericResponse> sendVerificationCode(@Body SendOtpRequest request);

    @POST("api/auth/resend-otp")
    Call<okhttp3.ResponseBody> resendOtp(@Body ResendOtpRequest request);

    @GET("dashboard/farmer/{id}")
    Call<DashboardResponse> getFarmerDashboard(@Path("id") String farmerId);

    @GET("weather")
    Call<WeatherResponse> getWeather(@Query("location") String location);

    @GET("market/trend")
    Call<MarketTrendResponse> getMarketTrend(@Query("crop") String cropType, @Query("days") int days);

    @POST("api/auth/forgot-password")
    Call<okhttp3.ResponseBody> forgotPassword(@Body ForgotPasswordRequest request);

    @POST("api/auth/reset-password")
    Call<okhttp3.ResponseBody> resetPassword(@Body ResetPasswordRequest request);

    @POST("api/auth/register")
    Call<okhttp3.ResponseBody> registerFarmer(@Body RegisterRequest request);

    @POST("farmer/location")
    Call<okhttp3.ResponseBody> saveFarmLocation(@Body LocationRequest request);

    @GET("batches/verified")
    Call<List<CropBatch>> getFarmerVerifiedBatches(@Query("farmerId") String farmerId, @Query("cropType") String cropType);

    @POST("offers/create")
    Call<GenericResponse> createSupplyOffer(@Body SupplyOfferRequest request);

    @GET("notifications/{farmerId}")
    Call<List<NotificationItem>> getNotifications(@Path("farmerId") String farmerId);

    @POST("notifications/{id}/read")
    Call<okhttp3.ResponseBody> markNotificationRead(@Path("id") String notificationId);

    @POST("notifications/mark-all-read")
    Call<okhttp3.ResponseBody> markAllNotificationsRead(@Query("farmerId") String farmerId);

    @GET("chat/{orderId}/messages")
    Call<List<ChatMessage>> getChatMessages(@Path("orderId") String orderId);

    @POST("chat/{orderId}/send")
    Call<ChatMessage> sendMessage(@Path("orderId") String orderId, @Body SendMessageRequest request);

    @POST("orders/{orderId}/confirm-deal")
    Call<GenericResponse> confirmDeal(@Path("orderId") String orderId, @Body ConfirmDealRequest request);

    @GET("farmer/{id}/summary")
    Call<FarmerSummaryResponse> getFarmerSummary(@Path("id") String farmerId);

    @GET("wanted-ads")
    Call<List<WantedAd>> getWantedAds(@Query("crop") String cropFilter, @Query("search") String searchQuery);

    @GET("farmer/{id}/active-listings")
    Call<List<ListingItem>> getActiveListings(@Path("id") String farmerId);

    @GET("farmer/{id}/listing-history")
    Call<List<HistoryListing>> getListingHistory(@Path("id") String farmerId, @Query("search") String searchQuery);

    @GET("wallet/{farmerId}/summary")
    Call<WalletSummaryResponse> getWalletSummary(@Path("farmerId") String farmerId);

    @GET("wallet/{farmerId}/transactions")
    Call<List<Transaction>> getRecentTransactions(@Path("farmerId") String farmerId, @Query("limit") int limit);

    @GET("farmer/{id}/profile")
    Call<FarmerProfileResponse> getFarmerProfile(@Path("id") String farmerId);

    @Multipart
    @POST("farmer/{id}/update-profile")
    Call<okhttp3.ResponseBody> updateFarmerProfile(
            @Path("id") String farmerId,
            @Part("fullName") RequestBody fullName,
            @Part("phoneNumber") RequestBody phoneNumber,
            @Part("nic") RequestBody nic,
            @Part MultipartBody.Part photo
    );

    @POST("listings/create")
    Call<GenericResponse> createListing(@Body Listing listing);

    @PUT("listings/{id}")
    Call<GenericResponse> updateListing(@Path("id") String id, @Body Listing listing);

    @PUT("listings/{id}/status")
    Call<GenericResponse> changeListingStatus(@Path("id") String id, @Body Map<String, String> status);

    @GET("orders/farmer/{farmerId}")
    Call<List<Order>> getFarmerOrders(@Path("farmerId") String farmerId);

    @GET("orders/{orderId}")
    Call<Order> getOrder(@Path("orderId") String orderId);

    @PUT("orders/{orderId}/status")
    Call<GenericResponse> updateOrderStatus(@Path("orderId") String orderId, @Body Map<String, String> status);

    @POST("orders/{orderId}/rate")
    Call<GenericResponse> submitRating(@Path("orderId") String orderId, @Body Map<String, Object> rating);

    @GET("market/centers")
    Call<List<MarketCenter>> getMarketCenters();
}
