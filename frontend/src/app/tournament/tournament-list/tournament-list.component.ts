// src/app/tournament/tournament-list/tournament-list.component.ts

import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {MatDialog} from '@angular/material/dialog';
import {TournamentService} from '../../_services/tournament.service';
import {AccountService} from '../../_services/account.service';
import {Tournament, TournamentStatus} from '../../_models/tournament';
import {
  ConfirmDialogComponent,
  ConfirmDialogData
} from '../../_shared/components/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-tournament-list',
  templateUrl: './tournament-list.component.html',
  styleUrls: ['./tournament-list.component.scss'],
  standalone: false,
})
export class TournamentListComponent implements OnInit {
  tournaments: Tournament[] = [];
  filteredTournaments: Tournament[] = [];
  loading = true;
  error = '';

  // Filter options
  statusFilter: string = 'all';
  showMyTournamentsOnly = false;

  currentUserId?: string;

  constructor(
    private tournamentService: TournamentService,
    private accountService: AccountService,
    private router: Router,
    private dialog: MatDialog
  ) {
  }

  ngOnInit(): void {
    this.currentUserId = this.accountService.playerValue?.player.playerId;
    this.loadTournaments();
  }

  loadTournaments(): void {
    this.loading = true;
    this.tournamentService.getAllTournaments().subscribe({
      next: (data) => {
        this.tournaments = data;
        this.applyFilters();
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load tournaments. Please try again later.';
        this.loading = false;
        console.error(err);
      }
    });
  }

  applyFilters(): void {
    this.filteredTournaments = this.tournaments.filter(tournament => {
      // Apply status filter
      if (this.statusFilter !== 'all' && tournament.status !== this.statusFilter) {
        return false;
      }

      // Apply "my tournaments" filter
      if (this.showMyTournamentsOnly) {
        // Show tournaments where user is organizer or participant
        const isOrganizer = tournament.organizerId === this.currentUserId;
        const isParticipant = tournament.playerIds?.includes(this.currentUserId || '') || false;

        if (!isOrganizer && !isParticipant) {
          return false;
        }
      }

      return true;
    });
  }

  createTournament(): void {
    this.router.navigate(['/tournaments/create']);
  }

  viewTournament(id: string): void {
    this.router.navigate(['/tournaments', id]);
  }

  deleteTournament(tournament: Tournament): void {
    const dialogData: ConfirmDialogData = {
      title: 'Confirm Deletion',
      message: `Are you sure you want to delete the tournament "${tournament.name}"?`,
      confirmText: 'Delete',
      cancelText: 'Cancel'
    };

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      width: '400px',
      data: dialogData
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result && tournament.id) {
        this.tournamentService.deleteTournament(tournament.id).subscribe({
          next: () => {
            this.loadTournaments();
          },
          error: (err) => {
            this.error = 'Failed to delete tournament.';
            console.error(err);
          }
        });
      }
    });
  }

  isOrganizer(tournament: Tournament): boolean {
    return tournament.organizerId === this.currentUserId;
  }

  onFilterChange(): void {
    this.applyFilters();
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

  getTournamentTypeText(type: string): string {
    switch (type) {
      case 'SINGLE_ELIMINATION':
        return 'Single Elimination';
      case 'DOUBLE_ELIMINATION':
        return 'Double Elimination';
      case 'ROUND_ROBIN':
        return 'Round Robin';
      default:
        return type;
    }
  }

  getGameTypeText(type: string): string {
    switch (type) {
      case 'SINGLES':
        return 'Singles';
      case 'DOUBLES':
        return 'Doubles';
      default:
        return type;
    }
  }
}
