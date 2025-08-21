// src/app/tournament/tournament-create/tournament-create.component.ts

import {Component, OnInit} from '@angular/core';
import {FormBuilder, FormGroup, Validators} from '@angular/forms';
import {Router} from '@angular/router';
import {Observable} from 'rxjs';
import {map, startWith} from 'rxjs/operators';

import {TournamentService} from '../../_services/tournament.service';
import {PlayerService} from '../../_services/player.service';
import {AccountService} from '../../_services/account.service';
import {GameType, TeamPair, TournamentRequestDTO, TournamentType} from '../../_models/tournament';
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
  teamPairs: TeamPair[] = [];

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
    {value: TournamentType.DOUBLE_ELIMINATION, label: 'Double Elimination'},
    {value: TournamentType.ROUND_ROBIN, label: 'Round Robin'}
  ];

  gameTypes = [
    {value: GameType.SINGLES, label: 'Singles'},
    {value: GameType.DOUBLES, label: 'Doubles'}
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
      gameType: [GameType.SINGLES, Validators.required],
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
    const tournamentRequest: TournamentRequestDTO = {
      name: formValue.name,
      description: formValue.description,
      tournamentType: formValue.tournamentType,
      gameType: formValue.gameType,
      organizerId: currentPlayer.playerId,
      startDate: formValue.startDate,
      playerIds: this.selectedPlayers.map(p => p.playerId),
      teamPairs: [], // Will be populated for doubles tournaments
      seedingMethod: "RANDOM"
    };

    // For doubles tournaments, create team pairs
    if (formValue.gameType === GameType.DOUBLES) {
      if (this.selectedPlayers.length % 2 !== 0) {
        this.error = 'Doubles tournaments require an even number of players.';
        this.loading = false;
        return;
      }

      // For now, automatically pair consecutive players
      for (let i = 0; i < this.selectedPlayers.length; i += 2) {
        if (i + 1 < this.selectedPlayers.length) {
          tournamentRequest.teamPairs!.push({
            player1Id: this.selectedPlayers[i].playerId,
            player2Id: this.selectedPlayers[i + 1].playerId
          });
        }
      }
    }

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
    const minPlayers = this.tournamentForm.get('gameType')?.value === GameType.DOUBLES ? 4 : 2;
    const validPlayerCount = this.selectedPlayers.length >= minPlayers;

    if (this.tournamentForm.get('gameType')?.value === GameType.DOUBLES) {
      return this.tournamentForm.valid && validPlayerCount && this.selectedPlayers.length % 2 === 0;
    }

    return this.tournamentForm.valid && validPlayerCount;
  }

  getPlayerCountRequirement(): string {
    if (this.tournamentForm.get('gameType')?.value === GameType.DOUBLES) {
      return 'at least 4 players (even number required)';
    }
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
