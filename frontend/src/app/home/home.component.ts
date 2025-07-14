import {Component, OnDestroy, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {AccountService} from "../_services/account.service";
import {GameService} from "../_services/game.service";
import {PlayerService} from "../_services/player.service";
import {StatsService, SystemStats} from "../_services/stats.service";
import {GameSidebarService} from "../game/game-sidebar/game-sidebar.service";
import {forkJoin, Subscription} from 'rxjs';
import {AuthenticatedPlayer, Game, Player} from "../_models/models";

@Component({
  templateUrl: 'home.component.html',
  styleUrls: ['home.component.scss'],
  standalone: false
})
export class HomeComponent implements OnInit, OnDestroy {
  authenticatedPlayer: AuthenticatedPlayer | null;
  player: Player | undefined;
  recentGames: Game[] = [];
  systemStats: SystemStats | null = null;
  loading = true;
  error = '';
  // Store player names for display to avoid repeated API calls
  playerNames: Record<string, string> = {};
  // Color schemes for charts and visualizations
  colorSchemes = {
    gameTypes: {
      'Singles Ranked': '#4285F4', // Blue
      'Doubles Ranked': '#EA4335', // Red
      'Singles Normal': '#34A853', // Green
      'Doubles Normal': '#FBBC05', // Yellow
    } as Record<string, string>,
    winDistribution: {
      '0 wins': '#E57373', // Light Red
      '1-5 wins': '#FFB74D', // Light Orange
      '6-10 wins': '#81C784', // Light Green
      '11-20 wins': '#64B5F6', // Light Blue
      '20+ wins': '#9575CD', // Light Purple
    } as Record<string, string>,
    achievements: [
      '#4285F4', '#EA4335', '#34A853', '#FBBC05', '#9E9E9E'
    ]
  };
  // Subscription handling
  private subscriptions = new Subscription();

  constructor(
    private router: Router,
    private accountService: AccountService,
    private playerService: PlayerService,
    private gameService: GameService,
    private statsService: StatsService,
    private gameDetailService: GameSidebarService
  ) {
    this.authenticatedPlayer = this.accountService.playerValue;
    this.player = this.authenticatedPlayer?.player;
  }

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    this.loading = true;

    // Load both system stats and recent games
    const dataSubscription = forkJoin({
      recentGames: this.gameService.getRecentGames(6),
      systemStats: this.statsService.getSystemStats()
    }).subscribe({
      next: (data) => {
        this.recentGames = data.recentGames || [];
        this.systemStats = data.systemStats;

        if (this.recentGames.length > 0) {
          this.preloadAllPlayerData();
        } else {
          this.loading = false;
        }
      },
      error: (err) => {
        this.error = 'Failed to load dashboard data';
        this.loading = false;
        console.error(err);
      }
    });

    this.subscriptions.add(dataSubscription);
  }

  /**
   * Preload ALL player data needed for the page in a single batch
   * This prevents repeated API calls
   */
  preloadAllPlayerData(): void {
    // Get all player IDs from recent games
    const playerIds = new Set<string>();

    // Add players from recent games
    this.recentGames.forEach(game => {
      if (game.singlesGame) {
        if (game.challengerId) playerIds.add(game.challengerId);
        if (game.opponentId) playerIds.add(game.opponentId);
      } else if (game.doublesGame) {
        game.challengerTeam?.forEach(id => playerIds.add(id));
        game.opponentTeam?.forEach(id => playerIds.add(id));
      }
    });

    // Add current player if needed
    if (this.player) {
      // Pre-populate player name for current player
      this.playerNames[this.player.playerId] = `${this.player.firstName} ${this.player.lastName}`;
      playerIds.delete(this.player.playerId);
    }

    // Get all players at once
    if (playerIds.size > 0) {
      const playerArray = Array.from(playerIds);

      // Get all players at once (if your API supports it)
      this.subscriptions.add(
        this.playerService.getPlayers().subscribe({
          next: (players) => {
            if (players) {
              // Create a lookup map for player names
              players.forEach(player => {
                this.playerNames[player.playerId] = `${player.firstName} ${player.lastName}`;
              });
            }
            this.loading = false;
          },
          error: (err) => {
            console.error('Error fetching players:', err);
            this.loading = false;
          }
        })
      );
    } else {
      this.loading = false;
    }
  }

  /**
   * Get player name by ID safely (without triggering API calls)
   */
  getPlayerName(playerId: string): string {
    // Return from our pre-populated player names map
    if (this.playerNames[playerId]) {
      return this.playerNames[playerId];
    }

    // Fallback to current player
    if (this.player && this.player.playerId === playerId) {
      return `${this.player.firstName} ${this.player.lastName}`;
    }

    // Return placeholder if not found (but don't trigger any API calls!)
    return 'Unknown Player';
  }

  ngOnDestroy(): void {
    // Clean up all subscriptions
    this.subscriptions.unsubscribe();
  }

  navigateToCreateMatch(): void {
    this.router.navigate(['/match-builder']);
  }

  navigateToStats(): void {
    this.router.navigate(['/statistics']);
  }

  navigateToTournament(): void {
    this.router.navigate(['/tournament']);
  }

  navigateToGame(gameId: string): void {
    if (gameId) {
      // Open the game details drawer using the gameId
      this.gameDetailService.open(gameId);
    } else {
      console.warn('Attempted to open game detail with empty ID');
    }
  }

  navigateToPlayer(username: string): void {
    this.router.navigate(['/statistics/' + username]);
  }

  isWin(game: Game): boolean {
    if (!this.player) return false;

    if (game.singlesGame) {
      return (game.challengerId === this.player.playerId && game.challengerWin) ||
        (game.opponentId === this.player.playerId && game.opponentWin);
    } else {
      // For doubles games, check if player is in the winning team
      const inChallengerTeam = game.challengerTeam.includes(this.player.playerId);
      return (inChallengerTeam && game.challengerTeamWin) ||
        (!inChallengerTeam && game.opponentTeamWin);
    }
  }

  /**
   * Utility function to get color for a game type
   */
  getGameTypeColor(gameType: string): string {
    return this.colorSchemes.gameTypes[gameType] || '#9E9E9E';
  }

  /**
   * Utility function to get color for win distribution
   */
  getWinDistributionColor(key: string): string {
    return this.colorSchemes.winDistribution[key] || '#9E9E9E';
  }

  /**
   * Utility function to get color for achievement
   */
  getAchievementColor(index: number): string {
    return this.colorSchemes.achievements[index % this.colorSchemes.achievements.length];
  }

  /**
   * Utility function to get class for win rate
   */
  getWinRateClass(winRate: number): string {
    if (winRate >= 80) return 'excellent';
    if (winRate >= 60) return 'good';
    if (winRate >= 45) return 'average';
    if (winRate >= 30) return 'below-average';
    return 'poor';
  }

  /**
   * Helper method to convert an object to array of key-value pairs for rendering
   */
  objectToArray(obj: Record<string, number> | undefined): { name: string, value: number }[] {
    if (!obj) return [];
    return Object.entries(obj).map(([name, value]) => ({name, value}));
  }

  /**
   * Calculate percentage for visualization
   */
  calculatePercentage(value: number, total: number): number {
    return total > 0 ? (value / total) * 100 : 0;
  }

  /**
   * Calculate win rate for a player
   */
  calculatePlayerWinRate(player: any): number {
    const totalGames = player.totalWins + player.totalLosses;
    return totalGames > 0 ? (player.totalWins / totalGames) * 100 : 0;
  }
}
