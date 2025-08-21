// src/app/tournament/tournament-bracket/tournament-bracket.component.ts
import { Component, Input, Output, EventEmitter, OnChanges, SimpleChanges } from '@angular/core';
import { Match, Participant, Stage, MatchStatus } from '../../_models/tournament-new';

@Component({
  selector: 'app-tournament-bracket',
  templateUrl: './tournament-bracket.component.html',
  styleUrls: ['./tournament-bracket.component.scss'],
  standalone: false
})
export class TournamentBracketComponent implements OnChanges {
  @Input() matches: Match[] = [];
  @Input() participants: Participant[] = [];
  @Input() stages: Stage[] = [];
  @Input() canUpdateResults = false;
  @Output() matchClicked = new EventEmitter<Match>();

  // Organized bracket structure for display
  bracketRounds: Map<number, Match[]> = new Map();
  maxRounds = 0;
  matchStatus = MatchStatus;
  
  // For double elimination: separate winner/loser brackets
  winnerBracketRounds: Map<number, Match[]> = new Map();
  loserBracketRounds: Map<number, Match[]> = new Map();
  finalRounds: Match[] = [];
  isDoubleElimination = false;

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['matches'] || changes['participants'] || changes['stages']) {
      this.organizeBracket();
    }
  }

  private organizeBracket(): void {
    console.log('organizeBracket called with:', { 
      matches: this.matches?.length || 0, 
      participants: this.participants?.length || 0,
      stages: this.stages?.length || 0
    });
    
    if (!this.matches || this.matches.length === 0) {
      console.log('No matches found, clearing bracket data');
      this.bracketRounds = new Map();
      this.winnerBracketRounds = new Map();
      this.loserBracketRounds = new Map();
      this.finalRounds = [];
      return;
    }

    // Filter out bye matches (matches where opponent2 is null)
    const playableMatches = this.matches.filter(match => {
      const isBye = match.opponent2 === null || match.opponent2?.id === null;
      if (isBye) {
        console.log('Filtering out bye match:', match.id, 'opponent1:', match.opponent1?.id);
      }
      return !isBye;
    });
    
    console.log('Filtered matches:', { 
      total: this.matches.length, 
      playable: playableMatches.length,
      filtered: this.matches.length - playableMatches.length
    });

    // Check if this is a double elimination tournament
    this.isDoubleElimination = this.stages?.some(
      stage => stage.type === 'double_elimination'
    );

    // Group playable matches by round
    const roundsMap = new Map<number, Match[]>();
    this.maxRounds = 0;
    
    playableMatches.forEach(match => {
      const roundId = match.round_id;
      if (!roundsMap.has(roundId)) {
        roundsMap.set(roundId, []);
      }
      roundsMap.get(roundId)!.push(match);
      this.maxRounds = Math.max(this.maxRounds, roundId);
    });

    console.log('Tournament type:', this.isDoubleElimination ? 'Double Elimination' : 'Single Elimination');
    console.log('Rounds map:', roundsMap);
    
    if (this.isDoubleElimination) {
      this.organizeDoubleEliminationBracket(roundsMap);
    } else {
      // For single elimination, keep it simple
      this.bracketRounds = new Map(
        Array.from(roundsMap.entries())
          .map(([roundId, matches]) => [
            roundId,
            matches.sort((a, b) => a.number - b.number)
          ])
      );
    }
    
    console.log('Final bracket organization:', {
      bracketRounds: this.bracketRounds.size,
      winnerBracketRounds: this.winnerBracketRounds.size,
      loserBracketRounds: this.loserBracketRounds.size,
      finalRounds: this.finalRounds.length
    });
  }

  private organizeDoubleEliminationBracket(roundsMap: Map<number, Match[]>): void {
    // Clear previous data
    this.winnerBracketRounds = new Map();
    this.loserBracketRounds = new Map();
    this.finalRounds = [];
    
    // Determine the midpoint for separating winner's and loser's brackets
    const totalRounds = this.maxRounds;
    const midpoint = Math.ceil(totalRounds / 2);

    // Organize matches into winner's bracket, loser's bracket and finals
    Array.from(roundsMap.entries()).forEach(([roundId, matches]) => {
      const sortedMatches = matches.sort((a, b) => a.number - b.number);
      
      // Finals are usually the highest round numbers
      if (roundId >= totalRounds - 1) {
        this.finalRounds.push(...sortedMatches);
      }
      // Winner's bracket is usually the first half of rounds
      else if (roundId <= midpoint) {
        this.winnerBracketRounds.set(roundId, sortedMatches);
      } 
      // Loser's bracket is the middle rounds
      else {
        this.loserBracketRounds.set(roundId, sortedMatches);
      }
    });

    // Also organize into a single map for single-bracket view
    this.bracketRounds = new Map(
      Array.from(roundsMap.entries())
        .map(([roundId, matches]) => [
          roundId,
          matches.sort((a, b) => a.number - b.number)
        ])
    );
  }

  getParticipantName(participantId: number | null): string {
    if (participantId === null) return 'TBD';
    const participant = this.participants.find(p => p.id === participantId);
    return participant ? participant.name : `Player ${participantId}`;
  }

  onMatchClick(match: Match): void {
    if (this.canUpdateResults && this.isMatchPlayable(match)) {
      this.matchClicked.emit(match);
    }
  }

  isMatchPlayable(match: Match): boolean {
    return match.status === MatchStatus.READY;
  }

  getMatchStatusClass(match: Match): string {
    switch (match.status) {
      case MatchStatus.COMPLETED:
        return 'completed';
      case MatchStatus.READY:
        return 'ready';
      case MatchStatus.WAITING:
        return 'waiting';
      case MatchStatus.RUNNING:
        return 'running';
      default:
        return 'locked';
    }
  }

  getRoundName(roundId: number): string {
    if (this.maxRounds === roundId) {
      return 'Final';
    } else if (this.maxRounds === roundId + 1) {
      return 'Semifinal';
    } else if (this.maxRounds === roundId + 2) {
      return 'Quarterfinal';
    } else {
      return `Round ${roundId}`;
    }
  }

  getWinnerBracketRoundName(roundId: number): string {
    if (this.winnerBracketRounds.size === roundId) {
      return 'Winner Final';
    } else if (this.winnerBracketRounds.size === roundId + 1) {
      return 'Winner Semifinal';
    } else {
      return `Winner Round ${roundId}`;
    }
  }

  getLoserBracketRoundName(roundId: number): string {
    if (this.loserBracketRounds.size === 1) {
      return 'Loser Final';
    } else {
      return `Loser Round ${roundId - this.winnerBracketRounds.size}`;
    }
  }

  getWinnerBracketRounds(): [number, Match[]][] {
    return Array.from(this.winnerBracketRounds.entries())
      .sort(([a], [b]) => a - b);
  }

  getLoserBracketRounds(): [number, Match[]][] {
    return Array.from(this.loserBracketRounds.entries())
      .sort(([a], [b]) => a - b);
  }

  getSingleBracketRounds(): [number, Match[]][] {
    return Array.from(this.bracketRounds.entries())
      .sort(([a], [b]) => a - b);
  }

  // Check if a match has both opponents
  hasAllOpponents(match: Match): boolean {
    return !!(match.opponent1?.id && match.opponent2?.id);
  }
}