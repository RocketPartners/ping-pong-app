import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {PlayerService} from '../_services/player.service';
import {GameService} from '../_services/game.service';
import {forkJoin, of} from 'rxjs';
import {catchError, switchMap} from 'rxjs/operators';
import {PLAYER_STYLE_NAMES, PlayerStyle} from '../_models/player-style';
import {Game, Player} from "../_models/models";

@Component({
  selector: 'app-player-profile',
  templateUrl: './player-profile.component.html',
  styleUrls: ['./player-profile.component.scss'],
  standalone: false
})
export class PlayerProfileComponent implements OnInit {
  player: Player | undefined;
  recentGames: Game[] | any[] = [];
  loading = true;
  error = '';

  // Stats
  singleRankedWinRate = 0;
  singlesNormalWinRate = 0;
  doublesRankedWinRate = 0;
  doublesNormalWinRate = 0;
  totalGames = 0;

  // Player styles mapped to human-readable strings
  styleMap = new Map([
    [PlayerStyle.SPIN, 'Spin'],
    [PlayerStyle.POWER, 'Power'],
    [PlayerStyle.CREATIVE, 'Creative'],
    [PlayerStyle.AGGRESSIVE, 'Aggressive'],
    [PlayerStyle.RESILIENT, 'Resilient'],
    [PlayerStyle.ACE_MASTER, 'Ace Master'],
    [PlayerStyle.RALLY_KING, 'Rally King'],
    [PlayerStyle.TACTICIAN, 'Tactician']
  ]);

  constructor(
    private route: ActivatedRoute,
    private playerService: PlayerService,
    private gameService: GameService
  ) {
  }

  ngOnInit(): void {
    this.loadPlayerData();
  }

  loadPlayerData(): void {
    this.loading = true;

    this.route.paramMap.pipe(
      switchMap(params => {
        const username = params.get('username');
        if (!username) {
          this.error = 'No username provided';
          return of(null);
        }
        return this.playerService.getPlayerByUsername(username);
      }),
      switchMap(player => {
        if (!player) {
          this.error = 'Player not found';
          return of({player: null, games: []});
        }

        this.player = player;

        // Get the player's recent games
        return forkJoin({
          player: of(player),
          games: this.gameService.getPlayerGameHistory(player.playerId).pipe(
            catchError(() => of([]))
          )
        });
      })
    ).subscribe({
      next: (data) => {
        if (data.player) {
          this.player = data.player;
          this.recentGames = data.games || [];
          this.calculateStats();
        }
        this.loading = false;
      },
      error: (err) => {
        this.error = 'An error occurred while loading player data';
        this.loading = false;
        console.error(err);
      }
    });
  }

  calculateStats(): void {
    if (!this.player) return;

    // Calculate win rates
    const singlesRankedTotal = this.player.singlesRankedWins + this.player.singlesRankedLoses;
    this.singleRankedWinRate = singlesRankedTotal > 0
      ? Math.round((this.player.singlesRankedWins / singlesRankedTotal) * 100)
      : 0;

    const singlesNormalTotal = this.player.singlesNormalWins + this.player.singlesNormalLoses;
    this.singlesNormalWinRate = singlesNormalTotal > 0
      ? Math.round((this.player.singlesNormalWins / singlesNormalTotal) * 100)
      : 0;

    const doublesRankedTotal = this.player.doublesRankedWins + this.player.doublesRankedLoses;
    this.doublesRankedWinRate = doublesRankedTotal > 0
      ? Math.round((this.player.doublesRankedWins / doublesRankedTotal) * 100)
      : 0;

    // Calculate total games
    this.totalGames = singlesRankedTotal + singlesNormalTotal + doublesRankedTotal;
  }

  // Safe getter for style name with type guard
  getStyleName(style: string): string {
    return PLAYER_STYLE_NAMES[style as PlayerStyle] || 'Unknown';
  }

  getGameResult(game: Game): string {
    if (!this.player) return 'Unknown';

    if (game.singlesGame) {
      if (game.challengerId === this.player.playerId) {
        return game.challengerWin ? 'Win' : 'Loss';
      } else {
        return game.opponentWin ? 'Win' : 'Loss';
      }
    } else {
      // For doubles games, check if player is in the winning team
      const inChallengerTeam = game.challengerTeam.includes(this.player.playerId);
      if (inChallengerTeam) {
        return game.challengerTeamWin ? 'Win' : 'Loss';
      } else {
        return game.opponentTeamWin ? 'Win' : 'Loss';
      }
    }
  }

  isWin(game: Game): boolean {
    return this.getGameResult(game) === 'Win';
  }
}
