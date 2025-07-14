import {Component, OnInit} from '@angular/core';
import {PlayerReviewService} from '../_services/player-review.service';
import {AccountService} from '../_services/account.service';
import {MatDialog} from '@angular/material/dialog';
import {PlayerReviewDialogComponent} from '../player-review-dialog/player-review-dialog.component';
import {MatSnackBar} from '@angular/material/snack-bar';
import {PlayerStyleReview} from '../_models/models';

@Component({
  selector: 'app-player-unacknowledged-reviews',
  templateUrl: './player-unacknowledged-reviews.component.html',
  styleUrls: ['./player-unacknowledged-reviews.component.scss'],
  standalone: false,
})
export class PlayerUnacknowledgedReviewsComponent implements OnInit {
  unacknowledgedReviews: PlayerStyleReview[] = [];
  loading = true;

  constructor(
    private playerReviewService: PlayerReviewService,
    private accountService: AccountService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {
  }

  ngOnInit(): void {
    this.loadUnacknowledgedReviews();
  }

  loadUnacknowledgedReviews(): void {
    const currentPlayer = this.accountService.playerValue?.player;
    if (currentPlayer) {
      this.playerReviewService.getRecentUnacknowledgedReviews(currentPlayer.playerId)
        .subscribe({
          next: (reviews) => {
            this.unacknowledgedReviews = reviews || [];
            this.loading = false;
          },
          error: (err) => {
            console.error('Error loading reviews:', err);
            this.loading = false;
          }
        });
    } else {
      this.loading = false;
    }
  }

  /**
   * Dismiss the review by acknowledging it and submitting an empty response
   */
  dismissReview(review: PlayerStyleReview): void {
    if (!review.id) return;

    const currentPlayerId = this.accountService.playerValue?.player.playerId;
    if (!currentPlayerId) return;

    // Submit an empty response (no strengths or weaknesses)
    this.playerReviewService.respondToReview(
      review.id,
      currentPlayerId,
      review.playerId,
      review.gameIds || [],
      [], // Empty strengths array
      []  // Empty improvements array
    ).subscribe({
      error: (err) => {
        console.error('Error dismissing review:', err);
        this.snackBar.open('Error dismissing review', 'Close', {duration: 3000});
      }
    });
  }

  respondToReview(review: PlayerStyleReview): void {
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
          responseReview.playerId,
          review.gameIds || [],
          responseReview.strengths,
          responseReview.improvements
        ).subscribe({
          error: (err) => {
            console.error('Error sending response:', err);
            this.snackBar.open('Error sending response', 'Close', {duration: 3000});
          }
        });
      }
    });
  }

  formatStyleName(style: string): string {
    return style.replace('_', ' ').toLowerCase()
      .split(' ')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }
}
