package com.example.javapingpongelo.services.achievements;

import com.example.javapingpongelo.models.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
     * Context class for holding data needed during achievement evaluation.
     * This provides a flexible way to pass different data types to evaluators.
     */
    @Getter
    @Setter
    public static class EvaluationContext {
        private Game game;

        private Match match;

        private Tournament tournament;

        private List<Game> gameHistory;

        private List<Match> matchHistory;

        private List<Tournament> tournamentHistory;

        private Map<String, Object> additionalData;

    }
}