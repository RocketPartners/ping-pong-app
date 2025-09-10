package com.example.javapingpongelo.models;

import com.example.javapingpongelo.validators.ValidEmail;
import com.example.javapingpongelo.validators.ValidationGroups;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import java.util.*;

/**
 * Entity that represents a player in the system.
 * Contains personal information, game statistics, and rating information.
 */
@Entity
@Table(name = "player", indexes = {
        @Index(name = "idx_player_username", columnList = "username", unique = true),
        @Index(name = "idx_player_email", columnList = "email", unique = true),
        @Index(name = "idx_player_name", columnList = "firstName, lastName"),
        @Index(name = "idx_player_created", columnList = "created"),
        @Index(name = "idx_player_singles_ranked", columnList = "singlesRankedRating"),
        @Index(name = "idx_player_doubles_ranked", columnList = "doublesRankedRating"),
        @Index(name = "idx_player_singles_normal", columnList = "singlesNormalRating"),
        @Index(name = "idx_player_doubles_normal", columnList = "doublesNormalRating")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Audited
public class Player {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID playerId;

    @NotNull(message = "First name is required")
    @NotEmpty(message = "First name cannot be empty")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotNull(message = "Last name is required")
    @NotEmpty(message = "Last name cannot be empty")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JsonIgnore
    @NotNull(message = "Password is required", groups = ValidationGroups.Create.class)
    @Size(min = 8, message = "Password must be at least 8 characters", groups = {ValidationGroups.Create.class, ValidationGroups.Update.class})
    private String password;

    @Transient
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String matchingPassword;

    @ValidEmail
    @NotNull(message = "Email is required")
    @NotEmpty(message = "Email cannot be empty")
    @Column(unique = true)
    private String email;

    @Column(nullable = false, unique = true)
    @NotNull(message = "Username is required")
    @NotEmpty(message = "Username cannot be empty")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Past(message = "Birthday must be in the past")
    @Temporal(TemporalType.DATE)
    private Date birthday;

    @Column(length = 100)
    @Builder.Default
    private String profileImage = null;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean emailVerified = false;
    
    @Column
    private String invitationCode;

    @Column(name = "role", nullable = false)
    @Builder.Default
    private String role = "USER";
    
    @Column(name = "is_anonymous")
    @Builder.Default
    private Boolean isAnonymous = false;

    // Ratings - initialized to 1000
    @Builder.Default
    private int singlesRankedRating = 1000;

    @Builder.Default
    private int doublesRankedRating = 1000;

    @Builder.Default
    private int singlesNormalRating = 1000;

    @Builder.Default
    private int doublesNormalRating = 1000;

    // Easter egg hunting statistics
    @Column(columnDefinition = "INTEGER DEFAULT 0")
    @Builder.Default
    private int easterEggPoints = 0;

    @Column(columnDefinition = "INTEGER DEFAULT 0")
    @Builder.Default
    private int totalEggsFound = 0;

    @Temporal(TemporalType.TIMESTAMP)
    private Date lastEggFound;

    @Column(columnDefinition = "BOOLEAN DEFAULT true")
    @Builder.Default
    private boolean easterEggHuntingEnabled = true;

    // Game statistics
    @Builder.Default
    private int singlesRankedWins = 0;

    @Builder.Default
    private int singlesRankedLoses = 0;

    @Builder.Default
    private int doublesRankedWins = 0;

    @Builder.Default
    private int doublesRankedLoses = 0;

    @Builder.Default
    private int singlesNormalWins = 0;

    @Builder.Default
    private int singlesNormalLoses = 0;

    @Builder.Default
    private int doublesNormalWins = 0;

    @Builder.Default
    private int doublesNormalLoses = 0;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    @NotAudited
    @Builder.Default
    private List<Match> matchHistory = new ArrayList<>();

    @Transient
    @Builder.Default
    private List<GameResult> gameHistory = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "player_style_ratings",
            joinColumns = @JoinColumn(name = "player_id")
    )
    @NotAudited
    @Builder.Default
    private List<PlayerStyleRating> styleRatings = new ArrayList<>();

    // Audit timestamps
    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created", nullable = false, updatable = false)
    private Date created;

    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "updated", nullable = false)
    private Date updated;

    @Transient
    private String token;

    @PrePersist
    protected void onCreate() {
        updated = created = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        updated = new Date();
    }

    /**
     * Get the full name of the player.
     *
     * @return The full name (first name + last name)
     */
    @Transient
    public String getFullName() {
        return firstName + " " + lastName;
    }

    /**
     * Get the player's overall rating - average of all ratings.
     *
     * @return The overall rating
     */
    @Transient
    public int getOverallRating() {
        return (singlesRankedRating + doublesRankedRating +
                singlesNormalRating + doublesNormalRating) / 4;
    }

    /**
     * Get the total number of matches played.
     *
     * @return Total matches played
     */
    @Transient
    public int getTotalMatchesPlayed() {
        return singlesRankedWins + singlesRankedLoses +
                doublesRankedWins + doublesRankedLoses +
                singlesNormalWins + singlesNormalLoses +
                doublesNormalWins + doublesNormalLoses;
    }

    /**
     * Get the singles ranked win rate.
     *
     * @return Singles ranked win rate as a percentage with 2 decimal places
     */
    @Transient
    public double getSinglesRankedWinRate() {
        int total = singlesRankedWins + singlesRankedLoses;
        double rate = total > 0 ? (double) singlesRankedWins / total * 100 : 0;
        return Math.round(rate * 100.0) / 100.0;
    }

    /**
     * Get the doubles ranked win rate.
     *
     * @return Doubles ranked win rate as a percentage with 2 decimal places
     */
    @Transient
    public double getDoublesRankedWinRate() {
        int total = doublesRankedWins + doublesRankedLoses;
        double rate = total > 0 ? (double) doublesRankedWins / total * 100 : 0;
        return Math.round(rate * 100.0) / 100.0;
    }

    /**
     * Get the singles normal win rate.
     *
     * @return Singles normal win rate as a percentage with 2 decimal places
     */
    @Transient
    public double getSinglesNormalWinRate() {
        int total = singlesNormalWins + singlesNormalLoses;
        double rate = total > 0 ? (double) singlesNormalWins / total * 100 : 0;
        return Math.round(rate * 100.0) / 100.0;
    }

    /**
     * Get the doubles normal win rate.
     *
     * @return Doubles normal win rate as a percentage with 2 decimal places
     */
    @Transient
    public double getDoublesNormalWinRate() {
        int total = doublesNormalWins + doublesNormalLoses;
        double rate = total > 0 ? (double) doublesNormalWins / total * 100 : 0;
        return Math.round(rate * 100.0) / 100.0;
    }

    /**
     * Initialize all style ratings with default values
     */
    public void initializeStyleRatings() {
        // Create a rating for each style type with default value of 50
        for (PlayerStyle style : PlayerStyle.values()) {
            if (getStyleRating(style) == 0) { // Only initialize if not already set
                setStyleRating(style, 50);
            }
        }
    }

    /**
     * Get a player's rating for a specific style
     *
     * @param styleType The style to get the rating for
     * @return The rating value (0-100), or 0 if not rated
     */
    @Transient
    public int getStyleRating(PlayerStyle styleType) {
        return styleRatings.stream()
                           .filter(sr -> sr.getStyleType() == styleType)
                           .map(PlayerStyleRating::getRating)
                           .findFirst()
                           .orElse(0);
    }

    /**
     * Set a player's rating for a specific style
     *
     * @param styleType The style to set the rating for
     * @param rating    The rating value (0-100)
     */
    public void setStyleRating(PlayerStyle styleType, int rating) {
        // Ensure rating is within bounds
        int boundedRating = Math.max(0, Math.min(100, rating));
        if (styleRatings == null) {
            styleRatings = new ArrayList<>();
        }
        PlayerStyleRating styleRating = styleRatings.stream()
                                                    .filter(sr -> sr.getStyleType() == styleType)
                                                    .findFirst()
                                                    .orElse(null);

        if (styleRating != null) {
            styleRating.setRating(boundedRating);
        }
        else {
            styleRatings.add(new PlayerStyleRating(styleType, boundedRating));
        }
    }

    /**
     * Initialize style ratings with specified values
     *
     * @param ratings Map of style types to rating values
     */
    public void initializeStyleRatings(Map<PlayerStyle, Integer> ratings) {
        for (Map.Entry<PlayerStyle, Integer> entry : ratings.entrySet()) {
            setStyleRating(entry.getKey(), entry.getValue());
        }

        // Ensure all styles have a rating by setting defaults for any missing ones
        for (PlayerStyle style : PlayerStyle.values()) {
            if (getStyleRating(style) == 0) {
                setStyleRating(style, 50);
            }
        }
    }

    /**
     * Check if the player's email is verified
     *
     * @return True if email is verified, false otherwise
     */
    @Transient
    public boolean isEmailVerified() {
        return emailVerified != null && emailVerified;
    }


}