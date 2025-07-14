import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RouterModule} from '@angular/router';
import {PlayerSummaryComponent} from './player-summary/player-summary.component';
import {MatCardModule} from '@angular/material/card';
import {MatIconModule} from '@angular/material/icon';
import {MatButtonModule} from '@angular/material/button';
import {SharedComponentsModule} from './_shared/shared-components.module';

@NgModule({
  declarations: [
    PlayerSummaryComponent
  ],
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    SharedComponentsModule
  ],
  exports: [
    PlayerSummaryComponent
  ]
})
export class SharedDashboardModule {
}
