// src/app/_services/tournament-api.service.ts
// Service for tournament backend API communication

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError } from 'rxjs';
import { map, tap, catchError } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  Tournament,
  TournamentListItem,
  TournamentDetails,
  CreateTournamentRequest,
  UpdateMatchRequest,
  TournamentParticipant,
  PlayerInfo,
  TournamentStats,
  TournamentEvent,
  TournamentStatus,
  BracketData
} from '../_models/tournament-new';

@Injectable({
  providedIn: 'root'
})
export class TournamentApiService {
  private readonly apiUrl = `${environment.apiUrl}/api/tournaments`;
  
  // Real-time tournament updates
  private tournamentUpdates$ = new BehaviorSubject<TournamentEvent | null>(null);
  public readonly tournamentUpdates = this.tournamentUpdates$.asObservable();

  constructor(private http: HttpClient) {}

  // Tournament CRUD Operations

  /**
   * Creates a new tournament in the backend
   */
  createTournament(request: CreateTournamentRequest): Observable<Tournament> {
    return this.http.post<Tournament>(this.apiUrl, request).pipe(
      tap(tournament => this.emitTournamentEvent('tournament_created', tournament.id, tournament)),
      catchError(this.handleError('createTournament'))
    );
  }

  /**
   * Gets all tournaments for listing
   */
  getTournaments(
    page: number = 0,
    size: number = 20,
    status?: TournamentStatus,
    organizerId?: string
  ): Observable<{ content: TournamentListItem[], totalElements: number }> {
    let params: any = { page, size };
    if (status) params.status = status;
    if (organizerId) params.organizerId = organizerId;

    return this.http.get<{ content: TournamentListItem[], totalElements: number }>(
      this.apiUrl, { params }
    ).pipe(
      catchError(this.handleError('getTournaments'))
    );
  }

  /**
   * Gets detailed tournament information
   */
  getTournamentDetails(tournamentId: string): Observable<TournamentDetails> {
    return this.http.get<TournamentDetails>(`${this.apiUrl}/${tournamentId}`).pipe(
      catchError(this.handleError('getTournamentDetails'))
    );
  }

  /**
   * Updates tournament basic information
   */
  updateTournament(tournamentId: string, updates: Partial<Tournament>): Observable<Tournament> {
    return this.http.put<Tournament>(`${this.apiUrl}/${tournamentId}`, updates).pipe(
      catchError(this.handleError('updateTournament'))
    );
  }

  /**
   * Deletes a tournament
   */
  deleteTournament(tournamentId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${tournamentId}`).pipe(
      catchError(this.handleError('deleteTournament'))
    );
  }

  // Tournament State Management

  /**
   * Starts a tournament (changes status to IN_PROGRESS)
   */
  startTournament(tournamentId: string): Observable<Tournament> {
    return this.http.post<Tournament>(`${this.apiUrl}/${tournamentId}/start`, {}).pipe(
      tap(tournament => this.emitTournamentEvent('tournament_started', tournament.id, tournament)),
      catchError(this.handleError('startTournament'))
    );
  }

  /**
   * Completes a tournament (changes status to COMPLETED)
   */
  completeTournament(tournamentId: string, winnerId?: number, runnerUpId?: number): Observable<Tournament> {
    return this.http.post<Tournament>(`${this.apiUrl}/${tournamentId}/complete`, {
      winnerId,
      runnerUpId
    }).pipe(
      tap(tournament => this.emitTournamentEvent('tournament_completed', tournament.id, tournament)),
      catchError(this.handleError('completeTournament'))
    );
  }

  /**
   * Cancels a tournament
   */
  cancelTournament(tournamentId: string, reason?: string): Observable<Tournament> {
    return this.http.post<Tournament>(`${this.apiUrl}/${tournamentId}/cancel`, {
      reason
    }).pipe(
      catchError(this.handleError('cancelTournament'))
    );
  }

  // Bracket Data Synchronization

  /**
   * Saves bracket data to backend (for persistence)
   */
  saveBracketData(tournamentId: string, bracketData: BracketData): Observable<void> {
    return this.http.put<void>(`${this.apiUrl}/${tournamentId}/bracket`, bracketData).pipe(
      catchError(this.handleError('saveBracketData'))
    );
  }

  /**
   * Retrieves bracket data from backend
   */
  getBracketData(tournamentId: string): Observable<BracketData | null> {
    return this.http.get<BracketData>(`${this.apiUrl}/${tournamentId}/bracket`).pipe(
      catchError(err => {
        if (err.status === 404) {
          return [null]; // No bracket data exists yet
        }
        return throwError(err);
      })
    );
  }

  // Match Management

  /**
   * Updates match result and syncs with backend
   */
  updateMatchResult(tournamentId: string, request: UpdateMatchRequest): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/${tournamentId}/matches/${request.matchId}/result`, {
      winnerId: request.winnerId,
      winnerScore: request.winnerScore,
      loserScore: request.loserScore
    }).pipe(
      tap(() => this.emitTournamentEvent('match_completed', tournamentId, request)),
      catchError(this.handleError('updateMatchResult'))
    );
  }

  // Participant Management

  /**
   * Adds participants to a tournament
   */
  addParticipants(tournamentId: string, playerIds: string[]): Observable<TournamentParticipant[]> {
    return this.http.post<TournamentParticipant[]>(`${this.apiUrl}/${tournamentId}/participants`, {
      playerIds
    }).pipe(
      tap(() => this.emitTournamentEvent('participant_joined', tournamentId, playerIds)),
      catchError(this.handleError('addParticipants'))
    );
  }

  /**
   * Removes participants from a tournament
   */
  removeParticipants(tournamentId: string, participantIds: number[]): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${tournamentId}/participants`, {
      body: { participantIds }
    }).pipe(
      catchError(this.handleError('removeParticipants'))
    );
  }

  /**
   * Gets tournament participants with player details
   */
  getTournamentParticipants(tournamentId: string): Observable<TournamentParticipant[]> {
    return this.http.get<TournamentParticipant[]>(`${this.apiUrl}/${tournamentId}/participants`).pipe(
      catchError(this.handleError('getTournamentParticipants'))
    );
  }

  // Player Information

  /**
   * Gets player information for tournament display
   */
  getPlayersInfo(playerIds: string[]): Observable<PlayerInfo[]> {
    return this.http.post<PlayerInfo[]>(`${environment.apiUrl}/api/players/batch`, {
      playerIds
    }).pipe(
      catchError(this.handleError('getPlayersInfo'))
    );
  }

  /**
   * Searches players for tournament invitation
   */
  searchPlayers(query: string, limit: number = 10): Observable<PlayerInfo[]> {
    return this.http.get<PlayerInfo[]>(`${environment.apiUrl}/api/players/search`, {
      params: { query, limit: limit.toString() }
    }).pipe(
      catchError(this.handleError('searchPlayers'))
    );
  }

  // Statistics and Analytics

  /**
   * Gets tournament statistics
   */
  getTournamentStats(tournamentId: string): Observable<TournamentStats> {
    return this.http.get<TournamentStats>(`${this.apiUrl}/${tournamentId}/stats`).pipe(
      catchError(this.handleError('getTournamentStats'))
    );
  }

  /**
   * Gets user's tournament history
   */
  getUserTournamentHistory(
    playerId: string, 
    page: number = 0, 
    size: number = 10
  ): Observable<{ content: TournamentListItem[], totalElements: number }> {
    return this.http.get<{ content: TournamentListItem[], totalElements: number }>(
      `${environment.apiUrl}/api/players/${playerId}/tournaments`,
      { params: { page: page.toString(), size: size.toString() } }
    ).pipe(
      catchError(this.handleError('getUserTournamentHistory'))
    );
  }

  // Real-time Events

  /**
   * Emits tournament events for real-time updates
   */
  private emitTournamentEvent(
    type: TournamentEvent['type'], 
    tournamentId: string, 
    data: any
  ): void {
    const event: TournamentEvent = {
      type,
      tournamentId,
      data,
      timestamp: new Date()
    };
    this.tournamentUpdates$.next(event);
  }

  /**
   * Subscribes to real-time tournament updates for a specific tournament
   */
  subscribeToTournamentUpdates(tournamentId: string): Observable<TournamentEvent> {
    return this.tournamentUpdates.pipe(
      map(event => event!),
      tap(event => event && event.tournamentId === tournamentId)
    );
  }

  // Utility Methods

  /**
   * Validates tournament can be started
   */
  validateTournamentStart(tournamentId: string): Observable<{
    canStart: boolean;
    reasons?: string[];
  }> {
    return this.http.get<{ canStart: boolean; reasons?: string[] }>(
      `${this.apiUrl}/${tournamentId}/validate-start`
    ).pipe(
      catchError(this.handleError('validateTournamentStart'))
    );
  }

  /**
   * Gets suggested seeding for tournament participants
   */
  getSuggestedSeeding(tournamentId: string): Observable<{ [playerId: string]: number }> {
    return this.http.get<{ [playerId: string]: number }>(
      `${this.apiUrl}/${tournamentId}/suggested-seeding`
    ).pipe(
      catchError(this.handleError('getSuggestedSeeding'))
    );
  }

  // Error Handling

  private handleError(operation: string) {
    return (error: any): Observable<never> => {
      console.error(`${operation} failed:`, error);
      
      // Extract meaningful error message
      let errorMessage = 'An unexpected error occurred';
      if (error.error?.message) {
        errorMessage = error.error.message;
      } else if (error.message) {
        errorMessage = error.message;
      }
      
      return throwError({
        operation,
        message: errorMessage,
        originalError: error
      });
    };
  }
}