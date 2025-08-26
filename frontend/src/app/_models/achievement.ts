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
  opponentName?: string;
  gameDatePlayed?: Date;
}

// Combined data object returned from the API
export interface AchievementDTO {
  achievement: Achievement;
  playerProgress: PlayerAchievement;
  percentComplete: number; // Calculated field: (progress / threshold) * 100
}

// Interface for achievement notifications
export interface AchievementNotification {
  achievement: Achievement;
  timestamp: Date;
  seen: boolean;
}
