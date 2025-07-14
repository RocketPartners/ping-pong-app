package com.example.javapingpongelo.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for application caching.
 */
@Configuration
public class CacheConfig {

    /**
     * Configures the cache manager with Caffeine.
     *
     * @return The configured cache manager
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // Configure caches with different settings
        cacheManager.setCacheNames(Arrays.asList(
                "systemStats",
                "playerStats",
                "gameStats"
        ));

        cacheManager.setCaffeine(Caffeine.newBuilder()
                                         .expireAfterWrite(60, TimeUnit.MINUTES)  // Cache expires after 60 minutes
                                         .maximumSize(100)                        // Maximum number of entries
                                         .recordStats());                         // Enable statistics

        return cacheManager;
    }
}