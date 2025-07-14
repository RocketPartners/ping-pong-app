package com.example.javapingpongelo.services.achievements;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory for creating the appropriate achievement evaluator based on criteria.
 */
@Component
@Slf4j
public class AchievementEvaluatorFactory {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final List<AchievementEvaluator> evaluators;

    @Autowired
    public AchievementEvaluatorFactory(List<AchievementEvaluator> evaluators) {
        this.evaluators = evaluators;
    }

    /**
     * Gets the appropriate evaluator for the given achievement criteria.
     *
     * @param criteriaJson The criteria JSON string
     * @return The appropriate evaluator, or null if no evaluator can handle this criteria
     */
    public AchievementEvaluator getEvaluator(String criteriaJson) {
        try {
            JsonNode criteria = objectMapper.readTree(criteriaJson);

            for (AchievementEvaluator evaluator : evaluators) {
                if (evaluator.canHandle(criteria)) {
                    return evaluator;
                }
            }

            log.warn("No evaluator found for criteria: {}", criteriaJson);
            return null;
        }
        catch (Exception e) {
            log.error("Error parsing achievement criteria JSON", e);
            return null;
        }
    }
}