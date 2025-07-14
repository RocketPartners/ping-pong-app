package com.example.javapingpongelo.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tournament", indexes = {
        @Index(name = "idx_tournament_status", columnList = "status"),
        @Index(name = "idx_tournament_organizer", columnList = "organizer_id"),
        @Index(name = "idx_tournament_dates", columnList = "start_date, end_date"),
        @Index(name = "idx_tournament_type", columnList = "tournament_type"),
        @Index(name = "idx_tournament_game_type", columnList = "game_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tournament {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @NotNull(message = "Tournament name is required")
    @NotEmpty(message = "Tournament name cannot be empty")
    @Size(min = 3, max = 100, message = "Tournament name must be between 3 and 100 characters")
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "tournament_type")
    @NotNull(message = "Tournament type is required")
    private TournamentType tournamentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "game_type")
    @NotNull(message = "Game type is required")
    private GameType gameType;

    @Enumerated(EnumType.STRING)
    @Column(name = "seeding_method")
    @NotNull(message = "Seeding method is required")
    private SeedingMethod seedingMethod;

    @NotNull(message = "Organizer ID is required")
    @Column(name = "organizer_id")
    private UUID organizerId;

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    @Builder.Default
    private List<TournamentPlayer> tournamentPlayers = new ArrayList<>();

    @OneToMany(mappedBy = "tournament", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @Builder.Default
    private List<TournamentMatch> matches = new ArrayList<>();

    @Transient
    @Builder.Default
    private List<UUID> playerIds = new ArrayList<>();

    @Min(value = 2, message = "Number of players must be at least 2")
    @Column(name = "number_of_players")
    private int numberOfPlayers;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Tournament status is required")
    private TournamentStatus status;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "start_date")
    @NotNull(message = "Start date is required")
    private Date startDate;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "end_date")
    private Date endDate;

    @Transient
    private UUID championId;

    @Transient
    private UUID runnerUpId;

    @Column(name = "current_round")
    private Integer currentRound;

    @CreationTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false, updatable = false)
    private Date created;

    @UpdateTimestamp
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date updated;

    /**
     * Additional tournament settings stored as JSON
     */
    @Column(name = "settings", columnDefinition = "TEXT")
    private String settings;

    /**
     * Flag to indicate if the tournament is public or private
     */
    @Column(name = "is_public")
    @Builder.Default
    private boolean isPublic = true;

    @Override
    public String toString() {
        return "Tournament{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", tournamentType=" + tournamentType +
                ", gameType=" + gameType +
                ", seedingMethod=" + seedingMethod +
                ", organizerId='" + organizerId + '\'' +
                ", numberOfPlayers=" + numberOfPlayers +
                ", status=" + status +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", currentRound=" + currentRound +
                ", created=" + created +
                ", updated=" + updated +
                '}';
    }

    // Enum definitions
    public enum TournamentType {
        SINGLE_ELIMINATION,
        DOUBLE_ELIMINATION,
        ROUND_ROBIN
    }

    public enum GameType {
        SINGLES,
        DOUBLES
    }

    public enum SeedingMethod {
        RATING_BASED,
        RANDOM
    }

    public enum TournamentStatus {
        CREATED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }
}