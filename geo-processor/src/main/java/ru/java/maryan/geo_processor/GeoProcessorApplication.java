package ru.java.maryan.geo_processor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = {
		"ru.java.maryan.geo_common",
		"ru.java.maryan.geo_processor"
})
public class GeoProcessorApplication {

	public static void main(String[] args) {
		SpringApplication.run(GeoProcessorApplication.class, args);
	}

}
