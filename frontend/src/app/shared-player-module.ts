import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {PlayerCardComponent} from './player-card/player-card.component';
import {MatCardModule} from '@angular/material/card';
import {MatIconModule} from '@angular/material/icon';
import {MatButtonModule} from '@angular/material/button';
import {MatBadgeModule} from '@angular/material/badge';
import {MatTooltipModule} from '@angular/material/tooltip';

/**
 * Shared module for player-related components
 * Makes it easier to reuse these components across the application
 */
@NgModule({
  declarations: [
    PlayerCardComponent
  ],
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatButtonModule,
    MatBadgeModule,
    MatTooltipModule
  ],
  exports: [
    PlayerCardComponent
  ]
})
export class SharedPlayerModule {
}
