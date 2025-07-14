package com.example.javapingpongelo.utils;

import com.example.javapingpongelo.services.StatsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for periodically refreshing system statistics cache
 */
@Component
@Slf4j
public class StatsScheduler {

    @Autowired
    private StatsService statsService;

    /**
     * Refresh stats cache every hour
     */
    @Scheduled(fixedRate = 600000) // 20 minutes in milliseconds
    public void scheduledStatsRefresh() {
        log.info("Scheduled refresh of system statistics");
        statsService.refreshStats();
    }
}