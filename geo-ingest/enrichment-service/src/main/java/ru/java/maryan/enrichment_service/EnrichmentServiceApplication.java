package ru.java.maryan.enrichment_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = {
		"ru.java.maryan.geo_common",
		"ru.java.maryan.enrichment_service"
})
@EnableConfigurationProperties
public class EnrichmentServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(EnrichmentServiceApplication.class, args);
	}

}
