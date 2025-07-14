import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {GameService} from '../_services/game.service';
import {PlayerService} from '../_services/player.service';
import {forkJoin, of} from 'rxjs';
import {catchError, switchMap} from 'rxjs/operators';

// Import from centralized models
import {Game, Player} from '../_models/models';

@Component({
  selector: 'app-game-details',
  templateUrl: './game-details.component.html',
  styleUrls: ['./game-details.component.scss'],
  standalone: false
})
export class GameDetailsComponent implements OnInit {
  game: Game | null = null;
  challenger: Player | null = null;
  opponent: Player | null = null;
  challengerTeam: Player[] = [];
  opponentTeam: Player[] = [];
  loading = true;
  error = '';

  // Expose Math to the template
  Math = Math;

  constructor(
    public router: Router,
    private route: ActivatedRoute,
    private gameService: GameService,
    private playerService: PlayerService
  ) {
  }

  ngOnInit(): void {
    this.loadGameData();
  }

  loadGameData(): void {
    this.loading = true;

    this.route.paramMap.pipe(
      switchMap(params => {
        const gameId = params.get('id');
        if (!gameId) {
          this.error = 'No game ID provided';
          return of(null);
        }

        return this.gameService.getGameDetails(gameId).pipe(
          catchError(err => {
            this.error = `Failed to load game: ${err}`;
            return of(null);
          })
        );
      }),
      switchMap(game => {
        if (!game) {
          return of({
            game: null,
            challenger: null,
            opponent: null,
            challengerTeam: [],
            opponentTeam: []
          });
        }

        this.game = game;

        // Load the related player data based on game type
        if (game.singlesGame) {
          // Singles game - load challenger and opponent
          return forkJoin({
            game: of(game),
            challenger: this.playerService.getPlayerById(game.challengerId),
            opponent: this.playerService.getPlayerById(game.opponentId),
            challengerTeam: of([]),
            opponentTeam: of([])
          });
        } else {
          // Doubles game - load all team members
          const challengerTeamObservables = game.challengerTeam.map(playerId =>
            this.playerService.getPlayerById(playerId)
          );

          const opponentTeamObservables = game.opponentTeam.map(playerId =>
            this.playerService.getPlayerById(playerId)
          );

          return forkJoin({
            game: of(game),
            challenger: of(null),
            opponent: of(null),
            challengerTeam: challengerTeamObservables.length > 0
              ? forkJoin(challengerTeamObservables)
              : of([]),
            opponentTeam: opponentTeamObservables.length > 0
              ? forkJoin(opponentTeamObservables)
              : of([])
          });
        }
      })
    ).subscribe({
      next: (data) => {
        this.game = data.game;
        this.challenger = data.challenger;
        this.opponent = data.opponent;

        // Filter out null values and cast to Player[] since we know they're Players
        this.challengerTeam = (data.challengerTeam.filter(player => player !== null) as Player[]);
        this.opponentTeam = (data.opponentTeam.filter(player => player !== null) as Player[]);

        this.loading = false;
      },
      error: (err) => {
        this.error = 'An error occurred while loading game data';
        this.loading = false;
        console.error(err);
      }
    });
  }

  getGameType(): string {
    if (!this.game) return '';

    let type = this.game.singlesGame ? 'Singles' : 'Doubles';
    let mode = this.game.ratedGame ? 'Ranked' : 'Normals';

    return `${type} ${mode}`;
  }

  getWinner(): string {
    if (!this.game) return '';

    if (this.game.singlesGame) {
      if (this.game.challengerWin) {
        return this.challenger?.username || 'Challenger';
      } else {
        return this.opponent?.username || 'Opponent';
      }
    } else {
      if (this.game.challengerTeamWin) {
        return 'Challenger Team';
      } else {
        return 'Opponent Team';
      }
    }
  }

  calculateWinRate(player: Player | null): number {
    if (!player) return 0;

    const wins = player.singlesRankedWins || 0;
    const losses = player.singlesRankedLoses || 0;
    const total = wins + losses;

    if (total === 0) return 0;
    return Math.round((wins / total) * 100);
  }

  calculateDoublesWinRate(player: Player | null): number {
    if (!player) return 0;

    const wins = player.doublesRankedWins || 0;
    const losses = player.doublesRankedLoses || 0;
    const total = wins + losses;

    if (total === 0) return 0;
    return Math.round((wins / total) * 100);
  }

  getWinRateClass(winRate: number): string {
    if (winRate >= 70) return 'win-rate-excellent';
    if (winRate >= 55) return 'win-rate-good';
    if (winRate >= 45) return 'win-rate-average';
    if (winRate >= 30) return 'win-rate-poor';
    return 'win-rate-struggling';
  }

  navigateToPlayer(playerId: string): void {
    if (!playerId) return;

    this.router.navigate(['/player', playerId]);
  }
}
