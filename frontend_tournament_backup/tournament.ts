// src/app/_models/tournament.models.ts

export enum TournamentType {
  SINGLE_ELIMINATION = 'SINGLE_ELIMINATION',
  DOUBLE_ELIMINATION = 'DOUBLE_ELIMINATION',
  ROUND_ROBIN = 'ROUND_ROBIN'
}

export enum GameType {
  SINGLES = 'SINGLES',
  DOUBLES = 'DOUBLES'
}

export enum TournamentStatus {
  CREATED = 'CREATED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED'
}

export enum BracketType {
  WINNER = 'WINNER',
  LOSER = 'LOSER',
  FINAL = 'FINAL',
  CHAMPIONSHIP = 'CHAMPIONSHIP'
}

/**
 * Tournament interface matching the API response
 */
export interface Tournament {
  id?: string;
  name: string;
  description?: string;
  tournamentType: TournamentType;
  gameType: GameType;
  organizerId: string;
  numberOfPlayers: number;
  status: TournamentStatus;
  startDate?: Date;
  endDate?: Date;
  currentRound?: number;
  playerIds?: string[];
  championId?: string;
  runnerUpId?: string;
  isPublic?: boolean;
  matches?: TournamentMatch[];
}

/**
 * Tournament match interface
 */
export interface TournamentMatch {
  matchId?: string;
  id: string;  // Display ID (e.g., "R1M1", "W1", "L2")
  team1Ids: string[];
  team2Ids: string[];
  winnerIds?: string[];
  loserIds?: string[];
  bracketType: BracketType;
  completed: boolean;
  round: number;
  team1Score?: number;
  team2Score?: number;
  scheduledTime?: Date;
  location?: string;
}

/**
 * Tournament player interface
 */
export interface TournamentPlayer {
  id: string;
  playerId: string;
  partnerId?: string; // For doubles
  seedPosition: number;
  finalRanking?: number;
  eliminated: boolean;
}

/**
 * DTO for creating a tournament
 */
export interface TournamentRequestDTO {
  name: string;
  description?: string;
  tournamentType: TournamentType;
  gameType: GameType;
  organizerId: string;
  startDate?: Date;
  endDate?: Date;
  playerIds: string[];
  teamPairs?: TeamPair[];
  seedingMethod?: string;
}

/**
 * Team pair for doubles tournaments
 */
export interface TeamPair {
  player1Id: string;
  player2Id: string;
}

/**
 * DTO for updating match results
 */
export interface MatchResultDTO {
  winnerIds: string[];
  loserIds: string[];
  team1Score?: number;
  team2Score?: number;
}
