package com.example.javapingpongelo.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Represents a single round within a tournament
 * Helps manage round-based progression and hybrid manual/auto advancement
 */
@Entity
@Table(name = "tournament_round", indexes = {
        @Index(name = "idx_tournament_round_tournament", columnList = "tournament_id"),
        @Index(name = "idx_tournament_round_number", columnList = "round_number"),
        @Index(name = "idx_tournament_round_bracket", columnList = "bracket_type"),
        @Index(name = "idx_tournament_round_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentRound {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID roundId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    private Tournament tournament;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "bracket_type", nullable = false)
    private TournamentMatch.BracketType bracketType;

    @Column(name = "name")
    private String name; // e.g., "Round 1", "Quarterfinals", "Finals"

    @OneToMany(mappedBy = "tournamentRound", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<TournamentMatch> matches = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private RoundStatus status = RoundStatus.PENDING;

    /**
     * When this round was started (made playable)
     */
    @Column(name = "started_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date startedAt;

    /**
     * When this round was completed (all matches finished)
     */
    @Column(name = "completed_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date completedAt;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false, updatable = false)
    private Date created;

    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date updated;

    public enum RoundStatus {
        PENDING,        // Round created but not started
        READY,          // Round ready to start (waiting for admin approval)
        ACTIVE,         // Round is active and matches can be played
        COMPLETED       // All matches in round are finished
    }

    /**
     * Check if all matches in this round are completed
     */
    public boolean isComplete() {
        return matches.stream().allMatch(TournamentMatch::isCompleted);
    }

    /**
     * Get the number of completed matches in this round
     */
    public int getCompletedMatchCount() {
        return (int) matches.stream().filter(TournamentMatch::isCompleted).count();
    }

    /**
     * Get the total number of matches in this round
     */
    public int getTotalMatchCount() {
        return matches.size();
    }

    /**
     * Check if this round can be started
     */
    public boolean canStart() {
        return status == RoundStatus.READY && matches.stream().anyMatch(TournamentMatch::hasAllTeams);
    }
}