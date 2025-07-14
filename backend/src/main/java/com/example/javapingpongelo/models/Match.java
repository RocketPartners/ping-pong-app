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
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    UUID matchId;

    UUID challengerId;

    UUID opponentId;

    boolean challengerWin;

    boolean opponentWin;

    @ElementCollection
    List<UUID> gameIds;

    @ElementCollection
    List<UUID> challengerTeam;

    @ElementCollection
    List<UUID> opponentTeam;

    int challengerTeamTotalPoints;

    int opponentTeamTotalPoints;

    int challengerTeamGameScore;

    int opponentTeamGameScore;

    boolean challengerTeamWin;

    boolean opponentTeamWin;

    boolean singlesGame;

    boolean doublesGame;

    boolean ratedGame;

    boolean normalGame;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date datePlayed;

    @PrePersist
    protected void onCreate() {
        datePlayed = new Date();
    }
}
