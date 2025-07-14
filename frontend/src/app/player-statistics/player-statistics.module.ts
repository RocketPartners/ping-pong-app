import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RouterModule} from '@angular/router';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {BaseChartDirective} from 'ng2-charts';
import {PlayerStatisticsComponent} from './player-statistics.component';
import {SharedComponentsModule} from '../_shared/shared-components.module';

// Angular Material Imports
import {MatTabsModule} from '@angular/material/tabs';
import {MatButtonModule} from '@angular/material/button';
import {MatCardModule} from '@angular/material/card';
import {MatIconModule} from '@angular/material/icon';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatTooltipModule} from '@angular/material/tooltip';
import {MatButtonToggleModule} from '@angular/material/button-toggle';
import {MatDividerModule} from '@angular/material/divider';
import {MatFormFieldModule} from '@angular/material/form-field'; // Add this
import {MatInputModule} from '@angular/material/input'; // Add this
import {initializeChartJs} from '../_config/chart-config';
import {MatAutocompleteModule} from "@angular/material/autocomplete";
import {FormControlsModule} from "../_shared/form-controls.module";

@NgModule({
  declarations: [
    PlayerStatisticsComponent
  ],
  imports: [
    // Angular Core
    CommonModule,
    RouterModule,
    FormsModule,

    ReactiveFormsModule,
    MatAutocompleteModule,
    // Chart.js
    BaseChartDirective,

    // Shared Components
    SharedComponentsModule,

    // Angular Material
    MatTabsModule,
    MatButtonModule,
    MatCardModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatButtonToggleModule,
    MatDividerModule,
    MatFormFieldModule,
    MatInputModule,
    FormControlsModule,
  ],
  exports: [
    PlayerStatisticsComponent
  ],
})
export class PlayerStatisticsModule {
  constructor() {
    // Initialize Chart.js with global settings
    initializeChartJs();
  }
}
