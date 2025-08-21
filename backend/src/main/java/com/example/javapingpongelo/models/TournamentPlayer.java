package com.example.javapingpongelo.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Entity representing the many-to-many relationship between tournaments and players.
 * Also stores additional information like seeding position and doubles partner.
 */
@Entity
@Table(name = "tournament_player",
        uniqueConstraints = @UniqueConstraint(columnNames = {"tournament_id", "player_id"}),
        indexes = {
                @Index(name = "idx_tournament_player_tournament", columnList = "tournament_id"),
                @Index(name = "idx_tournament_player_player", columnList = "player_id"),
                @Index(name = "idx_tournament_player_seed", columnList = "seed_position")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    @JsonIgnore
    private Tournament tournament;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    /**
     * For doubles tournaments, stores the partner's player ID
     */
    @Column(name = "partner_id")
    private UUID partnerId;

    /**
     * The player's seeding position in the tournament (1 = best seed)
     */
    @Column(name = "seed_position")
    private Integer seed;

    /**
     * Player's final ranking in the tournament (after completion)
     */
    @Column(name = "final_ranking")
    private Integer finalRanking;

    /**
     * Whether the player has been eliminated from the tournament
     */
    @Column(name = "eliminated", nullable = false)
    @Builder.Default
    private boolean eliminated = false;

    /**
     * Round in which the player was eliminated (null if still active)
     */
    @Column(name = "eliminated_in_round")
    private Integer eliminatedInRound;

}