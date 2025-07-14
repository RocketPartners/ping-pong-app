import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {BaseHttpService} from './base-http.service';
import {HttpClient} from '@angular/common/http';
import {AlertService} from "./alert.services";

export interface SystemStats {
  recentPlayers: any[];
  totalPlayers: number;
  topRatedPlayers: {
    'Singles Ranked': any;
    'Doubles Ranked': any;
    'Singles Normal': any;
    'Doubles Normal': any;
  };
  totalGames: number;
  gamesByType: {
    'Singles Ranked': number;
    'Doubles Ranked': number;
    'Singles Normal': number;
    'Doubles Normal': number;
  };
  totalPointsScored: number;
  averageScore: number;
  totalAchievementsEarned: number;
  cumulativeAchievementScore: number;
  topAchievements: Record<string, number>;
  averageGamesPerPlayer: number;
  averageAchievementsPerPlayer: number;
  winDistribution: {
    '1-5 wins': number;
    '0 wins': number;
    '6-10 wins': number;
    '20+ wins': number;
    '11-20 wins': number;
  };
}

@Injectable({
  providedIn: 'root'
})
export class StatsService extends BaseHttpService {
  private endpoint = '/api/stats';

  constructor(
    http: HttpClient,
    alertService: AlertService
  ) {
    super(http, alertService);
  }

  /**
   * Get system-wide statistics
   * @returns Observable of SystemStats
   */
  getSystemStats(): Observable<SystemStats | null> {
    return this.get<SystemStats>(this.endpoint, undefined, null);
  }
}
