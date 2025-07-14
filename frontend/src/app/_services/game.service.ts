import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {BaseHttpService} from './base-http.service';
import {HttpClient, HttpParams} from '@angular/common/http';
import {Game, Player} from '../_models/models';
import {PageRequest, PageResponse} from '../_models/pagination';
import {map, tap, switchMap} from 'rxjs/operators';
import {AlertService} from "./alert.services";
import {SlackIntegrationService} from './slack-integration.service';
import {PlayerService} from './player.service';

@Injectable({
  providedIn: 'root'
})
export class GameService extends BaseHttpService {
  private endpoint = '/api/games';

  constructor(
    http: HttpClient,
    alertService: AlertService,
    private slackService: SlackIntegrationService,
    private playerService: PlayerService
  ) {
    super(http, alertService);
  }

  /**
   * Save multiple games at once
   * @param games Array of games to save
   * @returns Observable of the save result
   */
  saveGames(games: Game[]): Observable<any> {
    return this.post(this.endpoint, games).pipe(
      tap(result => {
        // If games were saved successfully, post to Slack
        if (result && games.length > 0) {
          this.postMatchResultsToSlack(games);
        }
      })
    );
  }

  /**
   * Get a specific game by ID
   * @param gameId Game ID to retrieve
   * @returns Observable of the game or null if not found
   */
  getGameDetails(gameId: string): Observable<Game | null> {
    return this.get<Game | null>(`${this.endpoint}/${gameId}`);
  }

  /**
   * Get games with pagination
   * @param pageRequest Pagination parameters
   * @returns Observable of paginated game results
   */
  getGames(pageRequest: PageRequest = {page: 0, size: 10}): Observable<PageResponse<Game> | null> {
    const params = new HttpParams()
      .set('page', pageRequest.page.toString())
      .set('size', pageRequest.size.toString());

    if (pageRequest.sort) {
      params.set('sort', pageRequest.sort);
    }

    return this.get<PageResponse<Game> | null>(this.endpoint, params, null);
  }

  /**
   * Get recent games with optional limit
   * @param limit Maximum number of games to return
   * @returns Observable array of games
   */
  getRecentGames(limit: number = 10): Observable<Game[] | null> {
    const params = new HttpParams().set('limit', limit.toString());
    return this.get<PageResponse<Game> | null>(this.endpoint, params, null).pipe(
      map(response => response ? response.content : [])
    );
  }

  /**
   * Get games for a specific player with pagination
   * @param playerId Player ID to get games for
   * @param pageRequest Pagination parameters
   * @returns Observable of paginated game results
   */
  getPlayerGames(playerId: string, pageRequest: PageRequest = {
    page: 0,
    size: 10
  }): Observable<PageResponse<Game> | null> {
    const params = new HttpParams()

    return this.get<PageResponse<Game> | null>(`${this.endpoint}/player/${playerId}`, params, null);
  }

  /**
   * Get games for a specific player (for backward compatibility)
   * @param playerId Player ID to get games for
   * @returns Observable array of games
   */
  getPlayerGameHistory(playerId: string): Observable<Game[] | null> {
    const params = new HttpParams()
    return this.get<Game[]>(`${this.endpoint}/player/${playerId}`, params);
  }

  /**
   * Get all games (for backward compatibility)
   * @returns Observable array of all games
   */
  getAllGames(): Observable<Game[] | null> {
    return this.getGames({page: 0, size: 1000}).pipe(
      map(response => response ? response.content : [])
    );
  }

  /**
   * Delete a specific game
   * @param gameId Game ID to delete
   * @returns Observable of the deletion result
   */
  deleteGame(gameId: string): Observable<any> {
    return this.delete(`${this.endpoint}/${gameId}`);
  }

  /**
   * Reset all player ratings
   * @returns Observable of the reset result
   */
  resetRatings(): Observable<any> {
    return this.patch(`${this.endpoint}/reset`, {});
  }

  /**
   * Post match results to Slack if integration is enabled
   * @param games Array of games that were just saved
   */
  private postMatchResultsToSlack(games: Game[]): void {
    // Process each game for Slack posting
    games.forEach(game => {
      // Get player details and post to Slack
      this.playerService.getPlayers().subscribe((players: Player[] | null) => {
        if (players) {
          const challenger = players.find((p: Player) => p.playerId === game.challengerId);
          const opponent = players.find((p: Player) => p.playerId === game.opponentId);
          
          if (challenger && opponent) {
            // Determine winner and loser
            const winner = game.challengerWin ? challenger : opponent;
            const loser = game.challengerWin ? opponent : challenger;
            
            // Post match result to Slack
            this.slackService.postMatchResult(game, winner, loser).subscribe(
              success => {
                if (success) {
                  console.log('Match result posted to Slack successfully');
                }
              },
              error => {
                console.error('Failed to post match result to Slack:', error);
              }
            );
          }
        }
      });
    });
  }
}
