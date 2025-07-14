package com.example.javapingpongelo.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * Configuration for database setup.
 * Note: Primary database settings are defined in application.properties
 * This class only handles initialization tasks.
 */
@Configuration
@Slf4j
public class DatabaseConfig {

    /**
     * Creates the data directory where the H2 database files will be stored
     * if it doesn't already exist.
     */
    @Bean
    public CommandLineRunner createDataDirectoryIfNotExists() {
        return args -> {
            File dataDir = new File("./data");
            if (!dataDir.exists()) {
                boolean created = dataDir.mkdir();
                if (created) {
                    log.info("Created data directory for H2 database");
                }
                else {
                    log.error("Failed to create data directory for H2 database");
                }
            }
            else {
                log.info("Data directory already exists");
            }
        };
    }
}