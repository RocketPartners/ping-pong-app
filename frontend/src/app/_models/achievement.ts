export enum AchievementCategory {
  EASY = 'EASY',
  MEDIUM = 'MEDIUM',
  HARD = 'HARD',
  LEGENDARY = 'LEGENDARY'
}

export enum AchievementType {
  ONE_TIME = 'ONE_TIME',
  PROGRESSIVE = 'PROGRESSIVE'
}

export interface Achievement {
  id: string;
  name: string;
  description: string;
  category: AchievementCategory;
  type: AchievementType;
  criteria: any; // JSON object representing criteria
  icon: string;
  points: number;
  isVisible: boolean;
}

export interface PlayerAchievement {
  id: string;
  playerId: string;
  achievementId: string;
  achieved: boolean;
  progress: number;
  dateEarned?: Date;
  opponentName?: string;        // Contextual data for achievements like Gilyed
  gameDatePlayed?: Date;        // When the contextual event occurred
  dependenciesMet?: boolean;    // Whether prerequisite achievements are met
  availableToEarn?: boolean;    // Whether this achievement can currently be earned
}

// Player dependency information for achievement cards
export interface PlayerDependencyInfo {
  totalDependencies: number;
  completedDependencies: number;
  hasBlockingDependencies: boolean;
  nextAvailableAchievements?: string[];  // IDs of achievements that will unlock
}

// Combined data object returned from the API
export interface AchievementDTO {
  achievement: Achievement;
  playerProgress: PlayerAchievement;
  percentComplete: number; // Calculated field: (progress / threshold) * 100
  playerDependencyInfo?: PlayerDependencyInfo;  // Dependency information for this achievement
  recentlyUnlocked?: boolean; // Flag indicating if this achievement was recently unlocked
}

// Interface for achievement notifications
export interface AchievementNotification {
  achievement: Achievement;
  timestamp: Date;
  seen: boolean;
  celebrationLevel?: CelebrationLevel;  // Enhanced with celebration levels
  contextualMessage?: string;           // Custom contextual message
  channels?: NotificationChannels;      // Which notification channels were used
}

// Celebration levels for achievement notifications
export enum CelebrationLevel {
  NORMAL = 'NORMAL',
  SPECIAL = 'SPECIAL',
  EPIC = 'EPIC'
}

// Notification channels configuration
export interface NotificationChannels {
  slackChannel: boolean;
  slackDm: boolean;
  inApp: boolean;
  email: boolean;
}

// Achievement analytics data
export interface AchievementAnalytics {
  id: string;
  achievementId: string;
  achievement?: Achievement;             // Reference to the achievement
  totalCompletions: number;
  totalPlayersAttempted: number;
  completionRate: number;               // Percentage (0-100)
  difficultyScore?: number;
  calculatedDifficulty: CalculatedDifficulty;
  averageCompletionTimeHours?: number;
  fastestCompletionTimeHours?: number;
  slowestCompletionTimeHours?: number;
  playersWithProgress: number;
  activePlayersPursuing: number;
  averageProgressPercentage?: number;
  completionsLast7Days: number;
  completionsLast30Days: number;
  completionTrend: string;              // INCREASING, STABLE, DECREASING
  firstCompletionDate?: Date;
  latestCompletionDate?: Date;
  lastUpdated?: Date;
}

// Calculated difficulty levels
export enum CalculatedDifficulty {
  VERY_EASY = 'VERY_EASY',
  EASY = 'EASY', 
  MODERATE = 'MODERATE',
  HARD = 'HARD',
  VERY_HARD = 'VERY_HARD'
}

// Achievement dependency relationship
export interface AchievementDependency {
  id: string;
  achievementId: string;
  prerequisiteAchievementId: string;
  dependencyType: DependencyType;
  dependencyOrder?: number;
  prerequisiteAchievement?: Achievement;  // Populated for easier access
}

// Dependency types
export enum DependencyType {
  REQUIRED = 'REQUIRED',     // Must complete before this achievement is available
  SUGGESTED = 'SUGGESTED',   // Recommended but not required
  UNLOCKS = 'UNLOCKS'        // This achievement unlocks others
}

// YAML configuration structure
export interface AchievementConfiguration {
  id: string;
  name: string;
  description: string;
  category: string;
  type: string;
  icon: string;
  points: number;
  visible: boolean;
  criteria: CriteriaConfig;
  triggers?: TriggerConfig[];
  dependencies?: DependencyConfig[];
  notifications?: NotificationConfig;
  metadata?: { [key: string]: any };
}

// Configuration sub-interfaces
export interface CriteriaConfig {
  type: string;
  threshold?: number;
  gameType?: string;
  secondaryValue?: number;
  achievementType?: string;
  additionalParams?: { [key: string]: any };
}

export interface TriggerConfig {
  triggerType: string;
  gameTypes?: string[];
  conditions?: { [key: string]: any };
}

export interface DependencyConfig {
  prerequisiteId: string;
  dependencyType: string;
  order?: number;
}

export interface NotificationConfig {
  slackChannel?: boolean;
  slackDm?: boolean;
  inApp?: boolean;
  email?: boolean;
  customMessage?: string;
  celebrationLevel?: string;
  templateData?: { [key: string]: any };
}

// Admin analytics summary
export interface AnalyticsSummary {
  totalAchievements: number;
  totalPlayersWithProgress: number;
  averageCompletionRate: number;
  averageDifficultyScore: number;
  averageCompletionTimeHours: number;
  completionsToday: number;
  completionRateDistribution: { [key: string]: number };
  difficultyDistribution: { [key: string]: number };
  recentTrends: { [key: string]: number };
  achievementsNeedingBalance: number;
  lastCalculated?: Date;
}

// Player dependency tree structure for visualization
export interface PlayerDependencyTree {
  playerId: string;
  achievements: AchievementNode[];
  dependencies: AchievementDependency[];
  completionStats: {
    totalEarned: number;
    totalAvailable: number;
    totalLocked: number;
    completionPercentage: number;
  };
}

export interface AchievementNode {
  achievement: Achievement;
  playerProgress: PlayerAchievement;
  analytics?: AchievementAnalytics;
  dependencies: AchievementDependency[];
  dependents: AchievementDependency[];    // Achievements that depend on this one
  status: AchievementStatus;
  recommendedNext?: boolean;              // Suggested as next achievement to pursue
}

// Achievement status for dependency tree visualization
export enum AchievementStatus {
  EARNED = 'EARNED',           // Achievement is completed
  AVAILABLE = 'AVAILABLE',     // Can be earned (dependencies met)
  IN_PROGRESS = 'IN_PROGRESS', // Has some progress
  LOCKED = 'LOCKED',           // Dependencies not met
  HIDDEN = 'HIDDEN'            // Not visible to player
}
