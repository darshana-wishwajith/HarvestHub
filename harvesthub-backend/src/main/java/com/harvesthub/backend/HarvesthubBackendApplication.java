package com.harvesthub.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // Enable Spring's ability to run tasks asynchronously
public class HarvesthubBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(HarvesthubBackendApplication.class, args);
	}
}
