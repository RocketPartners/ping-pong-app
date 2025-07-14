// src/app/tournament/tournament.module.ts

import {NgModule} from '@angular/core';
import {CommonModule} from '@angular/common';
// Routes
import {RouterModule, Routes} from '@angular/router';
import {FormsModule, ReactiveFormsModule} from '@angular/forms';

// Material imports
import {MatCardModule} from '@angular/material/card';
import {MatButtonModule} from '@angular/material/button';
import {MatIconModule} from '@angular/material/icon';
import {MatTabsModule} from '@angular/material/tabs';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatSelectModule} from '@angular/material/select';
import {MatCheckboxModule} from '@angular/material/checkbox';
import {MatProgressBarModule} from '@angular/material/progress-bar';
import {MatChipsModule} from '@angular/material/chips';
import {MatDialogModule} from '@angular/material/dialog';
import {MatDividerModule} from '@angular/material/divider';
import {MatAutocompleteModule} from '@angular/material/autocomplete';
import {MatProgressSpinnerModule} from '@angular/material/progress-spinner';
import {MatMenuModule} from '@angular/material/menu';
import {MatTooltipModule} from '@angular/material/tooltip';
import {MatDatepickerModule} from '@angular/material/datepicker';
import {MatNativeDateModule} from '@angular/material/core';
import {MatSlideToggleModule} from '@angular/material/slide-toggle';
import {MatButtonToggleModule} from '@angular/material/button-toggle';

// Shared components
import {SharedComponentsModule} from '../_shared/shared-components.module';

// Tournament components
import {TournamentListComponent} from './tournament-list/tournament-list.component';
import {TournamentCreateComponent} from './tournament-create/tournament-create.component';
import {TournamentDetailComponent} from './tournament-detail/tournament-detail.component';
import {AuthGuard} from '../_helpers/auth.guard';

export const tournamentRoutes: Routes = [
  {path: 'tournaments', component: TournamentListComponent, canActivate: [AuthGuard]},
  {path: 'tournaments/create', component: TournamentCreateComponent, canActivate: [AuthGuard]},
  {path: 'tournaments/:id', component: TournamentDetailComponent, canActivate: [AuthGuard]}
];

@NgModule({
  declarations: [
    TournamentListComponent,
    TournamentCreateComponent,
    TournamentDetailComponent
  ],
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    ReactiveFormsModule,

    // Material modules
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatProgressBarModule,
    MatChipsModule,
    MatDialogModule,
    MatDividerModule,
    MatAutocompleteModule,
    MatProgressSpinnerModule,
    MatMenuModule,
    MatTooltipModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatSlideToggleModule,
    MatButtonToggleModule,

    // Shared modules
    SharedComponentsModule
  ],
  exports: [
    TournamentListComponent,
    TournamentCreateComponent,
    TournamentDetailComponent
  ]
})
export class TournamentModule {
}
