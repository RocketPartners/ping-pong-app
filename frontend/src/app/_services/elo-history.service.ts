import {Injectable} from '@angular/core';
import {Observable, of} from 'rxjs';
import {catchError, map} from 'rxjs/operators';
import {BaseHttpService} from './base-http.service';
import {HttpClient, HttpParams} from '@angular/common/http';
import {EloChartData, RankHistoryData} from '../_models/models';
import {AlertService} from "./alert.services";

@Injectable({
  providedIn: 'root'
})
export class EloHistoryService extends BaseHttpService {
  private endpoint = '/api/players';

  constructor(
    http: HttpClient,
    alertService: AlertService
  ) {
    super(http, alertService);
  }

  /**
   * Gets a player's complete ELO history
   * @param playerId Player's UUID
   * @returns Observable array of ELO history entries
   */
  getCompleteEloHistory(playerId: string): Observable<any[] | null> {
    return this.get<any[]>(`${this.endpoint}/${playerId}/elo-history`, undefined, []);
  }

  /**
   * Gets a player's ELO history for a specific game type
   * @param playerId Player's UUID
   * @param gameType Game type (SINGLES_RANKED, etc.)
   * @returns Observable array of ELO history entries
   */
  getEloHistoryByGameType(playerId: string, gameType: string): Observable<any[] | null> {
    return this.get<any[]>(`${this.endpoint}/${playerId}/elo-history/${gameType}`, undefined, []);
  }

  /**
   * Gets a player's ELO history for a specific game type within a date range
   * @param playerId Player's UUID
   * @param gameType Game type
   * @param startDate Start date (YYYY-MM-DD)
   * @param endDate End date (YYYY-MM-DD)
   * @returns Observable array of ELO history entries
   */
  getEloHistoryWithDateRange(
    playerId: string,
    gameType: string,
    startDate: string,
    endDate: string
  ): Observable<any[] | null> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);

    return this.get<any[]>(
      `${this.endpoint}/${playerId}/elo-history/${gameType}/range`,
      params,
      []
    );
  }

  /**
   * Gets ELO chart data for visualization
   * @param playerId Player's UUID
   * @param gameType Game type
   * @param startDate Optional start date (YYYY-MM-DD)
   * @param endDate Optional end date (YYYY-MM-DD)
   * @returns Observable array of EloChartData
   */
  getEloChartData(
    playerId: string,
    gameType: string,
    startDate?: string,
    endDate?: string
  ): Observable<EloChartData[] | null> {
    // Ensure the game type has the correct format
    const formattedGameType = gameType.includes('_') ? gameType : gameType.replace(/([A-Z])/g, '_$1').toUpperCase();

    // If date range is provided, use range endpoint
    if (startDate && endDate) {
      return this.getEloHistoryWithDateRange(playerId, formattedGameType, startDate, endDate)
        .pipe(
          map(history => this.convertToEloChartData(history || [])),
          catchError(() => of(this.generateSampleEloChartData(formattedGameType)))
        );
    }

    // Otherwise use the dedicated chart endpoint
    return this.get<EloChartData[]>(
      `${this.endpoint}/${playerId}/elo-chart/${formattedGameType}`,
      undefined,
      this.generateSampleEloChartData(formattedGameType)
    );
  }

  /**
   * Gets a player's rank history for visualization
   * @param playerId Player's UUID
   * @param gameType Game type
   * @param startDate Optional start date (YYYY-MM-DD)
   * @param endDate Optional end date (YYYY-MM-DD)
   * @returns Observable array of RankHistoryData
   */
  getRankHistory(
    playerId: string,
    gameType: string,
    startDate?: string,
    endDate?: string
  ): Observable<RankHistoryData[] | null> {
    // Ensure the game type has the correct format
    const formattedGameType = gameType.includes('_') ? gameType : gameType.replace(/([A-Z])/g, '_$1').toUpperCase();

    // If date range is provided, filter from general history
    if (startDate && endDate) {
      return this.getEloHistoryWithDateRange(playerId, formattedGameType, startDate, endDate)
        .pipe(
          map(history => this.extractRankHistoryFromEloHistory(history || [])),
          catchError(() => of(this.generateSampleRankHistory(formattedGameType)))
        );
    }

    // Otherwise use the dedicated rank history endpoint
    return this.get<RankHistoryData[]>(
      `${this.endpoint}/${playerId}/rank-history/${formattedGameType}`,
      undefined,
      this.generateSampleRankHistory(formattedGameType)
    );
  }

  /**
   * Converts frontend game type string to backend enum format
   * @param gameType Frontend game type string
   * @returns Backend formatted game type
   */
  convertToBackendGameType(gameType: string): string {
    const mappings: { [key: string]: string } = {
      'singlesRanked': 'SINGLES_RANKED',
      'doublesRanked': 'DOUBLES_RANKED',
      'singlesNormal': 'SINGLES_NORMAL',
      'doublesNormal': 'DOUBLES_NORMAL'
    };

    // Use the mapping if available, otherwise format the string directly
    return mappings[gameType] || gameType.toUpperCase();
  }

  /**
   * Generates sample ELO chart data for testing purposes
   * @param gameType Game type
   * @returns Sample ELO chart data
   */
  generateSampleEloChartData(gameType: string): EloChartData[] {
    // Set base ELO depending on game type
    const baseElo = gameType.includes('SINGLES_RANKED') ? 1200 :
      gameType.includes('DOUBLES_RANKED') ? 1100 :
        gameType.includes('SINGLES_NORMAL') ? 1000 : 900;

    const data: EloChartData[] = [];
    const daysAgo = 60; // Generate 2 months of data

    let currentElo = baseElo;
    let lastTimestamp = new Date();

    // Start from the oldest date
    lastTimestamp.setDate(lastTimestamp.getDate() - daysAgo);

    for (let i = 0; i <= daysAgo; i += 2) { // Generate data every 2 days
      const date = new Date(lastTimestamp);
      date.setDate(date.getDate() + 2);
      lastTimestamp = date;

      // Random ELO change between -20 and +20
      const eloChange = Math.floor(Math.random() * 41) - 20;
      currentElo += eloChange;

      // Make sure ELO doesn't go below minimum
      if (currentElo < 500) {
        currentElo = 500;
      }

      data.push({
        timestamp: date.toISOString(),
        eloRating: currentElo,
        eloChange: eloChange
      });
    }

    // Sort data by timestamp
    return data.sort((a, b) =>
      new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
    );
  }

  /**
   * Generates sample rank history data for testing purposes
   * @param gameType Game type
   * @returns Sample rank history data
   */
  generateSampleRankHistory(gameType: string): RankHistoryData[] {
    // Random starting rank between 5 and 15
    let currentRank = Math.floor(Math.random() * 11) + 5;
    const totalPlayers = Math.floor(Math.random() * 50) + 50; // Random player count between 50-100

    const data: RankHistoryData[] = [];
    const daysAgo = 60; // Generate 2 months of data

    let lastTimestamp = new Date();

    // Start from the oldest date
    lastTimestamp.setDate(lastTimestamp.getDate() - daysAgo);

    for (let i = 0; i <= daysAgo; i += 3) { // Generate data every 3 days
      const date = new Date(lastTimestamp);
      date.setDate(date.getDate() + 3);
      lastTimestamp = date;

      // Random rank change between -2 and +2
      const rankChange = Math.floor(Math.random() * 5) - 2;
      currentRank = Math.max(1, currentRank + rankChange); // Rank can't go below 1
      currentRank = Math.min(totalPlayers, currentRank); // Rank can't exceed total players

      // Calculate percentile (higher is better)
      const percentile = ((totalPlayers - currentRank) / totalPlayers) * 100;

      data.push({
        timestamp: date.toISOString(),
        rankPosition: currentRank,
        totalPlayers: totalPlayers,
        percentile: parseFloat(percentile.toFixed(1))
      });
    }

    // Sort data by timestamp
    return data.sort((a, b) =>
      new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
    );
  }

  /**
   * Utility method to convert general ELO history to chart format
   * @param history ELO history data
   * @returns Formatted chart data
   */
  private convertToEloChartData(history: any[]): EloChartData[] {
    if (!history || !Array.isArray(history)) return [];

    return history.map(item => ({
      timestamp: item.timestamp,
      eloRating: item.newElo,
      eloChange: item.eloChange
    }));
  }

  /**
   * Utility method to extract rank history from general ELO history
   * @param history ELO history data
   * @returns Extracted rank history data
   */
  private extractRankHistoryFromEloHistory(history: any[]): RankHistoryData[] {
    if (!history || !Array.isArray(history)) return [];

    return history.map(item => ({
      timestamp: item.timestamp,
      rankPosition: item.rankPosition,
      totalPlayers: item.totalPlayers,
      percentile: item.percentile
    }));
  }
}
