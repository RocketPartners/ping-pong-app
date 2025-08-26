package com.example.javapingpongelo.services.achievements;

import com.example.javapingpongelo.models.*;
import com.example.javapingpongelo.repositories.AchievementRepository;
import com.example.javapingpongelo.repositories.GameRepository;
import com.example.javapingpongelo.repositories.MatchRepository;
import com.example.javapingpongelo.repositories.PlayerAchievementRepository;
import com.example.javapingpongelo.services.IPlayerService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Service implementation for managing achievements and evaluating achievement criteria
 */
@Service
@Slf4j
public class AchievementServiceImpl implements IAchievementService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    AchievementRepository achievementRepository;

    @Autowired
    PlayerAchievementRepository playerAchievementRepository;

    @Autowired
    GameRepository gameRepository;

    @Autowired
    MatchRepository matchRepository;

    @Autowired
    IPlayerService playerService;

    @Autowired
    AchievementEvaluatorFactory evaluatorFactory;

    private static Map<String, Object> getStringObjectMap(Player player) {
        Map<String, Object> additionalData = new HashMap<>();
        additionalData.put("singlesRankedRating", player.getSinglesRankedRating());
        additionalData.put("doublesRankedRating", player.getDoublesRankedRating());
        additionalData.put("singlesNormalRating", player.getSinglesNormalRating());
        additionalData.put("doublesNormalRating", player.getDoublesNormalRating());
        additionalData.put("totalMatchesPlayed", player.getTotalMatchesPlayed());
        return additionalData;
    }

    /**
     * Retrieve all achievements
     */
    @Override
    public List<Achievement> findAllAchievements() {
        return achievementRepository.findAll();
    }

    /**
     * Retrieve visible achievements only
     */
    @Override
    public List<Achievement> findVisibleAchievements() {
        return achievementRepository.findByIsVisible(true);
    }

    /**
     * Create a new achievement
     */
    @Override
    public Achievement createAchievement(Achievement achievement) {
        log.info("Creating new achievement: {}", achievement.getName());
        return achievementRepository.save(achievement);
    }

    /**
     * Find achievement by ID
     */
    @Override
    public Achievement findAchievementById(UUID id) {
        return achievementRepository.findById(id)
                                    .orElseThrow(() -> new RuntimeException("Achievement not found with id: " + id));
    }

    /**
     * Find a player's achievements
     */
    @Override
    public List<PlayerAchievement> findPlayerAchievements(UUID playerId) {
        return playerAchievementRepository.findByPlayerId(playerId);
    }

    /**
     * Find a player's achieved achievements
     */
    @Override
    public List<PlayerAchievement> findPlayerAchievedAchievements(UUID playerId) {
        return playerAchievementRepository.findByPlayerIdAndAchievedTrue(playerId);
    }

    /**
     * Initialize achievements for a new player
     */
    @Override
    @Transactional
    public void initializePlayerAchievements(UUID playerId) {
        log.info("Initializing achievements for player: {}", playerId);

        List<Achievement> allAchievements = achievementRepository.findAll();
        List<PlayerAchievement> playerAchievements = new ArrayList<>();

        for (Achievement achievement : allAchievements) {
            PlayerAchievement playerAchievement = PlayerAchievement.builder()
                                                                   .playerId(playerId)
                                                                   .achievementId(achievement.getId())
                                                                   .achieved(false)
                                                                   .progress(0)
                                                                   .build();

            playerAchievements.add(playerAchievement);
        }

        playerAchievementRepository.saveAll(playerAchievements);
    }

    /**
     * Mark an achievement as achieved for a player
     */
    @Override
    @Transactional
    public void achieveAchievement(UUID playerId, UUID achievementId) {
        log.info("Attempting to mark achievement {} as achieved for player {}",
                 achievementId, playerId);

        try {
            Achievement achievement = achievementRepository.findById(achievementId)
                                                           .orElseThrow(() -> new RuntimeException("Achievement not found: " + achievementId));

            log.info("Found achievement: {} ({})", achievement.getName(), achievement.getId());

            PlayerAchievement playerAchievement = playerAchievementRepository
                    .findByPlayerIdAndAchievementId(playerId, achievementId)
                    .orElseGet(() -> {
                        // Create if not exists
                        log.info("Creating new player achievement record for player {} and achievement {}",
                                 playerId, achievementId);
                        PlayerAchievement pa = new PlayerAchievement();
                        pa.setPlayerId(playerId);
                        pa.setAchievementId(achievementId);
                        pa.setProgress(0);
                        pa.setAchieved(false);
                        return pa;
                    });

            if (!playerAchievement.getAchieved()) {
                playerAchievement.setAchieved(true);
                playerAchievement.setDateEarned(new Date());
                playerAchievementRepository.save(playerAchievement);

                log.info("Achievement {} earned by player {}", achievement.getName(), playerId);
            }
            else {
                log.info("Achievement {} already earned by player {}", achievement.getName(), playerId);
            }

        }
        catch (Exception e) {
            log.error("Error achieving achievement: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Update progress for an achievement
     */
    @Override
    @Transactional
    public PlayerAchievement updateAchievementProgress(UUID playerId, UUID achievementId, int progressValue) {
        log.debug("Updating achievement {} progress for player {}: +{}", achievementId, playerId, progressValue);

        if (progressValue <= 0) {
            log.debug("Skipping progress update with non-positive value: {}", progressValue);
            // No progress to update
            return playerAchievementRepository
                    .findByPlayerIdAndAchievementId(playerId, achievementId)
                    .orElse(null);
        }

        Achievement achievement = findAchievementById(achievementId);

        PlayerAchievement playerAchievement = playerAchievementRepository
                .findByPlayerIdAndAchievementId(playerId, achievementId)
                .orElseGet(() -> {
                    // Create if not exists
                    PlayerAchievement pa = new PlayerAchievement();
                    pa.setPlayerId(playerId);
                    pa.setAchievementId(achievementId);
                    pa.setProgress(0);
                    pa.setAchieved(false);
                    return pa;
                });

        try {
            // Parse criteria to get threshold
            JsonNode criteria = objectMapper.readTree(achievement.getCriteria());
            int threshold = criteria.has("threshold") ? criteria.get("threshold").asInt() : 1;

            boolean newlyAchieved = playerAchievement.updateProgress(progressValue, threshold);
            playerAchievementRepository.save(playerAchievement);

            if (newlyAchieved) {
                log.info("Achievement {} earned by player {} after progress update", achievementId, playerId);
                // Additional notification logic could go here
            }

            return playerAchievement;
        }
        catch (JsonProcessingException e) {
            log.error("Error parsing achievement criteria JSON", e);
            throw new RuntimeException("Error processing achievement criteria", e);
        }
    }

    /**
     * Update progress for a contextual achievement (like Gilyed) with opponent info
     */
    @Override
    @Transactional
    public PlayerAchievement updateAchievementProgress(UUID playerId, UUID achievementId, int progressValue, 
                                                     String opponentName, Date gameDatePlayed) {
        log.debug("Updating contextual achievement {} progress for player {}: +{}", achievementId, playerId, progressValue);

        if (progressValue <= 0) {
            log.debug("Skipping progress update with non-positive value: {}", progressValue);
            return playerAchievementRepository
                    .findByPlayerIdAndAchievementId(playerId, achievementId)
                    .orElse(null);
        }

        Achievement achievement = findAchievementById(achievementId);

        // For contextual achievements like Gilyed, we create a new record each time
        // rather than updating existing progress
        PlayerAchievement playerAchievement = new PlayerAchievement();
        playerAchievement.setPlayerId(playerId);
        playerAchievement.setAchievementId(achievementId);
        playerAchievement.setProgress(1);
        playerAchievement.setAchieved(true);
        playerAchievement.setDateEarned(new Date());
        playerAchievement.setOpponentName(opponentName);
        playerAchievement.setGameDatePlayed(gameDatePlayed);

        playerAchievementRepository.save(playerAchievement);

        log.info("Contextual achievement {} earned by player {} against {}", achievementId, playerId, opponentName);
        return playerAchievement;
    }

    /**
     * Evaluate achievements after a game is played
     */
    @Override
    @Transactional
    public void evaluateAchievementsForGame(Game game, Player... players) {
        log.info("Evaluating achievements for game: {}", game.getGameId());

        // Create an evaluation context with the game and relevant history
        AchievementEvaluator.EvaluationContext context = new AchievementEvaluator.EvaluationContext();
        context.setGame(game);

        // Evaluate for each player
        for (Player player : players) {
            // Skip null players
            if (player == null) continue;

            UUID playerId = player.getPlayerId();

            // Get player's game history
            List<Game> gameHistory = gameRepository.findByPlayerId(playerId);
            context.setGameHistory(gameHistory);

            // Get player's match history
            List<Match> matchHistory = matchRepository.findByPlayerId(playerId);
            context.setMatchHistory(matchHistory);

            // Get all achievements
            List<Achievement> achievements = achievementRepository.findAll();

            // Evaluate each achievement
            evaluateEachAchievement(context, player, playerId, achievements);
        }
    }

    private void evaluateEachAchievement(AchievementEvaluator.EvaluationContext context, Player player, UUID playerId, List<Achievement> achievements) {
        for (Achievement achievement : achievements) {
            try {
                // Get the appropriate evaluator
                AchievementEvaluator evaluator = evaluatorFactory.getEvaluator(achievement.getCriteria());

                if (evaluator != null) {
                    // Evaluate the achievement
                    int progressUpdate = evaluator.evaluate(player, achievement, context);

                    // Update progress if needed
                    if (progressUpdate > 0) {
                        // Check if this is a Gilyed achievement (contextual)
                        JsonNode criteria = objectMapper.readTree(achievement.getCriteria());
                        String type = criteria.has("type") ? criteria.get("type").asText() : "";
                        
                        if ("GILYED".equals(type) && context.getGame() != null) {
                            // For Gilyed achievements, we need opponent info
                            Game game = context.getGame();
                            UUID opponentId = game.getChallengerId().equals(playerId) ? 
                                game.getOpponentId() : game.getChallengerId();
                            Player opponent = playerService.findPlayerById(opponentId);
                            
                            if (opponent != null) {
                                updateAchievementProgress(playerId, achievement.getId(), progressUpdate, 
                                    opponent.getFullName(), game.getDatePlayed());
                            }
                        } else {
                            // Regular achievement
                            updateAchievementProgress(playerId, achievement.getId(), progressUpdate);
                        }
                    }
                }
            }
            catch (Exception e) {
                log.error("Error evaluating achievement {}: {}", achievement.getId(), e.getMessage());
                // Continue with next achievement
            }
        }
    }

    /**
     * Recalculate all achievements for a player
     */
    @Override
    @Transactional
    public void recalculatePlayerAchievements(UUID playerId) {
        log.info("Recalculating all achievements for player: {}", playerId);

        try {
            // Get the player
            Player player = playerService.findPlayerById(playerId);

            // First, reset all progress
            List<PlayerAchievement> playerAchievements = playerAchievementRepository.findByPlayerId(playerId);
            for (PlayerAchievement pa : playerAchievements) {
                pa.setProgress(0);
                pa.setAchieved(false);
                pa.setDateEarned(null);
            }
            playerAchievementRepository.saveAll(playerAchievements);

            // Get player history
            List<Game> gameHistory = gameRepository.findByPlayerId(playerId);
            List<Match> matchHistory = matchRepository.findByPlayerId(playerId);

            // Create context for the latest achievements
            AchievementEvaluator.EvaluationContext context = new AchievementEvaluator.EvaluationContext();
            context.setGameHistory(gameHistory);
            context.setMatchHistory(matchHistory);

            // Add player statistics to context
            Map<String, Object> additionalData = getStringObjectMap(player);
            context.setAdditionalData(additionalData);

            // Get all achievements
            List<Achievement> achievements = achievementRepository.findAll();

            // Evaluate each achievement for current stats
            for (Achievement achievement : achievements) {
                try {
                    // Get the appropriate evaluator
                    AchievementEvaluator evaluator = evaluatorFactory.getEvaluator(achievement.getCriteria());

                    if (evaluator != null) {
                        // For certain achievement types, we need to handle them specially for recalculation
                        specialRecalculateAchievement(player, achievement, context);
                    }
                }
                catch (Exception e) {
                    log.error("Error recalculating achievement {}: {}", achievement.getId(), e.getMessage());
                    // Continue with next achievement
                }
            }

            // For historical data, process each game in chronological order
            gameHistory.sort(Comparator.comparing(Game::getDatePlayed));

            for (Game game : gameHistory) {
                context.setGame(game);

                // Evaluate achievements for this game
                evaluateEachAchievement(context, player, playerId, achievements);
            }
        }
        catch (Exception e) {
            log.error("Error recalculating achievements", e);
            throw new RuntimeException("Failed to recalculate achievements", e);
        }
    }

    /**
     * Special handling for certain achievement types during recalculation
     */
    private void specialRecalculateAchievement(Player player, Achievement achievement, AchievementEvaluator.EvaluationContext context) {
        try {
            JsonNode criteria = objectMapper.readTree(achievement.getCriteria());
            String type = criteria.has("type") ? criteria.get("type").asText() : "";

            if ("RATING_THRESHOLD".equals(type)) {
                // Check rating thresholds directly
                AchievementEvaluator evaluator = evaluatorFactory.getEvaluator(achievement.getCriteria());
                int progressUpdate = evaluator.evaluate(player, achievement, context);

                if (progressUpdate > 0) {
                    updateAchievementProgress(player.getPlayerId(), achievement.getId(), progressUpdate);
                }
            }
            // Add more special cases as needed

        }
        catch (Exception e) {
            log.error("Error in special recalculation for achievement {}", achievement.getId(), e);
        }
    }
}