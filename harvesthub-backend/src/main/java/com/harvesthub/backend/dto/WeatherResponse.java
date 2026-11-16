package com.harvesthub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Home weather line (Open-Meteo). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherResponse {
    private String temperature;   // "28°C"
    private String condition;     // "Cloudy"
    private String location;
}
