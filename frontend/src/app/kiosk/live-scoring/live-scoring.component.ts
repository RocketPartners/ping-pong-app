import {Component, OnDestroy, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {Subscription} from 'rxjs';
import {Player} from '../../_models/models';
import {KioskMatchState, LiveGameStateService} from '../_services/live-game-state.service';

@Component({
  selector: 'app-kiosk-live-scoring',
  templateUrl: './live-scoring.component.html',
  styleUrls: ['./live-scoring.component.scss'],
  standalone: false
})
export class LiveScoringComponent implements OnInit, OnDestroy {
  state: KioskMatchState | null = null;
  showGameCelebration = false;
  celebrationWinner: 1 | 2 | null = null;
  celebrationScore = {team1: 0, team2: 0};

  private sub?: Subscription;

  constructor(private router: Router, private stateService: LiveGameStateService) {}

  ngOnInit(): void {
    this.sub = this.stateService.state$.subscribe(s => {
      this.state = s;
      if (!s.config || s.team1.length === 0 || s.team2.length === 0) {
        this.router.navigate(['/kiosk/setup']);
      }
    });
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }

  get currentGame() {
    return this.state?.games[this.state.currentGameIndex];
  }

  get team1Wins(): number {
    return this.state?.games.filter(g => g.concluded && g.winner === 1).length ?? 0;
  }

  get team2Wins(): number {
    return this.state?.games.filter(g => g.concluded && g.winner === 2).length ?? 0;
  }

  teamLabel(players: Player[]): string {
    return players.map(p => p.firstName).join(' & ');
  }

  score(team: 1 | 2): void {
    const game = this.currentGame;
    if (!game || game.concluded) return;
    this.stateService.incrementScore(team);

    const updated = this.stateService.snapshot.games[this.stateService.snapshot.currentGameIndex];
    if (updated?.concluded) {
      this.celebrateGame(updated.winner!, updated.team1Score, updated.team2Score);
    }
  }

  undo(): void {
    this.stateService.undoLastPoint();
  }

  discard(): void {
    if (!confirm('Discard this match?')) return;
    this.stateService.discard();
    this.router.navigate(['/kiosk']);
  }

  private celebrateGame(winner: 1 | 2, team1Score: number, team2Score: number): void {
    this.celebrationWinner = winner;
    this.celebrationScore = {team1: team1Score, team2: team2Score};
    this.showGameCelebration = true;

    setTimeout(() => {
      this.showGameCelebration = false;
      if (this.stateService.matchIsOver()) {
        this.router.navigate(['/kiosk/summary']);
      } else {
        this.stateService.advanceToNextGame();
      }
    }, 2500);
  }
}
