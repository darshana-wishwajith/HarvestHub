package com.harvesthub.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harvesthub.backend.dto.WeatherResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Current weather at the farm from Open-Meteo (free, no API key). Cached 30 minutes per ~1 km cell. */
@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);
    private static final Duration TTL = Duration.ofMinutes(30);

    private record Cached(WeatherResponse value, Instant at) {}

    private final Map<String, Cached> cache = new ConcurrentHashMap<>();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    private final ObjectMapper json = new ObjectMapper();
    private final String baseUrl;

    public WeatherService(@Value("${harvesthub.weather-url:https://api.open-meteo.com/v1/forecast}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /** null when the weather cannot be fetched (the app then hides the line). */
    public WeatherResponse current(Double lat, Double lng, String place) {
        if (lat == null || lng == null || baseUrl.isBlank()) return null;
        String key = String.format(Locale.US, "%.2f,%.2f", lat, lng);
        Cached c = cache.get(key);
        if (c != null && c.at().plus(TTL).isAfter(Instant.now())) return withPlace(c.value(), place);
        try {
            String url = String.format(Locale.US, "%s?latitude=%.4f&longitude=%.4f&current=temperature_2m,weather_code",
                    baseUrl, lat, lng);
            HttpResponse<String> res = http.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(5)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            JsonNode cur = json.readTree(res.body()).path("current");
            if (cur.isMissingNode() || !cur.has("temperature_2m")) return null;
            WeatherResponse w = new WeatherResponse(Math.round(cur.path("temperature_2m").asDouble()) + "°C",
                    describe(cur.path("weather_code").asInt(-1)), null);
            cache.put(key, new Cached(w, Instant.now()));
            return withPlace(w, place);
        } catch (Exception e) {
            log.warn("Weather lookup failed: {}", e.getClass().getSimpleName());
            return null;
        }
    }

    private static WeatherResponse withPlace(WeatherResponse w, String place) {
        return new WeatherResponse(w.getTemperature(), w.getCondition(), place);
    }

    /** WMO weather interpretation codes used by Open-Meteo. */
    static String describe(int code) {
        if (code == 0) return "Clear";
        if (code <= 2) return "Partly Cloudy";
        if (code == 3) return "Cloudy";
        if (code == 45 || code == 48) return "Fog";
        if (code >= 51 && code <= 57) return "Drizzle";
        if (code >= 61 && code <= 67) return "Rain";
        if (code >= 80 && code <= 82) return "Showers";
        if (code >= 95) return "Thunderstorm";
        return "—";
    }
}
