// In app.component.ts
import {Component} from '@angular/core';
import {AccountService} from "./_services/account.service";
import {AuthenticatedPlayer} from "./_models/models";
import {AppConfig} from './_config/app.config';
import {PlayerUtilities} from "./player/player-utilities";
import {ThemeMode, ThemeService} from "./_services/theme.service"; // Add this import

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  standalone: false
})
export class AppComponent {
  authenticatedPlayer?: AuthenticatedPlayer | null;
  appName = AppConfig.appName;
  appVersion = AppConfig.version;
  playerColor: string | null = null;
  currentTheme: ThemeMode = 'light'; // Add this property
  protected readonly PlayerUtilities = PlayerUtilities;

  constructor(
    private accountService: AccountService,
    private themeService: ThemeService // Inject ThemeService
  ) {
    this.accountService.authenticatedPlayer.subscribe(x => {
      this.authenticatedPlayer = x;
      if (x) {
        this.playerColor = PlayerUtilities.getProfileIconColor(x.player.profileImage)
      }
    });

    // Subscribe to theme changes
    this.themeService.currentTheme$.subscribe(theme => {
      this.currentTheme = theme;
    });
  }

  logout() {
    this.accountService.logout();
  }

  // Add this method to toggle theme
  toggleTheme(): void {
    this.themeService.toggleTheme();
  }
}
