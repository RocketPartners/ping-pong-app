package com.example.javapingpongelo.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Entity that defines prerequisite relationships between achievements.
 * Some achievements require other achievements to be completed first.
 */
@Entity
@Table(name = "achievement_dependency",
       uniqueConstraints = @UniqueConstraint(columnNames = {"achievement_id", "prerequisite_achievement_id"}),
       indexes = {
           @Index(name = "idx_achievement_dep_achievement", columnList = "achievement_id"),
           @Index(name = "idx_achievement_dep_prerequisite", columnList = "prerequisite_achievement_id")
       })
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AchievementDependency {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "achievement_id", nullable = false)
    private UUID achievementId;

    @Column(name = "prerequisite_achievement_id", nullable = false)
    private UUID prerequisiteAchievementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "achievement_id", insertable = false, updatable = false)
    private Achievement achievement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prerequisite_achievement_id", insertable = false, updatable = false)
    private Achievement prerequisiteAchievement;

    @Enumerated(EnumType.STRING)
    @Column(name = "dependency_type")
    @Builder.Default
    private DependencyType dependencyType = DependencyType.REQUIRED;

    @Column(name = "dependency_order")
    private Integer dependencyOrder; // For ordering multiple prerequisites

    /**
     * Types of dependencies between achievements
     */
    public enum DependencyType {
        REQUIRED,    // Must have prerequisite to unlock this achievement
        SUGGESTED,   // Suggested to have prerequisite (shows in UI but not enforced)
        UNLOCKS      // Completing prerequisite makes this achievement visible
    }
}