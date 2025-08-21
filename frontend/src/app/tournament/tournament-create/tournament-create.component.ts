// src/app/tournament/tournament-create/tournament-create.component.ts

import {Component, OnInit} from '@angular/core';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {Router} from '@angular/router';
import {Observable} from 'rxjs';
import {map, startWith} from 'rxjs/operators';

import {TournamentService} from '../../_services/tournament.service';
import {PlayerService} from '../../_services/player.service';
import {AccountService} from '../../_services/account.service';
import {CreateTournamentRequest, TournamentType, SeedingMethod, GameType} from '../../_models/tournament-new';
import {Player} from '../../_models/models';

@Component({
  selector: 'app-tournament-create',
  templateUrl: './tournament-create.component.html',
  styleUrls: ['./tournament-create.component.scss'],
  standalone: false,
})
export class TournamentCreateComponent implements OnInit {
  tournamentForm: FormGroup;
  playerSelectionForm: FormGroup;

  // Stepper state
  currentStep = 0;

  // Data
  players: Player[] = [];
  selectedPlayers: Player[] = [];
  // teamPairs: TeamPair[] = []; // Removed for now

  // For autocomplete
  filteredPlayers: Observable<Player[]>;

  // Form controls
  loading = false;
  error = '';

  // Date config
  minDate = new Date(); // Today's date

  // Enum mappings for dropdown options
  tournamentTypes = [
    {value: TournamentType.SINGLE_ELIMINATION, label: 'Single Elimination'},
    {value: TournamentType.DOUBLE_ELIMINATION, label: 'Double Elimination'}
  ];

  // Seeding method options
  seedingMethods = [
    {value: SeedingMethod.RANDOM, label: 'Random Seeding'},
    {value: SeedingMethod.RATING_BASED, label: 'Rating-Based Seeding'},
    {value: SeedingMethod.MANUAL, label: 'Manual Seeding'}
  ];

  protected readonly Math = Math;

  constructor(
    private formBuilder: FormBuilder,
    private tournamentService: TournamentService,
    private playerService: PlayerService,
    private accountService: AccountService,
    private router: Router
  ) {
    this.tournamentForm = this.formBuilder.group({
      name: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(100)]],
      description: [''],
      tournamentType: [TournamentType.SINGLE_ELIMINATION, Validators.required],
      seedingMethod: [SeedingMethod.RANDOM, Validators.required],
      startDate: [this.minDate, Validators.required]
    });

    this.playerSelectionForm = this.formBuilder.group({
      playerSearch: ['']
    });

    // Setup player filtering for autocomplete
    this.filteredPlayers = this.playerSelectionForm.get('playerSearch')!.valueChanges.pipe(
      startWith(''),
      map(value => this._filterPlayers(value || ''))
    );
  }

  ngOnInit(): void {
    this.loadPlayers();
  }

  loadPlayers(): void {
    this.playerService.getPlayers().subscribe({
      next: (data) => {
        if (data) {
          this.players = data;
        }
      },
      error: (err) => {
        this.error = 'Failed to load players.';
        console.error(err);
      }
    });
  }

  nextStep(): void {
    if (this.currentStep === 0 && this.tournamentForm.valid) {
      this.currentStep = 1;
    } else if (this.currentStep === 1 && this.selectedPlayers.length >= 2) {
      this.createTournament();
    }
  }

  prevStep(): void {
    if (this.currentStep > 0) {
      this.currentStep--;
    }
  }

  displayPlayer(player: Player | string): string {
    if (typeof player === 'string') return player;
    return player ? `${player.firstName} ${player.lastName} (${player.username})` : '';
  }

  addPlayer(player: Player): void {
    if (!this.selectedPlayers.some(p => p.playerId === player.playerId)) {
      this.selectedPlayers.push(player);

      // Clear the search field
      this.playerSelectionForm.get('playerSearch')!.setValue('');
    }
  }

  removePlayer(player: Player): void {
    this.selectedPlayers = this.selectedPlayers.filter(p => p.playerId !== player.playerId);
  }

  createTournament(): void {
    if (this.tournamentForm.invalid || this.selectedPlayers.length < 2) {
      return;
    }

    this.loading = true;

    const formValue = this.tournamentForm.value;
    const currentPlayer = this.accountService.playerValue?.player;

    if (!currentPlayer) {
      this.error = 'You must be logged in to create a tournament.';
      this.loading = false;
      return;
    }

    // Create tournament request
    const tournamentRequest: CreateTournamentRequest = {
      name: formValue.name,
      description: formValue.description,
      tournamentType: formValue.tournamentType,
      gameType: GameType.SINGLES,
      seedingMethod: formValue.seedingMethod,
      organizerId: currentPlayer.playerId,
      playerIds: this.selectedPlayers.map(p => p.playerId),
      startDate: formValue.startDate
    };

    // TODO: Add doubles tournament support if needed

    this.tournamentService.createTournament(tournamentRequest).subscribe({
      next: (createdTournament) => {
        this.loading = false;
        if (createdTournament && createdTournament.id) {
          this.router.navigate(['/tournaments', createdTournament.id]);
        }
      },
      error: (err) => {
        this.error = err.error?.message || 'Failed to create tournament.';
        this.loading = false;
        console.error(err);
      }
    });
  }

  canCreateTournament(): boolean {
    const minPlayers = 2; // Simplified for now
    const validPlayerCount = this.selectedPlayers.length >= minPlayers;

    // Simplified for now - no doubles support
    return this.tournamentForm.valid && validPlayerCount;
  }

  getPlayerCountRequirement(): string {
    return 'at least 2 players';
  }

  private _filterPlayers(value: string | Player): Player[] {
    const filterValue = typeof value === 'string' ? value.toLowerCase() : '';

    // Filter players that aren't already selected
    return this.players.filter(player =>
      // Not already selected
      !this.selectedPlayers.some(p => p.playerId === player.playerId) &&
      // Match search string
      (player.username.toLowerCase().includes(filterValue) ||
        player.firstName.toLowerCase().includes(filterValue) ||
        player.lastName.toLowerCase().includes(filterValue))
    );
  }
}
