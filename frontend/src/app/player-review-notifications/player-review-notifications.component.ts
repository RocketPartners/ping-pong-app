import {Component, OnDestroy, OnInit} from '@angular/core';
import {BehaviorSubject, Subscription, combineLatest} from 'rxjs';
import {map} from 'rxjs/operators';
import {PlayerReviewService} from '../_services/player-review.service';
import {AchievementService} from '../_services/achievement.service';
import {AccountService} from '../_services/account.service';
import {MatDialog} from '@angular/material/dialog';
import {MatSnackBar} from '@angular/material/snack-bar';
import {Router} from '@angular/router';
import {PlayerReviewDialogComponent} from '../player-review-dialog/player-review-dialog.component';
import {PlayerStyleReview} from '../_models/models';
import {AchievementNotification} from '../_models/achievement';

@Component({
  selector: 'app-review-notifications', // Changed to match what's used in app.component.html
  templateUrl: './player-review-notifications.component.html',
  styleUrls: ['./player-review-notifications.component.scss'],
  standalone: false,
})
export class PlayerReviewNotificationsComponent implements OnInit, OnDestroy {
  loading = false;
  // Observable for review notifications
  private reviewNotifications = new BehaviorSubject<PlayerStyleReview[]>([]);
  public reviewNotifications$ = this.reviewNotifications.asObservable();
  
  // Observable for achievement notifications 
  private achievementNotifications = new BehaviorSubject<any[]>([]);
  public achievementNotifications$ = this.achievementNotifications.asObservable();
  
  // Individual notification counts for badges
  public reviewNotificationCount$ = this.reviewNotifications$.pipe(
    map(reviews => reviews.length)
  );
  
  public achievementNotificationCount$ = this.achievementNotifications$.pipe(
    map(achievements => achievements.length)
  );
  
  private subscriptions: Subscription[] = [];

  constructor(
    private playerReviewService: PlayerReviewService,
    private achievementService: AchievementService,
    private accountService: AccountService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private router: Router
  ) {
  }

  ngOnInit(): void {
    // Load notifications on init
    this.loadNotifications();

    // Set up a refresh interval (every 5 minutes)
    const refreshInterval = setInterval(() => {
      this.loadNotifications(true);
    }, 5 * 60 * 1000);

    // Store the interval for cleanup
    this.subscriptions.push({
      unsubscribe: () => clearInterval(refreshInterval)
    } as Subscription);

    // Listen for achievement acknowledgment events
    const handleAchievementAcknowledgment = (event: any) => {
      console.log('Achievement notifications acknowledged, clearing notifications');
      // Clear achievement notifications immediately
      this.achievementNotifications.next([]);
    };

    window.addEventListener('achievement-notifications-acknowledged', handleAchievementAcknowledgment);

    // Store the event listener for cleanup
    this.subscriptions.push({
      unsubscribe: () => window.removeEventListener('achievement-notifications-acknowledged', handleAchievementAcknowledgment)
    } as Subscription);
  }

  ngOnDestroy(): void {
    // Clean up subscriptions
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  /**
   * Load both types of notifications
   */
  loadNotifications(silent: boolean = false): void {
    this.loadReviewNotifications(silent);
    this.loadAchievementNotifications(silent);
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
          if (!silent) this.loading = false;
        },
        error: (err) => {
          console.error('Error loading review notifications:', err);
          if (!silent) this.loading = false;
        }
      });
  }

  /**
   * Load recent achievement notifications for the current player
   */
  loadAchievementNotifications(silent: boolean = false): void {
    const currentPlayer = this.accountService.playerValue?.player;
    if (!currentPlayer) return;

    this.achievementService.getRecentAchievementNotifications(currentPlayer.playerId)
      .subscribe({
        next: (achievements) => {
          const notificationAchievements = achievements || [];
          this.achievementNotifications.next(notificationAchievements);
        },
        error: (err) => {
          console.error('Error loading achievement notifications:', err);
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
   * Handle clicking on an achievement notification (navigate to achievements page)
   */
  viewAchievement(achievement: any, event: Event): void {
    event.stopPropagation();
    
    // Navigate to achievements page
    this.router.navigate(['/achievements']).then(() => {
      // Remove the achievement from notifications after navigation
      this.dismissAchievementNotification(achievement.id);
    });
  }

  /**
   * Dismiss an achievement notification
   */
  dismissAchievementNotification(achievementId: string): void {
    const currentAchievements = this.achievementNotifications.value;
    const updatedAchievements = currentAchievements.filter(a => a.id !== achievementId);
    this.achievementNotifications.next(updatedAchievements);
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
   * Format achievement category for display
   */
  formatCategoryName(category: string): string {
    return category.replace('_', ' ').toLowerCase()
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
  }
}
