package com.example.javapingpongelo.services.achievements;

import com.example.javapingpongelo.models.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for achievement evaluators.
 * Subclasses will implement specific evaluation logic for different achievement types.
 */
@Component
@Slf4j
public abstract class AchievementEvaluator {

    protected static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Evaluates if an achievement should be triggered for a player.
     *
     * @param player      The player to evaluate
     * @param achievement The achievement to check
     * @param context     Additional context data needed for evaluation
     * @return The progress update amount (0 if no update, positive number for progress)
     */
    public abstract int evaluate(Player player, Achievement achievement, EvaluationContext context);

    /**
     * Determines if this evaluator can handle the given achievement type.
     *
     * @param criteria The achievement criteria as a JsonNode
     * @return true if this evaluator can handle the achievement
     */
    public abstract boolean canHandle(JsonNode criteria);

    /**
     * Parses the criteria JSON string into a JsonNode.
     *
     * @param criteriaJson The criteria JSON string
     * @return The parsed JsonNode
     */
    protected JsonNode parseCriteria(String criteriaJson) {
        try {
            return objectMapper.readTree(criteriaJson);
        }
        catch (JsonProcessingException e) {
            log.error("Error parsing achievement criteria JSON", e);
            throw new RuntimeException("Error processing achievement criteria", e);
        }
    }

    /**
     * Enhanced context class for holding data needed during achievement evaluation.
     * This provides a flexible way to pass different data types to evaluators.
     */
    @Getter
    @Setter
    public static class EvaluationContext {
        // Current event data
        private Game game;
        private Match match;
        private Tournament tournament;

        // Historical data
        private List<Game> gameHistory;
        private List<Match> matchHistory;
        private List<Tournament> tournamentHistory;

        // Pre-computed player statistics (new!)
        private PlayerStatistics playerStatistics;

        // Achievement context
        private List<Achievement> earnedAchievements;
        private AchievementTrigger.TriggerType triggerType;

        // Computed values cache to avoid recalculation
        private Map<String, Object> computedValues;

        // Legacy additional data
        private Map<String, Object> additionalData;

        public EvaluationContext() {
            this.computedValues = new HashMap<>();
            this.additionalData = new HashMap<>();
        }

        /**
         * Gets or computes a value, caching it for subsequent calls
         */
        @SuppressWarnings("unchecked")
        public <T> T getOrCompute(String key, Class<T> type, java.util.function.Supplier<T> supplier) {
            return (T) computedValues.computeIfAbsent(key, k -> supplier.get());
        }

        /**
         * Caches a computed value
         */
        public void cache(String key, Object value) {
            computedValues.put(key, value);
        }

        /**
         * Checks if a value is cached
         */
        public boolean isCached(String key) {
            return computedValues.containsKey(key);
        }
    }
}