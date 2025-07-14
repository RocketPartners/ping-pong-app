// src/app/tournament/tournament-detail/tournament-detail.component.ts

import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {MatDialog} from '@angular/material/dialog';
import {forkJoin, of} from 'rxjs';
import {catchError, switchMap} from 'rxjs/operators';

import {TournamentService} from '../../_services/tournament.service';
import {PlayerService} from '../../_services/player.service';
import {AccountService} from '../../_services/account.service';
import {
  BracketType,
  MatchResultDTO,
  Tournament,
  TournamentMatch,
  TournamentPlayer,
  TournamentStatus
} from '../../_models/tournament';
import {Player} from '../../_models/models';
import {
  ConfirmDialogComponent,
  ConfirmDialogData
} from '../../_shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-tournament-detail',
  templateUrl: './tournament-detail.component.html',
  styleUrls: ['./tournament-detail.component.scss'],
  standalone: false,
})
export class TournamentDetailComponent implements OnInit {
  tournament: Tournament | null = null;
  matches: TournamentMatch[] = [];
  players: TournamentPlayer[] = [];
  playersMap: Map<string, Player> = new Map();

  loading = true;
  error = '';

  isOrganizer = false;
  currentUserId?: string;

  // For filtering matches
  showWinnersBracket = true;
  filteredMatches: TournamentMatch[] = [];

  // Track round data
  rounds: number[] = [];
  matchesByRound: { [round: number]: TournamentMatch[] } = {};

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private tournamentService: TournamentService,
    private playerService: PlayerService,
    private accountService: AccountService,
    private dialog: MatDialog
  ) {
  }

  ngOnInit(): void {
    this.currentUserId = this.accountService.playerValue?.player.playerId;
    this.loadTournamentData();
  }

  loadTournamentData(): void {
    this.loading = true;

    this.route.paramMap.pipe(
      switchMap(params => {
        const id = params.get('id');
        if (!id) {
          throw new Error('Tournament ID is required');
        }

        return forkJoin({
          tournament: this.tournamentService.getTournamentById(id),
          matches: this.tournamentService.getTournamentMatches(id),
          players: this.tournamentService.getTournamentPlayers(id)
        });
      }),
      catchError(error => {
        this.error = 'Failed to load tournament data';
        this.loading = false;
        console.error(error);
        return of({
          tournament: null,
          matches: [],
          players: []
        });
      })
    ).subscribe(data => {
      this.tournament = data.tournament;
      this.matches = data.matches;
      this.players = data.players;

      if (this.tournament) {
        this.isOrganizer = this.currentUserId === this.tournament.organizerId;
        this.organizeBracket();
        this.loadPlayerDetails();
      }

      this.loading = false;
    });
  }

  loadPlayerDetails(): void {
    if (!this.players || this.players.length === 0) return;

    // Extract unique player IDs
    const playerIds = this.players.map(p => p.playerId);

    // Fetch player details
    this.playerService.getPlayersByIds(playerIds).subscribe({
      next: (players) => {
        // Check if players is null before proceeding
        if (players) {
          // Create a map for easy lookup
          players.forEach(player => {
            this.playersMap.set(player.playerId, player);
          });
        }
      },
      error: (err) => {
        console.error('Failed to load player details', err);
      }
    });
  }

  organizeBracket(): void {
    if (!this.matches) return;

    // Reset
    this.rounds = [];
    this.matchesByRound = {};

    // Group matches by round
    this.matches.forEach(match => {
      if (!this.matchesByRound[match.round]) {
        this.matchesByRound[match.round] = [];
        this.rounds.push(match.round);
      }

      this.matchesByRound[match.round].push(match);
    });

    // Sort rounds
    this.rounds.sort((a, b) => a - b);

    this.filterMatches();
  }

  filterMatches(): void {
    this.filteredMatches = this.matches.filter(match => {
      // For double elimination tournaments
      if (this.tournament?.tournamentType === 'DOUBLE_ELIMINATION') {
        if (this.showWinnersBracket) {
          return match.bracketType === BracketType.WINNER ||
            match.bracketType === BracketType.CHAMPIONSHIP ||
            match.bracketType === BracketType.FINAL;
        } else {
          return match.bracketType === BracketType.LOSER ||
            match.bracketType === BracketType.CHAMPIONSHIP ||
            match.bracketType === BracketType.FINAL;
        }
      }

      // For single elimination, show all matches
      return true;
    });
  }

  toggleBracketView(): void {
    this.showWinnersBracket = !this.showWinnersBracket;
    this.filterMatches();
  }

  getPlayerName(playerId: string): string {
    const player = this.playersMap.get(playerId);
    return player ? player.username : 'Unknown Player';
  }

  getTeamNames(teamIds: string[]): string {
    if (!teamIds || teamIds.length === 0) return 'TBD';

    return teamIds.map(id => this.getPlayerName(id)).join(' & ');
  }

  updateMatchResult(matchId: string, winnerTeamIndex: number): void {
    if (!this.tournament?.id) return;

    const match = this.matches.find(m => m.matchId === matchId);
    if (!match) return;

    let winnerIds: string[] = [];
    let loserIds: string[] = [];

    if (winnerTeamIndex === 1) {
      winnerIds = [...match.team1Ids];
      loserIds = [...match.team2Ids];
    } else {
      winnerIds = [...match.team2Ids];
      loserIds = [...match.team1Ids];
    }

    const result: MatchResultDTO = {
      winnerIds,
      loserIds
    };

    // Confirm match result
    const dialogData: ConfirmDialogData = {
      title: 'Confirm Match Result',
      message: `Are you sure ${this.getTeamNames(winnerIds)} won this match?`,
      confirmText: 'Confirm',
      cancelText: 'Cancel'
    };

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: dialogData
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.tournamentService.updateMatchResult(this.tournament!.id!, matchId, result)
          .subscribe({
            next: () => {
              // Reload tournament data to get updated matches
              this.loadTournamentData();
            },
            error: (err) => {
              this.error = 'Failed to update match result. ' + (err.error?.message || '');
              console.error(err);
            }
          });
      }
    });
  }

  startTournament(): void {
    if (!this.tournament?.id) return;

    const dialogData: ConfirmDialogData = {
      title: 'Start Tournament',
      message: 'Are you sure you want to start this tournament? This will generate the bracket and lock in all players.',
      confirmText: 'Start Tournament',
      cancelText: 'Cancel'
    };

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: dialogData
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.tournamentService.startTournament(this.tournament!.id!)
          .subscribe({
            next: (tournament) => {
              this.tournament = tournament;
              this.loadTournamentData();
            },
            error: (err) => {
              this.error = 'Failed to start tournament. ' + (err.error?.message || '');
              console.error(err);
            }
          });
      }
    });
  }

  completeTournament(): void {
    if (!this.tournament?.id) return;

    const dialogData: ConfirmDialogData = {
      title: 'Complete Tournament',
      message: 'Are you sure you want to complete this tournament? This will finalize all results.',
      confirmText: 'Complete Tournament',
      cancelText: 'Cancel'
    };

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: dialogData
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        this.tournamentService.completeTournament(this.tournament!.id!)
          .subscribe({
            next: (tournament) => {
              this.tournament = tournament;
              this.loadTournamentData();
            },
            error: (err) => {
              this.error = 'Failed to complete tournament. ' + (err.error?.message || '');
              console.error(err);
            }
          });
      }
    });
  }

  canStartTournament(): boolean {
    return this.isOrganizer &&
      this.tournament?.status === TournamentStatus.CREATED &&
      (this.tournament.playerIds?.length || 0) >= 2;
  }

  canCompleteTournament(): boolean {
    // Check if all matches have been completed
    const allMatchesCompleted = this.matches.every(match => match.completed);

    return this.isOrganizer &&
      this.tournament?.status === TournamentStatus.IN_PROGRESS &&
      allMatchesCompleted;
  }

  canUpdateMatchResult(match: TournamentMatch): boolean {
    return this.isOrganizer &&
      this.tournament?.status === TournamentStatus.IN_PROGRESS &&
      !match.completed &&
      match.team1Ids.length > 0 &&
      match.team2Ids.length > 0;
  }

  getStatusClass(status: TournamentStatus): string {
    switch (status) {
      case TournamentStatus.CREATED:
        return 'status-created';
      case TournamentStatus.IN_PROGRESS:
        return 'status-in-progress';
      case TournamentStatus.COMPLETED:
        return 'status-completed';
      case TournamentStatus.CANCELLED:
        return 'status-cancelled';
      default:
        return '';
    }
  }

  getStatusText(status: TournamentStatus): string {
    switch (status) {
      case TournamentStatus.CREATED:
        return 'Created';
      case TournamentStatus.IN_PROGRESS:
        return 'In Progress';
      case TournamentStatus.COMPLETED:
        return 'Completed';
      case TournamentStatus.CANCELLED:
        return 'Cancelled';
      default:
        return 'Unknown';
    }
  }

  getBracketTypeClass(bracketType: BracketType): string {
    switch (bracketType) {
      case BracketType.WINNER:
        return 'bracket-winner';
      case BracketType.LOSER:
        return 'bracket-loser';
      case BracketType.FINAL:
        return 'bracket-final';
      case BracketType.CHAMPIONSHIP:
        return 'bracket-championship';
      default:
        return '';
    }
  }

  getBracketTypeLabel(bracketType: BracketType): string {
    switch (bracketType) {
      case BracketType.WINNER:
        return 'Winner Bracket';
      case BracketType.LOSER:
        return 'Loser Bracket';
      case BracketType.FINAL:
        return 'Final';
      case BracketType.CHAMPIONSHIP:
        return 'Championship';
      default:
        return 'Unknown';
    }
  }

  backToList(): void {
    this.router.navigate(['/tournaments']);
  }

  // Add these methods to your TournamentDetailComponent class

  /**
   * Checks if a match is a bye match (only one player/team assigned)
   */
  isByeMatch(match: TournamentMatch): boolean {
    // A bye match has one team with players and the other team empty
    const team1HasPlayers = match.team1Ids && match.team1Ids.length > 0;
    const team2HasPlayers = match.team2Ids && match.team2Ids.length > 0;

    return (team1HasPlayers && !team2HasPlayers) || (!team1HasPlayers && team2HasPlayers);
  }

  /**
   * Handles advancing a player in a bye match
   */
  handleByeMatch(match: TournamentMatch): void {
    if (!this.tournament?.id || match.completed) return;

    // Only allow organizing to handle bye matches
    if (!this.isOrganizer) return;

    // Determine which team has the player
    const team1HasPlayers = match.team1Ids && match.team1Ids.length > 0;
    const team2HasPlayers = match.team2Ids && match.team2Ids.length > 0;

    // Set up the match result with the correct winner/loser
    let result: MatchResultDTO;

    if (team1HasPlayers) {
      result = {
        winnerIds: [...match.team1Ids],
        loserIds: [] // No losers in a bye
      };
    } else if (team2HasPlayers) {
      result = {
        winnerIds: [...match.team2Ids],
        loserIds: [] // No losers in a bye
      };
    } else {
      // Shouldn't happen, but just in case
      return;
    }

    // Confirm bye advancement
    const dialogData: ConfirmDialogData = {
      title: 'Confirm Bye Match',
      message: `Are you sure you want to advance ${this.getTeamNames(result.winnerIds)} to the next round?`,
      confirmText: 'Advance',
      cancelText: 'Cancel'
    };

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: dialogData
    });

    dialogRef.afterClosed().subscribe(confirmed => {
      if (confirmed) {
        // Update the match result
        this.tournamentService.updateMatchResult(this.tournament!.id!, match.matchId!, result)
          .subscribe({
            next: () => {
              // Reload tournament data
              this.loadTournamentData();
            },
            error: (err) => {
              this.error = 'Failed to advance player in bye match. ' + (err.error?.message || '');
              console.error(err);
            }
          });
      }
    });
  }
}
