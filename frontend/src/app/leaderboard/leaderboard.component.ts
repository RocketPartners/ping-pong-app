import {AfterViewInit, Component, OnDestroy, OnInit, QueryList, ViewChildren, ViewEncapsulation} from '@angular/core';
import {Router} from '@angular/router';
import {MatTableDataSource} from '@angular/material/table';
import {MatTabChangeEvent} from '@angular/material/tabs';
import {MatSort} from '@angular/material/sort';
import {MatPaginator} from '@angular/material/paginator';
import {MatSnackBar} from '@angular/material/snack-bar';
import {Subject} from 'rxjs';
import {takeUntil} from 'rxjs/operators';

// Services
import {PlayerService} from '../_services/player.service';

// Models
import {GameType, Player, PlayerRanking} from '../_models/models';
// Assuming PlayerStyleObject is the actual type for items in player.playerStyles array
import {PLAYER_STYLE_COLORS, PLAYER_STYLE_NAMES, PlayerStyle, StyleRating} from '../_models/player-style';
import {PLAYER_STYLE_ICONS, getStyleIcon} from '../_models/player-style-icons';

@Component({
  selector: 'app-leaderboard',
  templateUrl: './leaderboard.component.html',
  styleUrls: ['./leaderboard.component.scss'],
  encapsulation: ViewEncapsulation.None,
  standalone: false
})
export class LeaderboardComponent implements OnInit, AfterViewInit, OnDestroy {
  // Table columns to display
  displayedColumns: string[] = ['position', 'player', 'rating', 'winRate', 'wins', 'losses', 'styles'];

  // Data sources for each game type
  dataSources: Record<string, MatTableDataSource<PlayerRanking>> = {
    [GameType.SINGLES_RANKED]: new MatTableDataSource<PlayerRanking>(),
    [GameType.DOUBLES_RANKED]: new MatTableDataSource<PlayerRanking>(),
    [GameType.SINGLES_NORMAL]: new MatTableDataSource<PlayerRanking>(),
    [GameType.DOUBLES_NORMAL]: new MatTableDataSource<PlayerRanking>()
  };

  // Current active tab identifier (using GameType enum values)
  activeTab: GameType = GameType.SINGLES_RANKED;
  // Map GameType enum keys to their index for easy lookup
  gameTypeIndices: { [key in GameType]: number } = {
    [GameType.SINGLES_RANKED]: 0,
    [GameType.DOUBLES_RANKED]: 1,
    [GameType.SINGLES_NORMAL]: 2,
    [GameType.DOUBLES_NORMAL]: 3
  };
  gameTypeKeys = Object.keys(this.dataSources) as GameType[]; // Order matters here

  // Loading state
  loading = true;

  // For style display - expose to template
  playerStyleColors = PLAYER_STYLE_COLORS;
  playerStyleNames = PLAYER_STYLE_NAMES;
  playerStyleIcons = PLAYER_STYLE_ICONS;
  PlayerStyleEnum = PlayerStyle; // Use different name to avoid conflict

  // Search filter
  searchFilter = '';

  // Store our players
  allPlayers: Player[] = [];
  // Use ViewChildren for Sort and Paginator as they are inside ng-template with multiple instances
  @ViewChildren(MatSort) sortList!: QueryList<MatSort>;
  @ViewChildren(MatPaginator) paginatorList!: QueryList<MatPaginator>;
  // Subject to manage subscription cleanup
  private destroy$ = new Subject<void>();

  constructor(
    private playerService: PlayerService,
    private router: Router,
    private snackBar: MatSnackBar // Inject MatSnackBar
  ) {
  }

  ngOnInit(): void {
    this.loadPlayers();
  }

  ngAfterViewInit(): void {
    // Connect sort and paginator for the initially active tab after view is initialized
    // Use setTimeout to ensure components are ready, especially within tabs
    setTimeout(() => this.connectDataSourceAccessories(), 50);
  }

  ngOnDestroy(): void {
    // Clean up subscriptions
    this.destroy$.next();
    this.destroy$.complete();
  }

  // Tab change handler
  onTabChange(event: MatTabChangeEvent): void {
    this.activeTab = this.gameTypeKeys[event.index];

    // Re-connect sort and paginator for the newly selected tab
    // Use setTimeout to allow tab content to render
    setTimeout(() => this.connectDataSourceAccessories(), 50);

    // Also re-apply any existing search filter to the new data source
    if (this.searchFilter) {
      this.getCurrentDataSource().filter = this.searchFilter;
    }
  }

  // Connect MatSort and MatPaginator to the current active DataSource
  connectDataSourceAccessories(): void {
    const currentDataSource = this.getCurrentDataSource();
    const activeIndex = this.gameTypeIndices[this.activeTab];

    const currentSort = this.sortList?.toArray()[activeIndex];
    const currentPaginator = this.paginatorList?.toArray()[activeIndex];

    if (currentDataSource && currentSort) {
      currentDataSource.sort = currentSort;
    }
    if (currentDataSource && currentPaginator) {
      currentDataSource.paginator = currentPaginator;
    }
  }

  // Get the DataSource for the currently active tab
  getCurrentDataSource(): MatTableDataSource<PlayerRanking> {
    return this.dataSources[this.activeTab];
  }

  // Load all players
  loadPlayers(): void {
    this.loading = true;
    this.playerService.getPlayers().pipe(
      takeUntil(this.destroy$) // Auto-unsubscribe when component destroyed
    ).subscribe({
      next: (players) => {
        // Loaded players data
        this.allPlayers = players || [];
        this.processPlayerData();
        this.loading = false;
        // Re-connect accessories after data is loaded and processed
        setTimeout(() => this.connectDataSourceAccessories(), 50);
      },
      error: (error) => {
        console.error('Error loading players:', error);
        this.loading = false;
        // Show user-friendly error message
        this.snackBar.open('Failed to load player data. Please try refreshing.', 'Close', {
          duration: 5000,
          panelClass: ['error-snackbar'] // Optional: for custom styling
        });
        // Clear existing data if load fails
        this.gameTypeKeys.forEach(gameType => {
          this.dataSources[gameType].data = [];
        });
      }
    });
  }

  // Process player data into rankings
  processPlayerData(): void {
    console.log(`Processing ${this.allPlayers.length} players`);

    this.gameTypeKeys.forEach(gameType => {
      const rankings = this.generatePlayerRankings(this.allPlayers, gameType);
      this.dataSources[gameType].data = rankings;
      // Set up filter predicate (moved setup to a separate method)
      this.dataSources[gameType].filterPredicate = this.createFilterPredicate();
      // Added players to datasource
    });

    // Apply any existing search filter initially
    if (this.searchFilter) {
      this.applyFilterInternal();
    }
  }

  // Generate rankings for a specific game type
  generatePlayerRankings(players: Player[], gameType: GameType): PlayerRanking[] {
    const validRankings: PlayerRanking[] = [];

    players.forEach(player => {
      const stats = this.getPlayerStatsForGameType(player, gameType);
      const totalGames = stats.wins + stats.losses;

      // Skip players with no games in this category
      if (totalGames === 0) {
        return;
      }

      validRankings.push({
        position: 0, // Will be assigned after sorting
        player: player,
        rating: stats.rating,
        winRate: stats.winRate,
        wins: stats.wins,
        losses: stats.losses,
        // Ensure these arrays exist, default to empty array if null/undefined
        styleRatings: player.styleRatings || [],
      });
    });

    // Sort by rating and assign positions
    validRankings.sort((a, b) => b.rating - a.rating);

    validRankings.forEach((ranking, index) => {
      ranking.position = index + 1;
    });

    return validRankings;
  }

  // Get player stats for a specific game type
  getPlayerStatsForGameType(player: Player, gameType: GameType): {
    rating: number,
    winRate: number,
    wins: number,
    losses: number
  } {
    let rating = 0, wins = 0, losses = 0, winRate = 0;

    // Use GameType enum for keys
    switch (gameType) {
      case GameType.SINGLES_RANKED:
        rating = player.singlesRankedRating ?? 0; // Use nullish coalescing
        wins = player.singlesRankedWins ?? 0;
        losses = player.singlesRankedLoses ?? 0; // Typo? Should likely be losses
        winRate = player.singlesRankedWinRate ?? 0;
        break;
      case GameType.DOUBLES_RANKED:
        rating = player.doublesRankedRating ?? 0;
        wins = player.doublesRankedWins ?? 0;
        losses = player.doublesRankedLoses ?? 0; // Typo?
        winRate = player.doublesRankedWinRate ?? 0;
        break;
      case GameType.SINGLES_NORMAL:
        rating = player.singlesNormalRating ?? 0;
        wins = player.singlesNormalWins ?? 0;
        losses = player.singlesNormalLoses ?? 0; // Typo?
        winRate = player.singlesNormalWinRate ?? 0;
        break;
      case GameType.DOUBLES_NORMAL:
        rating = player.doublesNormalRating ?? 0;
        wins = player.doublesNormalWins ?? 0;
        losses = player.doublesNormalLoses ?? 0; // Typo?
        winRate = player.doublesNormalWinRate ?? 0;
        break;
    }

    // Calculate win rate if not provided and games were played
    if (winRate === 0 && (wins + losses > 0)) {
      winRate = Math.round((wins / (wins + losses)) * 100);
    }

    return {rating, winRate, wins, losses};
  }

  // Creates the filter predicate function used by MatTableDataSource
  createFilterPredicate(): (data: PlayerRanking, filter: string) => boolean {
    return (data: PlayerRanking, filter: string): boolean => {
      const searchTerm = filter; // Already lowercase and trimmed in applyFilter

      if (!searchTerm) {
        return true;
      }

      const player = data.player;
      // Check against relevant player fields
      return (
        player.username?.toLowerCase().includes(searchTerm) ||
        player.firstName?.toLowerCase().includes(searchTerm) ||
        player.lastName?.toLowerCase().includes(searchTerm) ||
        `${player.firstName?.toLowerCase() ?? ''} ${player.lastName?.toLowerCase() ?? ''}`.includes(searchTerm)
      );
    };
  }

  // Apply filter when search input changes
  applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.searchFilter = filterValue.trim().toLowerCase();
    this.applyFilterInternal();
  }

  // Internal method to apply filter logic to the current data source
  applyFilterInternal(): void {
    const currentDataSource = this.getCurrentDataSource();
    currentDataSource.filter = this.searchFilter;

    // Go to the first page when filtering
    if (currentDataSource.paginator) {
      currentDataSource.paginator.firstPage();
    }
  }


  // Clear search filter
  clearFilter(): void {
    this.searchFilter = '';
    // Clear the input field visually if needed (requires ViewChild on input)
    // this.searchInput.nativeElement.value = '';
    this.applyFilterInternal(); // Re-apply empty filter
  }

  navigateToPlayerStats(player: Player): void {
    if (player && player.username) {
      this.router.navigate(['/statistics', player.username], {
        queryParams: {from: 'leaderboard'},
        state: {navigatedFrom: 'leaderboard'}
      });
    }
  }

// Get top player styles (for display) - Use StyleRating interface
  getTopStyles(playerStyles: StyleRating[] | undefined, limit: number = 3): StyleRating[] {
    if (!playerStyles || playerStyles.length === 0) {
      return [];
    }
    // Sort based on the 'rating' property of StyleRating
    return [...playerStyles]
      .sort((a, b) => (b.rating ?? 0) - (a.rating ?? 0))
      .slice(0, limit);
  }

  // Helper methods for accessing style properties safely
  // Assuming 'styleType' property holds a value from the PlayerStyle enum
  getStyleColor(styleType: PlayerStyle | string | undefined): string {
    if (typeof styleType === 'string' && Object.values(PlayerStyle).includes(styleType as PlayerStyle)) {
      return this.playerStyleColors[styleType as PlayerStyle] ?? '#999999';
    }
    return '#999999'; // Default gray color
  }

  getStyleName(styleType: PlayerStyle | string | undefined): string {
    if (typeof styleType === 'string' && Object.values(PlayerStyle).includes(styleType as PlayerStyle)) {
      return this.playerStyleNames[styleType as PlayerStyle] ?? 'Unknown';
    }
    return 'Unknown'; // Default name
  }

  getStyleFirstLetter(styleType: PlayerStyle | string | undefined): string {
    return this.getStyleName(styleType)?.charAt(0) ?? '?';
  }
  
  getStyleIcon(styleType: PlayerStyle | string | undefined): string {
    if (typeof styleType === 'string' && Object.values(PlayerStyle).includes(styleType as PlayerStyle)) {
      return this.playerStyleIcons[styleType as PlayerStyle] ?? 'sports_tennis';
    }
    return 'sports_tennis'; // Default icon
  }

  // Get medal class for top positions
  getMedalClass(position: number): string {
    if (position === 1) return 'gold-medal';
    if (position === 2) return 'silver-medal';
    if (position === 3) return 'bronze-medal';
    return '';
  }

  // For ngFor trackBy - using player username or ID if available and unique is often better
  trackByPlayerId(index: number, item: PlayerRanking): string | number {
    return item.player?.id || item.player?.username || index; // Use a unique player identifier if possible
  }

  // Refresh the leaderboard
  refresh(): void {
    // Refreshing leaderboard data
    // Clear filter before refresh maybe? Optional.
    // this.clearFilter();
    this.loadPlayers();
  }
}