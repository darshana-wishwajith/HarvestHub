package com.harvesthub.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// This annotation connects your 4 layers and bypasses security for testing
@SpringBootApplication
public class HarvesthubBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(HarvesthubBackendApplication.class, args);
	}
}