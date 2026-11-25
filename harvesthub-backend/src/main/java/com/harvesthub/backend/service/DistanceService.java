package com.harvesthub.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harvesthub.backend.config.MarketplaceRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Road distance between two points: Google Maps Distance Matrix when a key is configured,
 * otherwise straight-line distance × road factor.
 */
@Service
public class DistanceService {

    private static final Logger log = LoggerFactory.getLogger(DistanceService.class);

    public record Result(double km, String source) {}

    private final String apiKey;
    private final MarketplaceRules rules;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    private final ObjectMapper json = new ObjectMapper();

    public DistanceService(@Value("${google.maps.api-key:}") String apiKey, MarketplaceRules rules) {
        this.apiKey = apiKey;
        this.rules = rules;
    }

    public Result distance(double fromLat, double fromLng, double toLat, double toLng) {
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                String url = String.format(java.util.Locale.US,
                        "https://maps.googleapis.com/maps/api/distancematrix/json?origins=%f,%f&destinations=%f,%f&key=%s",
                        fromLat, fromLng, toLat, toLng, apiKey);
                HttpResponse<String> res = http.send(HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(6)).GET().build(), HttpResponse.BodyHandlers.ofString());
                JsonNode el = json.readTree(res.body()).path("rows").path(0).path("elements").path(0);
                if ("OK".equals(el.path("status").asText())) {
                    return new Result(FeeCalculator.round2(el.path("distance").path("value").asDouble() / 1000.0), "GOOGLE_MAPS");
                }
                log.warn("Distance Matrix returned status {}", el.path("status").asText());
            } catch (Exception e) {
                log.warn("Distance Matrix call failed: {}", e.getClass().getSimpleName());
            }
        }
        double km = FeeCalculator.haversineKm(fromLat, fromLng, toLat, toLng) * rules.getRoadDistanceFactor();
        return new Result(FeeCalculator.round2(Math.max(km, 1.0)), "ESTIMATE");
    }
}
