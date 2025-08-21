// src/app/_services/tournament.service.ts
// Unified tournament service combining brackets-manager and backend API

import { Injectable } from '@angular/core';
import { Observable, BehaviorSubject, forkJoin, of, throwError, from } from 'rxjs';
import { map, switchMap, tap, catchError, finalize } from 'rxjs/operators';
import { TournamentEngineService } from './tournament-engine.service';
import { TournamentApiService } from './tournament-api.service';
import {
  Tournament,
  TournamentDetails,
  TournamentListItem,
  CreateTournamentRequest,
  UpdateMatchRequest,
  TournamentParticipant,
  PlayerInfo,
  TournamentStats,
  TournamentStatus,
  TournamentType,
  BracketData,
  Match,
  Participant
} from '../_models/tournament-new';

@Injectable({
  providedIn: 'root'
})
export class TournamentService {
  private loadingSubject = new BehaviorSubject<boolean>(false);
  public readonly loading$ = this.loadingSubject.asObservable();

  private currentTournamentSubject = new BehaviorSubject<TournamentDetails | null>(null);
  public readonly currentTournament$ = this.currentTournamentSubject.asObservable();

  constructor(
    private engineService: TournamentEngineService,
    private apiService: TournamentApiService
  ) {}

  // Tournament Management

  /**
   * Creates a new tournament with complete bracket generation
   */
  createTournament(request: CreateTournamentRequest): Observable<Tournament> {
    this.setLoading(true);
    
    return this.apiService.createTournament(request).pipe(
      switchMap(tournament => {
        // Convert player IDs to tournament participants
        const participants: TournamentParticipant[] = request.playerIds.map((playerId, index) => ({
          id: index + 1, // brackets-manager needs numeric IDs starting from 1
          playerId,
          name: `Player ${index + 1}`, // Will be updated with real names
          seed: index + 1 // Sequential seeding for now
        }));

        // Tournament created successfully, return it
        return of(tournament);
      }),
      finalize(() => this.setLoading(false)),
      catchError(this.handleError('createTournament'))
    );
  }

  /**
   * Gets tournament list with filtering options
   */
  getTournaments(
    page: number = 0,
    size: number = 20,
    status?: TournamentStatus,
    organizerId?: string
  ): Observable<{ content: TournamentListItem[], totalElements: number }> {
    return this.apiService.getTournaments(page, size, status, organizerId);
  }

  /**
   * Gets detailed tournament information with bracket data
   */
  getTournamentDetails(tournamentId: string): Observable<TournamentDetails> {
    this.setLoading(true);

    return forkJoin({
      tournament: this.apiService.getTournamentDetails(tournamentId),
      bracketData: this.apiService.getBracketData(tournamentId)
    }).pipe(
      map(({ tournament, bracketData }) => {
        const details: TournamentDetails = {
          ...tournament,
          bracketData: bracketData || undefined
        };
        
        this.currentTournamentSubject.next(details);
        return details;
      }),
      finalize(() => this.setLoading(false)),
      catchError(this.handleError('getTournamentDetails'))
    );
  }

  /**
   * Starts a tournament by generating the bracket
   */
  startTournament(tournamentId: string): Observable<Tournament> {
    this.setLoading(true);

    return this.apiService.getTournamentDetails(tournamentId).pipe(
      switchMap(tournament => {
        if (tournament.status !== TournamentStatus.CREATED) {
          return throwError({ message: 'Tournament cannot be started in its current state' });
        }

        if (!tournament.participants || tournament.participants.length < 2) {
          return throwError({ message: 'Tournament needs at least 2 participants to start' });
        }

        // Start tournament in backend
        return this.apiService.startTournament(tournamentId);
      }),
      switchMap(tournament => {
        // After starting, refresh the tournament details to get bracket data
        return this.getTournamentDetails(tournamentId).pipe(
          map(() => tournament) // Return the started tournament while ensuring details are refreshed
        );
      }),
      finalize(() => this.setLoading(false)),
      catchError(this.handleError('startTournament'))
    );
  }

  /**
   * Updates match result and advances tournament
   */
  updateMatchResult(tournamentId: string, request: UpdateMatchRequest): Observable<void> {
    this.setLoading(true);

    return this.apiService.updateMatchResult(tournamentId, request).pipe(
      tap(() => {
        // Refresh current tournament data after update
        const current = this.currentTournamentSubject.value;
        if (current && current.id === tournamentId) {
          this.refreshCurrentTournament(tournamentId);
        }
      }),
      finalize(() => this.setLoading(false)),
      catchError(this.handleError('updateMatchResult'))
    );
  }

  /**
   * Completes a tournament
   */
  completeTournament(tournamentId: string): Observable<Tournament> {
    this.setLoading(true);

    return from(this.engineService.getTournamentWinner(tournamentId)).pipe(
      switchMap(winner => {
        return this.apiService.completeTournament(
          tournamentId,
          winner?.id,
          undefined // We could implement runner-up logic here
        ).pipe(
          tap(tournament => {
            const current = this.currentTournamentSubject.value;
            if (current && current.id === tournament.id) {
              this.currentTournamentSubject.next({ ...current, ...tournament });
            }
          })
        );
      }),
      finalize(() => this.setLoading(false)),
      catchError(this.handleError('completeTournament'))
    );
  }

  // Match and Participant Management

  /**
   * Gets matches ready to be played
   */
  getReadyMatches(tournamentId: string): Observable<Match[]> {
    return from(this.engineService.getReadyMatches(tournamentId)).pipe(
      catchError(() => of([]))
    );
  }

  /**
   * Gets tournament participants with player details
   */
  getTournamentParticipants(tournamentId: string): Observable<TournamentParticipant[]> {
    return this.apiService.getTournamentParticipants(tournamentId);
  }

  /**
   * Adds participants to tournament
   */
  addParticipants(tournamentId: string, playerIds: string[]): Observable<TournamentParticipant[]> {
    return this.apiService.addParticipants(tournamentId, playerIds).pipe(
      tap(() => {
        // Refresh current tournament if it's being viewed
        const current = this.currentTournamentSubject.value;
        if (current && current.id === tournamentId) {
          this.refreshCurrentTournament(tournamentId);
        }
      })
    );
  }

  // Player and Statistics

  /**
   * Gets player information for display
   */
  getPlayersInfo(playerIds: string[]): Observable<PlayerInfo[]> {
    return this.apiService.getPlayersInfo(playerIds);
  }

  /**
   * Searches players for tournament invitation
   */
  searchPlayers(query: string, limit?: number): Observable<PlayerInfo[]> {
    return this.apiService.searchPlayers(query, limit);
  }

  /**
   * Gets tournament statistics and progress
   */
  getTournamentStats(tournamentId: string): Observable<TournamentStats> {
    return forkJoin({
      apiStats: this.apiService.getTournamentStats(tournamentId),
      engineProgress: this.engineService.getTournamentProgress(tournamentId).catch(() => ({
        totalMatches: 0,
        completedMatches: 0,
        pendingMatches: 0,
        readyMatches: 0
      }))
    }).pipe(
      map(({ apiStats, engineProgress }) => ({
        ...apiStats,
        ...engineProgress
      }))
    );
  }

  // Tournament State

  /**
   * Checks if tournament is completed
   */
  isTournamentCompleted(tournamentId: string): Observable<boolean> {
    return from(this.engineService.isTournamentCompleted(tournamentId)).pipe(
      catchError(() => of(false))
    );
  }

  /**
   * Gets tournament winner
   */
  getTournamentWinner(tournamentId: string): Observable<Participant | null> {
    return from(this.engineService.getTournamentWinner(tournamentId)).pipe(
      catchError(() => of(null))
    );
  }

  /**
   * Validates if tournament can be started
   */
  validateTournamentStart(tournamentId: string): Observable<{ canStart: boolean; reasons?: string[] }> {
    return this.apiService.validateTournamentStart(tournamentId);
  }

  // Real-time Updates

  /**
   * Subscribes to tournament updates
   */
  subscribeToUpdates(tournamentId: string): Observable<any> {
    return this.apiService.subscribeToTournamentUpdates(tournamentId).pipe(
      tap(() => {
        // Refresh tournament data when updates are received
        this.refreshCurrentTournament(tournamentId);
      })
    );
  }

  // Utility Methods

  private refreshCurrentTournament(tournamentId: string): void {
    const current = this.currentTournamentSubject.value;
    if (current && current.id === tournamentId) {
      this.getTournamentDetails(tournamentId).subscribe();
    }
  }

  private setLoading(loading: boolean): void {
    this.loadingSubject.next(loading);
  }

  private handleError(operation: string) {
    return (error: any): Observable<never> => {
      console.error(`TournamentService ${operation} failed:`, error);
      this.setLoading(false);
      return throwError(error);
    };
  }

  /**
   * Clears current tournament data
   */
  clearCurrentTournament(): void {
    this.currentTournamentSubject.next(null);
  }

  /**
   * Gets current tournament value synchronously
   */
  getCurrentTournament(): TournamentDetails | null {
    return this.currentTournamentSubject.value;
  }

  /**
   * Deletes a tournament
   */
  deleteTournament(tournamentId: string): Observable<void> {
    return this.apiService.deleteTournament(tournamentId);
  }
}
