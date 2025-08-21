// src/app/tournament/simple-bracket/simple-bracket.component.ts
import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';
import { Match, Participant } from '../../_models/tournament-new';

@Component({
  selector: 'app-simple-bracket',
  templateUrl: './simple-bracket.component.html',
  styleUrls: ['./simple-bracket.component.scss'],
  standalone: false
})
export class SimpleBracketComponent implements OnChanges {
  @Input() matches: Match[] = [];
  @Input() participants: Participant[] = [];
  @Input() canUpdateResults = false;
  @Output() matchClicked = new EventEmitter<Match>();

  // Organized bracket structure for display
  bracketRounds: Match[][] = [];

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['matches'] || changes['participants']) {
      this.organizeBracket();
    }
  }

  private organizeBracket(): void {
    if (!this.matches || this.matches.length === 0) {
      this.bracketRounds = [];
      return;
    }

    // Group matches by round
    const roundsMap = new Map<number, Match[]>();
    
    this.matches.forEach(match => {
      const roundId = match.round_id;
      if (!roundsMap.has(roundId)) {
        roundsMap.set(roundId, []);
      }
      roundsMap.get(roundId)!.push(match);
    });

    // Convert to array and sort by round
    this.bracketRounds = Array.from(roundsMap.entries())
      .sort(([a], [b]) => a - b)
      .map(([_, matches]) => matches.sort((a, b) => a.number - b.number));
  }

  getParticipantName(participantId: number | null): string {
    if (participantId === null) return 'TBD';
    const participant = this.participants.find(p => p.id === participantId);
    return participant ? participant.name : `Player ${participantId}`;
  }

  onMatchClick(match: Match): void {
    if (this.canUpdateResults) {
      this.matchClicked.emit(match);
    }
  }

  getMatchStatusClass(match: Match): string {
    switch (match.status) {
      case 4: return 'completed'; // COMPLETED
      case 2: return 'ready';     // READY
      case 1: return 'waiting';   // WAITING
      default: return 'locked';   // LOCKED
    }
  }
}