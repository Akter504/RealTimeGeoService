package ru.java.maryan.deduplication_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication(scanBasePackages = {
		"ru.java.maryan.geo_common",
		"ru.java.maryan.deduplication_service"
})
@EnableConfigurationProperties
public class DeduplicationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DeduplicationServiceApplication.class, args);
	}

}
