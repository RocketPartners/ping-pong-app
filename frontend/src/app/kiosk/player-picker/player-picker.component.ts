import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {PlayerService} from '../../_services/player.service';
import {Player} from '../../_models/models';
import {KioskMatchConfig, LiveGameStateService} from '../_services/live-game-state.service';

type SlotIndex = number;

@Component({
  selector: 'app-kiosk-player-picker',
  templateUrl: './player-picker.component.html',
  styleUrls: ['./player-picker.component.scss'],
  standalone: false
})
export class PlayerPickerComponent implements OnInit {
  config: KioskMatchConfig | null = null;
  slots: (Player | null)[] = [null, null];
  allPlayers: Player[] = [];

  pickingSlot: SlotIndex | null = null;
  searchTerm = '';
  loading = false;
  error: string | null = null;

  constructor(
    private router: Router,
    private state: LiveGameStateService,
    private playerService: PlayerService
  ) {}

  ngOnInit(): void {
    const snapshot = this.state.snapshot;
    this.config = snapshot.config;
    if (!this.config) {
      this.router.navigate(['/kiosk/setup']);
      return;
    }
    this.slots = this.config.matchType === 'singles'
      ? [snapshot.team1[0] || null, snapshot.team2[0] || null]
      : [snapshot.team1[0] || null, snapshot.team1[1] || null, snapshot.team2[0] || null, snapshot.team2[1] || null];

    this.playerService.getPlayers().subscribe(players => {
      this.allPlayers = (players || []).filter(p => p.username !== 'kiosk-system');
    });
  }

  get slotLabels(): string[] {
    return this.config?.matchType === 'singles'
      ? ['Player 1', 'Player 2']
      : ['Team A — Player 1', 'Team A — Player 2', 'Team B — Player 1', 'Team B — Player 2'];
  }

  get canSubmit(): boolean {
    return this.slots.every(p => !!p);
  }

  get allowGuests(): boolean {
    return !(this.config?.isRanked ?? true);
  }

  openPicker(slot: SlotIndex): void {
    this.pickingSlot = slot;
    this.searchTerm = '';
    this.error = null;
  }

  closePicker(): void {
    this.pickingSlot = null;
  }

  get filteredPlayers(): Player[] {
    const taken = new Set(
      this.slots.filter((p, idx) => !!p && idx !== this.pickingSlot).map(p => p!.playerId)
    );
    const term = this.searchTerm.trim().toLowerCase();
    return this.allPlayers
      .filter(p => !taken.has(p.playerId))
      .filter(p => {
        if (!term) return true;
        return (
          p.username?.toLowerCase().includes(term) ||
          p.firstName?.toLowerCase().includes(term) ||
          p.lastName?.toLowerCase().includes(term)
        );
      })
      .slice(0, 40);
  }

  pickPlayer(player: Player): void {
    if (this.pickingSlot === null) return;
    this.slots[this.pickingSlot] = player;
    this.closePicker();
  }

  createGuest(): void {
    const name = (this.searchTerm || '').trim();
    if (!name) {
      this.error = 'Type a name for the guest.';
      return;
    }
    if (!this.allowGuests) {
      this.error = 'Guests are not allowed in ranked matches.';
      return;
    }
    this.loading = true;
    this.playerService.createAnonymousPlayer(name).subscribe({
      next: player => {
        this.loading = false;
        if (player) {
          this.allPlayers = [...this.allPlayers, player];
          this.pickPlayer(player);
        }
      },
      error: (err: any) => {
        this.loading = false;
        this.error = err?.error?.message || err?.message || 'Could not create guest.';
      }
    });
  }

  removeSlot(slot: SlotIndex): void {
    this.slots[slot] = null;
  }

  startMatch(): void {
    if (!this.canSubmit || !this.config) return;
    const picked = this.slots as Player[];
    const team1 = this.config.matchType === 'singles' ? [picked[0]] : [picked[0], picked[1]];
    const team2 = this.config.matchType === 'singles' ? [picked[1]] : [picked[2], picked[3]];
    this.state.setTeams(team1, team2);
    this.router.navigate(['/kiosk/live']);
  }

  back(): void {
    this.router.navigate(['/kiosk/setup']);
  }
}
