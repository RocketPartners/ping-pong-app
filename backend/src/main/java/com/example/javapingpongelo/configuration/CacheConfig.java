package com.example.javapingpongelo.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Cache configuration using Caffeine for high-performance caching
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure Caffeine cache manager with performance-optimized settings
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // Default cache configuration
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(10_000)  // Max 10k entries per cache
                .expireAfterAccess(30, TimeUnit.MINUTES)  // Expire after 30 minutes of no access
                .expireAfterWrite(2, TimeUnit.HOURS)      // Expire after 2 hours regardless
                .recordStats());  // Enable cache statistics
        
        // Pre-create important caches with specific configurations
        cacheManager.setCacheNames(Arrays.asList(
                "player-statistics",     // Player stats cache
                "player-achievements",   // Player achievements cache  
                "achievement-analytics", // Achievement analytics cache
                "achievement-summary",   // Analytics summary cache
                "achievements",         // Achievements cache
                "leaderboard",          // Leaderboard cache
                "player-games",         // Player games cache
                "systemStats"           // System statistics cache
        ));
        
        return cacheManager;
    }

    /**
     * Cache configuration for player statistics - more aggressive caching
     */
    @Bean("playerStatsCaffeine")
    public Caffeine<Object, Object> playerStatsCaffeine() {
        return Caffeine.newBuilder()
                .maximumSize(5_000)  // Smaller cache for stats
                .expireAfterAccess(15, TimeUnit.MINUTES)  // Shorter access expiry
                .expireAfterWrite(1, TimeUnit.HOURS)      // Shorter write expiry
                .recordStats();
    }

    /**
     * Cache configuration for achievement analytics - longer retention
     */
    @Bean("analyticsCaffeine")
    public Caffeine<Object, Object> analyticsCaffeine() {
        return Caffeine.newBuilder()
                .maximumSize(1_000)   // Smaller cache for analytics
                .expireAfterAccess(2, TimeUnit.HOURS)    // Longer access expiry
                .expireAfterWrite(6, TimeUnit.HOURS)     // Much longer write expiry
                .recordStats();
    }

    /**
     * Cache configuration for leaderboard - medium retention
     */
    @Bean("leaderboardCaffeine")
    public Caffeine<Object, Object> leaderboardCaffeine() {
        return Caffeine.newBuilder()
                .maximumSize(100)     // Small cache for leaderboards
                .expireAfterAccess(10, TimeUnit.MINUTES)  // Short access expiry
                .expireAfterWrite(30, TimeUnit.MINUTES)   // Medium write expiry
                .recordStats();
    }
}