import {PlayerStyle, StyleRating} from './player-style';

/**
 * Core interfaces for the application
 * Centralizes model definitions and ensures consistency
 */

/**
 * Player model
 */
export interface Player {
  id: number;
  playerId: string;
  firstName: string;
  lastName: string;
  password: string;
  matchingPassword: string;
  email: string;
  username: string;
  birthday: Date;
  singlesRankedRating: number;
  doublesRankedRating: number;
  singlesNormalRating: number;
  doublesNormalRating: number;
  singlesRankedWins: number;
  singlesRankedLoses: number;
  singlesNormalWins: number;
  singlesNormalLoses: number;
  doublesRankedWins: number;
  doublesRankedLoses: number;
  doublesNormalWins: number;
  doublesNormalLoses: number;
  matchHistory: string[];
  gameHistory: GameResult[];
  styleRatings: StyleRating[];
  created: Date;
  updated: Date;
  profileImage: string;
  token?: string;
  isAnonymous?: boolean;

  // Easter egg hunting fields
  easterEggHuntingEnabled?: boolean;
  easterEggPoints?: number;
  totalEggsFound?: number;
  lastEggFound?: Date;

  // Calculated properties
  fullName?: string;
  singlesRankedWinRate: number;
  doublesRankedWinRate: number;
  singlesNormalWinRate: number;
  doublesNormalWinRate: number;

  // For backward compatibility - returns dominant style types
  playerStyles?: PlayerStyle[];

  // New method to get rating for a specific style
  getStyleRating?(styleType: string): number;
}

export interface GameResult {
  date: string;
  gameType: GameType;
  gameIdentifier: string;
  win: boolean;
}

/**
 * Player ranking model for leaderboard displays
 */
export interface PlayerRanking {
  position: number;
  player: Player;
  rating: number;
  winRate: number;
  wins: number;
  losses: number;
  styleRatings: StyleRating[];
  streak?: number;
  streakType?: 'win' | 'loss';
  achievements?: Achievement[];
  trend?: 'up' | 'down' | 'stable';
  recentGames?: number;
  expertLevel?: number;
}

export enum GameType {
  SINGLES_RANKED = 'SINGLES_RANKED',
  DOUBLES_RANKED = 'DOUBLES_RANKED',
  SINGLES_NORMAL = 'SINGLES_NORMAL',
  DOUBLES_NORMAL = 'DOUBLES_NORMAL'
}

/**
 * Game model
 */
export interface Game {
  gameId?: string;
  matchId?: string | null;
  challengerWin: boolean;
  opponentWin: boolean;
  challengerId: string;
  opponentId: string;
  challengerTeam: string[];
  opponentTeam: string[];
  challengerTeamScore: number;
  opponentTeamScore: number;
  challengerTeamWin: boolean;
  opponentTeamWin: boolean;
  singlesGame: boolean;
  doublesGame: boolean;
  ratedGame: boolean;
  normalGame: boolean;
  datePlayed?: Date;
}

/**
 * AuthenticatedPlayer model
 */
export interface AuthenticatedPlayer {
  player: Player;
  token: string;
}

/**
 * Match model
 */
export interface Match {
  id?: string;
  created?: Date;
  concluded?: boolean;
  matchType?: string; // 'SINGLES' or 'DOUBLES'
  gameMode?: string;  // 'RANKED' or 'NORMAL'
  challengerId?: string;
  opponentId?: string;
  challengerTeam?: string[];
  opponentTeam?: string[];
  challengerWins?: number;
  opponentWins?: number;
  gameIds?: string[];
}

/**
 * Login request model
 */
export interface LoginRequest {
  username: string;
  password: string;
}

/**
 * Registration request model
 */
export interface RegistrationRequest {
  firstName: string;
  lastName: string;
  username: string;
  email: string;
  password: string;
  matchingPassword: string;
  birthday: Date;
}

/**
 * Achievement model for player achievements
 */
export interface Achievement {
  name: string;
  description: string;
  icon: string;
  rarity: 'common' | 'uncommon' | 'rare' | 'legendary';
}

/**
 * Selected players for match builder
 */
export interface SelectedPlayers {
  player1: Player | null;
  player2: Player | null;
  player3: Player | null;
  player4: Player | null;
}

/**
 * Match configuration for match builder
 */
export interface MatchConfig {
  matchType: string;
  isRanked: boolean;
  bestOf: number;
}

export interface EloChartData {
  timestamp: string;
  eloRating: number;
  eloChange: number;
}

export interface RankHistoryData {
  timestamp: string;
  rankPosition: number;
  totalPlayers: number;
  percentile: number;
}

export interface PlayerStyleReview {
  id?: string;
  reviewerId: string;
  playerId: string;
  gameIds: string[];
  strengths: PlayerStyle[];
  improvements: PlayerStyle[];
  reviewDate: Date;
  acknowledged: boolean;
  acknowledgedDate?: Date;
  response: boolean;
  parentReviewId?: string;
  reviewerUsername?: string;
  reviewerFirstName?: string;
  reviewerLastName?: string;
}

export interface PlayerReviewDialogData {
  players: Player[];
  currentPlayerId: string;
  parentReview?: PlayerStyleReview;
  isResponse?: boolean;
}

export interface PlayerReview {
  playerId: string;
  strengths: PlayerStyle[];
  improvements: PlayerStyle[];
  gameIds?: string[];
  response?: boolean;
  parentReviewId?: string;
}
