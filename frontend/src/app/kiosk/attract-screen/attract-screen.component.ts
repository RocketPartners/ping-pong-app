import {Component, OnDestroy, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {LiveGameStateService} from '../_services/live-game-state.service';
import {KioskAuthService} from '../_services/kiosk-auth.service';

const THIRTY_DAYS_SECONDS = 30 * 24 * 60 * 60;
const LONG_PRESS_MS = 1500;

@Component({
  selector: 'app-kiosk-attract',
  templateUrl: './attract-screen.component.html',
  styleUrls: ['./attract-screen.component.scss'],
  standalone: false
})
export class AttractScreenComponent implements OnInit, OnDestroy {
  hasActiveMatch = false;
  daysUntilExpiry: number | null = null;
  settingsOpen = false;
  pairedUsername: string | null = null;
  expiryDate: Date | null = null;
  longPressProgress = 0;

  private longPressTimer: number | null = null;
  private longPressAnimation: number | null = null;

  constructor(
    private router: Router,
    private state: LiveGameStateService,
    private kioskAuth: KioskAuthService
  ) {}

  ngOnInit(): void {
    const snapshot = this.state.snapshot;
    this.hasActiveMatch = !!snapshot.config && snapshot.games.length > 0 && !snapshot.finishedAt;

    const secs = this.kioskAuth.secondsUntilExpiry();
    if (Number.isFinite(secs) && secs > 0 && secs < THIRTY_DAYS_SECONDS) {
      this.daysUntilExpiry = Math.ceil(secs / (24 * 60 * 60));
    }

    const paired = this.kioskAuth.getPairedPlayer();
    this.pairedUsername = paired?.player?.username || null;
    this.expiryDate = this.kioskAuth.getExpiryDate();
  }

  ngOnDestroy(): void {
    this.cancelLongPress();
  }

  startNewMatch(): void {
    this.state.discard();
    this.router.navigate(['/kiosk/setup']);
  }

  resumeMatch(): void {
    const snapshot = this.state.snapshot;
    if (snapshot.team1.length === 0 || snapshot.team2.length === 0) {
      this.router.navigate(['/kiosk/players']);
    } else if (this.state.matchIsOver(snapshot)) {
      this.router.navigate(['/kiosk/summary']);
    } else {
      this.router.navigate(['/kiosk/live']);
    }
  }

  beginLongPress(): void {
    this.cancelLongPress();
    const start = performance.now();
    const tick = () => {
      const elapsed = performance.now() - start;
      this.longPressProgress = Math.min(1, elapsed / LONG_PRESS_MS);
      if (this.longPressProgress < 1) {
        this.longPressAnimation = requestAnimationFrame(tick);
      }
    };
    this.longPressAnimation = requestAnimationFrame(tick);
    this.longPressTimer = window.setTimeout(() => {
      this.settingsOpen = true;
      this.cancelLongPress();
    }, LONG_PRESS_MS);
  }

  cancelLongPress(): void {
    if (this.longPressTimer !== null) {
      clearTimeout(this.longPressTimer);
      this.longPressTimer = null;
    }
    if (this.longPressAnimation !== null) {
      cancelAnimationFrame(this.longPressAnimation);
      this.longPressAnimation = null;
    }
    this.longPressProgress = 0;
  }

  closeSettings(): void {
    this.settingsOpen = false;
  }

  unpair(): void {
    this.kioskAuth.unpair();
    this.settingsOpen = false;
    this.router.navigate(['/kiosk/pair']);
  }
}
