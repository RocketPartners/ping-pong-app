import {NgModule} from '@angular/core';
import {HTTP_INTERCEPTORS, provideHttpClient, withFetch, withInterceptorsFromDi} from '@angular/common/http';
import {CommonModule, DatePipe} from "@angular/common";
import {BrowserModule} from "@angular/platform-browser";
import {BrowserAnimationsModule} from "@angular/platform-browser/animations";
import {FormsModule, ReactiveFormsModule} from "@angular/forms";
import {RouterModule} from '@angular/router';
import {SharedDashboardModule} from './shared-dashboard.module';

// Components
import {AppComponent} from "./app.component";
import {AlertComponent} from "./alert/alert.component";
import {HomeComponent} from "./home/home.component";
import {LeaderboardComponent} from './leaderboard/leaderboard.component';
import {LoginComponent} from "./login/login.component";
import {RegisterComponent} from "./register/register.component";
import {MatchBuilderComponent} from "./match-builder/match-builder.component";
import {PlayerProfileComponent} from './player-profile/player-profile.component';
import {GameDetailsComponent} from './game-details/game-details.component';
import {ThemeToggleComponent} from './_shared/components/theme-toggle/theme-toggle.component';

// Services
import {PlayerService} from "./_services/player.service";
import {AccountService} from "./_services/account.service";
import {GameService} from "./_services/game.service";
import {AchievementService} from "./_services/achievement.service";
import {TournamentService} from "./_services/tournament.service";
import {StatsService} from "./_services/stats.service";
import {PasswordResetService} from "./_services/password-reset.service";
import {EloHistoryService} from "./_services/elo-history.service";
import {MatchService} from "./_services/match.service";

// Helpers
import {JwtInterceptor} from "./_helpers/jwt.interceptor";
import {ErrorInterceptor} from "./_helpers/error.interceptor";

// Modules
import {AppRoutingModule} from "./app-routing.modules";
import {AchievementModule} from "./achievement/achievement.module";
import {PlayerStatisticsModule} from "./player-statistics/player-statistics.module";
import {TournamentModule} from "./tournament/tournament.module";
import {SharedComponentsModule} from "./_shared/shared-components.module";
import {SharedPlayerModule} from "./shared-player-module";
import {PasswordResetModule} from "./password-reset/password-reset.module";
import {GameModule} from "./game/game.module";

// Angular Material Imports
import {AgGridAngular} from "ag-grid-angular";
import {MatSortHeader, MatSortModule} from "@angular/material/sort";
import {
  MatCell,
  MatCellDef,
  MatColumnDef,
  MatHeaderCell,
  MatHeaderCellDef,
  MatHeaderRow,
  MatHeaderRowDef,
  MatNoDataRow,
  MatRow,
  MatRowDef,
  MatTable,
  MatTableModule
} from "@angular/material/table";
import {MatSlideToggleModule} from '@angular/material/slide-toggle';
import {MatAnchor, MatButton, MatButtonModule} from "@angular/material/button";
import {MatDivider, MatDividerModule} from "@angular/material/divider";
import {MatIcon, MatIconModule} from '@angular/material/icon';
import {MatCardModule} from '@angular/material/card';
import {MatError, MatFormField, MatFormFieldModule, MatLabel} from "@angular/material/form-field";
import {MatInput, MatInputModule} from "@angular/material/input";
import {
  MatDatepicker,
  MatDatepickerInput,
  MatDatepickerModule,
  MatDatepickerToggle
} from "@angular/material/datepicker";
import {MatOption, provideNativeDateAdapter} from '@angular/material/core';
import {MatAutocompleteModule, MatAutocompleteTrigger} from "@angular/material/autocomplete";
import {MatSelect, MatSelectModule} from "@angular/material/select";
import {MatGridList, MatGridTile, MatGridTileHeaderCssMatStyler} from "@angular/material/grid-list";
import {MatStep, MatStepLabel, MatStepper, MatStepperNext, MatStepperPrevious} from "@angular/material/stepper";
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatTabsModule} from '@angular/material/tabs';
import {MatToolbarModule} from '@angular/material/toolbar';
import {MatSidenavModule} from '@angular/material/sidenav';
import {MatListModule} from '@angular/material/list';
import {MatMenuModule} from '@angular/material/menu';
import {MatChipsModule} from '@angular/material/chips';
import {MatButtonToggleModule} from '@angular/material/button-toggle';
import {provideAnimationsAsync} from "@angular/platform-browser/animations/async";
import {MatTooltip} from "@angular/material/tooltip";
import {MatSlider, MatSliderRangeThumb} from "@angular/material/slider";
import {MatCheckbox} from "@angular/material/checkbox";
import {BaseChartDirective, provideCharts, ThemeService, withDefaultRegisterables} from "ng2-charts";
import {PlayerReviewDialogComponent} from "./player-review-dialog/player-review-dialog.component";
import {
  PlayerUnacknowledgedReviewsComponent
} from "./player-unacknowledged-reviews/player-unacknowledged-reviews.component";
import {PlayerReviewNotificationsComponent} from "./player-review-notifications/player-review-notifications.component";
import {MatSnackBarModule} from "@angular/material/snack-bar";
import {MatBadgeModule} from "@angular/material/badge";
import {MatPaginator} from "@angular/material/paginator";
import {StyleRatingService} from "./_services/style-rating.service";
import {ProfileSettingsComponent} from "./profile-settings/profile-settings.component";
import {EasterEggDisplayComponent} from "./_shared/components/easter-egg-display/easter-egg-display.component";
import {EasterEggHunterComponent} from "./easter-egg-hunter/easter-egg-hunter.component";
import {EasterEggService} from "./_services/easter-egg.service";


@NgModule({
  declarations: [
    AppComponent,
    LeaderboardComponent,
    LoginComponent,
    RegisterComponent,
    AlertComponent,
    HomeComponent,
    MatchBuilderComponent,
    PlayerProfileComponent,
    GameDetailsComponent,
    PlayerReviewDialogComponent,
    PlayerUnacknowledgedReviewsComponent,
    PlayerReviewNotificationsComponent,
    ProfileSettingsComponent
  ],
  bootstrap: [AppComponent], imports: [
    // Angular Core
    DatePipe,
    ReactiveFormsModule,
    BrowserAnimationsModule,
    BrowserModule,
    AppRoutingModule,
    RouterModule,
    CommonModule,
    FormsModule,
    GameModule,
    // Third Party
    AgGridAngular,
    // Angular Material
    MatSortModule,
    MatTable,
    MatColumnDef,
    MatHeaderCell,
    MatHeaderCellDef,
    MatSortHeader,
    MatCellDef,
    MatCell,
    MatHeaderRow,
    MatRow,
    MatHeaderRowDef,
    MatRowDef,
    MatAnchor,
    MatDivider,
    MatButtonModule,
    MatDividerModule,
    MatIconModule,
    MatCardModule,
    MatFormField,
    MatFormFieldModule,
    MatInput,
    MatInputModule,
    MatSelectModule,
    MatDatepickerToggle,
    MatDatepicker,
    MatDatepickerInput,
    MatDatepickerModule,
    MatSlideToggleModule,
    MatAutocompleteModule,
    MatOption,
    MatAutocompleteTrigger,
    MatSelect,
    MatGridList,
    MatGridTile,
    MatGridTileHeaderCssMatStyler,
    MatButton,
    MatStepper,
    MatStep,
    MatStepperNext,
    MatStepLabel,
    MatStepperPrevious,
    MatProgressBarModule,
    MatProgressSpinnerModule,
    MatTabsModule,
    MatToolbarModule,
    MatSidenavModule,
    MatListModule,
    MatMenuModule,
    MatChipsModule,
    MatButtonToggleModule,
    MatTooltip,
    MatSlider,
    MatCheckbox,
    MatSliderRangeThumb,
    MatNoDataRow,
    MatIcon,
    MatError,
    MatLabel,
    MatBadgeModule,
    MatSnackBarModule,
    MatTableModule,
    // App modules
    SharedComponentsModule,
    SharedPlayerModule,
    SharedDashboardModule,
    TournamentModule,
    AchievementModule,
    PasswordResetModule,
    BaseChartDirective,
    PlayerStatisticsModule,
    MatPaginator,
    ThemeToggleComponent,
    EasterEggDisplayComponent,
    EasterEggHunterComponent,
  ], providers: [
    {provide: HTTP_INTERCEPTORS, useClass: JwtInterceptor, multi: true},
    {provide: HTTP_INTERCEPTORS, useClass: ErrorInterceptor, multi: true},
    PlayerService,
    AccountService,
    GameService,
    MatchService,
    TournamentService,
    AchievementService,
    StatsService,
    PasswordResetService,
    EloHistoryService,
    StyleRatingService,
    EasterEggService,
    ThemeService,
    provideHttpClient(withFetch()),
    provideAnimationsAsync(),
    provideNativeDateAdapter(),
    provideHttpClient(withInterceptorsFromDi()),
    provideCharts(withDefaultRegisterables())
  ]
})
export class AppModule {
}
