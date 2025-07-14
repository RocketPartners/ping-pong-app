import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {BaseHttpService} from './base-http.service';
import {HttpClient} from '@angular/common/http';
import {Player} from "../_models/models";
import {AlertService} from "./alert.services";

@Injectable({
  providedIn: 'root'
})
export class PlayerService extends BaseHttpService {
  private endpoint = '/api/players';

  constructor(
    http: HttpClient,
    alertService: AlertService
  ) {
    super(http, alertService);
  }

  /**
   * Get a player by ID with caching
   * @param playerId The player's unique ID
   * @param bypassCache Set to true to force a fresh request
   * @returns Observable of Player or null if not found
   */
  getPlayerById(playerId: string, bypassCache: boolean = false): Observable<Player | null> {
    if (!playerId) {
      return this.handleEmptyParam<Player | null>(null);
    }
    return this.getCached<Player | null>(`${this.endpoint}/${playerId}`, undefined, null, bypassCache);
  }

  /**
   * Get multiple players by their IDs
   * @param playerIds Array of player IDs to fetch
   * @returns Observable array of Players or null
   */
  getPlayersByIds(playerIds: string[]): Observable<Player[] | null> {
    if (!playerIds || playerIds.length === 0) {
      return this.handleEmptyParam<Player[] | null>([]);
    }

    return this.post<Player[] | null>(`${this.endpoint}/by-ids`, playerIds);
  }

  /**
   * Get all players with caching
   * @param bypassCache Set to true to force a fresh request
   * @returns Observable array of Players
   */
  getPlayers(bypassCache: boolean = false): Observable<Player[] | null> {
    return this.getCached<Player[] | null>(`${this.endpoint}`, undefined, [], bypassCache);
  }

  /**
   * Get a player by first name and last name
   * @param firstName First name
   * @param lastName Last name
   * @returns Observable of Player or null if not found
   */
  getPlayerByName(firstName: string, lastName: string): Observable<Player | null> {
    return this.get<Player | null>(`${this.endpoint}/name/${firstName}/${lastName}`, undefined, null);
  }

  /**
   * Get a player by username
   * @param username Username
   * @returns Observable of Player or null if not found
   */
  getPlayerByUsername(username: string): Observable<Player | null> {
    if (!username) {
      return this.handleEmptyParam<Player | null>(null);
    }
    return this.get<Player | null>(`${this.endpoint}/username/${username}`, undefined, null);
  }

  /**
   * Get all player usernames
   * @returns Observable array of usernames
   */
  getPlayerUsernames(): Observable<string[] | null> {
    return this.get<string[] | null>(`${this.endpoint}/usernames`, undefined, []);
  }

  /**
   * Create a new player
   * @param player Player data
   * @returns Observable of creation result
   */
  createPlayer(player: Player): Observable<any | null> {
    return this.post<any | null>(`${this.endpoint}`, player);
  }

  /**
   * Update an existing player
   * @param player Player data with ID
   * @returns Observable of update result
   */
  updatePlayer(player: Player): Observable<any | null> {
    return this.put<any | null>(`${this.endpoint}/${player.playerId}`, player);
  }

  /**
   * Delete a player by ID
   * @param playerId Player ID to delete
   * @returns Observable of deletion result
   */
  deletePlayer(playerId: string): Observable<any | null> {
    return this.delete<any | null>(`${this.endpoint}/${playerId}`);
  }

  /**
   * Handler for empty parameters
   * @param defaultValue Default value to return
   * @returns Observable of default value
   */
  private handleEmptyParam<T>(defaultValue: T): Observable<T> {
    return new Observable(subscriber => {
      subscriber.next(defaultValue);
      subscriber.complete();
    });
  }
}
