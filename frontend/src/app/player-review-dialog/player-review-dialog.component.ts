import {ChangeDetectionStrategy, ChangeDetectorRef, Component, Inject, OnDestroy, OnInit} from '@angular/core';
import {AbstractControl, FormArray, FormBuilder, FormGroup, Validators} from '@angular/forms';
import {MAT_DIALOG_DATA, MatDialogRef} from '@angular/material/dialog';
import {PlayerReview, PlayerReviewDialogData} from '../_models/models';
import {PlayerStyle} from '../_models/player-style';
import {Subscription} from 'rxjs'; // Import Subscription
import {PLAYER_STYLE_DESCRIPTIONS} from '../player/player-constants'
// Define constants
const REQUIRED_SELECTION_COUNT = 3;


@Component({
  selector: 'app-player-review-dialog',
  templateUrl: './player-review-dialog.component.html',
  styleUrls: ['./player-review-dialog.component.scss'],
  // Consider OnPush for performance if child components allow it
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false // Keeping as provided
})
export class PlayerReviewDialogComponent implements OnInit, OnDestroy {
  reviewForm: FormGroup;
  playerStyles = Object.values(PlayerStyle);
  currentStep = 0;
  dialogTitle = 'Review Your Match Partners'; // Default title

  // Constant for template usage
  readonly requiredSelectionCount = REQUIRED_SELECTION_COUNT;

  private formStatusChangesSubscription: Subscription | null = null; // To manage subscriptions

  constructor(
    private fb: FormBuilder,
    public dialogRef: MatDialogRef<PlayerReviewDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: PlayerReviewDialogData,
    private cdr: ChangeDetectorRef // Inject ChangeDetectorRef
  ) {
  }

  get playerReviews(): FormArray {
    return this.reviewForm.get('playerReviews') as FormArray;
  }

  ngOnInit(): void {
    this.initForm();

    // Set title if this is a response review
    if (this.data.isResponse && this.data.parentReview) {
      this.dialogTitle = `Respond to Review for ${this.getPlayerUsername(this.data.parentReview.playerId) || 'Player'}`;
      // Alternatively: `Response to ${this.data.parentReview.reviewerUsername}'s Review` if you pass that data
    }

    // Subscribe to status changes to manually trigger change detection if needed
    // This helps ensure the Next button's disabled state updates reliably with OnPush
    this.formStatusChangesSubscription = this.reviewForm.statusChanges.subscribe(() => {
      this.cdr.markForCheck(); // Mark component for check when form status changes
    });
  }

  ngOnDestroy(): void {
    // Unsubscribe from subscriptions to prevent memory leaks
    this.formStatusChangesSubscription?.unsubscribe();
  }

  initForm(): void {
    this.reviewForm = this.fb.group({
      playerReviews: this.fb.array([])
    });

    const playersToReview = this.data.players.filter(p => p.playerId !== this.data.currentPlayerId);

    playersToReview.forEach(player => {
      const playerReview = this.fb.group({
        playerId: [player.playerId, Validators.required],
        // Storing potentially display-only data in the form isn't always ideal,
        // but acceptable here for simplicity. Could fetch dynamically if needed.
        playerName: [`${player.firstName} ${player.lastName}`],
        username: [player.username],
        strengths: [[], [
          Validators.required,
          Validators.minLength(REQUIRED_SELECTION_COUNT),
          Validators.maxLength(REQUIRED_SELECTION_COUNT)
        ]],
        improvements: [[], [
          Validators.required,
          Validators.minLength(REQUIRED_SELECTION_COUNT),
          Validators.maxLength(REQUIRED_SELECTION_COUNT)
        ]]
      });
      this.playerReviews.push(playerReview);
    });

    // Initial check in case the form array is empty
    this.cdr.markForCheck();
  }

  getPlayerForm(index: number): FormGroup {
    // Added a check for safety, although typically index should be valid
    if (index < 0 || index >= this.playerReviews.length) {
      // Return an empty FormGroup or handle error appropriately
      console.error("Invalid player index requested:", index);
      return this.fb.group({}); // Or throw an error
    }
    return this.playerReviews.at(index) as FormGroup;
  }

  /**
   * Checks if a style is selected in the strengths list for the current player.
   * Used to disable the corresponding chip in the improvements list.
   */
  isSelectedInStrengths(style: PlayerStyle): boolean {
    const currentForm = this.getPlayerForm(this.currentStep);
    const strengths = currentForm.get('strengths')?.value || [];
    return strengths.includes(style);
  }

  /**
   * Checks if a style is selected in the improvements list for the current player.
   * Used to disable the corresponding chip in the strengths list.
   */
  isSelectedInImprovements(style: PlayerStyle): boolean {
    const currentForm = this.getPlayerForm(this.currentStep);
    const improvements = currentForm.get('improvements')?.value || [];
    return improvements.includes(style);
  }

  submit(): void {
    this.reviewForm.markAllAsTouched(); // Ensure validation messages show if needed
    if (this.reviewForm.valid) {
      // Use map directly on the FormArray's value for cleaner typing
      const reviews: PlayerReview[] = this.playerReviews.value.map((formValue: any): PlayerReview => {
        const reviewData: Partial<PlayerReview> = { // Use Partial initially
          playerId: formValue.playerId,
          strengths: formValue.strengths,
          improvements: formValue.improvements
          // Exclude playerName and username as they are not part of PlayerReview model
        };

        if (this.data.isResponse && this.data.parentReview?.id) {
          reviewData.response = true;
          reviewData.parentReviewId = this.data.parentReview.id;
        }

        // Perform a final cast, assuming the structure aligns
        return reviewData as PlayerReview;
      });

      this.dialogRef.close(reviews);
    } else {
      console.warn('Review form is invalid.');
      // Optional: provide user feedback that the form is invalid
    }
  }

  getStyleName(style: PlayerStyle): string {
    // Consider caching these results if performance becomes an issue (unlikely here)
    return style.replace(/_/g, ' ') // Replace all underscores
      .toLowerCase()
      .split(' ')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1))
      .join(' ');
  }

  getStyleDescription(style: PlayerStyle): string {
    return PLAYER_STYLE_DESCRIPTIONS[style] || 'No description available.'; // Provide fallback
  }

  nextPlayer(): void {
    if (this.currentStep < this.playerReviews.length - 1) {
      if (this.isCurrentFormValid()) { // Only proceed if current step is valid
        this.currentStep++;
        this.cdr.markForCheck(); // Trigger change detection
      } else {
        // Optionally mark the current form as touched to show errors
        this.getPlayerForm(this.currentStep).markAllAsTouched();
        this.cdr.markForCheck(); // Trigger change detection
      }
    }
  }

  previousPlayer(): void {
    if (this.currentStep > 0) {
      this.currentStep--;
      this.cdr.markForCheck(); // Trigger change detection
    }
  }

  isLastPlayer(): boolean {
    return this.currentStep === this.playerReviews.length - 1;
  }

  isFirstPlayer(): boolean {
    return this.currentStep === 0;
  }

  // Check validity of the form group at the current step index
  isCurrentFormValid(): boolean {
    if (this.playerReviews.length === 0) return true; // No forms means valid? Or handle differently.
    const currentForm = this.getPlayerForm(this.currentStep);
    return currentForm.valid;
  }

  // Check validity of the entire FormArray
  isFormValid(): boolean {
    return this.reviewForm.valid;
  }

  // Helper to get the AbstractControl for strength/improvement count display
  getStrengthControl(index: number): AbstractControl | null {
    return this.getPlayerForm(index)?.get('strengths');
  }

  getImprovementControl(index: number): AbstractControl | null {
    return this.getPlayerForm(index)?.get('improvements');
  }

  cancel(): void {
    this.dialogRef.close(); // Close without sending data
  }

  // Add this public helper method
  public isStyleSelected(control: AbstractControl | null, style: PlayerStyle): boolean {
    if (!control || !control.value) {
      return false;
    }
    // Explicitly cast the value to the expected array type
    const selectedStyles = control.value as PlayerStyle[];
    // Now .includes should work correctly with the right types
    return Array.isArray(selectedStyles) && selectedStyles.includes(style);
  }

  // Helper to get username for title setting
  private getPlayerUsername(playerId: string): string | undefined {
    return this.data.players.find(p => p.playerId === playerId)?.username;
  }

// ... rest of your component code
}
