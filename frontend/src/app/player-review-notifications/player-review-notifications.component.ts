import {Component, OnDestroy, OnInit} from '@angular/core';
import {BehaviorSubject, Subscription} from 'rxjs';
import {PlayerReviewService} from '../_services/player-review.service';
import {AccountService} from '../_services/account.service';
import {MatDialog} from '@angular/material/dialog';
import {MatSnackBar} from '@angular/material/snack-bar';
import {PlayerReviewDialogComponent} from '../player-review-dialog/player-review-dialog.component';
import {PlayerStyleReview} from '../_models/models';

@Component({
  selector: 'app-review-notifications', // Changed to match what's used in app.component.html
  templateUrl: './player-review-notifications.component.html',
  styleUrls: ['./player-review-notifications.component.scss'],
  standalone: false,
})
export class PlayerReviewNotificationsComponent implements OnInit, OnDestroy {
  loading = false;
  // Observable for notifications
  private reviewNotifications = new BehaviorSubject<PlayerStyleReview[]>([]);
  public reviewNotifications$ = this.reviewNotifications.asObservable();
  // Track if there are unseen notifications
  private hasUnseenReviews = new BehaviorSubject<boolean>(false);
  public hasUnseenReviews$ = this.hasUnseenReviews.asObservable();
  private subscriptions: Subscription[] = [];

  constructor(
    private playerReviewService: PlayerReviewService,
    private accountService: AccountService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {
  }

  ngOnInit(): void {
    // Load notifications on init
    this.loadReviewNotifications();

    // Set up a refresh interval (every 5 minutes)
    const refreshInterval = setInterval(() => {
      this.loadReviewNotifications(true);
    }, 5 * 60 * 1000);

    // Store the interval for cleanup
    this.subscriptions.push({
      unsubscribe: () => clearInterval(refreshInterval)
    } as Subscription);
  }

  ngOnDestroy(): void {
    // Clean up subscriptions
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  /**
   * Load unacknowledged reviews for the current player
   */
  loadReviewNotifications(silent: boolean = false): void {
    const currentPlayer = this.accountService.playerValue?.player;
    if (!currentPlayer) return;

    if (!silent) this.loading = true;

    this.playerReviewService.getRecentUnacknowledgedReviews(currentPlayer.playerId)
      .subscribe({
        next: (reviews) => {
          const notificationReviews = reviews || [];
          this.reviewNotifications.next(notificationReviews);
          this.hasUnseenReviews.next(notificationReviews.length > 0);
          this.loading = false;
        },
        error: (err) => {
          console.error('Error loading review notifications:', err);
          this.loading = false;
        }
      });
  }

  /**
   * Handle quick dismiss of a review (X button)
   */
  dismissReview(review: PlayerStyleReview, event: Event): void {
    event.stopPropagation(); // Prevent menu from closing

    if (!review.id) return;

    const currentPlayerId = this.accountService.playerValue?.player.playerId;
    if (!currentPlayerId) return;

    // Submit an empty response (no strengths or weaknesses)
    this.playerReviewService.respondToReview(
      review.id,
      currentPlayerId,
      review.reviewerId,
      review.gameIds || [],
      [], // Empty strengths array
      []  // Empty improvements array
    ).subscribe({
      next: () => {
        // Also acknowledge the review
        this.acknowledgeReview(review.id as string, 'Review dismissed');
      },
      error: (err) => {
        console.error('Error dismissing review:', err);
        this.snackBar.open('Error dismissing review', 'Close', {duration: 3000});
      }
    });
  }

  /**
   * Open dialog to respond to a review (check button)
   */
  respondToReview(review: PlayerStyleReview, event: Event): void {
    event.stopPropagation(); // Prevent menu from closing
    // Get the reviewer info to show in the dialog
    const reviewerPlayer = {
      playerId: review.reviewerId,
      username: review.reviewerUsername || 'Reviewer',
      firstName: review.reviewerFirstName || '',
      lastName: review.reviewerLastName || ''
    };

    const currentPlayerId = this.accountService.playerValue?.player.playerId;
    if (!currentPlayerId) return;

    const dialogRef = this.dialog.open(PlayerReviewDialogComponent, {
      width: '650px',
      data: {
        players: [reviewerPlayer],
        currentPlayerId: currentPlayerId,
        parentReview: review,
        isResponse: true
      }
    });

    dialogRef.afterClosed().subscribe((result) => {
      if (result && result.length > 0 && review.id) {
        const responseReview = result[0];

        this.playerReviewService.respondToReview(
          review.id,
          currentPlayerId,
          review.reviewerId,
          review.gameIds || [],
          responseReview.strengths,
          responseReview.improvements
        ).subscribe({
          next: () => {
            this.snackBar.open('Response sent successfully', 'Close', {duration: 2000});
            // Also acknowledge the review
            if (review.id) {
              this.acknowledgeReview(review.id);
            }
          },
          error: (err) => {
            console.error('Error sending response:', err);
            this.snackBar.open('Error sending response', 'Close', {duration: 3000});
          }
        });
      }
    });
  }

  /**
   * Format player style names for display
   */
  formatStyleName(style: string): string {
    return style.replace('_', ' ').toLowerCase()
      .split(' ')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }

  /**
   * Acknowledge a review to remove it from notifications
   */
  private acknowledgeReview(reviewId: string, message: string = 'Review acknowledged'): void {
    // Update the notifications list
    const currentReviews = this.reviewNotifications.value;
    const updatedReviews = currentReviews.filter(review => review.id !== reviewId);
    this.reviewNotifications.next(updatedReviews);
    this.hasUnseenReviews.next(updatedReviews.length > 0);
  }
}
