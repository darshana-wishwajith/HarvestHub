package com.harvesthub.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync // Enable Spring's ability to run tasks asynchronously
@EnableScheduling // re-dispatch of orders waiting for a vehicle (workflow 3.3)
@ConfigurationPropertiesScan // MarketplaceRules
public class HarvesthubBackendApplication {

	/** All users are in Sri Lanka; timestamps are stored as local date-times, so the JVM must not use the server's UTC. */
	public static final String TIME_ZONE = "Asia/Colombo";

	public static void main(String[] args) {
		java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone(TIME_ZONE));
		SpringApplication.run(HarvesthubBackendApplication.class, args);
	}
}
