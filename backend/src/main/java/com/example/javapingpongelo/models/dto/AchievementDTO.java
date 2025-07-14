package com.example.javapingpongelo.models.dto;

import com.example.javapingpongelo.models.Achievement;
import com.example.javapingpongelo.models.PlayerAchievement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.UUID;

/**
 * Data Transfer Object combining Achievement and PlayerAchievement data
 * for sending comprehensive achievement information to the client
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AchievementDTO {
    private UUID id;

    private String name;

    private String description;

    private Achievement.AchievementCategory category;

    private Achievement.AchievementType type;

    private String icon;

    private Integer points;

    // Player-specific data
    private boolean achieved;

    private int progress;

    private int threshold;

    private Date dateEarned;

    /**
     * Create DTO from Achievement and PlayerAchievement entities
     */
    public static AchievementDTO fromEntities(Achievement achievement, PlayerAchievement playerAchievement) {
        AchievementDTO dto = new AchievementDTO();

        // Achievement data
        dto.setId(achievement.getId());
        dto.setName(achievement.getName());
        dto.setDescription(achievement.getDescription());
        dto.setCategory(achievement.getCategory());
        dto.setType(achievement.getType());
        dto.setIcon(achievement.getIcon());
        dto.setPoints(achievement.getPoints());

        // Player achievement data
        if (playerAchievement != null) {
            dto.setAchieved(playerAchievement.getAchieved());
            dto.setProgress(playerAchievement.getProgress());
            dto.setDateEarned(playerAchievement.getDateEarned());
        }
        else {
            dto.setAchieved(false);
            dto.setProgress(0);
        }

        try {
            dto.setThreshold(100);
        }
        catch (Exception e) {
            dto.setThreshold(1);
        }

        return dto;
    }
}