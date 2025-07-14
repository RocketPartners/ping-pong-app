package com.example.javapingpongelo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "game", indexes = {
        @Index(name = "idx_game_date", columnList = "datePlayed"),
        @Index(name = "idx_game_match", columnList = "matchId"),
        @Index(name = "idx_game_challenger", columnList = "challengerId"),
        @Index(name = "idx_game_opponent", columnList = "opponentId"),
        @Index(name = "idx_game_type", columnList = "singlesGame, doublesGame, ratedGame, normalGame")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Game {
    @Column
    UUID matchId;

    @Column
    boolean challengerWin;

    @Column
    boolean opponentWin;

    @Column
    UUID challengerId;

    @Column
    UUID opponentId;

    @ElementCollection(fetch = FetchType.EAGER) // Small list, EAGER is fine
    @CollectionTable(
            name = "game_challenger_team",
            joinColumns = @JoinColumn(name = "game_id"),
            indexes = {
                    @Index(name = "idx_challenger_team_game", columnList = "game_id"),
                    @Index(name = "idx_challenger_team_member", columnList = "challengerTeam")
            }
    )
    List<UUID> challengerTeam;

    @ElementCollection(fetch = FetchType.EAGER) // Small list, EAGER is fine
    @CollectionTable(
            name = "game_opponent_team",
            joinColumns = @JoinColumn(name = "game_id"),
            indexes = {
                    @Index(name = "idx_opponent_team_game", columnList = "game_id"),
                    @Index(name = "idx_opponent_team_member", columnList = "opponentTeam")
            }
    )
    List<UUID> opponentTeam;

    @Column
    int challengerTeamScore;

    @Column
    int opponentTeamScore;

    @Column
    boolean challengerTeamWin;

    @Column
    boolean opponentTeamWin;

    @Column
    boolean singlesGame;

    @Column
    boolean doublesGame;

    @Column
    boolean ratedGame;

    @Column
    boolean normalGame;

    @Id
    @GeneratedValue
    @Column(name = "game_id", updatable = false, nullable = false)
    private UUID gameId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date datePlayed;

    @PrePersist
    protected void onCreate() {
        datePlayed = new Date();
    }
}