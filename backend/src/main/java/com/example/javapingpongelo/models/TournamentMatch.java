package com.example.javapingpongelo.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
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
    private String id;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id")
    @JsonIgnore
    private Tournament tournament;

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

    public enum BracketType {
        WINNER,
        FINAL,
        CHAMPIONSHIP,
        LOSER
    }
}