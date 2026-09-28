package com.harvesthub.backend.service;

import com.harvesthub.backend.dto.MarketTrendResponse;
import com.harvesthub.backend.dto.WeatherResponse;
import com.harvesthub.backend.model.WantedAd;
import com.harvesthub.backend.repository.WantedAdRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MarketService {
    @Autowired
    private WantedAdRepository wantedAdRepository;

    public MarketTrendResponse getMarketTrend(String crop, int days) {
        // Generate sample trend data based on crop type and days
        List<Double> trends = new java.util.ArrayList<>();
        java.util.Random random = new java.util.Random(crop.hashCode());
        double basePrice = switch (crop.toLowerCase()) {
            case "rice" -> 120.0;
            case "tea" -> 350.0;
            case "coconut" -> 85.0;
            case "rubber" -> 280.0;
            case "cinnamon" -> 1500.0;
            default -> 150.0;
        };
        for (int i = 0; i < days; i++) {
            trends.add(basePrice + (random.nextDouble() * 30 - 15));
        }
        return new MarketTrendResponse(trends);
    }

    public WeatherResponse getWeather(String location) {
        // Return sample weather data
        return new WeatherResponse("28°C", "Partly Cloudy", location != null ? location : "Colombo");
    }

    public List<WantedAd> getWantedAds(String cropFilter, String searchQuery) {
        List<WantedAd> ads;
        if (cropFilter != null && !cropFilter.isEmpty() && !"All".equalsIgnoreCase(cropFilter)) {
            ads = wantedAdRepository.findByCropNameContainingIgnoreCase(cropFilter);
        } else {
            ads = wantedAdRepository.findAll();
        }
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            String query = searchQuery.toLowerCase();
            ads = ads.stream()
                    .filter(ad -> ad.getCropName().toLowerCase().contains(query)
                            || ad.getLocation().toLowerCase().contains(query))
                    .collect(Collectors.toList());
        }
        return ads;
    }

    public List<java.util.Map<String, Object>> getMarketCenters() {
        return Arrays.asList(
            java.util.Map.of("name", "Dambulla", "supplyLevel", "High", "demandLevel", "High"),
            java.util.Map.of("name", "Keppetipola", "supplyLevel", "Medium", "demandLevel", "High"),
            java.util.Map.of("name", "Meegoda", "supplyLevel", "High", "demandLevel", "Medium"),
            java.util.Map.of("name", "Narahenpita", "supplyLevel", "Medium", "demandLevel", "Low")
        );
    }
}
