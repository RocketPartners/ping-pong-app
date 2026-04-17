import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {FormsModule} from '@angular/forms';
import {KioskRoutingModule} from './kiosk-routing.module';
import {KioskLayoutComponent} from './kiosk-layout/kiosk-layout.component';
import {AttractScreenComponent} from './attract-screen/attract-screen.component';
import {MatchSetupComponent} from './match-setup/match-setup.component';
import {PlayerPickerComponent} from './player-picker/player-picker.component';
import {LiveScoringComponent} from './live-scoring/live-scoring.component';
import {MatchSummaryComponent} from './match-summary/match-summary.component';
import {PairingComponent} from './pairing/pairing.component';

@NgModule({
  declarations: [
    KioskLayoutComponent,
    AttractScreenComponent,
    MatchSetupComponent,
    PlayerPickerComponent,
    LiveScoringComponent,
    MatchSummaryComponent,
    PairingComponent
  ],
  imports: [CommonModule, FormsModule, KioskRoutingModule]
})
export class KioskModule {}
