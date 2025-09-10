package com.example.javapingpongelo.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * One-time database schema fix for achievement trigger constraints.
 * This runs before the achievement system initializes to fix the constraint issue.
 */
@Component
@Order(0) // Run before AchievementInitializer (which has Order(1))
public class DatabaseConstraintFix implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConstraintFix.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            logger.info("Checking and fixing achievement trigger constraints...");
            
            // First, try to drop the existing constraint
            try {
                jdbcTemplate.execute("ALTER TABLE achievement_trigger DROP CONSTRAINT IF EXISTS achievement_trigger_trigger_type_check");
                logger.info("Dropped existing trigger_type constraint (if it existed)");
            } catch (Exception e) {
                logger.debug("No existing constraint to drop or error dropping: {}", e.getMessage());
            }
            
            // Add the new constraint with all enum values including EASTER_EGG_FOUND
            String constraintSql = """
                ALTER TABLE achievement_trigger 
                ADD CONSTRAINT achievement_trigger_trigger_type_check 
                CHECK (trigger_type IN (
                    'GAME_COMPLETED',
                    'RATING_UPDATED',
                    'STREAK_CHANGED', 
                    'MATCH_COMPLETED',
                    'TOURNAMENT_EVENT',
                    'EASTER_EGG_FOUND',
                    'MANUAL_TRIGGER',
                    'PERIODIC'
                ))
                """;
                
            jdbcTemplate.execute(constraintSql);
            logger.info("Successfully added achievement trigger constraint with EASTER_EGG_FOUND support");
            
        } catch (Exception e) {
            logger.error("Failed to fix achievement trigger constraint: {}", e.getMessage(), e);
            logger.warn("Achievement system may fail to initialize Easter egg achievements");
        }
    }
}