package com.harvesthub.desktop.api;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Thin wrapper around java.net.http.HttpClient for calling the HarvestHub backend.
 * Attaches the JWT from {@link Session} to every call once the officer has logged in.
 */
public final class ApiClient {

    /** Default AWS cloud backend URL. Can be overridden via env var HARVESTHUB_BACKEND_URL or system property -Dharvesthub.backend.url */
    public static final String DEFAULT_BASE_URL = "https://harvesthubapp.tech";
    private static volatile String baseUrl = resolveBaseUrl();

    public static String getBaseUrl() {
        return baseUrl;
    }

    public static void setBaseUrl(String url) {
        if (url != null && !url.isBlank()) {
            baseUrl = cleanUrl(url);
        }
    }

    private static String resolveBaseUrl() {
        String env = System.getenv("HARVESTHUB_BACKEND_URL");
        if (env != null && !env.isBlank()) {
            return cleanUrl(env);
        }
        String prop = System.getProperty("harvesthub.backend.url");
        if (prop != null && !prop.isBlank()) {
            return cleanUrl(prop);
        }
        return DEFAULT_BASE_URL;
    }

    private static String cleanUrl(String url) {
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ApiClient() {
    }

    // ---- Auth ---------------------------------------------------------

    public static LoginResponseDto login(String username, String password) {
        Map<String, Object> body = Map.of("username", username, "password", password);
        return post("/api/auth/login", body, LoginResponseDto.class, false);
    }

    public static OfficerDto currentOfficer() {
        return get("/api/officers/me", OfficerDto.class);
    }

    // ---- Dashboard ------------------------------------------------------

    public static List<ActivityLogDto> getRecentActivity() {
        return getList("/api/dashboard/activity", ActivityLogDto.class);
    }

    public static OfficerStatsDto getDashboardStats() {
        return get("/api/dashboard/stats", OfficerStatsDto.class);
    }

    // ---- Verification Queue ---------------------------------------------

    public static List<QueueListingDto> getQueue(String tab, String search) {
        StringBuilder path = new StringBuilder("/api/queue?tab=").append(urlEncode(tab));
        if (search != null && !search.isBlank()) {
            path.append("&search=").append(urlEncode(search));
        }
        return getList(path.toString(), QueueListingDto.class);
    }

    public static ListingDetailDto claimListing(String listingId) {
        return post("/api/queue/" + listingId + "/claim", Map.of(), ListingDetailDto.class, true);
    }

    public static ListingDetailDto claimListing(long listingId) {
        return claimListing(String.valueOf(listingId));
    }

    public static ListingDetailDto getListing(String listingId) {
        return get("/api/listings/" + listingId, ListingDetailDto.class);
    }

    public static ListingDetailDto getListing(long listingId) {
        return getListing(String.valueOf(listingId));
    }

    // ---- Review -----------------------------------------------------------

    public static void submitReview(String listingId, ReviewDecisionRequestDto request) {
        post("/api/listings/" + listingId + "/review", request, Void.class, true);
    }

    public static void submitReview(long listingId, ReviewDecisionRequestDto request) {
        submitReview(String.valueOf(listingId), request);
    }

    public static List<ReviewHistoryRecordDto> getReviewHistory(String search) {
        String path = "/api/review-history" + (search != null && !search.isBlank()
                ? "?search=" + urlEncode(search) : "");
        return getList(path, ReviewHistoryRecordDto.class);
    }

    // ---- Core HTTP helpers ------------------------------------------------

    private static <T> T get(String path, Class<T> type) {
        HttpRequest request = requestBuilder(path)
                .GET()
                .build();
        return send(request, type);
    }

    private static <T> List<T> getList(String path, Class<T> elementType) {
        HttpRequest request = requestBuilder(path)
                .GET()
                .build();
        return sendList(request, elementType);
    }

    private static <T> T post(String path, Object body, Class<T> type, boolean authorized) {
        try {
            String json = MAPPER.writeValueAsString(body);
            HttpRequest.Builder builder = requestBuilder(path)
                    .POST(HttpRequest.BodyPublishers.ofString(json));
            HttpRequest request = builder.build();
            return send(request, type);
        } catch (IOException e) {
            throw new ApiException("Could not reach HarvestHub server: " + e.getMessage());
        }
    }

    private static HttpRequest.Builder requestBuilder(String path) {
        String targetPath = (path != null && path.startsWith("/")) ? path : ("/" + (path == null ? "" : path));
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + targetPath))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
        if (Session.isLoggedIn()) {
            builder.header("Authorization", "Bearer " + Session.getToken());
        }
        return builder;
    }

    private static <T> T send(HttpRequest request, Class<T> type) {
        try {
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            checkStatus(response);
            if (type == Void.class || response.body() == null || response.body().isBlank()) {
                return null;
            }
            return MAPPER.readValue(response.body(), type);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ApiException("Could not reach HarvestHub server: " + e.getMessage());
        }
    }

    private static <T> List<T> sendList(HttpRequest request, Class<T> elementType) {
        try {
            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            checkStatus(response);
            var type = MAPPER.getTypeFactory().constructCollectionType(List.class, elementType);
            return MAPPER.readValue(response.body(), type);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ApiException("Could not reach HarvestHub server: " + e.getMessage());
        }
    }

    private static void checkStatus(HttpResponse<String> response) throws IOException {
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return;
        }
        String message = "Request failed (HTTP " + status + ")";
        try {
            Map<?, ?> error = MAPPER.readValue(response.body(), Map.class);
            if (error.get("message") != null) {
                message = error.get("message").toString();
            }
        } catch (Exception ignored) {
            // Body wasn't JSON — fall back to the generic message above.
        }
        throw new ApiException(message, status);
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
