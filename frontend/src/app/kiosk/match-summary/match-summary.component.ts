import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {GameService} from '../../_services/game.service';
import {buildGameDtos} from '../../_shared/utils/game-conversion.util';
import {KioskMatchState, LiveGameStateService} from '../_services/live-game-state.service';
import {Player} from '../../_models/models';

type Status = 'idle' | 'submitting' | 'success' | 'error';

@Component({
  selector: 'app-kiosk-match-summary',
  templateUrl: './match-summary.component.html',
  styleUrls: ['./match-summary.component.scss'],
  standalone: false
})
export class MatchSummaryComponent implements OnInit {
  state: KioskMatchState | null = null;
  status: Status = 'idle';
  errorMessage = '';

  constructor(
    private router: Router,
    private stateService: LiveGameStateService,
    private gameService: GameService
  ) {}

  ngOnInit(): void {
    this.state = this.stateService.snapshot;
    if (!this.state.config || this.state.team1.length === 0 || this.state.team2.length === 0) {
      this.router.navigate(['/kiosk']);
    }
  }

  teamLabel(players: Player[]): string {
    return players.map(p => p.firstName).join(' & ');
  }

  get team1Wins(): number {
    return this.state?.games.filter(g => g.concluded && g.winner === 1).length ?? 0;
  }

  get team2Wins(): number {
    return this.state?.games.filter(g => g.concluded && g.winner === 2).length ?? 0;
  }

  get matchWinner(): Player[] | null {
    if (!this.state) return null;
    if (this.team1Wins > this.team2Wins) return this.state.team1;
    if (this.team2Wins > this.team1Wins) return this.state.team2;
    return null;
  }

  get concludedGames() {
    return this.state?.games.filter(g => g.concluded) ?? [];
  }

  submit(): void {
    if (!this.state?.config || this.status === 'submitting') return;
    this.status = 'submitting';
    this.errorMessage = '';

    const dtos = buildGameDtos({
      matchType: this.state.config.matchType,
      isRanked: this.state.config.isRanked,
      team1: this.state.team1,
      team2: this.state.team2,
      games: this.concludedGames.map(g => ({team1Score: g.team1Score, team2Score: g.team2Score}))
    });

    this.gameService.saveGames(dtos).subscribe({
      next: () => {
        this.status = 'success';
        this.stateService.markFinished();
        setTimeout(() => {
          this.stateService.discard();
          this.router.navigate(['/kiosk']);
        }, 3500);
      },
      error: err => {
        this.status = 'error';
        this.errorMessage = err?.error?.message || err?.message || 'Unable to submit match.';
      }
    });
  }

  discard(): void {
    if (!confirm('Discard match without saving?')) return;
    this.stateService.discard();
    this.router.navigate(['/kiosk']);
  }

  retry(): void {
    this.status = 'idle';
    this.submit();
  }
}
