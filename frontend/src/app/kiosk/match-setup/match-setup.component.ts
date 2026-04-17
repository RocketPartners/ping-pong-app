import {Component} from '@angular/core';
import {Router} from '@angular/router';
import {KioskMatchType, LiveGameStateService} from '../_services/live-game-state.service';

@Component({
  selector: 'app-kiosk-match-setup',
  templateUrl: './match-setup.component.html',
  styleUrls: ['./match-setup.component.scss'],
  standalone: false
})
export class MatchSetupComponent {
  matchType: KioskMatchType | null = null;
  isRanked: boolean | null = null;
  bestOf: number | null = null;

  readonly bestOfOptions = [1, 3, 5, 7];

  constructor(private router: Router, private state: LiveGameStateService) {}

  get canContinue(): boolean {
    return this.matchType !== null && this.isRanked !== null && this.bestOf !== null;
  }

  continue(): void {
    if (!this.canContinue) return;
    this.state.startMatch({
      matchType: this.matchType!,
      isRanked: this.isRanked!,
      bestOf: this.bestOf!
    });
    this.router.navigate(['/kiosk/players']);
  }

  cancel(): void {
    this.router.navigate(['/kiosk']);
  }
}
