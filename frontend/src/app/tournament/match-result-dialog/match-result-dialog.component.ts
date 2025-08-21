// src/app/tournament/match-result-dialog/match-result-dialog.component.ts
import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Match, Participant } from '../../_models/tournament-new';

export interface MatchResultDialogData {
  match: Match;
  participants: Participant[];
}

export interface MatchResultDialogResult {
  winnerId: number;
  winnerScore: number;
  loserScore: number;
}

@Component({
  selector: 'app-match-result-dialog',
  templateUrl: './match-result-dialog.component.html',
  styleUrls: ['./match-result-dialog.component.scss'],
  standalone: false
})
export class MatchResultDialogComponent {
  resultForm: FormGroup;
  participant1Id: number | null;
  participant2Id: number | null;
  participant1Name: string;
  participant2Name: string;

  constructor(
    private fb: FormBuilder,
    public dialogRef: MatDialogRef<MatchResultDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: MatchResultDialogData
  ) {
    // Get participant information
    this.participant1Id = this.data.match.opponent1?.id || null;
    this.participant2Id = this.data.match.opponent2?.id || null;
    
    this.participant1Name = this.getParticipantName(this.participant1Id);
    this.participant2Name = this.getParticipantName(this.participant2Id);

    // Initialize form with validation
    this.resultForm = this.fb.group({
      winner: [null, Validators.required],
      winnerScore: [11, [Validators.required, Validators.min(0), Validators.max(99)]],
      loserScore: [0, [Validators.required, Validators.min(0), Validators.max(99)]]
    });

    // Set default selection if match already has result
    if (this.data.match.opponent1?.result === 'win') {
      this.resultForm.patchValue({ winner: this.participant1Id });
    } else if (this.data.match.opponent2?.result === 'win') {
      this.resultForm.patchValue({ winner: this.participant2Id });
    }
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSubmit(): void {
    if (this.resultForm.valid) {
      const formValue = this.resultForm.value;
      const result: MatchResultDialogResult = {
        winnerId: formValue.winner,
        winnerScore: formValue.winnerScore,
        loserScore: formValue.loserScore
      };
      this.dialogRef.close(result);
    }
  }

  private getParticipantName(participantId: number | null): string {
    if (participantId === null) return 'TBD';
    const participant = this.data.participants.find(p => p.id === participantId);
    return participant ? participant.name : `Player ${participantId}`;
  }

  switchWinner(): void {
    const currentWinner = this.resultForm.get('winner')?.value;
    
    // Toggle winner between participant1 and participant2
    if (currentWinner === this.participant1Id && this.participant2Id) {
      this.resultForm.get('winner')?.setValue(this.participant2Id);
    } else if (this.participant1Id) {
      this.resultForm.get('winner')?.setValue(this.participant1Id);
    }
  }

  quickScore(winnerScore: number, loserScore: number): void {
    this.resultForm.patchValue({
      winnerScore: winnerScore,
      loserScore: loserScore
    });
  }
}