import {Injectable} from '@angular/core';
import {BehaviorSubject, Observable} from 'rxjs';
import {BaseHttpService} from './base-http.service';
import {Match, MatchConfig, Player, SelectedPlayers} from "../_models/models";
import {HttpClient} from "@angular/common/http";
import {AlertService} from "./alert.services";

@Injectable({providedIn: 'root'})
export class MatchService extends BaseHttpService {
  private endpoint = '/api/matches';

  // State management for match building
  private matchConfig = new BehaviorSubject<MatchConfig | null>(null);
  public matchConfig$ = this.matchConfig.asObservable();

  private selectedPlayers = new BehaviorSubject<SelectedPlayers>({
    player1: null,
    player2: null,
    player3: null,
    player4: null
  });
  public selectedPlayers$ = this.selectedPlayers.asObservable();

  constructor(
    http: HttpClient,
    alertService: AlertService
  ) {
    super(http, alertService);
  }

  // Player management methods
  addPlayer(player: Player, position: string): void {
    const current = this.selectedPlayers.value;
    current[position as keyof SelectedPlayers] = player;
    this.selectedPlayers.next({...current});
  }

  removePlayer(position: string): void {
    const current = this.selectedPlayers.value;
    current[position as keyof SelectedPlayers] = null;
    this.selectedPlayers.next({...current});
  }

  clearPlayers(): void {
    this.selectedPlayers.next({
      player1: null,
      player2: null,
      player3: null,
      player4: null
    });
  }

  // Match configuration methods
  setMatchType(type: string, isRanked: boolean, bestOf: number = 3): void {
    this.matchConfig.next({
      matchType: type,
      isRanked: isRanked,
      bestOf: bestOf
    });
  }

  getMatchConfig(): MatchConfig | null {
    return this.matchConfig.value;
  }

  resetMatchBuilder(): void {
    this.matchConfig.next(null);
    this.clearPlayers();
  }

  // API methods
  getMatchById(matchId: string): Observable<Match | null> {
    return this.get<Match | null>(`${this.endpoint}/${matchId}`, undefined, null);
  }

  getAllMatches(): Observable<Match[] | null> {
    return this.get<Match[] | null>(this.endpoint, undefined, []);
  }

  getPlayerMatches(playerId: string): Observable<Match[] | null> {
    return this.get<Match[] | null>(`${this.endpoint}/player/${playerId}`, undefined, []);
  }

  createMatch(match: Match): Observable<Match | null> {
    return this.post<Match | null>(this.endpoint, match);
  }

  updateMatch(matchId: string, match: Match): Observable<Match | null> {
    return this.put<Match | null>(`${this.endpoint}/${matchId}`, match);
  }

  concludeMatch(matchId: string): Observable<any | null> {
    return this.post<any | null>(`${this.endpoint}/${matchId}/conclude`, {});
  }

  deleteMatch(matchId: string): Observable<any | null> {
    return this.delete<any | null>(`${this.endpoint}/${matchId}`);
  }
}
