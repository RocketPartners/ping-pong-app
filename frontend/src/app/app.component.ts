// In app.component.ts
import {Component} from '@angular/core';
import {NavigationEnd, Router} from "@angular/router";
import {filter} from 'rxjs/operators';
import {AccountService} from "./_services/account.service";
import {AuthenticatedPlayer} from "./_models/models";
import {AppConfig} from './_config/app.config';
import {PlayerUtilities} from "./player/player-utilities";
import {ThemeMode, ThemeService} from "./_services/theme.service"; // Add this import
import {KioskAdminService, PendingKioskPairing} from "./_services/kiosk-admin.service";

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
  isKioskRoute = false;
  pendingKioskPairings: PendingKioskPairing[] = [];
  protected readonly PlayerUtilities = PlayerUtilities;

  constructor(
    private accountService: AccountService,
    private themeService: ThemeService, // Inject ThemeService
    private router: Router,
    private kioskAdminService: KioskAdminService
  ) {
    this.kioskAdminService.pending$.subscribe(list => this.pendingKioskPairings = list);
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

    // Track whether we're inside the kiosk route tree so we can hide the app shell.
    this.isKioskRoute = this.router.url.startsWith('/kiosk');
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe((event: any) => {
        this.isKioskRoute = (event.urlAfterRedirects || event.url || '').startsWith('/kiosk');
      });
  }

  get isAdmin(): boolean {
    return this.authenticatedPlayer?.player?.role === 'ADMIN';
  }

  logout() {
    this.accountService.logout();
  }

  goToKioskPairings(): void {
    this.router.navigate(['/admin/kiosk-pairings']);
  }

  // Add this method to toggle theme
  toggleTheme(): void {
    this.themeService.toggleTheme();
  }
}
