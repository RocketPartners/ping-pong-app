// src/app/_services/tournament.service.ts

import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {
  MatchResultDTO,
  Tournament,
  TournamentMatch,
  TournamentPlayer,
  TournamentRequestDTO
} from '../_models/tournament';
import {AppConfig} from '../_config/app.config';

@Injectable({
  providedIn: 'root'
})
export class TournamentService {
  private apiUrl = `${AppConfig.apiUrl}/api/tournaments`;

  constructor(private http: HttpClient) {
  }

  // Tournament Management
  getAllTournaments(): Observable<Tournament[]> {
    return this.http.get<Tournament[]>(this.apiUrl);
  }

  getTournamentById(id: string): Observable<Tournament> {
    return this.http.get<Tournament>(`${this.apiUrl}/${id}`);
  }

  createTournament(tournament: TournamentRequestDTO): Observable<Tournament> {
    return this.http.post<Tournament>(this.apiUrl, tournament);
  }

  updateTournament(id: string, tournament: Tournament): Observable<Tournament> {
    return this.http.put<Tournament>(`${this.apiUrl}/${id}`, tournament);
  }

  deleteTournament(id: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${id}`);
  }

  // Tournament Filtering
  getTournamentsByOrganizer(organizerId: string): Observable<Tournament[]> {
    return this.http.get<Tournament[]>(`${this.apiUrl}/organizer/${organizerId}`);
  }

  getTournamentsByPlayer(playerId: string): Observable<Tournament[]> {
    return this.http.get<Tournament[]>(`${this.apiUrl}/player/${playerId}`);
  }

  getTournamentsByStatus(status: string): Observable<Tournament[]> {
    return this.http.get<Tournament[]>(`${this.apiUrl}/status/${status}`);
  }

  // Tournament Lifecycle Management
  startTournament(id: string): Observable<Tournament> {
    return this.http.patch<Tournament>(`${this.apiUrl}/${id}/start`, {});
  }

  completeTournament(id: string): Observable<Tournament> {
    return this.http.patch<Tournament>(`${this.apiUrl}/${id}/complete`, {});
  }

  // Tournament Match Management
  getTournamentMatches(id: string): Observable<TournamentMatch[]> {
    return this.http.get<TournamentMatch[]>(`${this.apiUrl}/${id}/matches`);
  }

  getMatchesByBracketType(id: string, bracketType: string): Observable<TournamentMatch[]> {
    return this.http.get<TournamentMatch[]>(`${this.apiUrl}/${id}/matches/bracket/${bracketType}`);
  }

  updateMatchResult(tournamentId: string, matchId: string, result: MatchResultDTO): Observable<TournamentMatch> {
    return this.http.patch<TournamentMatch>(`${this.apiUrl}/${tournamentId}/matches/${matchId}`, result);
  }

  // Tournament Player Management
  getTournamentPlayers(id: string): Observable<TournamentPlayer[]> {
    return this.http.get<TournamentPlayer[]>(`${this.apiUrl}/${id}/players`);
  }
}
