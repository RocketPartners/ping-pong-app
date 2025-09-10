package com.example.javapingpongelo.models.dto;

import com.example.javapingpongelo.models.EasterEgg;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * DTO representing a recent easter egg find for activity feeds.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentEggFindDto {
    private String finderName;
    private String finderUsername;
    private EasterEgg.EggType eggType;
    private int pointsEarned;
    private Date foundAt;
    private String pageFoundOn;
    private String relativeTime;       // "2 minutes ago", "1 hour ago", etc.
    
    /**
     * Calculate and set the relative time string
     */
    public void calculateRelativeTime() {
        if (foundAt == null) {
            relativeTime = "unknown";
            return;
        }
        
        long diffMs = new Date().getTime() - foundAt.getTime();
        long diffSec = diffMs / 1000;
        long diffMin = diffSec / 60;
        long diffHour = diffMin / 60;
        long diffDay = diffHour / 24;
        
        if (diffSec < 60) {
            relativeTime = "just now";
        } else if (diffMin < 60) {
            relativeTime = diffMin + (diffMin == 1 ? " minute ago" : " minutes ago");
        } else if (diffHour < 24) {
            relativeTime = diffHour + (diffHour == 1 ? " hour ago" : " hours ago");
        } else if (diffDay < 7) {
            relativeTime = diffDay + (diffDay == 1 ? " day ago" : " days ago");
        } else {
            relativeTime = "over a week ago";
        }
    }
    
    /**
     * Get the image filename of the egg type
     */
    public String getEggImageFilename() {
        return eggType != null ? eggType.getImageFilename() : "common-egg.png";
    }
    
    /**
     * Get the emoji fallback for the egg type (for backwards compatibility)
     */
    public String getEggEmoji() {
        if (eggType == null) return "🥚";
        return switch (eggType) {
            case COMMON -> "🥚";
            case UNCOMMON -> "🥚";
            case RARE -> "🌟";
            case EPIC -> "💎";
            case LEGENDARY -> "✨";
            case MYTHICAL -> "🔮";
        };
    }
    
    /**
     * Get a formatted display message for the find
     */
    public String getDisplayMessage() {
        return String.format("%s found a %s egg on %s (%s) - %d points!", 
            finderName, 
            eggType.name().toLowerCase(), 
            pageFoundOn, 
            relativeTime,
            pointsEarned);
    }
}