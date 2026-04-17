import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {KioskLayoutComponent} from './kiosk-layout/kiosk-layout.component';
import {AttractScreenComponent} from './attract-screen/attract-screen.component';
import {MatchSetupComponent} from './match-setup/match-setup.component';
import {PlayerPickerComponent} from './player-picker/player-picker.component';
import {LiveScoringComponent} from './live-scoring/live-scoring.component';
import {MatchSummaryComponent} from './match-summary/match-summary.component';
import {PairingComponent} from './pairing/pairing.component';
import {KioskReadyGuard} from './_guards/kiosk-ready.guard';

const routes: Routes = [
  {
    path: '',
    component: KioskLayoutComponent,
    children: [
      {path: 'pair', component: PairingComponent},
      {path: '', component: AttractScreenComponent, canActivate: [KioskReadyGuard]},
      {path: 'setup', component: MatchSetupComponent, canActivate: [KioskReadyGuard]},
      {path: 'players', component: PlayerPickerComponent, canActivate: [KioskReadyGuard]},
      {path: 'live', component: LiveScoringComponent, canActivate: [KioskReadyGuard]},
      {path: 'summary', component: MatchSummaryComponent, canActivate: [KioskReadyGuard]}
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class KioskRoutingModule {}
