package com.example.javapingpongelo.utils;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class DatabaseMigration {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Initialize emailVerified field for existing players
     */
    @PostConstruct
    @Transactional
    public void initEmailVerifiedField() {
        log.info("Checking if emailVerified initialization is needed");
        
        // Check if the column exists and has null values
        try {
            Integer nullCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM player WHERE email_verified IS NULL", 
                Integer.class
            );
            
            if (nullCount != null && nullCount > 0) {
                log.info("Initializing emailVerified field for {} existing players", nullCount);
                
                // Set all existing records to verified by default
                jdbcTemplate.update("UPDATE player SET email_verified = true WHERE email_verified IS NULL");
                
                log.info("Successfully initialized emailVerified field for all players");
            } else {
                log.info("No emailVerified initialization needed");
            }
        } catch (Exception e) {
            // Column might not exist yet, which is fine
            log.info("emailVerified column might not exist yet, initialization skipped: {}", e.getMessage());
        }
    }
}