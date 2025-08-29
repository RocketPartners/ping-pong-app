import {RouterModule, Routes} from "@angular/router";
import {NgModule} from "@angular/core";
import {HomeComponent} from "./home/home.component";
import {RegisterComponent} from "./register/register.component";
import {LoginComponent} from "./login/login.component";
import {LeaderboardComponent} from "./leaderboard/leaderboard.component";
import {AuthGuard} from "./_helpers/auth.guard";
import {MatchBuilderComponent} from "./match-builder/match-builder.component";
import {PlayerProfileComponent} from "./player-profile/player-profile.component";
import {GameDetailsComponent} from "./game-details/game-details.component";
import {PlayerStatisticsComponent} from "./player-statistics/player-statistics.component";
// Import tournament components
import {TournamentListComponent} from "./tournament/tournament-list/tournament-list.component";
import {TournamentCreateComponent} from "./tournament/tournament-create/tournament-create.component";
import {TournamentDetailComponent} from "./tournament/tournament-detail/tournament-detail.component";
// Import achievement components
import {AchievementListComponent} from "./achievement/achievement-list/achievement-list.component";
// Import password reset components
import {ForgotPasswordComponent} from "./password-reset/forgot-password/forgot-password.component";
import {ResetPasswordComponent} from "./password-reset/reset-password/reset-password.component";
import {ProfileSettingsComponent} from "./profile-settings/profile-settings.component";
// Import live matches component
import {LiveMatchesComponent} from "./live-matches/live-matches.component";
// Import Slack integration component
import {SlackIntegrationComponent} from "./slack-integration/slack-integration.component";

const routes: Routes = [
  {path: '', component: HomeComponent, canActivate: [AuthGuard]},
  {path: 'login', component: LoginComponent},
  {path: 'register', component: RegisterComponent},
  {path: 'profile-settings', component: ProfileSettingsComponent, canActivate: [AuthGuard]},
  {path: 'forgot-password', component: ForgotPasswordComponent},
  {path: 'reset-password', component: ResetPasswordComponent},
  {path: 'leaderboard', component: LeaderboardComponent, canActivate: [AuthGuard]},
  {path: 'match-builder', component: MatchBuilderComponent, canActivate: [AuthGuard]},
  {path: 'player/:username', component: PlayerProfileComponent, canActivate: [AuthGuard]},
  {path: 'game/:id', component: GameDetailsComponent, canActivate: [AuthGuard]},
  {path: 'statistics', component: PlayerStatisticsComponent, canActivate: [AuthGuard]},
  {path: 'statistics/:username', component: PlayerStatisticsComponent, canActivate: [AuthGuard]},

  // Live matches route
  {path: 'live-matches', component: LiveMatchesComponent, canActivate: [AuthGuard]},

  // Slack integration route
  {path: 'slack-integration', component: SlackIntegrationComponent, canActivate: [AuthGuard]},

  // Tournament routes
  {path: 'tournaments', component: TournamentListComponent, canActivate: [AuthGuard]},
  {path: 'tournaments/create', component: TournamentCreateComponent, canActivate: [AuthGuard]},
  {path: 'tournaments/:id', component: TournamentDetailComponent, canActivate: [AuthGuard]},

  // Achievement routes
  {path: 'achievements', component: AchievementListComponent, canActivate: [AuthGuard]},
  {path: 'achievements/:username', component: AchievementListComponent, canActivate: [AuthGuard]},

  // Admin routes (lazy loaded)
  {
    path: 'admin',
    loadChildren: () => import('./admin/admin.module').then(m => m.AdminModule),
    canActivate: [AuthGuard]
  },

  // otherwise redirect to home
  {path: '**', redirectTo: ''}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {
}
