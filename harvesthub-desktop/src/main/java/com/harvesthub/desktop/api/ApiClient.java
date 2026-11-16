package com.harvesthub.desktop.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTTP client for the HarvestHub backend. Attaches the officer's JWT from {@link Session} to every call.
 * Base URL: env HARVESTHUB_BACKEND_URL, else -Dharvesthub.backend.url, else production.
 * Responses are returned as Jackson trees so screens show exactly what the server sent.
 */
public final class ApiClient {

    public static final String DEFAULT_BASE_URL = "https://harvesthubapp.tech";
    private static volatile String baseUrl = resolveBaseUrl();

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Called when the server says the session is no longer valid (401, or 403 for a suspended account). */
    private static volatile Runnable onSessionExpired = () -> { };

    private ApiClient() {
    }

    public static String getBaseUrl() {
        return baseUrl;
    }

    public static void setOnSessionExpired(Runnable r) {
        onSessionExpired = r == null ? () -> { } : r;
    }

    static String resolveBaseUrl() {
        String env = System.getenv("HARVESTHUB_BACKEND_URL");
        if (env != null && !env.isBlank()) return cleanUrl(env);
        String prop = System.getProperty("harvesthub.backend.url");
        if (prop != null && !prop.isBlank()) return cleanUrl(prop);
        return DEFAULT_BASE_URL;
    }

    static String cleanUrl(String url) {
        String t = url.trim();
        while (t.endsWith("/")) t = t.substring(0, t.length() - 1);
        return t;
    }

    /** Absolute URL for a server path such as "/uploads/abc.jpg"; null stays null. */
    public static String absolute(String path) {
        if (path == null || path.isBlank()) return null;
        if (path.startsWith("http://") || path.startsWith("https://")) return path;
        return baseUrl + (path.startsWith("/") ? path : "/" + path);
    }

    // ---- auth (public endpoints) ------------------------------------------------------------

    public static LoginResponseDto login(String username, String password) {
        JsonNode body = send("POST", "/api/auth/login", Map.of("username", username, "password", password), false);
        return MAPPER.convertValue(body, LoginResponseDto.class);
    }

    public static void forgotPassword(String email) {
        send("POST", "/api/auth/forgot-password", Map.of("email", email), false);
    }

    public static void verifyResetCode(String email, String code) {
        send("POST", "/api/auth/verify-reset-code", Map.of("email", email, "code", code), false);
    }

    public static void resetPassword(String email, String code, String newPassword) {
        send("POST", "/api/auth/reset-password", Map.of("email", email, "token", code, "newPassword", newPassword), false);
    }

    // ---- generic authenticated calls ------------------------------------------------------------

    public static JsonNode get(String path) {
        return send("GET", path, null, true);
    }

    public static JsonNode post(String path, Object body) {
        return send("POST", path, body == null ? Map.of() : body, true);
    }

    /** Builds "path?k=v&..." skipping null/blank values. */
    public static String query(String path, Object... keyValues) {
        Map<String, String> params = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object v = keyValues[i + 1];
            if (v != null && !v.toString().isBlank()) params.put(keyValues[i].toString(), v.toString());
        }
        if (params.isEmpty()) return path;
        StringBuilder sb = new StringBuilder(path).append('?');
        params.forEach((k, v) -> sb.append(enc(k)).append('=').append(enc(v)).append('&'));
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    public static String enc(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    // ---- core -------------------------------------------------------------------------------------

    private static JsonNode send(String method, String path, Object body, boolean authorized) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + (path.startsWith("/") ? path : "/" + path)))
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "application/json");
            if (authorized && Session.isLoggedIn()) b.header("Authorization", "Bearer " + Session.getToken());
            if ("GET".equals(method)) {
                b.GET();
            } else {
                b.header("Content-Type", "application/json");
                b.method(method, HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)));
            }
            HttpResponse<String> response = HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                String text = response.body();
                return text == null || text.isBlank() ? MAPPER.nullNode() : MAPPER.readTree(text);
            }
            String message = errorMessage(status, response.body());
            if (authorized && (status == 401 || (status == 403 && Session.isLoggedIn() && isSessionDead(path)))) {
                onSessionExpired.run();
            }
            throw new ApiException(message, status);
        } catch (IOException e) {
            throw new ApiException("Cannot reach the HarvestHub server (" + baseUrl + "). Check your internet connection.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("The request was interrupted.");
        }
    }

    /** A 403 on the officer's own profile means the token is no longer accepted (e.g. account suspended). */
    private static boolean isSessionDead(String path) {
        return path.startsWith("/api/officers/me");
    }

    static String errorMessage(int status, String body) {
        String server = null;
        try {
            JsonNode n = MAPPER.readTree(body);
            if (n != null && n.hasNonNull("error")) server = n.get("error").asText();
            else if (n != null && n.hasNonNull("message")) server = n.get("message").asText();
        } catch (Exception ignored) {
            // not JSON
        }
        if (server != null && !server.isBlank() && !server.startsWith("Request failed")) return server;
        return switch (status) {
            case 400 -> "The server did not accept this request.";
            case 401 -> "Your session has ended. Please sign in again.";
            case 403 -> "Your account is not allowed to do this.";
            case 404 -> "Not found. It may have been removed.";
            case 409 -> "Someone else changed this item. Refresh and try again.";
            case 429 -> "Too many attempts. Please wait and try again.";
            default -> status >= 500 ? "The server had a problem (HTTP " + status + "). Try again in a moment."
                    : "Request failed (HTTP " + status + ").";
        };
    }
}
