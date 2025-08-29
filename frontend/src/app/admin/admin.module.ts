import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';

// Angular Material Modules
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { MatSortModule } from '@angular/material/sort';
import { MatPaginatorModule } from '@angular/material/paginator';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatBadgeModule } from '@angular/material/badge';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialogModule } from '@angular/material/dialog';
import { MatSnackBarModule } from '@angular/material/snack-bar';

// Admin Components
import { AchievementAdminComponent } from './achievement-admin/achievement-admin.component';
import { ConfigManagerComponent } from './achievement-admin/config-manager/config-manager.component';
import { AnalyticsDashboardComponent } from './achievement-admin/analytics-dashboard/analytics-dashboard.component';
import { PerformanceMonitorComponent } from './achievement-admin/performance-monitor/performance-monitor.component';
import { DependencyTreeComponent } from './achievement-admin/dependency-tree/dependency-tree.component';
import { AnalyticsChartsComponent } from './achievement-admin/analytics-charts/analytics-charts.component';

// Shared Components
import { SharedComponentsModule } from '../_shared/shared-components.module';

@NgModule({
  declarations: [
    AchievementAdminComponent,
    ConfigManagerComponent,
    AnalyticsDashboardComponent,
    PerformanceMonitorComponent,
    DependencyTreeComponent,
    AnalyticsChartsComponent
  ],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    RouterModule.forChild([
      {
        path: '',
        component: AchievementAdminComponent,
        children: [
          { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
          { path: 'dashboard', component: AnalyticsDashboardComponent },
          { path: 'config', component: ConfigManagerComponent },
          { path: 'monitor', component: PerformanceMonitorComponent },
          { path: 'analytics', component: AnalyticsChartsComponent },
          { path: 'dependencies', component: DependencyTreeComponent }
        ]
      }
    ]),
    
    // Angular Material
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatTableModule,
    MatSortModule,
    MatPaginatorModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    MatChipsModule,
    MatBadgeModule,
    MatTooltipModule,
    MatDialogModule,
    MatSnackBarModule,
    
    // Shared Components
    SharedComponentsModule
  ]
})
export class AdminModule { }