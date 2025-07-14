import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RouterModule} from '@angular/router';

// Components
import {LoadingSpinnerComponent} from './components/loading-spinner/loading-spinner.component';
import {ErrorMessageComponent} from './components/error-message/error-message.component';
import {RatingBadgeComponent} from './components/rating-badge/rating-badge.component';
import {WinRateBarComponent} from './components/win-rate-bar/win-rate-bar.component';
import {SectionTitleComponent} from './components/section-title/section-title.component';
import {LineChartComponent} from './components/line-chart/line-chart.component';
import {ConfirmDialogComponent} from './components/confirm-dialog/confirm-dialog.component';
import {SkeletonLoadingDirective} from './components/skeleton-loading/skeleton-loading.directive';

// Material imports
import {MatCardModule} from '@angular/material/card';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatBadgeModule} from '@angular/material/badge';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatTooltipModule} from '@angular/material/tooltip';
import {MatChipsModule} from '@angular/material/chips';
import {MatDividerModule} from '@angular/material/divider';
import {MatDialogModule} from '@angular/material/dialog';
import {AchievementCardComponent} from "./components/achievement-card/achievement-card.component";
import {
  AchievementNotificationsComponent
} from "./components/achievement-notifications/achievement-notifications.component";
import {MatMenu, MatMenuTrigger} from "@angular/material/menu";
import {ProfileAvatarComponent} from "./components/profile-avatar/profile-avatar.component";

@NgModule({
  declarations: [
    LoadingSpinnerComponent,
    ErrorMessageComponent,
    RatingBadgeComponent,
    WinRateBarComponent,
    SectionTitleComponent,
    LineChartComponent,
    ConfirmDialogComponent,
    AchievementCardComponent,
    AchievementNotificationsComponent,
    ProfileAvatarComponent
  ],
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatBadgeModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatChipsModule,
    MatDividerModule,
    MatDialogModule,
    MatMenuTrigger,
    MatMenu,
    SkeletonLoadingDirective
  ],
  exports: [
    // Re-export components
    LoadingSpinnerComponent,
    ErrorMessageComponent,
    RatingBadgeComponent,
    WinRateBarComponent,
    SectionTitleComponent,
    LineChartComponent,
    ConfirmDialogComponent,
    AchievementCardComponent,
    AchievementNotificationsComponent,
    ProfileAvatarComponent,
    SkeletonLoadingDirective,
    // Re-export modules
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatBadgeModule,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatChipsModule,
    MatDividerModule,
    MatDialogModule
  ]
})
export class SharedComponentsModule {
}
