package com.example.javapingpongelo.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Allow requests from configured origins
        String allowedOrigins = System.getenv("ALLOWED_ORIGINS");
        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            for (String origin : allowedOrigins.split(",")) {
                config.addAllowedOrigin(origin.trim());
            }
        } else {
            // Fallback for development environment
            config.addAllowedOrigin("http://localhost:4200");
        }

        // Dev-friendly: allow any LAN origin on the Angular dev-server port so a
        // phone/tablet on the same WiFi can hit the Mac's IP without us hard-coding
        // that IP in env vars every time. Uses allowedOriginPatterns so wildcards work.
        config.addAllowedOriginPattern("http://192.168.*.*:4200");
        config.addAllowedOriginPattern("http://10.*.*.*:4200");
        config.addAllowedOriginPattern("http://172.16.*.*:4200");
        config.addAllowedOriginPattern("http://172.17.*.*:4200");
        config.addAllowedOriginPattern("http://172.18.*.*:4200");
        config.addAllowedOriginPattern("http://172.19.*.*:4200");
        config.addAllowedOriginPattern("http://172.2*.*.*:4200");
        config.addAllowedOriginPattern("http://172.30.*.*:4200");
        config.addAllowedOriginPattern("http://172.31.*.*:4200");

        // Allow common HTTP methods
        config.addAllowedMethod("GET");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("PATCH");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("OPTIONS");

        // Allow specific headers instead of wildcard
        config.addAllowedHeader("Authorization");
        config.addAllowedHeader("Content-Type");
        config.addAllowedHeader("Accept");
        config.addAllowedHeader("Origin");

        // Allow credentials (cookies, authorization headers)
        config.setAllowCredentials(true);

        // Apply this configuration to all paths
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}