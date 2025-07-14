import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
import {RouterModule} from '@angular/router';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';
import {OverlayModule} from '@angular/cdk/overlay';
import {PortalModule} from '@angular/cdk/portal';

// Angular Material Imports
import {MatCardModule} from '@angular/material/card';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatBadgeModule} from '@angular/material/badge';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {MatTabsModule} from '@angular/material/tabs';
import {MatSelectModule} from '@angular/material/select';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatButtonToggleModule} from '@angular/material/button-toggle';
import {MatMenuModule} from '@angular/material/menu';
import {MatDividerModule} from '@angular/material/divider';
import {MatTooltipModule} from '@angular/material/tooltip';
import {MatDialogModule} from '@angular/material/dialog';

// Shared Components
import {SharedComponentsModule} from '../_shared/shared-components.module';

// Achievement Components
import {AchievementListComponent} from './achievement-list/achievement-list.component';
import {PlayerAchievementsComponent} from './player-achievements/player-achievements.component';
import {AchievementDetailComponent} from './achievement-detail/achievement-detail.component';
import {PlayerService} from "../_services/player.service";
import {MatAutocomplete, MatAutocompleteTrigger} from "@angular/material/autocomplete";
import {FormControlsModule} from "../_shared/form-controls.module";

@NgModule({
  declarations: [
    AchievementListComponent,
    PlayerAchievementsComponent,
    AchievementDetailComponent
  ],
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    ReactiveFormsModule,
    SharedComponentsModule,
    OverlayModule,
    PortalModule,

    // Material modules
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatBadgeModule,
    MatProgressBarModule,
    MatTabsModule,
    MatSelectModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonToggleModule,
    MatMenuModule,
    MatDividerModule,
    MatTooltipModule,
    MatDialogModule,
    FormsModule,
    MatAutocompleteTrigger,
    MatAutocomplete,
    FormControlsModule
  ],
  exports: [
    AchievementListComponent,
    PlayerAchievementsComponent,
    AchievementDetailComponent,
  ],
  providers: [
    PlayerService
  ]
})
export class AchievementModule {
}
