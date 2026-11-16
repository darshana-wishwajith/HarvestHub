package com.harvesthub.backend.controller;

import com.harvesthub.backend.dto.DashboardResponse;
import com.harvesthub.backend.dto.WeatherResponse;
import com.harvesthub.backend.repository.UserRepository;
import com.harvesthub.backend.security.CurrentUser;
import com.harvesthub.backend.service.DashboardService;
import com.harvesthub.backend.service.WeatherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api", ""})
public class DashboardController {

    private final CurrentUser currentUser;
    private final DashboardService dashboardService;
    private final WeatherService weatherService;
    private final UserRepository users;

    public DashboardController(CurrentUser currentUser, DashboardService dashboardService,
                               WeatherService weatherService, UserRepository users) {
        this.currentUser = currentUser;
        this.dashboardService = dashboardService;
        this.weatherService = weatherService;
        this.users = users;
    }

    @GetMapping({"/dashboard/farmer/{id}", "/api/dashboard/farmer/{id}"})
    public ResponseEntity<DashboardResponse> getFarmerDashboard(@PathVariable String id) {
        currentUser.requireSelfOrStaff(id);
        return ResponseEntity.ok(dashboardService.getFarmerDashboard(id));
    }

    /** Current weather at the caller's registered location; 204 when unavailable. */
    @GetMapping({"/weather", "/api/weather"})
    public ResponseEntity<WeatherResponse> getWeather(@RequestParam(value = "location", required = false) String ignored) {
        var loc = users.findById(currentUser.id()).map(u -> u.getFarmLocation()).orElse(null);
        WeatherResponse w = loc == null ? null : weatherService.current(loc.getLatitude(), loc.getLongitude(), loc.getAddress());
        return w == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(w);
    }
}
