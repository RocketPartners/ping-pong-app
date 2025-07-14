import {Component, OnDestroy, OnInit} from '@angular/core';
import {MatDialog} from '@angular/material/dialog';
import {Subscription} from 'rxjs';
import {AchievementService} from '../../../_services/achievement.service';
import {AchievementNotification} from '../../../_models/achievement';

@Component({
  selector: 'app-achievement-notifications',
  templateUrl: './achievement-notifications.component.html',
  styleUrls: ['./achievement-notifications.component.scss'],
  standalone: false
})
export class AchievementNotificationsComponent implements OnInit, OnDestroy {
  notifications: AchievementNotification[] = [];
  hasUnseen: boolean = false;
  private subscription = new Subscription();

  constructor(
    private achievementService: AchievementService,
    private dialog: MatDialog
  ) {
  }

  ngOnInit(): void {
    // Subscribe to notifications
    this.subscription.add(
      this.achievementService.achievementNotifications$.subscribe(
        notifications => {
          this.notifications = notifications;
        }
      )
    );

    // Subscribe to unseen state
    this.subscription.add(
      this.achievementService.hasUnseenNotifications$.subscribe(
        hasUnseen => {
          this.hasUnseen = hasUnseen;
        }
      )
    );
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  /**
   * Mark all notifications as seen
   */
  markAsSeen(): void {
    this.achievementService.markNotificationsAsSeen();
  }

  /**
   * Clear all notifications
   */
  clearNotifications(): void {
    this.achievementService.clearNotifications();
  }

  /**
   * Format notification timestamp
   */
  formatTimestamp(date: Date): string {
    const now = new Date();
    const timestamp = new Date(date);
    const diffMs = now.getTime() - timestamp.getTime();
    const diffSec = Math.round(diffMs / 1000);
    const diffMin = Math.round(diffSec / 60);
    const diffHour = Math.round(diffMin / 60);
    const diffDay = Math.round(diffHour / 24);

    if (diffSec < 60) {
      return 'just now';
    } else if (diffMin < 60) {
      return `${diffMin}m ago`;
    } else if (diffHour < 24) {
      return `${diffHour}h ago`;
    } else if (diffDay === 1) {
      return 'yesterday';
    } else {
      return timestamp.toLocaleDateString();
    }
  }
}
