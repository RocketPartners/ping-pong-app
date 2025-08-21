package com.example.javapingpongelo.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tournament_match", indexes = {
        @Index(name = "idx_tournament_match_display_id", columnList = "match_display_id"),
        @Index(name = "idx_tournament_match_bracket", columnList = "bracket_type"),
        @Index(name = "idx_tournament_match_completed", columnList = "completed"),
        @Index(name = "idx_tournament_match_round", columnList = "round")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID matchId;

    @Column(nullable = false, name = "match_display_id")
    private String displayId;

    @ElementCollection
    @CollectionTable(
            name = "tournament_match_team1",
            joinColumns = @JoinColumn(name = "match_id")
    )
    @Column(name = "player_id")
    @Builder.Default
    private List<UUID> team1Ids = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "tournament_match_team2",
            joinColumns = @JoinColumn(name = "match_id")
    )
    @Column(name = "player_id")
    @Builder.Default
    private List<UUID> team2Ids = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "tournament_match_winner",
            joinColumns = @JoinColumn(name = "match_id")
    )
    @Column(name = "player_id")
    @Builder.Default
    private List<UUID> winnerIds = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "tournament_match_loser",
            joinColumns = @JoinColumn(name = "match_id")
    )
    @Column(name = "player_id")
    @Builder.Default
    private List<UUID> loserIds = new ArrayList<>();

    @Column(name = "winner_next_match_id")
    private UUID winnerNextMatchId;

    @Column(name = "loser_next_match_id")
    private UUID loserNextMatchId;

    @Column(nullable = false)
    @Builder.Default
    private boolean completed = false;

    @Column(name = "round")
    private Integer round;

    /**
     * Position within the round (for UI display ordering)
     */
    @Column(name = "position_in_round")
    private Integer positionInRound;

    /**
     * Seeding/ranking of team1 at time of match creation
     */
    @Column(name = "team1_seed")
    private Integer team1Seed;

    /**
     * Seeding/ranking of team2 at time of match creation
     */
    @Column(name = "team2_seed")
    private Integer team2Seed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id")
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private Tournament tournament;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_round_id")
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private TournamentRound tournamentRound;

    @Enumerated(EnumType.STRING)
    @Column(name = "bracket_type")
    private BracketType bracketType;

    /**
     * Score for team 1 (if applicable)
     */
    @Column(name = "team1_score")
    private Integer team1Score;

    /**
     * Score for team 2 (if applicable)
     */
    @Column(name = "team2_score")
    private Integer team2Score;

    /**
     * Optional scheduled time for the match
     */
    @Column(name = "scheduled_time")
    private java.util.Date scheduledTime;

    /**
     * Optional location/court information
     */
    @Column(name = "location")
    private String location;

    /**
     * Whether this match is a bye (one team automatically advances)
     */
    @Column(name = "is_bye")
    @Builder.Default
    private boolean isBye = false;

    /**
     * Which team gets the bye (if applicable)
     */
    @Column(name = "bye_team")
    private Integer byeTeam; // 1 or 2

    public enum BracketType {
        WINNER,           // Winner's bracket match
        LOSER,           // Loser's bracket match  
        FINAL,           // Final match
        GRAND_FINAL,     // Grand final (winner's bracket winner vs loser's bracket winner)
        GRAND_FINAL_RESET // Second grand final if loser's bracket winner wins first
    }

    /**
     * Helper method to check if this match can be played
     */
    public boolean isPlayable() {
        return !isBye && !completed && 
               ((team1Ids != null && !team1Ids.isEmpty()) || 
                (team2Ids != null && !team2Ids.isEmpty()));
    }

    /**
     * Helper method to check if match has both teams assigned
     */
    public boolean hasAllTeams() {
        return team1Ids != null && !team1Ids.isEmpty() && 
               team2Ids != null && !team2Ids.isEmpty();
    }
}