package com.example.javapingpongelo.services.achievements;

import com.example.javapingpongelo.events.*;
import com.example.javapingpongelo.models.*;
import com.example.javapingpongelo.services.PlayerStatisticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Event handler for achievement evaluation.
 * Listens to game events and triggers smart achievement evaluation.
 */
@Component
@Slf4j
public class AchievementEventHandler {

    @Autowired
    private SmartAchievementFilterService filterService;

    @Autowired
    private AchievementServiceImpl achievementService;

    @Autowired
    private PlayerStatisticsService playerStatisticsService;

    /**
     * Handles game completed events
     */
    @EventListener
    @Async
    public void handleGameCompleted(GameCompletedEvent event) {
        log.info("Processing achievements for game completion: {}", event.getGame().getGameId());
        
        try {
            Game game = event.getGame();
            List<Player> players = event.getPlayers();
            
            // Determine game type for smart filtering
            GameType gameType = determineGameType(game);
            
            // Get only relevant achievements for this game type
            List<Achievement> relevantAchievements = filterService.getAchievementsForGameCompletion(gameType);
            
            log.info("SmartAchievementFilterService filtered {} achievements for {} trigger", 
                    relevantAchievements.size(), "GAME_COMPLETED");
            
            // Skip processing if no relevant achievements found
            if (relevantAchievements.isEmpty()) {
                log.debug("No relevant achievements found for game type: {}", gameType);
                return;
            }
            
            // Create enhanced evaluation context
            AchievementEvaluator.EvaluationContext context = createGameCompletionContext(game);
            
            // Evaluate achievements for each player
            for (Player player : players) {
                try {
                    evaluateAchievementsForPlayerSafely(player, relevantAchievements, context);
                } catch (Exception playerException) {
                    log.error("Error evaluating achievements for player {}: {}", 
                             player.getPlayerId(), playerException.getMessage(), playerException);
                    // Continue with other players instead of failing the entire batch
                }
            }
            
            log.info("Completed achievement evaluation for {} players, {} achievements", 
                    players.size(), relevantAchievements.size());
            
        } catch (Exception e) {
            log.error("Error processing achievements for game completion: {}", e.getMessage(), e);
            // Don't rethrow to prevent UndeclaredThrowableException
        }
    }

    /**
     * Handles rating updated events
     */
    @EventListener
    @Async
    public void handleRatingUpdated(RatingUpdatedEvent event) {
        log.info("Processing achievements for rating update: {} -> {} for player {}", 
                event.getOldRating(), event.getNewRating(), event.getPlayer().getPlayerId());
        
        try {
            Player player = event.getPlayer();
            GameType gameType = event.getGameType();
            
            // Get only rating-related achievements
            List<Achievement> ratingAchievements = filterService.getAchievementsForRatingUpdate(gameType);
            
            // Create rating-specific context
            AchievementEvaluator.EvaluationContext context = createRatingUpdateContext(event);
            
            // Evaluate rating achievements for the player
            evaluateAchievementsForPlayer(player, ratingAchievements, context);
            
            log.info("Completed rating achievement evaluation for player {}, {} achievements", 
                    player.getPlayerId(), ratingAchievements.size());
            
        } catch (Exception e) {
            log.error("Error processing achievements for rating update", e);
        }
    }

    /**
     * Handles streak changed events
     */
    @EventListener
    @Async
    public void handleStreakChanged(StreakChangedEvent event) {
        log.info("Processing achievements for streak change: win {} -> {}, loss {} -> {} for player {}", 
                event.getPreviousWinStreak(), event.getNewWinStreak(),
                event.getPreviousLossStreak(), event.getNewLossStreak(),
                event.getPlayer().getPlayerId());
        
        try {
            Player player = event.getPlayer();
            
            // Get only streak-related achievements
            List<Achievement> streakAchievements = filterService.getAchievementsForStreakChange();
            
            // Create streak-specific context
            AchievementEvaluator.EvaluationContext context = createStreakChangeContext(event);
            
            // Evaluate streak achievements for the player
            evaluateAchievementsForPlayer(player, streakAchievements, context);
            
            log.info("Completed streak achievement evaluation for player {}, {} achievements", 
                    player.getPlayerId(), streakAchievements.size());
            
        } catch (Exception e) {
            log.error("Error processing achievements for streak change", e);
        }
    }

    /**
     * Handles tournament events
     */
    @EventListener
    @Async
    public void handleTournamentEvent(TournamentEvent event) {
        log.info("Processing achievements for tournament event: {} for tournament {}", 
                event.getEventType(), event.getTournament().getId());
        
        try {
            // Get only tournament-related achievements
            List<Achievement> tournamentAchievements = filterService.getAchievementsForTournamentEvent();
            
            // Create tournament-specific context
            AchievementEvaluator.EvaluationContext context = createTournamentContext(event);
            
            // Evaluate for single player or multiple players
            if (event.getPlayer() != null) {
                evaluateAchievementsForPlayer(event.getPlayer(), tournamentAchievements, context);
            } else if (event.getPlayers() != null) {
                for (Player player : event.getPlayers()) {
                    evaluateAchievementsForPlayer(player, tournamentAchievements, context);
                }
            }
            
            log.info("Completed tournament achievement evaluation, {} achievements", 
                    tournamentAchievements.size());
            
        } catch (Exception e) {
            log.error("Error processing achievements for tournament event", e);
        }
    }

    /**
     * Evaluates achievements for a specific player
     */
    private void evaluateAchievementsForPlayer(Player player, List<Achievement> achievements, 
                                             AchievementEvaluator.EvaluationContext context) {
        evaluateAchievementsForPlayerSafely(player, achievements, context);
    }
    
    /**
     * Safely evaluates achievements for a specific player with proper exception handling
     */
    private void evaluateAchievementsForPlayerSafely(Player player, List<Achievement> achievements, 
                                                   AchievementEvaluator.EvaluationContext context) {
        try {
            // Get or create player statistics for context
            // Use a separate transaction to avoid conflicts with async processing
            PlayerStatistics playerStats = getPlayerStatisticsSafely(player.getPlayerId());
            if (playerStats != null) {
                context.setPlayerStatistics(playerStats);
                
                // Use the existing achievement service to evaluate
                // Wrap in additional try-catch to prevent proxy exceptions from bubbling up
                try {
                    achievementService.evaluateSpecificAchievements(player, achievements, context);
                } catch (RuntimeException re) {
                    // Log and handle runtime exceptions that might cause proxy issues
                    log.error("Runtime exception during achievement evaluation for player {}: {}", 
                             player.getPlayerId(), re.getMessage(), re);
                    // Don't re-throw to prevent UndeclaredThrowableException
                } catch (Exception e) {
                    // Handle all other exceptions
                    log.error("Exception during achievement evaluation for player {}: {}", 
                             player.getPlayerId(), e.getMessage(), e);
                    // Don't re-throw to prevent UndeclaredThrowableException
                }
            } else {
                log.warn("Could not retrieve player statistics for player: {}", player.getPlayerId());
            }
        } catch (Exception e) {
            log.error("Error in evaluateAchievementsForPlayerSafely for player {}: {}", 
                     player.getPlayerId(), e.getMessage(), e);
            // Don't re-throw to prevent UndeclaredThrowableException
        }
    }
    
    /**
     * Safely gets player statistics without causing transaction conflicts
     */
    private PlayerStatistics getPlayerStatisticsSafely(UUID playerId) {
        try {
            return playerStatisticsService.getOrCreatePlayerStatistics(playerId);
        } catch (Exception e) {
            log.error("Error retrieving player statistics for player {}: {}", playerId, e.getMessage());
            return null;
        }
    }

    /**
     * Creates context for game completion events
     */
    private AchievementEvaluator.EvaluationContext createGameCompletionContext(Game game) {
        AchievementEvaluator.EvaluationContext context = new AchievementEvaluator.EvaluationContext();
        context.setGame(game);
        context.setTriggerType(AchievementTrigger.TriggerType.GAME_COMPLETED);
        return context;
    }

    /**
     * Creates context for rating update events
     */
    private AchievementEvaluator.EvaluationContext createRatingUpdateContext(RatingUpdatedEvent event) {
        AchievementEvaluator.EvaluationContext context = new AchievementEvaluator.EvaluationContext();
        context.setTriggerType(AchievementTrigger.TriggerType.RATING_UPDATED);
        
        // Cache rating information for evaluators
        context.cache("oldRating", event.getOldRating());
        context.cache("newRating", event.getNewRating());
        context.cache("ratingChange", event.getRatingChange());
        context.cache("gameType", event.getGameType());
        
        return context;
    }

    /**
     * Creates context for streak change events
     */
    private AchievementEvaluator.EvaluationContext createStreakChangeContext(StreakChangedEvent event) {
        AchievementEvaluator.EvaluationContext context = new AchievementEvaluator.EvaluationContext();
        context.setTriggerType(AchievementTrigger.TriggerType.STREAK_CHANGED);
        
        // Cache streak information for evaluators
        context.cache("newWinStreak", event.getNewWinStreak());
        context.cache("newLossStreak", event.getNewLossStreak());
        context.cache("previousWinStreak", event.getPreviousWinStreak());
        context.cache("previousLossStreak", event.getPreviousLossStreak());
        context.cache("isNewWinStreakRecord", event.isNewWinStreakRecord());
        
        return context;
    }

    /**
     * Creates context for tournament events
     */
    private AchievementEvaluator.EvaluationContext createTournamentContext(TournamentEvent event) {
        AchievementEvaluator.EvaluationContext context = new AchievementEvaluator.EvaluationContext();
        context.setTournament(event.getTournament());
        context.setTriggerType(AchievementTrigger.TriggerType.TOURNAMENT_EVENT);
        
        // Cache tournament information for evaluators
        context.cache("tournamentEventType", event.getEventType());
        context.cache("round", event.getRound());
        context.cache("additionalData", event.getAdditionalData());
        
        return context;
    }

    /**
     * Determines game type from game object
     */
    private GameType determineGameType(Game game) {
        if (game.isSinglesGame() && game.isRatedGame()) {
            return GameType.SINGLES_RANKED;
        } else if (game.isSinglesGame() && game.isNormalGame()) {
            return GameType.SINGLES_NORMAL;
        } else if (game.isDoublesGame() && game.isRatedGame()) {
            return GameType.DOUBLES_RANKED;
        } else if (game.isDoublesGame() && game.isNormalGame()) {
            return GameType.DOUBLES_NORMAL;
        }
        
        // Default fallback
        return game.isSinglesGame() ? GameType.SINGLES_NORMAL : GameType.DOUBLES_NORMAL;
    }
}