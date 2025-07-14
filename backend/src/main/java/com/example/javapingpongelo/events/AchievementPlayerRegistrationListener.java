package com.example.javapingpongelo.events;

import com.example.javapingpongelo.configuration.ApplicationContextProvider;
import com.example.javapingpongelo.models.Player;
import com.example.javapingpongelo.services.achievements.IAchievementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listener that initializes achievements for new players.
 */
@Component
@Slf4j
public class AchievementPlayerRegistrationListener implements ApplicationListener<ContextRefreshedEvent> {

    @Transactional
    public void onPlayerRegistration(Player player) {
        log.info("Initializing achievements for new player: {}", player.getUsername());

        try {
            IAchievementService achievementService =
                    ApplicationContextProvider.getApplicationContext().getBean(IAchievementService.class);
            achievementService.initializePlayerAchievements(player.getPlayerId());
        }
        catch (Exception e) {
            log.error("Error initializing achievements for player: {}", player.getUsername(), e);
        }
    }

    /**
     * This method is automatically triggered on application startup.
     * It ensures the service is correctly initialized.
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        log.info("Achievement player registration listener initialized");
    }
}