import {Injectable} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {Observable} from 'rxjs';
import {BaseHttpService} from './base-http.service';
import {AlertService} from './alert.services';
import {PlayerReview, PlayerStyleReview} from '../_models/models';

@Injectable({
  providedIn: 'root'
})
export class PlayerReviewService extends BaseHttpService {
  constructor(
    http: HttpClient,
    alertService: AlertService
  ) {
    super(http, alertService);
  }

  /**
   * Submit a player review after games
   * @param playerId The ID of the player being reviewed
   * @param reviewData The review data
   * @returns Observable of the save result
   */
  submitReview(playerId: string, reviewData: any): Observable<any> {
    return this.post(`/api/players/${playerId}/style-review`, reviewData);
  }

  /**
   * Submit multiple player reviews (one for each player in the games)
   * @param reviews Array of player reviews
   * @param reviewerId ID of the player submitting the reviews
   * @param gameIds IDs of the games these reviews are for
   * @returns Observable of the combined results
   */
  submitBatchReviews(reviews: PlayerReview[], reviewerId: string, gameIds: string[]): Observable<any> {
    // Create an array of observables for each review submission
    const observables = reviews.map(review => {
      const reviewData = {
        reviewerId: reviewerId,
        playerId: review.playerId,
        gameIds: gameIds,
        strengths: review.strengths,
        improvements: review.improvements,
        response: false
      };

      return this.submitReview(review.playerId, reviewData);
    });

    // Use executeBatchRequests to execute all requests and combine the results
    return this.executeBatchRequests(observables);
  }

  getPlayerReviews(playerId: string): Observable<PlayerStyleReview[] | null> {
    return this.get<PlayerStyleReview[]>(`/api/players/${playerId}/style-reviews`);
  }

  getRecentUnacknowledgedReviews(playerId: string): Observable<PlayerStyleReview[] | null> {
    return this.get<PlayerStyleReview[]>(`/api/players/${playerId}/recent-reviews`);
  }

  respondToReview(
    reviewId: string,
    reviewerId: string,
    playerId: string,
    gameIds: string[],
    strengths: string[],
    improvements: string[]
  ): Observable<PlayerStyleReview | null> {
    const responseData = {
      reviewerId: reviewerId,
      gameIds: gameIds,
      strengths: strengths,
      improvements: improvements,
      response: true,
      parentReviewId: reviewId,
      playerId: playerId
    };
    return this.post<PlayerStyleReview>(`/api/players/reviews/${reviewId}/respond`, responseData);
  }

  /**
   * Helper method to execute multiple requests in parallel
   * @param observables Array of observables to execute
   * @returns Combined observable result
   */
  private executeBatchRequests(observables: Observable<any>[]): Observable<any[]> {
    if (observables.length === 0) {
      return new Observable(subscriber => {
        subscriber.next([]);
        subscriber.complete();
      });
    }

    // Create a custom observable that executes each request sequentially
    return new Observable(subscriber => {
      const results: any[] = [];
      let completed = 0;
      let hasError = false;

      // Execute each observable sequentially
      const executeNext = (index: number) => {
        if (index >= observables.length) {
          subscriber.next(results);
          subscriber.complete();
          return;
        }

        observables[index].subscribe({
          next: (result) => {
            results.push(result);
            completed++;
            executeNext(index + 1);
          },
          error: (error) => {
            if (!hasError) {
              hasError = true;
              subscriber.error(error);
            }
          }
        });
      };

      // Start execution with the first observable
      executeNext(0);
    });
  }
}
