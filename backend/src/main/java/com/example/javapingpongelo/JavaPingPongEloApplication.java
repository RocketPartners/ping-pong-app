package com.example.javapingpongelo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Main Spring Boot application class.
 * Configures CORS and starts the application.
 */
@SpringBootApplication
@EnableScheduling // Enable scheduling for stats refresh
@Slf4j
@EnableCaching
public class JavaPingPongEloApplication {

    public static void main(String[] args) {
        log.info("Starting PingPong Elo Rating Application");
        SpringApplication.run(JavaPingPongEloApplication.class, args);
        log.info("PingPong Elo Rating Application started successfully");
    }

    /**
     * Configure CORS settings to allow cross-origin requests from the frontend.
     * This enables frontend applications running on different domains to interact with the API.
     *
     * @return WebMvcConfigurer with CORS configuration
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                log.info("Configuring CORS mappings");
                registry.addMapping("/**")
                        .allowedOrigins(
                                "http://localhost:4200",  // Angular default
                                "http://localhost:3000"   // React default
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600); // 1 hour
                log.info("CORS mappings configured successfully");
            }
        };
    }
}