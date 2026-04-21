import {Injectable} from '@angular/core';
import {BehaviorSubject, Observable} from 'rxjs';
import {Player} from '../../_models/models';

export type KioskMatchType = 'singles' | 'doubles';

export interface KioskMatchConfig {
  matchType: KioskMatchType;
  isRanked: boolean;
  bestOf: number;
}

export interface KioskGame {
  team1Score: number;
  team2Score: number;
  concluded: boolean;
  winner?: 1 | 2;
}

export interface KioskMatchState {
  config: KioskMatchConfig | null;
  team1: Player[];
  team2: Player[];
  games: KioskGame[];
  pointLog: (1 | 2)[];
  currentGameIndex: number;
  finishedAt?: string;
}

const STORAGE_KEY = 'kiosk.activeMatch';
const MAX_POINTS_PER_GAME = 40;

/**
 * Office rule: singles plays to 11, doubles plays to 21 (both win-by-2).
 */
function winningScoreFor(matchType: KioskMatchType): number {
  return matchType === 'doubles' ? 21 : 11;
}

function emptyState(): KioskMatchState {
  return {
    config: null,
    team1: [],
    team2: [],
    games: [],
    pointLog: [],
    currentGameIndex: 0
  };
}

@Injectable({providedIn: 'root'})
export class LiveGameStateService {
  private subject = new BehaviorSubject<KioskMatchState>(this.load());
  state$: Observable<KioskMatchState> = this.subject.asObservable();

  get snapshot(): KioskMatchState {
    return this.subject.value;
  }

  startMatch(config: KioskMatchConfig): void {
    const state: KioskMatchState = {
      ...emptyState(),
      config,
      games: [{team1Score: 0, team2Score: 0, concluded: false}]
    };
    this.emit(state);
  }

  setTeams(team1: Player[], team2: Player[]): void {
    const state = {...this.snapshot, team1: [...team1], team2: [...team2]};
    this.emit(state);
  }

  incrementScore(team: 1 | 2): void {
    const state = structuredClone(this.snapshot) as KioskMatchState;
    const game = state.games[state.currentGameIndex];
    if (!game || game.concluded || !state.config) return;
    if (team === 1) game.team1Score += 1;
    else game.team2Score += 1;
    state.pointLog.push(team);
    if (this.gameIsOver(game, state.config.matchType)) {
      game.concluded = true;
      game.winner = game.team1Score > game.team2Score ? 1 : 2;
    }
    if (game.team1Score + game.team2Score > MAX_POINTS_PER_GAME) {
      // Safety valve: never allow runaway scores
      return;
    }
    this.emit(state);
  }

  undoLastPoint(): void {
    const state = structuredClone(this.snapshot) as KioskMatchState;
    const last = state.pointLog.pop();
    if (last === undefined) return;
    const game = state.games[state.currentGameIndex];
    if (!game) return;
    if (last === 1 && game.team1Score > 0) game.team1Score -= 1;
    else if (last === 2 && game.team2Score > 0) game.team2Score -= 1;
    if (game.concluded) {
      game.concluded = false;
      game.winner = undefined;
    }
    this.emit(state);
  }

  advanceToNextGame(): boolean {
    const state = structuredClone(this.snapshot) as KioskMatchState;
    if (!state.config) return false;
    if (this.matchIsOver(state)) return false;
    state.games.push({team1Score: 0, team2Score: 0, concluded: false});
    state.currentGameIndex = state.games.length - 1;
    this.emit(state);
    return true;
  }

  /**
   * True when a team has reached the best-of win threshold (e.g. 2 wins in best-of-3).
   */
  matchIsOver(state: KioskMatchState = this.snapshot): boolean {
    if (!state.config) return false;
    const threshold = Math.ceil(state.config.bestOf / 2);
    const team1Wins = state.games.filter(g => g.concluded && g.winner === 1).length;
    const team2Wins = state.games.filter(g => g.concluded && g.winner === 2).length;
    return team1Wins >= threshold || team2Wins >= threshold;
  }

  discard(): void {
    this.emit(emptyState());
  }

  markFinished(): void {
    const state = {...this.snapshot, finishedAt: new Date().toISOString()};
    this.emit(state);
  }

  private gameIsOver(game: KioskGame, matchType: KioskMatchType): boolean {
    const leader = Math.max(game.team1Score, game.team2Score);
    const trailer = Math.min(game.team1Score, game.team2Score);
    return leader >= winningScoreFor(matchType) && leader - trailer >= 2;
  }

  private emit(state: KioskMatchState): void {
    this.subject.next(state);
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    } catch {
      // localStorage is best-effort; not fatal.
    }
  }

  private load(): KioskMatchState {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (!raw) return emptyState();
      const parsed = JSON.parse(raw) as KioskMatchState;
      if (!parsed || typeof parsed !== 'object') return emptyState();
      return {...emptyState(), ...parsed};
    } catch {
      return emptyState();
    }
  }
}
