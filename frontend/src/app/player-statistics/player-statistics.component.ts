import {AfterViewInit, ChangeDetectorRef, Component, DestroyRef, inject, OnInit, ViewChild} from '@angular/core';
import {FormControl} from '@angular/forms';
import {ActivatedRoute, ParamMap, Router} from '@angular/router';
import {BaseChartDirective} from 'ng2-charts';
import {ChartConfiguration, ChartData, ChartOptions, ChartType} from 'chart.js';
import {forkJoin, Observable, of} from 'rxjs';
import {catchError, finalize, map, startWith} from 'rxjs/operators';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';

// Services
import {AccountService} from '../_services/account.service';
import {PlayerService} from '../_services/player.service';
import {GameService} from '../_services/game.service';
import {AchievementService} from '../_services/achievement.service';
import {EloHistoryService} from '../_services/elo-history.service';

// Models and utilities
import {AuthenticatedPlayer, EloChartData, Game, Player, RankHistoryData} from '../_models/models';
import {AverageStyleRating, PlayerStyle, StyleRating, TopStyleRating} from '../_models/player-style';
import {AchievementDTO} from '../_models/achievement';
import {PlayerUtilities} from '../player/player-utilities';
import {getStyleDescription, getStyleRecommendation} from "../player/player-constants";
import {PLAYER_STYLE_ICONS, getStyleIcon} from '../_models/player-style-icons';
import {StyleRatingService} from "../_services/style-rating.service";

// Type definitions
type GameType = 'singlesRanked' | 'doublesRanked' | 'singlesNormal' | 'doublesNormal';

interface GameTypeData {
  name: string;
  wins: number;
  losses: number;
  total: number;
  percentage: number;
  winRate: number;
  gameType: GameType; // Type is correct here
}

interface StyleRadarData {
  name: string;
  value: number;
  styleType: PlayerStyle;
}

// Type for forkJoin results - Added explicit types
interface PlayerDataObservables {
  player: Observable<Player | null>;
  games: Observable<Game[]>; // Ensure catchError returns Game[]
  achievements: Observable<AchievementDTO[]>; // Ensure catchError returns AchievementDTO[]
  eloResults: Observable<Array<{ gameType: GameType; data: EloChartData[] }>>;
  rankResults: Observable<Array<{ gameType: GameType; data: RankHistoryData[] }>>;
}

const RADAR_STYLE_ORDER: PlayerStyle[] = [
  PlayerStyle.POWER, PlayerStyle.SPIN, PlayerStyle.CREATIVE, PlayerStyle.AGGRESSIVE,
  PlayerStyle.RESILIENT, PlayerStyle.TACTICIAN, PlayerStyle.RALLY_KING, PlayerStyle.ACE_MASTER,
  PlayerStyle.AURA, PlayerStyle.SPORTSMANSHIP
];

@Component({
  selector: 'app-player-statistics',
  templateUrl: './player-statistics.component.html',
  styleUrls: ['./player-statistics.component.scss'],
  standalone: false
})
export class PlayerStatisticsComponent implements OnInit, AfterViewInit {
  // --- Component State ---
  public loading = true;
  public error = '';
  public isChartLoading = false;
  public isRankChartLoading = false;
  public showAllGames = false;
  public showAllStyles = false;
  public isLoadingStyleComparisons = false;
  // --- Player Data ---
  public authenticatedPlayer: AuthenticatedPlayer | null = null;
  public player: Player | undefined;
  public usernameFromRoute: string | null = null;
  public recentGames: Game[] | null = [];
  public playerAchievements: AchievementDTO[] = [];
  public averagePlayerStyleRatings: AverageStyleRating[] = [];
  public topPlayerStyleRatings: TopStyleRating[] = [];
  // --- Search Functionality ---
  public searchControl = new FormControl('');
  public playerUsernames: string[] = [];
  public filteredUsernames$: Observable<string[]>;
  // --- Calculated Stats & Visualizations ---
  public overallWinRate = 0;
  public gameTypeData: GameTypeData[] = [];
  public styleRadarData: StyleRadarData[] = [];
  // --- Chart Data Storage ---
  public eloChartData: Record<GameType, EloChartData[]> = this.initializeEmptyChartData<EloChartData>();
  public rankHistoryData: Record<GameType, RankHistoryData[]> = this.initializeEmptyChartData<RankHistoryData>();
  // --- Chart Filters & Time Ranges ---
  public currentEloGameTypeFilter: GameType = 'singlesRanked';
  public currentRankGameTypeFilter: GameType = 'singlesRanked';
  public currentEloTimeRange = 30;
  public currentRankTimeRange = 30;
  public readonly timeRangeOptions = [7, 30, 90, 180, 365];
  // --- Chart Configurations ---
  public readonly chartColors: Record<GameType, string> = {
    singlesRanked: '#1976d2',
    doublesRanked: '#9c27b0',
    singlesNormal: '#4caf50',
    doublesNormal: '#ff9800',
  };
  public readonly eloChartOptions: ChartOptions = {
    responsive: true, maintainAspectRatio: false,
    plugins: {
      legend: {display: false},
      tooltip: {
        mode: 'index', intersect: false,
        callbacks: {
          title: (tooltipItems) => tooltipItems[0] ? new Date(this.getEloDataForCurrentFilter()[tooltipItems[0].dataIndex]?.timestamp).toLocaleDateString() : '',
          label: (context) => {
            const dataPoint = this.getEloDataForCurrentFilter()[context.dataIndex];
            if (!dataPoint) return '';
            const label = this.getGameTypeName(this.currentEloGameTypeFilter);
            const value = context.parsed.y;
            const change = dataPoint.eloChange ?? 0;
            return `${label}: ${value} (${change >= 0 ? '+' : ''}${change})`;
          }
        }
      }
    },
    scales: {
      x: {
        title: {display: true, text: 'Date'}, 
        grid: {display: false}
      },
      y: {
        title: {display: true, text: 'Elo Rating'}, 
        beginAtZero: false, 
        grace: '5%'
      }
    },
    elements: {line: {tension: 0.2}, point: {radius: 3, hoverRadius: 5}},
    interaction: {mode: 'index', intersect: false},
  };
  public eloChartConfigData: ChartConfiguration['data'] = {datasets: [], labels: []};
  public readonly rankChartOptions: ChartOptions = {
    responsive: true, maintainAspectRatio: false,
    animation: {duration: 800, easing: 'easeOutQuart'},
    plugins: {
      legend: {display: false},
      tooltip: {
        mode: 'index',
        intersect: false,
        backgroundColor: 'rgba(0, 0, 0, 0.8)',
        titleColor: '#fff',
        bodyColor: '#fff',
        padding: 10,
        cornerRadius: 4,
        callbacks: {
          title: (tooltipItems) => tooltipItems[0] ? `Rank on ${tooltipItems[0].label}` : '',
          label: (context) => {
            const dataPoint = this.getFilteredRankDataForCurrentFilter()[context.dataIndex];
            if (!dataPoint) return '';
            const value = context.parsed.y;
            const totalPlayers = dataPoint.totalPlayers ?? 0;
            const percentile = dataPoint.percentile?.toFixed(1) ?? 0;
            return [`Rank: #${value} of ${totalPlayers} players`, `Top ${percentile}%`];
          }
        }
      }
    },
    scales: {
      x: {
        title: {
          display: true, 
          text: 'Date', 
          font: {size: 12, weight: 'bold'}, 
          padding: {top: 10}
        },
        ticks: {
          maxRotation: 45, 
          minRotation: 45, 
          font: {size: 11}
        },
        grid: {display: false}
      },
      y: {
        title: {
          display: true, 
          text: 'Rank Position', 
          font: {size: 12, weight: 'bold'}, 
          padding: {bottom: 10}
        },
        reverse: true,
        min: 1,
        grace: '10%',
        grid: {color: 'rgba(0, 0, 0, 0.05)'},
        ticks: {
          font: {size: 11}, 
          precision: 0, 
          callback: (value) => `#${value}`
        }
      }
    },
    elements: {line: {tension: 0.3, borderWidth: 3, fill: true}, point: {radius: 4, hoverRadius: 6, hitRadius: 8}},
    interaction: {mode: 'index', intersect: false},
  };
  public radarChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    animation: {
      duration: 1200,
      easing: 'easeOutQuart'
    },
    elements: {
      line: {
        tension: 0.2,
        borderWidth: 3
      },
      point: {
        radius: 4,
        hoverRadius: 6,
        hitRadius: 8
      }
    },
    scales: {
      r: {
        min: 0,
        max: 100,
        ticks: {
          stepSize: 20,
          display: true,
          backdropColor: 'rgba(255, 255, 255, 0.8)',
          backdropPadding: 2,
          color: '#616161',
          font: {
            size: 10,
            weight: 500
          }
        },
        pointLabels: {
          font: {
            size: 12,
            weight: 'bold',
            family: 'Roboto, sans-serif'
          },
          color: '#212121'
        },
        grid: {
          circular: true,
          color: 'rgba(0, 0, 0, 0.1)'
        },
        angleLines: {
          color: 'rgba(0, 0, 0, 0.1)'
        },
        beginAtZero: true
      }
    },
    plugins: {
      legend: {
        display: true,
        position: 'bottom',
        labels: {
          font: {
            size: 12,
            family: 'Roboto, sans-serif',
            weight: 500
          },
          padding: 15,
          usePointStyle: true
        }
      },
      tooltip: {
        backgroundColor: 'rgba(33, 33, 33, 0.9)',
        titleFont: {
          size: 13,
          weight: 'bold'
        },
        bodyFont: {
          size: 12
        },
        padding: 12,
        cornerRadius: 6,
        displayColors: true,
        callbacks: {
          label: (context) => {
            const label = context.dataset.label || '';
            const value = context.raw as number || 0;
            let skillLevel = 'Novice';

            if (value >= 90) skillLevel = 'Elite';
            else if (value >= 75) skillLevel = 'Expert';
            else if (value >= 60) skillLevel = 'Advanced';
            else if (value >= 40) skillLevel = 'Intermediate';

            // If this is the "Top Players" dataset, include the player name
            if (label === 'Top Players' && context.datasetIndex === 2) {
              const styleIndex = context.dataIndex;
              if (styleIndex >= 0 && styleIndex < RADAR_STYLE_ORDER.length) {
                const styleType = RADAR_STYLE_ORDER[styleIndex];
                // Find the top rating for this style
                const topRating = this.topPlayerStyleRatings.find(tr => tr.style === styleType);
                if (topRating && topRating.playerUsername) {
                  return `${label}: ${value} (${topRating.playerUsername}, ${skillLevel})`;
                }
              }
            }

            return `${label}: ${value} (${skillLevel})`;
          }
        }
      }
    }
  };
  public radarChartData: ChartData<'radar'> = {
    labels: [], // Will be populated with style names in order
    datasets: [
      {
        data: [], // Will be populated with ratings in order
        label: 'Skill Rating', // Used in tooltip
        fill: true,
        backgroundColor: 'rgba(25, 118, 210, 0.2)', // Primary color fill (e.g., blue)
        borderColor: 'rgb(25, 118, 210)',       // Primary color border
        pointBackgroundColor: 'rgb(25, 118, 210)', // Primary color points
        pointBorderColor: '#fff',
        pointHoverBackgroundColor: '#fff',
        pointHoverBorderColor: 'rgb(25, 118, 210)'
      },
      // Add another dataset here if you want to compare two players
    ]
  };
  public radarChartType: ChartType = 'radar';
  public rankChartConfigData: ChartConfiguration['data'] = {datasets: [], labels: []};
  public readonly playerUtils = PlayerUtilities;
  public navigatedFromLeaderboard = false;
  public backButtonText = 'Back to Your Profile';
  public backButtonIcon = 'arrow_back';

  protected readonly PlayerUtilities = PlayerUtilities;
  protected readonly getStyleRecommendation = getStyleRecommendation;
  protected readonly getStyleDescription = getStyleDescription;
  protected readonly getStyleIcon = getStyleIcon;
  protected readonly playerStyleIcons = PLAYER_STYLE_ICONS;
  // --- Utilities ---
  protected readonly Math = Math;
  // Dependency Injection & Lifecycle
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly accountService = inject(AccountService);
  private readonly playerService = inject(PlayerService);
  private readonly gameService = inject(GameService);
  private readonly achievementService = inject(AchievementService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly eloHistoryService = inject(EloHistoryService);
  private styleRatingService: StyleRatingService = inject(StyleRatingService)
  private readonly destroyRef = inject(DestroyRef);
  // Chart Elements
  @ViewChild('eloChartRef', {static: false}) private eloChartRef: BaseChartDirective | undefined;
  @ViewChild('rankChartRef', {static: false}) private rankChartRef: BaseChartDirective | undefined;
  @ViewChild('radarChartRef', {static: false}) private radarChartRef: BaseChartDirective | undefined;

  constructor() {
    this.authenticatedPlayer = this.accountService.playerValue;
    this.filteredUsernames$ = this.searchControl.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef),
      startWith(''),
      map(value => this._filterUsernames(value || ''))
    );
  }

  ngOnInit(): void {
    this.loadUsernames();

    // Check navigation source from query params first thing
    this.route.queryParams
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(params => {
        this.navigatedFromLeaderboard = params['from'] === 'leaderboard';
        this.updateBackButtonText();
      });

    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params: ParamMap) => {
        this.usernameFromRoute = params.get('username');
        this.resetComponentState();

        // Also check router state as a fallback
        if (!this.navigatedFromLeaderboard && window.history.state.navigatedFrom === 'leaderboard') {
          this.navigatedFromLeaderboard = true;
          this.updateBackButtonText();
        }

        if (this.usernameFromRoute) {
          this.loadSpecificPlayer(this.usernameFromRoute);
        } else if (this.authenticatedPlayer?.player) {
          this.player = this.authenticatedPlayer.player;
          this.loadPlayerData();
        } else {
          this.handleLoadingError('No player information available. Please log in or specify a player.');
        }
      });
      
    // Listen for theme changes to update chart colors
    const observer = new MutationObserver(() => {
      setTimeout(() => {
        this.updateEloAndRankCharts();
      }, 100);
    });
    
    // Watch for class changes on body element to detect theme changes
    observer.observe(document.body, { 
      attributes: true, 
      attributeFilter: ['class'] 
    });
  }


  ngAfterViewInit(): void {
    setTimeout(() => {
      this.updateEloChart();
      this.updateRankChart();
    }, 150);
  }

  // --- Data Loading ---

  public loadRankDataWithDateRange(days: number): void {
    if (!this.player?.playerId) return;
    this.currentRankTimeRange = days;
    const {startDateStr, endDateStr} = this.getDateRange(days);
    this.isRankChartLoading = true;
    const playerId = this.player.playerId;
    const gameTypes: GameType[] = ['singlesRanked', 'doublesRanked', 'singlesNormal', 'doublesNormal'];

    const requests = gameTypes.map(gt =>
      this.eloHistoryService.getRankHistory(playerId, this.eloHistoryService.convertToBackendGameType(gt), startDateStr, endDateStr)
        .pipe(
          map(data => ({gameType: gt, data: data || []})),
          catchError(() => {
            console.error(`Error loading ${gt} Rank data for range`);
            return of({gameType: gt, data: this.rankHistoryData[gt] || []});
          })
        )
    );

    forkJoin(requests).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.isRankChartLoading = false)
    ).subscribe({
      next: (results: Array<{ gameType: GameType, data: RankHistoryData[] }>) => {
        results.forEach(res => {
          this.rankHistoryData[res.gameType] = res.data;
        });
        this.updateRankChart();
      },
      error: (err) => {
        console.error('Failed to load Rank data with date range', err);
      }
    });
  }

  public searchPlayer(): void {
    const username = this.searchControl.value?.trim();
    if (username) {
      this.router.navigate(['/statistics', username]);
    }
  }

  public goBack(): void {
    if (this.navigatedFromLeaderboard) {
      this.router.navigate(['/leaderboard']);
    } else {
      this.router.navigate(['/statistics']);
    }
  }

  public changeEloTimeRange(days: number): void {
    this.loadEloDataWithDateRange(days);
  }

  public changeRankTimeRange(days: number): void {
    this.loadRankDataWithDateRange(days);
  }

  public changeEloGameTypeFilter(gameType: GameType): void {
    this.currentEloGameTypeFilter = gameType;
    this.updateEloChart();
  }

  // --- Data Processing & Calculations ---

  public changeRankGameTypeFilter(gameType: GameType): void {
    this.currentRankGameTypeFilter = gameType;
    this.updateRankChart();
  }

  public toggleShowAllGames(): void {
    this.showAllGames = !this.showAllGames;
  }

  public toggleShowAllStyles(): void {
    this.showAllStyles = !this.showAllStyles;
  }

  public isViewingOtherPlayer(): boolean {
    return !!this.player && !!this.authenticatedPlayer?.player &&
      this.player.playerId !== this.authenticatedPlayer.player.playerId;
  }

  // --- Chart Update Methods ---

  public getGameResult(game: Game): 'Win' | 'Loss' | 'Unknown' {
    if (!this.player) return 'Unknown';
    return this.getPlayerGameResult(game, this.player.playerId);
  }

  public isWin(game: Game): boolean {
    return this.getGameResult(game) === 'Win';
  }

  public getCurrentEloRating(gameType: GameType): string {
    const data = this.eloChartData[gameType];
    return data?.length ? [...data].sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())[0].eloRating.toString() : '-';
  }

  // --- Event Handlers & UI Logic ---

  public getMaxEloRating(gameType: GameType): string {
    const data = this.eloChartData[gameType];
    return data?.length ? Math.max(...data.map(d => d.eloRating)).toString() : '-';
  }

  public getCurrentRank(gameType: GameType): string {
    const data = this.rankHistoryData[gameType];
    return data?.length ? [...data].sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())[0].rankPosition.toString() : '-';
  }

  public getBestRank(gameType: GameType): string {
    const data = this.rankHistoryData[gameType];
    return data?.length ? Math.min(...data.map(d => d.rankPosition)).toString() : '-';
  }

  public getCurrentPercentile(gameType: GameType): string {
    const data = this.rankHistoryData[gameType];
    if (!data?.length) return '-';
    const latest = [...data].sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())[0];
    return latest.percentile?.toFixed(1) ?? '-';
  }

  public getTotalPlayersCount(gameType: GameType): string {
    const data = this.rankHistoryData[gameType];
    if (!data?.length) return '-';
    const latest = [...data].sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime())[0];
    return latest.totalPlayers?.toString() ?? '-';
  }

  public getEloTrend(gameType: GameType): 'up' | 'down' | 'stable' {
    const data = this.eloChartData[gameType];
    if (!data || data.length < 2) return 'stable';
    const sorted = [...data].sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
    const recentData = sorted.slice(0, Math.min(5, sorted.length));
    if (recentData.length < 2) return 'stable'; // Need at least 2 points for trend
    const startElo = recentData[recentData.length - 1].eloRating;
    const endElo = recentData[0].eloRating;
    const change = endElo - startElo;
    if (change > 10) return 'up';
    if (change < -10) return 'down';
    return 'stable';
  }

  public getRankChangeOverPeriod(gameType: GameType): number {
    const data = this.getFilteredRankDataForCurrentFilter(gameType);
    if (!data || data.length < 2) return 0;
    const sorted = [...data].sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime());
    const startRank = sorted[0].rankPosition;
    const endRank = sorted[sorted.length - 1].rankPosition;
    return endRank - startRank;
  }

  public getRadarPolygonPoints(radarData: StyleRadarData[] | undefined): string {
    if (!radarData || radarData.length === 0) {
      return 'polygon(50% 50%)';
    }
    const points: string[] = [];
    const totalPoints = radarData.length;
    const radius = 45;
    const centerX = 50;
    const centerY = 50;
    radarData.forEach((style, index) => {
      const valuePercentage = (style.value ?? 0) / 100;
      const angle = (Math.PI * 2 * index) / totalPoints - Math.PI / 2;
      const x = centerX + radius * valuePercentage * Math.cos(angle);
      const y = centerY + radius * valuePercentage * Math.sin(angle);
      points.push(`${x.toFixed(2)}% ${y.toFixed(2)}%`);
    });
    return `polygon(${points.join(', ')})`;
  }

  public calculateWinRate(wins: number | undefined, losses: number | undefined): number {
    const w = wins ?? 0;
    const l = losses ?? 0;
    const total = w + l;
    return total > 0 ? Math.round((w / total) * 100) : 0;
  }

  public calculatePercentage(value: number | undefined, total: number | undefined): number {
    const v = value ?? 0;
    const t = total ?? 0;
    return t > 0 ? Math.round((v / t) * 100) : 0;
  }

// Re-add Donut chart helpers from original code if they were removed
  public getDonutSegment(percentage: number): string {
    const circumference = 314; // 2 * PI * 50
    return `${(percentage / 100) * circumference} ${circumference - ((percentage / 100) * circumference)}`;
  }

  public getDonutOffset(index: number): number {
    let offset = 0;
    // Calculate cumulative percentage for all previous segments
    for (let i = 0; i < index; i++) {
      // Ensure gameTypeData exists and has elements before accessing
      if (this.gameTypeData && this.gameTypeData[i]) {
        offset += (this.gameTypeData[i].percentage / 100) * 314; // 2 * PI * 50
      }
    }
    // SVG stroke-dashoffset needs to be adjusted for correct starting position
    // -78.5 positions the first segment at the top (12 o'clock position)
    return -78.5 - offset; // Negative offset makes segments appear clockwise
  }

// Re-add Style helpers from original code if they were removed
  public getSortedStyleRatings(): StyleRating[] {
    if (!this.player?.styleRatings) return [];
    // Sort descending by rating
    return [...this.player.styleRatings].sort((a, b) => b.rating - a.rating);
  }

  public getTopPlayerStyles(): StyleRating[] {
    return PlayerUtilities.getTopStyles(4, this.player);
  }

// Add getStyleName back (or ensure it's public static on PlayerUtilities and keep playerUtils.getStyleName)
  public getStyleName(styleType: PlayerStyle): string {
    // Assumes PLAYER_STYLE_NAMES is accessible, e.g., via playerUtils
    return this.playerUtils.PLAYER_STYLE_NAMES[styleType] || 'Unknown';
  }

  private updateBackButtonText(): void {
    if (this.navigatedFromLeaderboard) {
      this.backButtonText = 'Back to Leaderboard';
      this.backButtonIcon = 'leaderboard';
    } else {
      this.backButtonText = 'Back to Your Profile';
      this.backButtonIcon = 'arrow_back';
    }
  }

  private loadSpecificPlayer(username: string): void {
    this.loading = true;
    this.playerService.getPlayerByUsername(username)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        catchError(err => {
          console.error('Error loading player:', err);
          this.handleLoadingError(`Failed to load player: "${username}".`);
          return of(null);
        }),
        finalize(() => this.loading = false)
      )
      .subscribe(player => {
        if (player) {
          this.player = player;
          this.loadPlayerData();
        } else if (!this.error) {
          this.handleLoadingError(`Player "${username}" not found.`);
        }
      });
  }

  private loadPlayerData(): void {
    if (!this.player?.playerId) {
      this.handleLoadingError('Player ID is missing, cannot load data.');
      return;
    }

    this.loading = true;
    this.isChartLoading = true;
    this.isRankChartLoading = true;

    const playerId = this.player.playerId;
    const gameTypes: GameType[] = ['singlesRanked', 'doublesRanked', 'singlesNormal', 'doublesNormal'];

    // Define observables directly within the object passed to forkJoin
    const observables = {
      player: this.playerService.getPlayerByUsername(this.player.username).pipe(catchError(() => of(this.player))), // Fallback to existing
      games: this.gameService.getPlayerGameHistory(playerId).pipe(catchError(() => of([] as Game[]))), // Ensure array on error
      achievements: this.achievementService.getPlayerAchievements(playerId).pipe(catchError(() => of([] as AchievementDTO[]))), // Ensure array on error
      eloResults: forkJoin(gameTypes.map(gt =>
        this.eloHistoryService.getEloChartData(playerId, this.eloHistoryService.convertToBackendGameType(gt))
          .pipe(
            map(data => ({gameType: gt, data: data || []})),
            catchError(() => of({gameType: gt, data: [] as EloChartData[]})) // Ensure typed array
          )
      )),
      rankResults: forkJoin(gameTypes.map(gt =>
        this.eloHistoryService.getRankHistory(playerId, this.eloHistoryService.convertToBackendGameType(gt))
          .pipe(
            map(data => ({gameType: gt, data: data || []})),
            catchError(() => of({gameType: gt, data: [] as RankHistoryData[]})) // Ensure typed array
          )
      ))
    };


    // Pass the object literal directly to forkJoin
    forkJoin(observables).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => {
        this.loading = false;
        this.isChartLoading = false;
        this.isRankChartLoading = false;
      })
    ).subscribe({
      // 'result' type should now be correctly inferred by TypeScript based on the 'observables' object structure
      next: (result) => {
        if (!result.player) {
          this.handleLoadingError('Failed to load core player data.');
          return;
        }
        this.player = result.player;
        this.recentGames = result.games; // Type is Game[]
        this.playerAchievements = result.achievements; // Type is AchievementDTO[]

        const initialEloData = this.initializeEmptyChartData<EloChartData>();
        // Add explicit types for acc/cur in reduce for clarity, although inference should work now
        this.eloChartData = result.eloResults.reduce(
          (acc: Record<GameType, EloChartData[]>, cur: { gameType: GameType; data: EloChartData[] }) => {
            acc[cur.gameType] = cur.data;
            return acc;
          }, initialEloData);

        const initialRankData = this.initializeEmptyChartData<RankHistoryData>();
        // Add explicit types for acc/cur in reduce for clarity
        this.rankHistoryData = result.rankResults.reduce(
          (acc: Record<GameType, RankHistoryData[]>, cur: { gameType: GameType; data: RankHistoryData[] }) => {
            acc[cur.gameType] = cur.data;
            return acc;
          }, initialRankData);

        this.processLoadedData();
      },
      error: (err) => {
        console.error('Error loading combined player data:', err);
        this.handleLoadingError('Failed to load some player statistics. Please try again.');
        // Still attempt to process data in case some parts succeeded before error
        this.processLoadedData();
      }
    });
  }

  private loadUsernames(): void {
    this.playerService.getPlayerUsernames()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        map(data => data || []),
        catchError(() => {
          console.error('Error loading usernames for search');
          return of([]);
        })
      )
      .subscribe(usernames => {
        this.playerUsernames = usernames;
        this.searchControl.updateValueAndValidity({emitEvent: true});
      });
  }

  private loadEloDataWithDateRange(days: number): void {
    if (!this.player?.playerId) return;
    this.currentEloTimeRange = days;
    const {startDateStr, endDateStr} = this.getDateRange(days);
    this.isChartLoading = true;
    const playerId = this.player.playerId;
    const gameTypes: GameType[] = ['singlesRanked', 'doublesRanked', 'singlesNormal', 'doublesNormal'];

    const requests = gameTypes.map(gt =>
      this.eloHistoryService.getEloChartData(playerId, this.eloHistoryService.convertToBackendGameType(gt), startDateStr, endDateStr)
        .pipe(
          map(data => ({gameType: gt, data: data || []})),
          catchError(() => {
            console.error(`Error loading ${gt} ELO data for range`);
            return of({gameType: gt, data: this.eloChartData[gt] || []});
          })
        )
    );

    forkJoin(requests).pipe(
      takeUntilDestroyed(this.destroyRef),
      finalize(() => this.isChartLoading = false)
    ).subscribe({
      next: (results: Array<{ gameType: GameType, data: EloChartData[] }>) => {
        results.forEach(res => {
          this.eloChartData[res.gameType] = res.data;
        });
        this.updateEloChart();
      },
      error: (err) => {
        console.error('Failed to load ELO data with date range', err);
      }
    });
  }

  private processLoadedData(): void {
    if (!this.player) return;
    this.calculateOverallStats();
    this.prepareGameTypeData();
    this.loadStyleRatingsComparisons();
    this.prepareRadarChartData();
    this.updateEloAndRankCharts();
  }

  // --- Private Helper Methods ---

  private calculateOverallStats(): void {
    if (!this.player) return;
    this.overallWinRate = this.playerUtils.getOverallWinRate(this.player);
  }

  private prepareGameTypeData(): void {
    if (!this.player) {
      this.gameTypeData = [];
      return;
    }
    const totalGames = this.playerUtils.getTotalMatches(this.player);
    const p = this.player; // Shorthand

    // FIX 4: Cast string literals to GameType
    this.gameTypeData = [
      {
        name: 'Singles Ranked',
        gameType: 'singlesRanked' as GameType,
        wins: p.singlesRankedWins,
        losses: p.singlesRankedLoses,
        total: (p.singlesRankedWins ?? 0) + (p.singlesRankedLoses ?? 0)
      },
      {
        name: 'Doubles Ranked',
        gameType: 'doublesRanked' as GameType,
        wins: p.doublesRankedWins,
        losses: p.doublesRankedLoses,
        total: (p.doublesRankedWins ?? 0) + (p.doublesRankedLoses ?? 0)
      },
      {
        name: 'Singles Normal',
        gameType: 'singlesNormal' as GameType,
        wins: p.singlesNormalWins,
        losses: p.singlesNormalLoses,
        total: (p.singlesNormalWins ?? 0) + (p.singlesNormalLoses ?? 0)
      },
      {
        name: 'Doubles Normal',
        gameType: 'doublesNormal' as GameType,
        wins: p.doublesNormalWins,
        losses: p.doublesNormalLoses,
        total: (p.doublesNormalWins ?? 0) + (p.doublesNormalLoses ?? 0)
      },
    ].map(data => ({
      ...data,
      percentage: this.calculatePercentage(data.total, totalGames),
      winRate: this.calculateWinRate(data.wins, data.losses)
    }));
  }

  private prepareRadarChartData(): void {
    const labels: string[] = [];
    const playerDataPoints: number[] = [];
    const averagePlayerPoints: number[] = [];
    const topPlayerPoints: number[] = [];

    const playerRatingsMap = new Map<PlayerStyle, number>();
    if (this.player?.styleRatings?.length) {
      this.player.styleRatings.forEach(style => {
        playerRatingsMap.set(style.styleType, style.rating);
      });
    }

    // Create maps for easier lookups
    const topRatingsMap = new Map<PlayerStyle, number>();
    this.topPlayerStyleRatings.forEach(tr => {
      topRatingsMap.set(tr.style, tr.rating);
    });

    // Iterate through the fixed order defined in RADAR_STYLE_ORDER
    for (const styleType of RADAR_STYLE_ORDER) {
      const rating = playerRatingsMap.get(styleType) ?? 0; // Default to 0 if missing
      const name = this.playerUtils.PLAYER_STYLE_NAMES[styleType] ?? 'Unknown Style';
      const avgRating = this.averagePlayerStyleRatings.find(ar => ar.style === styleType)?.averageRating ?? 50;
      const topRating = topRatingsMap.get(styleType) ?? 85; // Default to 85

      labels.push(name);
      playerDataPoints.push(rating);
      averagePlayerPoints.push(avgRating);
      topPlayerPoints.push(topRating);
    }

    // Update the ChartData object
    this.radarChartData = {
      labels: labels,
      datasets: [
        {
          data: playerDataPoints,
          label: 'Your Skills',
          backgroundColor: 'rgba(25, 118, 210, 0.3)',
          borderColor: 'rgb(25, 118, 210)',
          pointBackgroundColor: 'rgb(25, 118, 210)',
          pointBorderColor: '#fff',
          pointHoverBackgroundColor: '#fff',
          pointHoverBorderColor: 'rgb(25, 118, 210)',
          fill: true
        },
        {
          data: averagePlayerPoints,
          label: 'Average Player',
          backgroundColor: 'rgba(76, 175, 80, 0.15)',
          borderColor: 'rgb(76, 175, 80)',
          borderWidth: 1,
          borderDash: [5, 5],
          pointBackgroundColor: 'rgb(76, 175, 80)',
          pointBorderColor: '#fff',
          pointRadius: 3,
          pointHoverRadius: 5,
          fill: true
        },
        {
          data: topPlayerPoints,
          label: 'Top Players',
          backgroundColor: 'rgba(156, 39, 176, 0.15)',
          borderColor: 'rgb(156, 39, 176)',
          borderWidth: 1,
          borderDash: [3, 3],
          pointBackgroundColor: 'rgb(156, 39, 176)',
          pointBorderColor: '#fff',
          pointRadius: 3,
          pointHoverRadius: 5,
          fill: true
        }
      ]
    };
  }

  private updateEloAndRankCharts(): void {
    this.updateEloChart();
    this.updateRankChart();
    this.cdr.detectChanges();
  }

  private updateEloChart(): void {
    const currentData = this.getEloDataForCurrentFilter();
    if (!currentData?.length) {
      this.eloChartConfigData = {labels: [], datasets: []};
    } else {
      const sortedData = [...currentData].sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime());
      const labels = sortedData.map(item => this.formatDateForChart(new Date(item.timestamp), this.currentEloTimeRange));
      const eloValues = sortedData.map(item => item.eloRating);
      const color = this.chartColors[this.currentEloGameTypeFilter];
      
      // Check if dark mode is active
      const isDarkMode = document.body.classList.contains('dark-theme');
      
      // Update chart options for dark mode before updating data
      if (this.eloChartRef?.chart) {
        const chart = this.eloChartRef.chart;
        
        // Apply dark mode colors to chart axes and ticks
        if (isDarkMode && chart.options.scales) {
          // Safe access to scales - handle missing properties gracefully
          try {
            // Update x-axis ticks color
            if (chart.options.scales.x?.ticks) {
              chart.options.scales.x.ticks.color = 'rgba(255, 255, 255, 0.7)';
            }
            
            // Update y-axis ticks and grid colors
            if (chart.options.scales.y?.ticks) {
              chart.options.scales.y.ticks.color = 'rgba(255, 255, 255, 0.7)';
            }
            
            if (chart.options.scales.y?.grid) {
              chart.options.scales.y.grid.color = 'rgba(255, 255, 255, 0.1)';
            }
          } catch (error) {
            console.warn('Error updating chart colors:', error);
          }
        }
      }
      
      this.eloChartConfigData = {
        labels: labels,
        datasets: [{
          data: eloValues, label: this.getGameTypeName(this.currentEloGameTypeFilter) + ' Elo',
          backgroundColor: this.hexToRgba(color, 0.2), borderColor: color, pointBackgroundColor: color,
          pointBorderColor: '#fff', pointHoverBackgroundColor: '#fff', pointHoverBorderColor: color,
          fill: 'origin', tension: 0.2
        }]
      };
    }
  }

  private updateRankChart(): void {
    const filteredData = this.getFilteredRankDataForCurrentFilter();
    let maxRankAxisValue = 10; // Default max if no data

    if (filteredData?.length) {
      // Data is sorted ascending by date in getFilteredRankDataForCurrentFilter if time range applied,
      // or directly from rankHistoryData. Assume latest entry has representative totalPlayers.
      // Let's sort desc here to be sure we get the latest point easily.
      const sortedDesc = [...filteredData].sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
      const latestDataPoint = sortedDesc[0];

      // Use totalPlayers from the latest data point for the axis max
      if (latestDataPoint?.totalPlayers && latestDataPoint.totalPlayers > 0) {
        // Add a little buffer so the highest rank isn't exactly at the edge
        maxRankAxisValue = latestDataPoint.totalPlayers + Math.ceil(latestDataPoint.totalPlayers * 0.05); // e.g., 5% buffer, rounded up
        // Ensure max is at least a bit higher than min (1) if totalPlayers is very low
        if (maxRankAxisValue <= 1) maxRankAxisValue = 10;
      }

      const sortedData = filteredData.sort((a, b) => new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()); // Sort ascending for chart display
      const labels = sortedData.map(item => this.formatDateForChart(new Date(item.timestamp), this.currentRankTimeRange));
      const rankValues = sortedData.map(item => item.rankPosition);
      const color = this.chartColors[this.currentRankGameTypeFilter];

      this.rankChartConfigData = {
        labels: labels,
        datasets: [{
          data: rankValues, label: 'Rank Position',
          backgroundColor: this.hexToRgba(color, 0.2), borderColor: color, pointBackgroundColor: color,
          pointBorderColor: '#fff', pointHoverBackgroundColor: '#fff', pointHoverBorderColor: color,
          fill: 'origin', tension: 0.3
        }]
      };
    } else {
      // No data, clear the chart
      this.rankChartConfigData = {labels: [], datasets: []};
    }

    // --- Dynamic Axis Update ---
    // Access the chart instance directly to update options before redrawing
    const chartInstance = this.rankChartRef?.chart;
    if (chartInstance?.options?.scales?.y) {
      // Safely update the max value on the existing options object
      chartInstance.options.scales.y.max = maxRankAxisValue;
      // Ensure min is still 1 (it's set in the base options, but good to be sure)
      chartInstance.options.scales.y.min = 1;
      
      // Check if dark mode is active
      const isDarkMode = document.body.classList.contains('dark-theme');
      
      // Apply dark mode colors to chart axes and ticks
      if (isDarkMode && chartInstance.options.scales) {
        // Safe access to scales - handle missing properties gracefully
        try {
          // Update x-axis ticks color
          if (chartInstance.options.scales.x?.ticks) {
            chartInstance.options.scales.x.ticks.color = 'rgba(255, 255, 255, 0.7)';
          }
          
          // Update y-axis ticks and grid colors
          if (chartInstance.options.scales.y?.ticks) {
            chartInstance.options.scales.y.ticks.color = 'rgba(255, 255, 255, 0.7)';
          }
          
          if (chartInstance.options.scales.y?.grid) {
            chartInstance.options.scales.y.grid.color = 'rgba(255, 255, 255, 0.1)';
          }
        } catch (error) {
          console.warn('Error updating rank chart colors:', error);
        }
      }
    }

    // --- Trigger Chart Update ---
    // Update chart with change detection, 'none' prevents animation reset on filter change
    if (chartInstance) {
      chartInstance.update('none');
      this.cdr.detectChanges();
    }
  }

  // FIX 5: Restore original logic if not in PlayerUtilities
  private getPlayerGameResult(game: Game, playerId: string): 'Win' | 'Loss' | 'Unknown' {
    if (game.singlesGame) {
      if (game.challengerId === playerId) return game.challengerWin ? 'Win' : 'Loss';
      if (game.opponentId === playerId) return game.opponentWin ? 'Win' : 'Loss';
    } else { // Doubles game
      if (game.challengerTeam?.includes(playerId)) return game.challengerTeamWin ? 'Win' : 'Loss';
      if (game.opponentTeam?.includes(playerId)) return game.opponentTeamWin ? 'Win' : 'Loss';
    }
    return 'Unknown';
  }

  private _filterUsernames(value: string): string[] {
    const filterValue = value.toLowerCase();
    return this.playerUsernames.filter(username => username.toLowerCase().includes(filterValue));
  }

  private resetComponentState(): void {
    this.player = undefined;
    this.recentGames = [];
    this.playerAchievements = [];
    this.eloChartData = this.initializeEmptyChartData<EloChartData>();
    this.rankHistoryData = this.initializeEmptyChartData<RankHistoryData>();
    this.gameTypeData = [];
    this.styleRadarData = [];
    this.error = '';
    this.loading = true;
    this.isChartLoading = true;
    this.isRankChartLoading = true;
  }

  // Made generic for reuse
  private initializeEmptyChartData<T>(): Record<GameType, T[]> {
    return {
      singlesRanked: [], doublesRanked: [], singlesNormal: [], doublesNormal: []
    };
  }

  private handleLoadingError(message: string): void {
    this.error = message;
    this.loading = false;
    this.isChartLoading = false;
    this.isRankChartLoading = false;
    this.player = undefined;
    this.recentGames = [];
    this.playerAchievements = [];
    this.eloChartData = this.initializeEmptyChartData<EloChartData>();
    this.rankHistoryData = this.initializeEmptyChartData<RankHistoryData>();
    this.updateEloAndRankCharts();
  }

  private getDateRange(days: number): { startDateStr: string, endDateStr: string } {
    const endDate = new Date();
    const startDate = new Date();
    startDate.setDate(endDate.getDate() - days);
    const endDateStr = endDate.toISOString().split('T')[0];
    const startDateStr = startDate.toISOString().split('T')[0];
    return {startDateStr, endDateStr};
  }

  private getEloDataForCurrentFilter(): EloChartData[] {
    return this.eloChartData[this.currentEloGameTypeFilter] || [];
  }

  private getFilteredRankDataForCurrentFilter(gameType?: GameType): RankHistoryData[] {
    const type = gameType || this.currentRankGameTypeFilter;
    const data = this.rankHistoryData[type] || [];
    if (this.currentRankTimeRange > 0) {
      const cutoffDate = new Date();
      cutoffDate.setDate(cutoffDate.getDate() - this.currentRankTimeRange);
      return data.filter(item => new Date(item.timestamp) >= cutoffDate);
    }
    return data;
  }

  private getGameTypeName(gameType: GameType): string {
    const names: Record<GameType, string> = {
      singlesRanked: 'Singles Ranked', doublesRanked: 'Doubles Ranked',
      singlesNormal: 'Singles Normal', doublesNormal: 'Doubles Normal'
    };
    return names[gameType] ?? 'Unknown Game Type';
  }

  private formatDateForChart(date: Date, timeRangeDays: number): string {
    const options: Intl.DateTimeFormatOptions = {month: 'short', day: 'numeric'};
    if (timeRangeDays > 180) {
      options.year = 'numeric';
    }
    return date.toLocaleDateString('en-US', options);
  }

  private hexToRgba(hex: string, alpha: number): string {
    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    if (!result) return `rgba(0, 0, 0, ${alpha})`;
    const r = parseInt(result[1], 16);
    const g = parseInt(result[2], 16);
    const b = parseInt(result[3], 16);
    return `rgba(${r}, ${g}, ${b}, ${alpha})`;
  }

  private loadStyleRatingsComparisons(): void {
    if (!this.player?.playerId) return;

    this.isLoadingStyleComparisons = true;

    // Fetch average player style ratings
    this.styleRatingService.getAverageStyleRatings().pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (topRatings) => {
        this.averagePlayerStyleRatings = topRatings || [];
        this.prepareRadarChartData();
      },
      error: (err) => {
        console.error('Failed to load average style ratings', err);
        // Use fallback data
        this.setFallbackAverageRatings();
        this.prepareRadarChartData();
      },
      complete: () => {
        this.isLoadingStyleComparisons = false;
      }
    });

    // Fetch top player style ratings
    this.styleRatingService.getHighestStyleRatings().pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe({
      next: (topRatings) => {
        // Store the complete TopStyleRating objects rather than just extracting rating values
        this.topPlayerStyleRatings = topRatings || [];
        this.prepareRadarChartData();
      },
      error: (err) => {
        console.error('Failed to load top style ratings', err);
        // Use fallback data
        this.setFallbackTopRatings();
        this.prepareRadarChartData();
      }
    });
  }

  private setFallbackAverageRatings(): void {
    // Clear existing data
    this.averagePlayerStyleRatings = [];

    const averageValues = [
      {value: 50, name: 'Player1'},
      {value: 50, name: 'Player2'},
      {value: 50, name: 'Player3'},
      {value: 50, name: 'Player4'},
      {value: 50, name: 'Player5'},
      {value: 50, name: 'Player6'},
      {value: 50, name: 'Player7'},
      {value: 50, name: 'Player8'},
      {value: 50, name: 'Player9'},
      {value: 50, name: 'Player10'}
    ];

    // Set fallback average ratings (50 for all styles)
    RADAR_STYLE_ORDER.forEach((style, index) => {
      const data = index < averageValues.length ? averageValues[index] : {value: 50, name: 'Unknown'};
      this.topPlayerStyleRatings.push({
        style: style,
        rating: data.value,
        playerUsername: data.name,
        playerId: 'temp',
        playerFullName: 'temp'
      });
    });
  }

  private setFallbackTopRatings(): void {
    // Clear existing data
    this.topPlayerStyleRatings = [];

    // Set fallback top ratings with player names
    const topValues = [
      {value: 90, name: 'Player1'},
      {value: 85, name: 'Player2'},
      {value: 95, name: 'Player3'},
      {value: 85, name: 'Player4'},
      {value: 90, name: 'Player5'},
      {value: 80, name: 'Player6'},
      {value: 75, name: 'Player7'},
      {value: 90, name: 'Player8'},
      {value: 80, name: 'Player9'},
      {value: 85, name: 'Player10'}
    ];

    RADAR_STYLE_ORDER.forEach((style, index) => {
      const data = index < topValues.length ? topValues[index] : {value: 85, name: 'Unknown'};
      this.topPlayerStyleRatings.push({
        style: style,
        rating: data.value,
        playerUsername: data.name,
        playerId: 'temp',
        playerFullName: 'temp'
      });
    });
  }

}
