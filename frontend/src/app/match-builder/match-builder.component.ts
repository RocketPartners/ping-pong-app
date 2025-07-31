import {Component, OnDestroy, OnInit, ViewChild, ViewEncapsulation} from '@angular/core';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {MatStepper} from '@angular/material/stepper';
import {MatAutocompleteSelectedEvent} from '@angular/material/autocomplete';
import {map, Observable, startWith, Subscription} from 'rxjs';
import {MatSnackBar} from '@angular/material/snack-bar';

import {MatchService} from '../_services/match.service';
import {PlayerService} from '../_services/player.service';
import {GameService} from '../_services/game.service';
import {AccountService} from '../_services/account.service';
import {Router} from '@angular/router';
import {Game, Player, PlayerReview, SelectedPlayers} from "../_models/models";
import {PlayerStyle} from "../_models/player-style";
import {PlayerReviewService} from "../_services/player-review.service";
import {PlayerReviewDialogComponent} from "../player-review-dialog/player-review-dialog.component";
import {MatDialog} from "@angular/material/dialog";

interface MatchGame {
  id: number;
  team1Score: number;
  team2Score: number;
  concluded: boolean;
}

@Component({
  selector: 'app-match-builder',
  templateUrl: './match-builder.component.html',
  styleUrls: ['./match-builder.component.scss'],
  encapsulation: ViewEncapsulation.None,
  standalone: false
})
export class MatchBuilderComponent implements OnInit, OnDestroy {
  @ViewChild('stepper') stepper: MatStepper;

  // Forms
  matchConfigForm: FormGroup;
  playerSelectForm: FormGroup;

  // Current logged-in player
  currentPlayer: Player;

  // Players in the match
  selectedPlayers: SelectedPlayers = {
    player1: null,
    player2: null,
    player3: null,
    player4: null
  };

  // Player selection
  options: string[] = [];
  filteredOptions1: Observable<string[]>;
  filteredOptions2: Observable<string[]>;
  filteredOptions3: Observable<string[]>;

  // Games
  games: MatchGame[] = [];
  concludedGames: MatchGame[] = [];
  
  // Score buttons for quick selection
  scoreButtons: number[] = [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11];
  nextId: number = 1;
  gamesToSave: Game[] = [];

  // State
  saving: boolean = false;
  today = new Date();
  private subscriptions: Subscription[] = [];
// Add this property to your component class if not already present
  private playerStyleNames: Record<number, string> = {
    0: 'Spin',
    1: 'Power',
    2: 'Creative',
    3: 'Aggressive',
    4: 'Resilient',
    5: 'Ace Master',
    6: 'Rally King',
    7: 'Tactician'
  };

  constructor(
    private formBuilder: FormBuilder,
    private accountService: AccountService,
    private playerService: PlayerService,
    private gameService: GameService,
    private matchService: MatchService,
    private playerReviewService: PlayerReviewService,
    private snackBar: MatSnackBar,
    private router: Router,
    private dialog: MatDialog,
  ) {
    this.currentPlayer = this.accountService.playerValue?.player as Player;
  }

  ngOnInit(): void {
    this.initForms();
    this.setupPlayerAutoComplete();
    this.loadPlayers();

    // Subscribe to match service for player updates
    const playerSub = this.matchService.selectedPlayers$.subscribe(players => {
      this.selectedPlayers = players;
    });
    this.subscriptions.push(playerSub);

    // Subscribe to match config changes to update requirements
    const configSub = this.matchConfigForm.valueChanges.subscribe(() => {
      this.updatePlayerFormValidators();
    });
    this.subscriptions.push(configSub);

    const matchTypeControl = this.matchConfigForm.get('matchType');
    if (matchTypeControl) {
      const matchTypeSub = matchTypeControl.valueChanges.subscribe(() => {
        // When match type changes (singles to doubles or vice versa), refresh all player filters
        const allPositions = ['player1', 'player2', 'player3'];
        allPositions.forEach(pos => {
          const control = this.playerSelectForm.get(pos);
          if (control) {
            // Re-emit the current value to trigger the filter with updated exclusions
            const currentValue = control.value;
            control.setValue(currentValue, {emitEvent: true});
          }
        });
      });
      this.subscriptions.push(matchTypeSub);
    }
  }

  ngOnDestroy(): void {
    // Clean up subscriptions
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  playerSelected(event: MatAutocompleteSelectedEvent, position: string): void {
    const username = event.option.value;

    this.playerService.getPlayerByUsername(username).subscribe(player => {
      if (player) {
        // Add player to the service
        this.matchService.addPlayer(player, position);

        // Update form state
        this.playerSelectForm.get(position)?.setValue(username);
        this.playerSelectForm.get(position)?.markAsPristine();
        this.playerSelectForm.get(position)?.updateValueAndValidity();

        // Refresh the other search boxes by re-emitting their current values
        // This forces the filter pipes to re-evaluate with the updated selected players
        const allPositions = ['player1', 'player2', 'player3'];

        allPositions.forEach(pos => {
          if (pos !== position) {  // Skip the position we just updated
            const control = this.playerSelectForm.get(pos);
            if (control) {
              // Re-emit the current value to trigger the filter
              const currentValue = control.value;
              control.setValue(currentValue, {emitEvent: true});
            }
          }
        });
      }
    });
  }

  removePlayer(position: string): void {
    // Remove from the service
    this.matchService.removePlayer(position);

    // Properly reset the form control
    this.playerSelectForm.get(position)?.setValue('');

    // Mark as pristine to reset validation state
    this.playerSelectForm.get(position)?.markAsPristine();

    // Update validity to ensure UI is refreshed
    this.playerSelectForm.get(position)?.updateValueAndValidity();
  }

  createAnonymousPlayer(position: string): void {
    // Check if ranked mode is selected
    const matchMode = this.matchConfigForm.get('matchMode')?.value;
    if (matchMode === 'Ranked') {
      this.snackBar.open('Guest players can only be used in Normal games, not Ranked games', 'OK', {
        duration: 5000,
        panelClass: ['warning-snackbar']
      });
      return;
    }

    // Prompt for name
    const name = prompt('Enter the name for the guest player:');
    if (!name || name.trim().length === 0) {
      return;
    }

    const trimmedName = name.trim();
    if (trimmedName.length < 2 || trimmedName.length > 50) {
      this.snackBar.open('Guest player name must be between 2 and 50 characters', 'OK', {
        duration: 4000,
        panelClass: ['error-snackbar']
      });
      return;
    }

    // Create the anonymous player
    this.playerService.createAnonymousPlayer(trimmedName).subscribe({
      next: (player) => {
        if (player) {
          // Add player to the service
          this.matchService.addPlayer(player, position);
          // Update form state
          this.playerSelectForm.get(position)?.setValue(player.username);
          this.playerSelectForm.get(position)?.markAsPristine();
          this.playerSelectForm.get(position)?.updateValueAndValidity();
          
          this.snackBar.open(`Guest player "${player.firstName}" created successfully`, 'OK', {
            duration: 3000,
            panelClass: ['success-snackbar']
          });
        }
      },
      error: (error) => {
        const errorMessage = error.error?.message || 'Failed to create guest player';
        this.snackBar.open(errorMessage, 'OK', {
          duration: 5000,
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  isPlayerSelectionValid(): boolean {
    const matchType = this.matchConfigForm.get('matchType')?.value;

    if (matchType === 'singles') {
      return !!this.selectedPlayers.player1;
    } else { // doubles
      return !!(this.selectedPlayers.player1 &&
        this.selectedPlayers.player2 &&
        this.selectedPlayers.player3);
    }
  }

  // Add a new game
  addGame(): void {
    if (!this.canAddMoreGames()) {
      return;
    }

    this.games.push({
      id: this.nextId++,
      team1Score: 0,
      team2Score: 0,
      concluded: false
    });
  }

  // Delete a game
  deleteGame(id: number): void {
    this.games = this.games.filter(game => game.id !== id);
    this.concludedGames = this.concludedGames.filter(game => game.id !== id);
  }

// Set score directly
  setScore(game: MatchGame, team: 'team1' | 'team2', score: number): void {
    if (game.concluded) return;

    if (team === 'team1') {
      game.team1Score = score;
    } else {
      game.team2Score = score;
    }

    // Check if game can be automatically concluded
    this.checkAutoConclusion(game);
  }

  // Simplified "Win by 2" function - sets winning score to 13 and losing to 11
  openWinByTwoDialog(game: MatchGame, team: 'team1' | 'team2'): void {
    if (game.concluded) return;
    
    if (team === 'team1') {
      // Set team1 as winner with score 13
      game.team1Score = 13;
      // Set team2 as loser with score 11
      game.team2Score = 11;
    } else {
      // Set team2 as winner with score 13
      game.team2Score = 13;
      // Set team1 as loser with score 11
      game.team1Score = 11;
    }
    
    // Check if game can be automatically concluded
    this.checkAutoConclusion(game);
    
    // Show a brief confirmation message
    this.snackBar.open('Score set to 13-11 (win by 2)', '', {
      duration: 1500
    });
  }
  
  // For backward compatibility
  incrementScore(game: MatchGame, team: 'team1' | 'team2'): void {
    if (game.concluded) return;

    if (team === 'team1') {
      game.team1Score++;
    } else {
      game.team2Score++;
    }

    // Check if game can be automatically concluded
    this.checkAutoConclusion(game);
  }

  // For backward compatibility
  decrementScore(game: MatchGame, team: 'team1' | 'team2'): void {
    if (game.concluded) return;

    if (team === 'team1') {
      if (game.team1Score <= 0) return;
      game.team1Score--;
    } else {
      if (game.team2Score <= 0) return;
      game.team2Score--;
    }
  }
  
  // Return progress badges for match visualization
  getProgressBadges(): { status: 'pending' | 'team1' | 'team2' }[] {
    const badges: { status: 'pending' | 'team1' | 'team2' }[] = [];
    
    // Add badges for existing games
    for (const game of this.games) {
      if (!game.concluded) {
        badges.push({ status: 'pending' });
      } else if (game.team1Score > game.team2Score) {
        badges.push({ status: 'team1' });
      } else {
        badges.push({ status: 'team2' });
      }
    }
    
    // Add placeholders for remaining games if not in infinite mode
    if (!this.isInfiniteGamesEnabled()) {
      const maxGames = this.getMaxGames();
      const remainingGames = maxGames - this.games.length;
      for (let i = 0; i < remainingGames; i++) {
        badges.push({ status: 'pending' });
      }
    }
    
    return badges;
  }

  // Conclude a game
  concludeGame(game: MatchGame): void {
    if (!this.canConcludeGame(game)) return;

    game.concluded = true;

    if (!this.concludedGames.includes(game)) {
      this.concludedGames.push(game);
    }

    // Check if match is complete (best of X)
    this.checkMatchCompletion();
    
    // Show confirmation message
    this.snackBar.open('Game concluded!', '', {
      duration: 1500,
      panelClass: 'success-snackbar'
    });
  }
  
  // Reopen a concluded game for editing
  reopenGame(game: MatchGame): void {
    if (!game.concluded) return;
    
    game.concluded = false;
    this.concludedGames = this.concludedGames.filter(g => g.id !== game.id);
    
    // Show notification
    this.snackBar.open('Game reopened for editing', '', {
      duration: 1500
    });
  }

  // Check if a game can be concluded
  canConcludeGame(game: MatchGame): boolean {
    if (game.concluded) return false;

    // A game can be concluded when one team has at least 11 points
    // and is at least 2 points ahead of the opponent
    const team1Score = game.team1Score;
    const team2Score = game.team2Score;

    if (team1Score === team2Score) return false;

    const leader = team1Score > team2Score ? team1Score : team2Score;
    const trailer = team1Score > team2Score ? team2Score : team1Score;

    return leader >= 11 && (leader - trailer) >= 2;
  }

  // Check if all games are concluded
  areAllGamesConcluded(): boolean {
    return this.games.length > 0 && this.games.every(game => game.concluded);
  }

  // Check if more games can be added
  canAddMoreGames(): boolean {
    // If using "Infinite" format, allow unlimited games
    if (this.isInfiniteGamesEnabled()) {
      return true;
    }
    
    // If using single game (bestOf = 1), allow 1 game
    if (!this.bestOfEnabled()) {
      return this.games.length < 1;
    }

    // For Best of X format, check if we've reached the limit
    const bestOf = parseInt(this.matchConfigForm.get('bestOf')?.value || '1', 10);
    return this.games.length < bestOf;
  }

  // Check if "Best of X" is enabled
  bestOfEnabled(): boolean {
    const bestOfValue = this.matchConfigForm.get('bestOf')?.value;
    return bestOfValue !== '1' && bestOfValue !== 'infinite';
  }

  // Check if "Infinite Games" is enabled
  isInfiniteGamesEnabled(): boolean {
    return this.matchConfigForm.get('bestOf')?.value === 'infinite';
  }

  // Get maximum games for "Best of X" format
  getMaxGames(): number {
    const bestOfValue = this.matchConfigForm.get('bestOf')?.value;
    if (bestOfValue === 'infinite') {
      return Infinity;
    }
    return parseInt(bestOfValue || '1', 10);
  }

  saveMatch(): void {
    this.saving = true;
    this.convertGamesToSave();

    this.gameService.saveGames(this.gamesToSave).subscribe({
      next: (response) => {
        this.snackBar.open('Match saved successfully!', 'Close', {
          duration: 3000,
          panelClass: 'success-snackbar'
        });

        // Extract all gameIds from the saved games response
        const savedGameIds = response && Array.isArray(response)
          ? response.map(game => game.gameId).filter(Boolean) as string[]
          : [];

        console.log('Saved games with IDs:', savedGameIds);

        // Show player review dialog with all saved game IDs
        this.showPlayerReviewDialog(savedGameIds);
      },
      error: (error) => {
        console.error('Error saving games:', error);
        this.snackBar.open('Error saving match. Please try again.', 'Close', {
          duration: 5000,
          panelClass: 'error-snackbar'
        });
        this.saving = false;
      }
    });
  }

  // Reset the match builder
  resetMatch(): void {
    this.matchConfigForm.reset({
      matchType: 'singles',
      matchMode: 'Ranked',
      bestOf: '3'
    });

    this.playerSelectForm.reset();
    this.matchService.resetMatchBuilder();
    this.games = [];
    this.concludedGames = [];
    this.gamesToSave = [];
    this.nextId = 1;
    this.saving = false;

    if (this.stepper) {
      this.stepper.reset();
    }
  }

  // Helper methods for templates
  isWinner(game: MatchGame, team: string): boolean {
    if (!game.concluded) return false;

    if (team === 'team1') {
      return game.team1Score > game.team2Score;
    } else {
      return game.team2Score > game.team1Score;
    }
  }

  isSinglesMatch(): boolean {
    return this.matchConfigForm.get('matchType')?.value === 'singles';
  }

  isDoublesMatch(): boolean {
    return this.matchConfigForm.get('matchType')?.value === 'doubles';
  }

  getTeam1Wins(): number {
    return this.concludedGames.filter(game => game.team1Score > game.team2Score).length;
  }

  getTeam2Wins(): number {
    return this.concludedGames.filter(game => game.team2Score > game.team1Score).length;
  }

  getMatchTypeLabel(): string {
    const matchType = this.matchConfigForm.get('matchType')?.value;
    const matchMode = this.matchConfigForm.get('matchMode')?.value;

    return `${matchType === 'singles' ? 'Singles' : 'Doubles'} ${matchMode} Match`;
  }

  getTeam1Name(): string {
    if (this.isSinglesMatch()) {
      return this.currentPlayer.username;
    } else {
      return `${this.currentPlayer.username} & ${this.selectedPlayers.player1?.username || 'Teammate'}`;
    }
  }

  getTeam2Name(): string {
    if (this.isSinglesMatch()) {
      return this.selectedPlayers.player1?.username || 'Opponent';
    } else {
      return `${this.selectedPlayers.player2?.username || 'Opponent 1'} & ${this.selectedPlayers.player3?.username || 'Opponent 2'}`;
    }
  }

  getGameWinnerName(game: MatchGame): string {
    if (game.team1Score > game.team2Score) {
      return this.getTeam1Name();
    } else {
      return this.getTeam2Name();
    }
  }

  getMatchWinnerName(): string {
    const team1Wins = this.getTeam1Wins();
    const team2Wins = this.getTeam2Wins();

    if (team1Wins > team2Wins) {
      return this.getTeam1Name();
    } else if (team2Wins > team1Wins) {
      return this.getTeam2Name();
    } else {
      return 'Tie';
    }
  }

  /**
   * Get the singles rating based on match mode
   */
  getSinglesRating(player: Player | null): number {
    if (!player) return 0;

    const isRanked = this.matchConfigForm.get('matchMode')?.value === 'Ranked';
    return isRanked ? player.singlesRankedRating : player.singlesNormalRating;
  }

  /**
   * Get the singles win rate based on match mode
   */
  getSinglesWinRate(player: Player | null): number {
    if (!player) return 0;

    const isRanked = this.matchConfigForm.get('matchMode')?.value === 'Ranked';
    if (isRanked) {
      return player.singlesRankedWinRate;
    } else {
      // Calculate normal win rate if not available
      const wins = player.singlesNormalWins;
      const losses = player.singlesNormalLoses;
      if (wins + losses === 0) return 0;
      return Math.round((wins / (wins + losses)) * 100);
    }
  }

  /**
   * Get the singles wins based on match mode
   */
  getSinglesWins(player: Player | null): number {
    if (!player) return 0;

    const isRanked = this.matchConfigForm.get('matchMode')?.value === 'Ranked';
    return isRanked ? player.singlesRankedWins : player.singlesNormalWins;
  }

  /**
   * Get the singles losses based on match mode
   */
  getSinglesLosses(player: Player | null): number {
    if (!player) return 0;

    const isRanked = this.matchConfigForm.get('matchMode')?.value === 'Ranked';
    return isRanked ? player.singlesRankedLoses : player.singlesNormalLoses;
  }

  /**
   * Get the doubles rating based on match mode
   */
  getDoublesRating(player: Player | null): number {
    if (!player) return 0;

    const isRanked = this.matchConfigForm.get('matchMode')?.value === 'Ranked';
    return isRanked ? player.doublesRankedRating : player.doublesNormalRating;
  }

  /**
   * Get the doubles win rate based on match mode
   */
  getDoublesWinRate(player: Player | null): number {
    if (!player) return 0;

    const isRanked = this.matchConfigForm.get('matchMode')?.value === 'Ranked';
    if (isRanked) {
      return player.doublesRankedWinRate;
    } else {
      // Calculate normal win rate if not available
      const wins = player.doublesNormalWins;
      const losses = player.doublesNormalLoses;
      if (wins + losses === 0) return 0;
      return Math.round((wins / (wins + losses)) * 100);
    }
  }

  /**
   * Get the doubles wins based on match mode
   */
  getDoublesWins(player: Player | null): number {
    if (!player) return 0;

    const isRanked = this.matchConfigForm.get('matchMode')?.value === 'Ranked';
    return isRanked ? player.doublesRankedWins : player.doublesNormalWins;
  }

  // Add these methods to your match-builder.component.ts file

  /**
   * Get the doubles losses based on match mode
   */
  getDoublesLosses(player: Player | null): number {
    if (!player) return 0;

    const isRanked = this.matchConfigForm.get('matchMode')?.value === 'Ranked';
    return isRanked ? player.doublesRankedLoses : player.doublesNormalLoses;
  }

  /**
   * Calculate expert level for singles based on match mode
   */
  getSinglesExpertLevel(player: Player | null): number {
    if (!player) return 0;

    const isRanked = this.matchConfigForm.get('matchMode')?.value === 'Ranked';
    const rating = isRanked ? player.singlesRankedRating : player.singlesNormalRating;
    const winRate = this.getSinglesWinRate(player);
    const wins = isRanked ? player.singlesRankedWins : player.singlesNormalWins;
    const losses = isRanked ? player.singlesRankedLoses : player.singlesNormalLoses;
    const totalGames = wins + losses;

    // A simple formula to calculate an "expert level" from 0-10
    const ratingFactor = Math.min(rating / 2000, 1) * 4;
    const winRateFactor = Math.min(winRate / 100, 1) * 4;
    const gamesFactor = Math.min(totalGames / 50, 1) * 2;

    return Math.round(ratingFactor + winRateFactor + gamesFactor);
  }

  /**
   * Calculate expert level for doubles based on match mode
   */
  getDoublesExpertLevel(player: Player | null): number {
    if (!player) return 0;

    const isRanked = this.matchConfigForm.get('matchMode')?.value === 'Ranked';
    const rating = isRanked ? player.doublesRankedRating : player.doublesNormalRating;
    const winRate = this.getDoublesWinRate(player);
    const wins = isRanked ? player.doublesRankedWins : player.doublesNormalWins;
    const losses = isRanked ? player.doublesRankedLoses : player.doublesNormalLoses;
    const totalGames = wins + losses;

    // A simple formula to calculate an "expert level" from 0-10
    const ratingFactor = Math.min(rating / 2000, 1) * 4;
    const winRateFactor = Math.min(winRate / 100, 1) * 4;
    const gamesFactor = Math.min(totalGames / 50, 1) * 2;

    return Math.round(ratingFactor + winRateFactor + gamesFactor);
  }

  /**
   * Get text representation of expert level
   */
  getExpertLevelText(level: number): string {
    if (level >= 9) return 'Legendary';
    if (level >= 7) return 'Master';
    if (level >= 5) return 'Expert';
    if (level >= 3) return 'Intermediate';
    return 'Beginner';
  }

  /**
   * Get CSS class for expert level
   */
  getExpertLevelClass(level: number): string {
    if (level >= 9) return 'expert-legendary';
    if (level >= 7) return 'expert-master';
    if (level >= 5) return 'expert-expert';
    if (level >= 3) return 'expert-intermediate';
    return 'expert-beginner';
  }

  /**
   * Get icon for expert level
   */
  getExpertLevelIcon(level: number): string {
    if (level >= 9) return 'workspace_premium';
    if (level >= 7) return 'star';
    if (level >= 5) return 'military_tech';
    if (level >= 3) return 'trending_up';
    return 'show_chart';
  }

  /**
   * Get CSS class for player style badge
   */
  getStyleBadgeClass(style: string): string {
    switch (style) {
      case PlayerStyle.SPIN:
        return 'style-spin';
      case PlayerStyle.POWER:
        return 'style-power';
      case PlayerStyle.CREATIVE:
        return 'style-creative';
      case PlayerStyle.AGGRESSIVE:
        return 'style-aggressive';
      case PlayerStyle.RESILIENT:
        return 'style-resilient';
      case PlayerStyle.ACE_MASTER:
        return 'style-ace';
      case PlayerStyle.RALLY_KING:
        return 'style-rally';
      case PlayerStyle.TACTICIAN:
        return 'style-tactician';
      default:
        return '';
    }
  }

  /**
   * Get name of player style
   */
  getStyleName(style: any): string {
    if (typeof style === 'number' && style in PlayerStyle) {
      // This assumes you have a playerStyleNames object similar to the one in leaderboard component
      // You may need to add this to your component
      return this.playerStyleNames ? this.playerStyleNames[style] : '';
    }
    return 'Unknown';
  }

  /**
   * Check if matchup is complete for prediction display
   */
  isMatchupComplete(): boolean {
    const isSingles = this.matchConfigForm.get('matchType')?.value === 'singles';

    if (isSingles) {
      return !!this.selectedPlayers.player1;
    } else {
      return !!(this.selectedPlayers.player1 && this.selectedPlayers.player2 && this.selectedPlayers.player3);
    }
  }

  /**
   * Calculate win prediction percentage for left team
   */
  getLeftTeamPrediction(): number {
    const isSingles = this.matchConfigForm.get('matchType')?.value === 'singles';
    const isRanked = this.matchConfigForm.get('matchMode')?.value === 'Ranked';

    if (!this.isMatchupComplete()) {
      return 50;
    }

    if (isSingles) {
      const yourRating = isRanked ?
        this.currentPlayer.singlesRankedRating :
        this.currentPlayer.singlesNormalRating;

      const opponentRating = isRanked ?
        this.selectedPlayers.player1?.singlesRankedRating || 0 :
        this.selectedPlayers.player1?.singlesNormalRating || 0;

      // Simple ELO-based prediction
      return Math.round(this.calculateWinProbability(yourRating, opponentRating) * 100);
    } else {
      // For doubles, average the team ratings
      const yourTeamRating = (
        (isRanked ? this.currentPlayer.doublesRankedRating : this.currentPlayer.doublesNormalRating) +
        (isRanked ? this.selectedPlayers.player1?.doublesRankedRating || 0 : this.selectedPlayers.player1?.doublesNormalRating || 0)
      ) / 2;

      const opponentTeamRating = (
        (isRanked ? this.selectedPlayers.player2?.doublesRankedRating || 0 : this.selectedPlayers.player2?.doublesNormalRating || 0) +
        (isRanked ? this.selectedPlayers.player3?.doublesRankedRating || 0 : this.selectedPlayers.player3?.doublesNormalRating || 0)
      ) / 2;

      return Math.round(this.calculateWinProbability(yourTeamRating, opponentTeamRating) * 100);
    }
  }

  /**
   * Calculate win prediction percentage for right team
   */
  getRightTeamPrediction(): number {
    return 100 - this.getLeftTeamPrediction();
  }

  /**
   * Show the player review dialog after match save
   */
  private showPlayerReviewDialog(gameIds: string[]): void {
    console.log('Showing player review dialog with gameIds:', gameIds);

    // Get all players involved in this match
    const players: Player[] = [];

    // Add opponent in singles match
    if (this.isSinglesMatch() && this.selectedPlayers.player1) {
      console.log('Adding singles opponent:', this.selectedPlayers.player1.username);
      players.push(this.selectedPlayers.player1);
    }

    // Add teammates and opponents in doubles match
    if (this.isDoublesMatch()) {
      if (this.selectedPlayers.player1) {
        console.log('Adding doubles teammate:', this.selectedPlayers.player1.username);
        players.push(this.selectedPlayers.player1); // Teammate
      }
      if (this.selectedPlayers.player2) {
        console.log('Adding doubles opponent 1:', this.selectedPlayers.player2.username);
        players.push(this.selectedPlayers.player2); // Opponent 1
      }
      if (this.selectedPlayers.player3) {
        console.log('Adding doubles opponent 2:', this.selectedPlayers.player3.username);
        players.push(this.selectedPlayers.player3); // Opponent 2
      }
    }

    console.log('Players to review:', players.length);

    // Only show dialog if there are players to review and we have game IDs
    if (players.length > 0 && gameIds.length > 0) {
      console.log('Opening player review dialog');
      // Add a small delay to ensure everything is initialized
      setTimeout(() => {
        const dialogRef = this.dialog.open(PlayerReviewDialogComponent, {
          width: '650px',
          disableClose: false,
          data: {
            players: players,
            currentPlayerId: this.currentPlayer.playerId
          }
        });

        dialogRef.afterOpened().subscribe(() => {
          console.log('Dialog opened successfully');
        });

        dialogRef.afterClosed().subscribe((result: PlayerReview[] | undefined) => {
          console.log('Dialog closed with result:', result);
          this.saving = false;

          // Handle the reviews if user submitted them
          if (result && result.length > 0) {
            // Submit batch reviews with the current player as reviewer
            this.playerReviewService.submitBatchReviews(
              result,
              this.currentPlayer.playerId,
              gameIds  // Use the saved game IDs here
            ).subscribe({
              next: () => {
                this.snackBar.open('Thank you for your reviews!', 'Close', {duration: 2000});
                this.resetMatch();
                this.router.navigate(['/']);
              },
              error: (err) => {
                console.error('Error submitting reviews:', err);
                this.snackBar.open('Unable to save reviews, but your match was recorded.', 'Close', {duration: 3000});
                this.resetMatch();
                this.router.navigate(['/']);
              }
            });
          } else {
            // If user skipped reviews, just reset and navigate away
            this.resetMatch();
            this.router.navigate(['/']);
          }
        });
      }, 300); // 300ms delay
    } else {
      console.log('No players to review or no game IDs');
      // No players to review or no game IDs, just reset and navigate
      this.resetMatch();
      this.router.navigate(['/']);
    }
  }

  // Initialize forms
  private initForms(): void {
    this.matchConfigForm = this.formBuilder.group({
      matchType: ['singles', Validators.required],
      matchMode: ['Ranked', Validators.required],
      bestOf: ['3']
    });

    this.playerSelectForm = this.formBuilder.group({
      player1: ['', Validators.required],
      player2: [''],
      player3: ['']
    });
  }

  // Update validators based on match type
  private updatePlayerFormValidators(): void {
    const matchType = this.matchConfigForm.get('matchType')?.value;

    if (matchType === 'singles') {
      this.playerSelectForm.get('player2')?.clearValidators();
      this.playerSelectForm.get('player3')?.clearValidators();
    } else { // doubles
      this.playerSelectForm.get('player2')?.setValidators(Validators.required);
      this.playerSelectForm.get('player3')?.setValidators(Validators.required);
    }

    this.playerSelectForm.get('player2')?.updateValueAndValidity();
    this.playerSelectForm.get('player3')?.updateValueAndValidity();
  }

  private setupPlayerAutoComplete(): void {
    // For player1 (teammate in doubles or opponent in singles)
    this.filteredOptions1 = this.playerSelectForm.get('player1')!.valueChanges.pipe(
      startWith(''),
      map(value => {
        const isDoubles = this.matchConfigForm.get('matchType')?.value === 'doubles';
        // In doubles mode, exclude opponents; in singles mode, no additional exclusions
        const exclusions = isDoubles ? ['player2', 'player3'] : [];
        return this._filterPlayers(value || '', exclusions);
      })
    );

    // For player2 (opponent 1 in doubles)
    this.filteredOptions2 = this.playerSelectForm.get('player2')!.valueChanges.pipe(
      startWith(''),
      map(value => this._filterPlayers(value || '', ['player1']))
    );

    // For player3 (opponent 2 in doubles)
    this.filteredOptions3 = this.playerSelectForm.get('player3')!.valueChanges.pipe(
      startWith(''),
      map(value => this._filterPlayers(value || '', ['player1', 'player2']))
    );
  }

  // Filter players for autocomplete
  private _filterPlayers(value: string, excludeFields: string[] = []): string[] {
    const filterValue = value.toLowerCase();

    // Create a list of usernames to exclude
    const excludedUsernames = [this.currentPlayer.username];

    // Add usernames from the excluded fields
    excludeFields.forEach(field => {
      const selectedPlayer = this.selectedPlayers[field as keyof SelectedPlayers];
      if (selectedPlayer) {
        excludedUsernames.push(selectedPlayer.username);
      }
    });

    // Filter options
    return this.options.filter(option =>
      option.toLowerCase().includes(filterValue) &&
      !excludedUsernames.includes(option)
    );
  }

  // Load players from the service
  private loadPlayers(): void {
    this.playerService.getPlayerUsernames().subscribe(usernames => {
      if (usernames) {
        // Filter out the current user
        this.options = usernames.filter(username =>
          username !== this.currentPlayer.username
        );
      }
    });
  }

  // Check if a game should be automatically concluded
  private checkAutoConclusion(game: MatchGame): void {
    if (this.canConcludeGame(game)) {
      this.concludeGame(game);
    }
  }

  // Check if the match is complete (based on best of X)
  private checkMatchCompletion(): void {
    const bestOfValue = this.matchConfigForm.get('bestOf')?.value;
    
    // Don't auto-advance if we're in infinite mode
    if (bestOfValue === 'infinite') return;
    
    // Also don't auto-advance for single game matches
    if (bestOfValue === '1') return;
    
    const bestOf = parseInt(bestOfValue || '1', 10);
    const team1Wins = this.getTeam1Wins();
    const team2Wins = this.getTeam2Wins();
    const winThreshold = Math.ceil(bestOf / 2);

    if (team1Wins >= winThreshold || team2Wins >= winThreshold) {
      // Match is complete - optionally auto-advance to review step
      if (this.areAllGamesConcluded()) {
        setTimeout(() => {
          if (this.stepper) {
            this.stepper.next();
          }
        }, 1000);
      }
    }
  }

  // Convert the games to the backend format
  private convertGamesToSave(): void {
    const matchType = this.matchConfigForm.get('matchType')?.value;
    const isRanked = this.matchConfigForm.get('matchMode')?.value === 'Ranked';

    this.gamesToSave = this.games.map(game => {
      const team1Wins = game.team1Score > game.team2Score;

      if (matchType === 'singles') {
        return {
          challengerId: this.currentPlayer.playerId,
          challengerTeam: [],
          challengerTeamScore: game.team1Score,
          challengerTeamWin: false,
          challengerWin: team1Wins,
          doublesGame: false,
          normalGame: !isRanked,
          ratedGame: isRanked,
          singlesGame: true,
          opponentId: this.selectedPlayers.player1?.playerId || '',
          opponentTeam: [],
          opponentTeamScore: game.team2Score,
          opponentTeamWin: false,
          opponentWin: !team1Wins
        } as Game;
      } else { // doubles
        return {
          challengerId: '',
          challengerTeam: [
            this.currentPlayer.playerId,
            this.selectedPlayers.player1?.playerId || ''
          ].filter(Boolean) as string[],
          challengerTeamScore: game.team1Score,
          challengerTeamWin: team1Wins,
          challengerWin: false,
          doublesGame: true,
          normalGame: !isRanked,
          ratedGame: isRanked,
          singlesGame: false,
          opponentId: '',
          opponentTeam: [
            this.selectedPlayers.player2?.playerId || '',
            this.selectedPlayers.player3?.playerId || ''
          ].filter(Boolean) as string[],
          opponentTeamScore: game.team2Score,
          opponentTeamWin: !team1Wins,
          opponentWin: false
        } as Game;
      }
    });
  }

  /**
   * Calculate win probability based on ELO-style formula
   */
  private calculateWinProbability(playerRating: number, opponentRating: number): number {
    // Standard ELO formula for win probability
    return 1 / (1 + Math.pow(10, (opponentRating - playerRating) / 400));
  }

}
