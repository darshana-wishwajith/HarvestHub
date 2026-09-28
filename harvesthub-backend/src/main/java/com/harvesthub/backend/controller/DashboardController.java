package com.harvesthub.backend.controller;

import com.harvesthub.backend.dto.DashboardResponse;
import com.harvesthub.backend.dto.WeatherResponse;
import com.harvesthub.backend.service.DashboardService;
import com.harvesthub.backend.service.MarketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class DashboardController {
    @Autowired
    private DashboardService dashboardService;
    @Autowired
    private MarketService marketService;

    @GetMapping("dashboard/farmer/{id}")
    public ResponseEntity<DashboardResponse> getFarmerDashboard(@PathVariable String id) {
        return ResponseEntity.ok(dashboardService.getFarmerDashboard(id));
    }

    @GetMapping("weather")
    public ResponseEntity<WeatherResponse> getWeather(@RequestParam(value = "location", required = false) String location) {
        return ResponseEntity.ok(marketService.getWeather(location));
    }
}
