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
import com.harvesthub.mobile.model.TransporterEventRequest;
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

    /** Forgot-password step 2: 200 if the code is right (does not use it up), 400 otherwise. */
    @POST("api/auth/verify-reset-code")
    Call<okhttp3.ResponseBody> verifyResetCode(@Body com.harvesthub.mobile.model.VerifyResetCodeRequest request);

    @POST("api/auth/reset-password")
    Call<okhttp3.ResponseBody> resetPassword(@Body ResetPasswordRequest request);

    /** Returns {"email":bool,"mobileNumber":bool,"nic":bool}; true = not yet registered. */
    @POST("api/auth/check-availability")
    Call<java.util.Map<String, Boolean>> checkAvailability(@Body java.util.Map<String, String> body);

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
    Call<Listing> createListing(@Body Listing listing);

    /** Farmer card "Edit": estimated yield (quantity) and price per kg only. */
    @PUT("listings/{id}")
    Call<okhttp3.ResponseBody> updateListing(@Path("id") String id, @Body Listing listing);

    @POST("listings/{id}/deactivate")
    Call<okhttp3.ResponseBody> deactivateListing(@Path("id") String id);

    @POST("listings/{id}/reactivate")
    Call<okhttp3.ResponseBody> reactivateListing(@Path("id") String id);

    @PUT("listings/{id}/status")
    Call<GenericResponse> changeListingStatus(@Path("id") String id, @Body Map<String, String> status);

    @GET("orders/farmer/{farmerId}")
    Call<List<Order>> getFarmerOrders(@Path("farmerId") String farmerId);

    @GET("orders/buyer/{buyerId}")
    Call<List<Order>> getBuyerOrders(@Path("buyerId") String buyerId);

    @POST("orders/create")
    Call<Order> createOrder(@Body Order order);

    @POST("orders/{orderId}/payment")
    Call<Order> recordAdvancePayment(@Path("orderId") String orderId, @Body Map<String, Object> payment);

    @GET("orders/transporter/{transporterId}")
    Call<List<Order>> getTransporterOrders(@Path("transporterId") String transporterId, @Query("status") String status);

    @POST("orders/{orderId}/transporter-events")
    Call<Order> applyTransporterEvent(@Path("orderId") String orderId, @Body TransporterEventRequest request);

    @GET("orders/{orderId}")
    Call<Order> getOrder(@Path("orderId") String orderId);

    @PUT("orders/{orderId}/status")
    Call<GenericResponse> updateOrderStatus(@Path("orderId") String orderId, @Body Map<String, String> status);

    @POST("orders/{orderId}/rate")
    Call<GenericResponse> submitRating(@Path("orderId") String orderId, @Body Map<String, Object> rating);

    @GET("market/centers")
    Call<List<MarketCenter>> getMarketCenters();

    @GET("buyer/listings")
    Call<List<ListingItem>> getBuyerListings(@Query("search") String search, @Query("grade") String grade);

    /** Buyer marketplace with the Figma 2.2 filters; null parameters are left out. */
    @GET("buyer/listings")
    Call<List<ListingItem>> getBuyerListings(@Query("search") String search, @Query("grade") String grades,
                                             @Query("category") String category, @Query("district") String district,
                                             @Query("maxPrice") Double maxPrice, @Query("minQty") Double minQty);

    @GET("wanted-ads/buyer/{buyerId}")
    Call<List<WantedAd>> getBuyerWantedAds(@Path("buyerId") String buyerId);

    @POST("wanted-ads/create")
    Call<WantedAd> createWantedAd(@Body WantedAd ad);

    @GET("offers/buyer/{buyerId}")
    Call<List<com.harvesthub.mobile.model.SupplyOffer>> getOffersForBuyer(@Path("buyerId") String buyerId);

    @GET("offers")
    Call<List<com.harvesthub.mobile.model.SupplyOffer>> getAllOffers();

    // ---- Farmer workflow (backend OrderService / ListingService, 14 Nov) ----

    @GET("market/centers")
    Call<List<com.harvesthub.mobile.model.MarketCenterInfo>> getMarketCenterInfo(@Query("crop") String crop);

    @GET("api/crops")
    Call<List<com.harvesthub.mobile.model.CropInfo>> getCrops();

    @GET("listings/location-check")
    Call<java.util.Map<String, Object>> checkFarmLocation(@Query("lat") double lat, @Query("lng") double lng);

    @Multipart
    @POST("media/upload")
    Call<java.util.Map<String, String>> uploadMedia(@Part okhttp3.MultipartBody.Part file);

    @PUT("listings/{id}/retake")
    Call<Listing> retakeListing(@Path("id") String id, @Body Listing capture);

    /** event: START_HARVESTING | HARVEST_READY (farmer) */
    @POST("orders/{orderId}/events")
    Call<Order> sendOrderEvent(@Path("orderId") String orderId, @Body Map<String, String> body);

    @POST("orders/{orderId}/cancel")
    Call<Order> cancelOrder(@Path("orderId") String orderId);

    @POST("wallet/{userId}/withdraw")
    Call<Transaction> withdraw(@Path("userId") String userId, @Body Map<String, Object> body);

    @POST("api/me/change-password")
    Call<GenericResponse> changePassword(@Body Map<String, String> body);

    @GET("api/me/preferences")
    Call<Map<String, Object>> getPreferences();

    @PUT("api/me/preferences")
    Call<Map<String, Object>> updatePreferences(@Body Map<String, Object> body);

    // ---- Buyer workflow (backend, 15 Nov) ----

    @GET("dashboard/buyer/{id}")
    Call<com.harvesthub.mobile.model.BuyerDashboard> getBuyerDashboard(@Path("id") String buyerId);

    @PUT("api/me/delivery-location")
    Call<Map<String, Object>> setDeliveryLocation(@Body Map<String, Object> body);

    @POST("wallet/{userId}/deposit")
    Call<Transaction> deposit(@Path("userId") String userId, @Body Map<String, Object> body);

    @POST("offers/{offerId}/accept")
    Call<Order> acceptSupplyOffer(@Path("offerId") String offerId);

    @GET("offers/request/{requestId}")
    Call<List<com.harvesthub.mobile.model.SupplyOffer>> getOffersForRequest(@Path("requestId") String requestId);

    @PUT("wanted-ads/{id}")
    Call<WantedAd> updateWantedAd(@Path("id") String id, @Body WantedAd ad);

    @POST("wanted-ads/{id}/close")
    Call<WantedAd> closeWantedAd(@Path("id") String id);

    @POST("wanted-ads/{id}/republish")
    Call<WantedAd> republishWantedAd(@Path("id") String id, @Body Map<String, String> body);

    @GET("orders/{orderId}/tracking")
    Call<com.harvesthub.mobile.model.Tracking> getTracking(@Path("orderId") String orderId);

    @POST("orders/{orderId}/disputes")
    Call<Map<String, Object>> openDispute(@Path("orderId") String orderId, @Body Map<String, String> body);

    // ---- transporter view (15 Nov) ----
    @GET("api/vehicle-types")
    Call<List<Map<String, Object>>> getVehicleTypes();

    @GET("dashboard/transporter/{id}")
    Call<com.harvesthub.mobile.model.TransporterDashboard> getTransporterDashboard(@Path("id") String transporterId);

    @PUT("api/me/availability")
    Call<com.harvesthub.mobile.model.TransporterDashboard> setAvailability(@Body Map<String, Object> body);

    @PUT("api/me/vehicle")
    Call<User> updateVehicle(@Body Map<String, Object> body);

    /** Appeal / issue with photos: {"reason", "details", "photoUrls": [...]} */
    @POST("orders/{orderId}/disputes")
    Call<Map<String, Object>> openDisputeWithPhotos(@Path("orderId") String orderId, @Body Map<String, Object> body);

    @GET("orders/{orderId}/disputes")
    Call<List<Map<String, Object>>> getDisputes(@Path("orderId") String orderId);
}
