// src/app/tournament/tournament-detail/tournament-detail.component.ts
// New tournament detail component with brackets-viewer integration

import { Component, OnInit, OnDestroy, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { Subject, takeUntil, switchMap, catchError, of } from 'rxjs';

import { TournamentService } from '../../_services/tournament.service';
import { AccountService } from '../../_services/account.service';
import {
  TournamentDetails,
  TournamentStatus,
  TournamentType,
  Match,
  MatchStatus,
  UpdateMatchRequest,
  Participant,
  PlayerInfo
} from '../../_models/tournament-new';
import {
  ConfirmDialogComponent,
  ConfirmDialogData
} from '../../_shared/components/confirm-dialog/confirm-dialog.component';
import { MatchResultDialogComponent, MatchResultDialogResult } from '../match-result-dialog/match-result-dialog.component';

@Component({
  selector: 'app-tournament-detail',
  templateUrl: './tournament-detail.component.html',
  styleUrls: ['./tournament-detail.component.scss'],
  standalone: false,
})
export class TournamentDetailComponent implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('bracketContainer', { static: false }) bracketContainer!: ElementRef<HTMLDivElement>;

  tournament: TournamentDetails | null = null;
  readyMatches: Match[] = [];
  playersInfo: Map<number, PlayerInfo> = new Map();

  loading$ = this.tournamentService.loading$;
  error = '';

  isOrganizer = false;
  currentUserId?: string;

  private destroy$ = new Subject<void>();

  // Tournament status enum for template
  TournamentStatus = TournamentStatus;
  TournamentType = TournamentType;
  MatchStatus = MatchStatus;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private tournamentService: TournamentService,
    private accountService: AccountService,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.currentUserId = this.accountService.playerValue?.player.playerId;
    this.loadTournamentData();
  }

  ngAfterViewInit(): void {
    // Simple bracket component handles its own initialization
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.tournamentService.clearCurrentTournament();
  }

  private loadTournamentData(): void {
    this.route.paramMap.pipe(
      switchMap(params => {
        const id = params.get('id');
        if (!id) {
          throw new Error('Tournament ID is required');
        }
        return this.tournamentService.getTournamentDetails(id);
      }),
      catchError(error => {
        this.error = 'Failed to load tournament data';
        console.error(error);
        return of(null);
      }),
      takeUntil(this.destroy$)
    ).subscribe(tournament => {
      if (tournament) {
        this.tournament = tournament;
        this.isOrganizer = this.currentUserId === tournament.organizerId;
        
        // Load additional data
        this.loadReadyMatches();
        this.loadPlayersInfo();
        
        // Subscribe to real-time updates
        this.subscribeToUpdates();
      }
    });
  }

  private loadReadyMatches(): void {
    if (!this.tournament?.id) return;

    this.tournamentService.getReadyMatches(this.tournament.id).pipe(
      takeUntil(this.destroy$)
    ).subscribe(matches => {
      this.readyMatches = matches;
    });
  }

  private loadPlayersInfo(): void {
    if (!this.tournament?.participants) return;

    const playerIds = this.tournament.participants.map(p => p.playerId);
    if (playerIds.length === 0) return;

    this.tournamentService.getPlayersInfo(playerIds).pipe(
      takeUntil(this.destroy$)
    ).subscribe(players => {
      players.forEach(player => {
        // Map player info by the participant's numeric ID
        const participant = this.tournament!.participants.find(p => p.playerId === player.playerId);
        if (participant) {
          this.playersInfo.set(participant.id, player);
        }
      });
      
      // Player names loaded and mapped to participant IDs
    });
  }

  // Handle match result updates from bracket component
  onMatchClicked(match: Match): void {
    if (!this.canUpdateMatchResult(match)) {
      return;
    }

    this.showMatchResultDialog(match);
  }

  private subscribeToUpdates(): void {
    if (!this.tournament?.id) return;

    this.tournamentService.subscribeToUpdates(this.tournament.id).pipe(
      takeUntil(this.destroy$)
    ).subscribe(() => {
      // Tournament data will be refreshed automatically by the service
      this.loadReadyMatches();
    });
  }

  // Event Handlers

  private showMatchResultDialog(match: Match): void {
    if (!this.tournament?.id) return;

    // Open detailed match result dialog
    const dialogRef = this.dialog.open(MatchResultDialogComponent, {
      width: '500px',
      data: {
        match: match,
        participants: this.tournament.bracketData?.participant || []
      },
      disableClose: false
    });

    dialogRef.afterClosed().subscribe((result: MatchResultDialogResult | undefined) => {
      if (result) {
        // Update match result via backend with complete data
        this.updateMatchResult(
          match.id, 
          result.winnerId, 
          result.winnerScore, 
          result.loserScore
        );
      }
    });
  }

  updateMatchResult(matchId: number, winnerId: number, winnerScore: number, loserScore: number): void {
    if (!this.tournament?.id) return;

    const request: UpdateMatchRequest = {
      matchId,
      winnerId,
      winnerScore,
      loserScore
    };

    this.tournamentService.updateMatchResult(this.tournament.id, request).subscribe({
      next: () => {
        // Success - refresh the tournament data to get updated bracket
        this.loadTournamentData();
      },
      error: (err) => {
        this.error = 'Failed to update match result. ' + (err.message || '');
        console.error(err);
      }
    });
  }

  // Tournament Actions

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
      if (confirmed && this.tournament?.id) {
        console.log('Starting tournament:', this.tournament.id, 'Current status:', this.tournament.status);
        this.tournamentService.startTournament(this.tournament.id).subscribe({
          next: (result) => {
            console.log('Tournament start result:', result);
            // Refresh tournament data to get updated status
            this.loadTournamentData();
          },
          error: (err) => {
            console.error('Tournament start error:', err);
            this.error = 'Failed to start tournament. ' + (err.message || '');
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
      if (confirmed && this.tournament?.id) {
        this.tournamentService.completeTournament(this.tournament.id).subscribe({
          error: (err) => {
            this.error = 'Failed to complete tournament. ' + (err.message || '');
            console.error(err);
          }
        });
      }
    });
  }

  // Utility Methods

  canStartTournament(): boolean {
    return this.isOrganizer &&
      (this.tournament?.status === TournamentStatus.CREATED || this.tournament?.status === TournamentStatus.READY_TO_START) &&
      (this.tournament?.participants?.length || 0) >= 2;
  }

  canCompleteTournament(): boolean {
    return this.isOrganizer &&
      (this.tournament?.status === TournamentStatus.IN_PROGRESS || 
       this.tournament?.status === TournamentStatus.ROUND_COMPLETE) &&
      this.readyMatches.length === 0; // No ready matches means tournament is complete
  }

  canUpdateMatchResult(match: Match): boolean {
    return !!(this.isOrganizer &&
      (this.tournament?.status === TournamentStatus.IN_PROGRESS ||
       this.tournament?.status === TournamentStatus.ROUND_COMPLETE) &&
      match.status === MatchStatus.READY &&
      match.opponent1?.id &&
      match.opponent2?.id);
  }

  getStatusClass(status: TournamentStatus): string {
    switch (status) {
      case TournamentStatus.CREATED:
        return 'status-created';
      case TournamentStatus.READY_TO_START:
        return 'status-ready';
      case TournamentStatus.IN_PROGRESS:
        return 'status-in-progress';
      case TournamentStatus.ROUND_COMPLETE:
        return 'status-round-complete';
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
      case TournamentStatus.READY_TO_START:
        return 'Ready to Start';
      case TournamentStatus.IN_PROGRESS:
        return 'In Progress';
      case TournamentStatus.ROUND_COMPLETE:
        return 'Round Complete';
      case TournamentStatus.COMPLETED:
        return 'Completed';
      case TournamentStatus.CANCELLED:
        return 'Cancelled';
      default:
        return 'Unknown';
    }
  }

  getPlayerName(participantId: number): string {
    const playerInfo = this.playersInfo.get(participantId);
    return playerInfo?.username || `Player ${participantId}`;
  }

  backToList(): void {
    this.router.navigate(['/tournaments']);
  }
}
