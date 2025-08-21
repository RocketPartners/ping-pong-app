// src/app/_models/tournament-new.ts
// New tournament models optimized for brackets-manager integration

/**
 * Tournament types supported by brackets-manager
 */
export enum TournamentType {
  SINGLE_ELIMINATION = 'SINGLE_ELIMINATION',
  DOUBLE_ELIMINATION = 'DOUBLE_ELIMINATION',
  ROUND_ROBIN = 'ROUND_ROBIN'
}

export enum TournamentStatus {
  CREATED = 'CREATED',
  READY_TO_START = 'READY_TO_START',
  IN_PROGRESS = 'IN_PROGRESS',
  ROUND_COMPLETE = 'ROUND_COMPLETE',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED'
}

/**
 * Main tournament interface - simplified for brackets-manager integration
 */
export interface Tournament {
  // Basic tournament info
  id: string;
  name: string;
  description?: string;
  type: TournamentType;
  status: TournamentStatus;
  
  // Tournament metadata
  organizerId: string;
  isPublic: boolean;
  
  // Dates
  createdAt: Date;
  startDate?: Date;
  endDate?: Date;
  
  // brackets-manager data (stored as JSON in backend)
  bracketData?: BracketData;
  
  // Participant info
  maxParticipants: number;
  currentParticipants: number;
  
  // Results
  winnerId?: number;
  runnerUpId?: number;
}

/**
 * Tournament participant - matches brackets-manager participant structure
 */
export interface TournamentParticipant {
  id: number;           // brackets-manager uses numeric IDs
  playerId: string;     // Our backend player ID
  name: string;         // Display name
  seed?: number;        // Seeding position
}

/**
 * Complete bracket data structure from brackets-manager
 */
export interface BracketData {
  stage: Stage[];
  match: Match[];
  match_game: MatchGame[];
  participant: Participant[];
}

/**
 * brackets-manager Stage interface
 */
export interface Stage {
  id: number;
  tournament_id: number;
  name: string;
  type: 'single_elimination' | 'double_elimination';
  number: number;
  settings: StageSettings;
}

export interface StageSettings {
  seedOrdering?: string[];
  grandFinal?: 'simple' | 'double';
  skipFirstRound?: boolean;
  balanceByes?: boolean;
}

/**
 * brackets-manager Match interface
 */
export interface Match {
  id: number;
  stage_id: number;
  group_id: number;
  round_id: number;
  number: number;
  status: MatchStatus;
  opponent1?: Opponent;
  opponent2?: Opponent;
}

export interface Opponent {
  id: number | null;
  position?: number;
  forfeit?: boolean;
  score?: number;
  result?: 'win' | 'loss';
}

export enum MatchStatus {
  LOCKED = 0,
  WAITING = 1,
  READY = 2,
  RUNNING = 3,
  COMPLETED = 4,
  ARCHIVED = 5
}

/**
 * brackets-manager MatchGame interface (for best-of series)
 */
export interface MatchGame {
  id: number;
  match_id: number;
  number: number;
  status: MatchStatus;
  opponent1: Opponent;
  opponent2: Opponent;
}

/**
 * brackets-manager Participant interface
 */
export interface Participant {
  id: number;
  tournament_id?: number;
  name: string;
}

/**
 * DTO for creating a new tournament
 */
export enum SeedingMethod {
  RATING_BASED = 'RATING_BASED',
  RANDOM = 'RANDOM', 
  MANUAL = 'MANUAL'
}

export enum GameType {
  SINGLES = 'SINGLES',
  DOUBLES = 'DOUBLES'
}

/**
 * Bracket type for tournament matches - matches backend BracketType exactly
 */
export enum BracketType {
  WINNER = 'WINNER',             // Winner's bracket match
  LOSER = 'LOSER',               // Loser's bracket match  
  FINAL = 'FINAL',               // Final match
  GRAND_FINAL = 'GRAND_FINAL',   // Grand final (winner's bracket winner vs loser's bracket winner)
  GRAND_FINAL_RESET = 'GRAND_FINAL_RESET' // Second grand final if loser's bracket winner wins first
}

export interface CreateTournamentRequest {
  name: string;
  description?: string;
  tournamentType: TournamentType;  // Match backend field name
  gameType: GameType;              // Add required field
  seedingMethod: SeedingMethod;    // Use enum instead of string union
  organizerId: string;
  playerIds: string[];            // Player IDs to include
  startDate: Date;               // Make required to match backend
  endDate?: Date;                // Add optional end date
}

/**
 * DTO for updating match results
 */
export interface UpdateMatchRequest {
  matchId: number;
  winnerId: number;
  winnerScore?: number;
  loserScore?: number;
}

/**
 * Tournament list item (for tournament-list component)
 */
export interface TournamentListItem {
  id: string;
  name: string;
  description?: string;
  type: TournamentType;
  status: TournamentStatus;
  organizerId: string;
  organizerName: string;
  currentParticipants: number;
  maxParticipants: number;
  createdAt: Date;
  startDate?: Date;
  isPublic: boolean;
}

/**
 * Tournament details for display
 */
export interface TournamentDetails extends Tournament {
  participants: TournamentParticipant[];
  organizerName: string;
  canEdit: boolean;
  canJoin: boolean;
  canStart: boolean;
  canComplete: boolean;
}

/**
 * Player information for tournament display
 */
export interface PlayerInfo {
  playerId: string;
  username: string;
  currentElo: number;
  profilePictureUrl?: string;
}

/**
 * Tournament statistics
 */
export interface TournamentStats {
  totalMatches: number;
  completedMatches: number;
  pendingMatches: number;
  averageMatchDuration?: number;
  longestMatch?: number;
  shortestMatch?: number;
}

/**
 * Error response from tournament operations
 */
export interface TournamentError {
  code: string;
  message: string;
  details?: any;
}

/**
 * Tournament event for real-time updates
 */
export interface TournamentEvent {
  type: 'match_completed' | 'tournament_started' | 'tournament_completed' | 'tournament_created' | 'participant_joined';
  tournamentId: string;
  data: any;
  timestamp: Date;
}