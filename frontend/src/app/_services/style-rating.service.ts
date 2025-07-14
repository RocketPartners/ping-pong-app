// src/app/_services/style-rating.service.ts

import {Injectable} from '@angular/core';
import {Observable} from 'rxjs';
import {BaseHttpService} from './base-http.service';
import {HttpClient} from '@angular/common/http';
import {AlertService} from './alert.services';
import {AverageStyleRating, TopStyleRating} from "../_models/player-style";

@Injectable({
  providedIn: 'root'
})
export class StyleRatingService extends BaseHttpService {
  private endpoint = '/api/players';

  constructor(
    http: HttpClient,
    alertService: AlertService
  ) {
    super(http, alertService);
  }

  /**
   * Gets average style ratings across all players
   * @returns Observable of average style ratings
   */
  getAverageStyleRatings(): Observable<AverageStyleRating[] | null> {
    return this.get<AverageStyleRating[]>(`${this.endpoint}/style-ratings/average`, undefined, []);
  }

  /**
   * Gets highest style ratings with player details
   * @returns Observable of highest style ratings
   */
  getHighestStyleRatings(): Observable<TopStyleRating[] | null> {
    return this.get<TopStyleRating[]>(`${this.endpoint}/style-ratings/highest`, undefined, []);
  }
}
