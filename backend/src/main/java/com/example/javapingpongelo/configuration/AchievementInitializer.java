package com.example.javapingpongelo.configuration;

import com.example.javapingpongelo.models.Achievement;
import com.example.javapingpongelo.repositories.AchievementRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for initializing default achievements.
 */
@Configuration
@Slf4j
public class AchievementInitializer {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private AchievementRepository achievementRepository;

    /**
     * Initializes default achievements if none exist.
     */
    @Bean
    public CommandLineRunner initializeAchievements() {
        return args -> {
            if (achievementRepository.count() == 0) {
                log.info("No achievements found. Initializing default achievements.");
                createDefaultAchievements();
            }
            else {
                log.info("Achievements already exist, skipping initialization.");
            }
        };
    }

    /**
     * Creates the default set of achievements.
     */
    private void createDefaultAchievements() {
        try {
            // EASY ACHIEVEMENTS

            // First Steps - Win your first game
            createAchievement(
                    "First Steps",
                    "Win your first game",
                    Achievement.AchievementCategory.EASY,
                    Achievement.AchievementType.ONE_TIME,
                    createCriteria("WIN_COUNT", 1),
                    "trophy_bronze",
                    10
            );

            // Getting Started - Play 5 games
            createAchievement(
                    "Getting Started",
                    "Play 5 games (win or lose)",
                    Achievement.AchievementCategory.EASY,
                    Achievement.AchievementType.PROGRESSIVE,
                    createCriteria("GAME_COUNT", 5),
                    "controller",
                    20
            );

            // Social Butterfly - Play against 5 different opponents
            createAchievement(
                    "Social Butterfly",
                    "Play against 5 different opponents",
                    Achievement.AchievementCategory.EASY,
                    Achievement.AchievementType.PROGRESSIVE,
                    createCriteria("UNIQUE_OPPONENTS", 5),
                    "friends",
                    25
            );

            // Singles Player - Win a singles game
            createAchievement(
                    "Singles Player",
                    "Win a singles game",
                    Achievement.AchievementCategory.EASY,
                    Achievement.AchievementType.ONE_TIME,
                    createCriteriaWithGameType("WIN_COUNT", 1, "SINGLES"),
                    "person",
                    15
            );

            // Team Player - Win a doubles game
            createAchievement(
                    "Team Player",
                    "Win a doubles game",
                    Achievement.AchievementCategory.EASY,
                    Achievement.AchievementType.ONE_TIME,
                    createCriteriaWithGameType("WIN_COUNT", 1, "DOUBLES"),
                    "people",
                    15
            );

            // Ranked Rookie - Play your first ranked game
            createAchievement(
                    "Ranked Rookie",
                    "Play your first ranked game",
                    Achievement.AchievementCategory.EASY,
                    Achievement.AchievementType.ONE_TIME,
                    createCriteriaWithGameType("GAME_COUNT", 1, "RANKED"),
                    "medal_bronze",
                    15
            );

            // Casual Contender - Win 5 normal (unranked) games
            createAchievement(
                    "Casual Contender",
                    "Win 5 normal (unranked) games",
                    Achievement.AchievementCategory.EASY,
                    Achievement.AchievementType.PROGRESSIVE,
                    createCriteriaWithGameType("WIN_COUNT", 5, "NORMAL"),
                    "casual",
                    30
            );

            // Consistent Competitor - Maintain a 50% win rate after 10 games
            createAchievement(
                    "Consistent Competitor",
                    "Maintain a 50% win rate after 10 games",
                    Achievement.AchievementCategory.EASY,
                    Achievement.AchievementType.ONE_TIME,
                    createComplexCriteria("WIN_RATE", 50, 10), // 50% after 10 games
                    "chart",
                    40
            );

            // Shutout Victory - Win a game where opponent scored fewer than 5 points
            createAchievement(
                    "Shutout Victory",
                    "Win a game where your opponent scored fewer than 5 points",
                    Achievement.AchievementCategory.EASY,
                    Achievement.AchievementType.ONE_TIME,
                    createComplexCriteria("OPPONENT_SCORE_BELOW", 5, 1),
                    "zero",
                    35
            );

            // MEDIUM ACHIEVEMENTS

            // Win Streak - Win 5 consecutive games
            createAchievement(
                    "Win Streak",
                    "Win 5 consecutive games",
                    Achievement.AchievementCategory.MEDIUM,
                    Achievement.AchievementType.ONE_TIME,
                    createCriteria("WIN_STREAK", 5),
                    "fire",
                    50
            );

            // Versatile Athlete - Win at least one game in each game type
            createAchievement(
                    "Versatile Athlete",
                    "Win at least one game in each game type (singles ranked, singles normal, doubles ranked, doubles normal)",
                    Achievement.AchievementCategory.MEDIUM,
                    Achievement.AchievementType.ONE_TIME,
                    createCriteria("ALL_GAME_TYPES", 4),
                    "dice",
                    75
            );

            // Rising Star - Reach a rating of 1200 in any game type
            createAchievement(
                    "Rising Star",
                    "Reach a rating of 1200 in any game type",
                    Achievement.AchievementCategory.MEDIUM,
                    Achievement.AchievementType.ONE_TIME,
                    createCriteria("RATING_THRESHOLD", 1200),
                    "star",
                    100
            );

            // Point Machine - Score 100 total points across all games
            createAchievement(
                    "Point Machine",
                    "Score 100 total points across all games",
                    Achievement.AchievementCategory.MEDIUM,
                    Achievement.AchievementType.PROGRESSIVE,
                    createCriteria("POINTS_SCORED", 100),
                    "target",
                    60
            );

            // Perfect Set - Win a best-of-3 match without losing a game
            createAchievement(
                    "Perfect Set",
                    "Win a best-of-3 match without losing a game",
                    Achievement.AchievementCategory.MEDIUM,
                    Achievement.AchievementType.ONE_TIME,
                    createCriteria("PERFECT_MATCH", 3),
                    "check",
                    80
            );

            // Giant Slayer - Defeat a player with a rating 200+ points higher than yours
            createAchievement(
                    "Giant Slayer",
                    "Defeat a player with a rating 200+ points higher than yours",
                    Achievement.AchievementCategory.MEDIUM,
                    Achievement.AchievementType.ONE_TIME,
                    createComplexCriteria("RATING_DIFFERENCE_WIN", 200, 1),
                    "giant",
                    120
            );

            // Marathon Match - Win a game with a score difference of only 2 points
            createAchievement(
                    "Marathon Match",
                    "Win a game that had a score difference of only 2 points",
                    Achievement.AchievementCategory.MEDIUM,
                    Achievement.AchievementType.ONE_TIME,
                    createComplexCriteria("CLOSE_GAME_WIN", 2, 1),
                    "clock",
                    65
            );

            // Doubles Domination - Win 10 doubles games
            createAchievement(
                    "Doubles Domination",
                    "Win 10 doubles games",
                    Achievement.AchievementCategory.MEDIUM,
                    Achievement.AchievementType.PROGRESSIVE,
                    createCriteriaWithGameType("WIN_COUNT", 10, "DOUBLES"),
                    "team",
                    85
            );

            // Singles Specialist - Win 10 singles games
            createAchievement(
                    "Singles Specialist",
                    "Win 10 singles games",
                    Achievement.AchievementCategory.MEDIUM,
                    Achievement.AchievementType.PROGRESSIVE,
                    createCriteriaWithGameType("WIN_COUNT", 10, "SINGLES"),
                    "person_star",
                    85
            );

            // Tournament Contender - Reach the semi-finals in any tournament
            createAchievement(
                    "Tournament Contender",
                    "Reach the semi-finals in any tournament",
                    Achievement.AchievementCategory.MEDIUM,
                    Achievement.AchievementType.ONE_TIME,
                    createCriteria("TOURNAMENT_ROUND", 2), // Semi-finals = round 2
                    "tournament",
                    90
            );

            // HARD ACHIEVEMENTS

            // Unstoppable - Win 10 consecutive games
            createAchievement(
                    "Unstoppable",
                    "Win 10 consecutive games",
                    Achievement.AchievementCategory.HARD,
                    Achievement.AchievementType.ONE_TIME,
                    createCriteria("WIN_STREAK", 10),
                    "trophy_gold",
                    150
            );

            // Perfect Match - Win a best-of-7 match without losing a game
            createAchievement(
                    "Perfect Match",
                    "Win a best-of-7 match without losing a game",
                    Achievement.AchievementCategory.HARD,
                    Achievement.AchievementType.ONE_TIME,
                    createCriteria("PERFECT_MATCH", 7),
                    "crown",
                    200
            );

            // Elite Player - Reach a rating of 1500 in any game type
            createAchievement(
                    "Elite Player",
                    "Reach a rating of 1500 in any game type",
                    Achievement.AchievementCategory.HARD,
                    Achievement.AchievementType.ONE_TIME,
                    createCriteria("RATING_THRESHOLD", 1500),
                    "medal",
                    250
            );

            // Master of All - Reach a rating of 1300+ in all four game types
            createAchievement(
                    "Master of All",
                    "Reach a rating of 1300+ in all four game types",
                    Achievement.AchievementCategory.HARD,
                    Achievement.AchievementType.ONE_TIME,
                    createComplexCriteria("ALL_RATINGS_THRESHOLD", 1300, 4),
                    "star_gold",
                    300
            );

            // Century Club - Win 100 total games
            createAchievement(
                    "Century Club",
                    "Win 100 total games",
                    Achievement.AchievementCategory.HARD,
                    Achievement.AchievementType.PROGRESSIVE,
                    createCriteria("WIN_COUNT", 100),
                    "100",
                    200
            );

            // Tournament Champion - Win a tournament
            createAchievement(
                    "Tournament Champion",
                    "Win a tournament",
                    Achievement.AchievementCategory.HARD,
                    Achievement.AchievementType.ONE_TIME,
                    createCriteria("TOURNAMENT_WIN", 1),
                    "trophy",
                    350
            );

            // Season Dominator - Maintain a 75%+ win rate over 50+ games in a season
            createAchievement(
                    "Season Dominator",
                    "Maintain a 75%+ win rate over 50+ games in a season",
                    Achievement.AchievementCategory.HARD,
                    Achievement.AchievementType.ONE_TIME,
                    createComplexCriteria("WIN_RATE", 75, 50),
                    "crown_silver",
                    400
            );

            // Comeback King/Queen - Win a best-of-5 match after losing the first two games
            createAchievement(
                    "Comeback King/Queen",
                    "Win a best-of-5 match after losing the first two games",
                    Achievement.AchievementCategory.HARD,
                    Achievement.AchievementType.ONE_TIME,
                    createCriteria("COMEBACK_WIN", 5), // 5 = best-of-5
                    "reverse",
                    275
            );

            // Nemesis - Defeat the same opponent 10 times
            createAchievement(
                    "Nemesis",
                    "Defeat the same opponent 10 times",
                    Achievement.AchievementCategory.HARD,
                    Achievement.AchievementType.ONE_TIME,
                    createCriteria("SAME_OPPONENT_WINS", 10),
                    "skull",
                    225
            );

            // Unbeatable Duo - Win 15 consecutive doubles games with the same partner
            createAchievement(
                    "Unbeatable Duo",
                    "Win 15 consecutive doubles games with the same partner",
                    Achievement.AchievementCategory.HARD,
                    Achievement.AchievementType.ONE_TIME,
                    createComplexCriteria("PARTNER_WIN_STREAK", 15, 1),
                    "team_gold",
                    325
            );

            // LEGENDARY ACHIEVEMENTS

            // Grandmaster - Reach a rating of 1800 in any game type
            createAchievement(
                    "Grandmaster",
                    "Reach a rating of 1800 in any game type",
                    Achievement.AchievementCategory.LEGENDARY,
                    Achievement.AchievementType.ONE_TIME,
                    createCriteria("RATING_THRESHOLD", 1800),
                    "crown_gold",
                    500
            );

            // Perfect Season - Win 25+ games in a season without a single loss
            createAchievement(
                    "Perfect Season",
                    "Win 25+ games in a season without a single loss",
                    Achievement.AchievementCategory.LEGENDARY,
                    Achievement.AchievementType.ONE_TIME,
                    createCriteria("PERFECT_SEASON", 25),
                    "diamond",
                    1000
            );

            // The Legend - Win 500 total games
            createAchievement(
                    "The Legend",
                    "Win 500 total games",
                    Achievement.AchievementCategory.LEGENDARY,
                    Achievement.AchievementType.PROGRESSIVE,
                    createCriteria("WIN_COUNT", 500),
                    "legend",
                    800
            );

            // Tournament Dynasty - Win 5 tournaments
            createAchievement(
                    "Tournament Dynasty",
                    "Win 5 tournaments",
                    Achievement.AchievementCategory.LEGENDARY,
                    Achievement.AchievementType.PROGRESSIVE,
                    createCriteria("TOURNAMENT_WIN", 5),
                    "trophy_diamond",
                    750
            );

            // Ultimate Rival - Defeat the top-rated player 3 times in a row
            createAchievement(
                    "Ultimate Rival",
                    "Defeat the top-rated player 3 times in a row",
                    Achievement.AchievementCategory.LEGENDARY,
                    Achievement.AchievementType.ONE_TIME,
                    createComplexCriteria("TOP_PLAYER_WIN_STREAK", 3, 1),
                    "king",
                    600
            );

            // Flawless Victory - Win a tournament without losing a single game
            createAchievement(
                    "Flawless Victory",
                    "Win a tournament without losing a single game",
                    Achievement.AchievementCategory.LEGENDARY,
                    Achievement.AchievementType.ONE_TIME,
                    createComplexCriteria("TOURNAMENT_PERFECT", 1, 1),
                    "flawless",
                    900
            );

            // The Specialist - Reach a 2000+ rating in one specific game type
            createAchievement(
                    "The Specialist",
                    "Reach a 2000+ rating in one specific game type",
                    Achievement.AchievementCategory.LEGENDARY,
                    Achievement.AchievementType.ONE_TIME,
                    createCriteria("RATING_THRESHOLD", 2000),
                    "specialist",
                    700
            );

            // The All-Rounder - Reach a 1600+ rating in all four game types
            createAchievement(
                    "The All-Rounder",
                    "Reach a 1600+ rating in all four game types",
                    Achievement.AchievementCategory.LEGENDARY,
                    Achievement.AchievementType.ONE_TIME,
                    createComplexCriteria("ALL_RATINGS_THRESHOLD", 1600, 4),
                    "all_round",
                    850
            );

            // Against All Odds - Win a tournament after starting as the lowest-ranked player
            createAchievement(
                    "Against All Odds",
                    "Win a tournament after starting as the lowest-ranked player",
                    Achievement.AchievementCategory.LEGENDARY,
                    Achievement.AchievementType.ONE_TIME,
                    createCriteria("UNDERDOG_TOURNAMENT_WIN", 1),
                    "odds",
                    950
            );

            // Table Tennis Immortal - Maintain a 70%+ win rate over 1000+ career games
            createAchievement(
                    "Table Tennis Immortal",
                    "Maintain a 70%+ win rate over 1000+ career games",
                    Achievement.AchievementCategory.LEGENDARY,
                    Achievement.AchievementType.ONE_TIME,
                    createComplexCriteria("WIN_RATE", 70, 1000),
                    "immortal",
                    1500
            );

            log.info("Successfully initialized {} achievements", achievementRepository.count());
        }
        catch (Exception e) {
            log.error("Error initializing default achievements", e);
        }
    }

    /**
     * Helper method to create an achievement
     */
    private Achievement createAchievement(
            String name,
            String description,
            Achievement.AchievementCategory category,
            Achievement.AchievementType type,
            String criteria,
            String icon,
            Integer points
    ) {
        Achievement achievement = Achievement.builder()
                                             .name(name)
                                             .description(description)
                                             .category(category)
                                             .type(type)
                                             .criteria(criteria)
                                             .icon(icon)
                                             .points(points)
                                             .isVisible(true)
                                             .build();

        return achievementRepository.save(achievement);
    }

    /**
     * Helper method to create basic JSON criteria string
     */
    private String createCriteria(String type, int threshold) {
        try {
            Map<String, Object> criteria = new HashMap<>();
            criteria.put("type", type);
            criteria.put("threshold", threshold);
            return objectMapper.writeValueAsString(criteria);
        }
        catch (Exception e) {
            log.error("Error creating criteria JSON", e);
            return "{}";
        }
    }

    /**
     * Helper method to create JSON criteria with game type
     */
    private String createCriteriaWithGameType(String type, int threshold, String gameType) {
        try {
            Map<String, Object> criteria = new HashMap<>();
            criteria.put("type", type);
            criteria.put("threshold", threshold);
            criteria.put("gameType", gameType);
            return objectMapper.writeValueAsString(criteria);
        }
        catch (Exception e) {
            log.error("Error creating criteria JSON", e);
            return "{}";
        }
    }

    /**
     * Helper method to create more complex criteria with additional parameters
     */
    private String createComplexCriteria(String type, int primaryValue, int secondaryValue) {
        try {
            Map<String, Object> criteria = new HashMap<>();
            criteria.put("type", type);
            criteria.put("threshold", primaryValue);
            criteria.put("secondaryValue", secondaryValue);
            return objectMapper.writeValueAsString(criteria);
        }
        catch (Exception e) {
            log.error("Error creating complex criteria JSON", e);
            return "{}";
        }
    }
}