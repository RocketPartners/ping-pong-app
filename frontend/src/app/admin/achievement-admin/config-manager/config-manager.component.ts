import { Component, OnInit, OnDestroy } from '@angular/core';
import { Subject, takeUntil, finalize } from 'rxjs';
import { AchievementService } from '../../../_services/achievement.service';
import { AlertService } from '../../../_services/alert.services';
import { AchievementConfiguration } from '../../../_models/achievement';

@Component({
  selector: 'app-config-manager',
  templateUrl: './config-manager.component.html',
  styleUrls: ['./config-manager.component.scss'],
  standalone: false
})
export class ConfigManagerComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  
  loading = false;
  loadedConfigurations: { [key: string]: AchievementConfiguration } = {};
  validationResults: any = null;
  selectedFileName = 'achievements-config.yaml';

  constructor(
    private achievementService: AchievementService,
    private alertService: AlertService
  ) { }

  ngOnInit(): void {
    this.loadConfigurations();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load current configurations
   */
  loadConfigurations(): void {
    this.loading = true;
    
    this.achievementService.getLoadedConfigurations()
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.loading = false)
      )
      .subscribe({
        next: (configs) => {
          this.loadedConfigurations = configs;
        },
        error: (error) => {
          console.error('Error loading configurations:', error);
          this.alertService.error('Failed to load configurations');
        }
      });
  }

  /**
   * Load and apply configurations from file
   */
  loadAndApplyConfig(): void {
    this.loading = true;
    
    this.achievementService.loadAndApplyConfig(this.selectedFileName)
      .pipe(
        takeUntil(this.destroy$),
        finalize(() => this.loading = false)
      )
      .subscribe({
        next: (response) => {
          this.alertService.success(response.message || 'Configurations loaded and applied successfully');
          this.loadConfigurations();
          this.validateConfigurations();
        },
        error: (error) => {
          console.error('Error loading and applying configurations:', error);
          this.alertService.error('Failed to load and apply configurations');
        }
      });
  }

  /**
   * Validate current configurations
   */
  validateConfigurations(): void {
    this.achievementService.validateConfigurations()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (results) => {
          this.validationResults = results;
          if (results.success) {
            this.alertService.success('All configurations are valid');
          } else {
            this.alertService.error(`${results.configurationWithErrors} configurations have errors`);
          }
        },
        error: (error) => {
          console.error('Error validating configurations:', error);
          this.alertService.error('Failed to validate configurations');
        }
      });
  }

  /**
   * Export configurations to YAML
   */
  exportConfigurations(): void {
    this.achievementService.exportConfigurations()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (yamlContent) => {
          this.downloadYamlFile(yamlContent);
          this.alertService.success('Configurations exported successfully');
        },
        error: (error) => {
          console.error('Error exporting configurations:', error);
          this.alertService.error('Failed to export configurations');
        }
      });
  }

  /**
   * Download YAML content as file
   */
  private downloadYamlFile(content: string): void {
    const blob = new Blob([content], { type: 'text/yaml' });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = 'achievements-export.yaml';
    link.click();
    window.URL.revokeObjectURL(url);
  }

  /**
   * Get configuration keys
   */
  getConfigurationKeys(): string[] {
    return Object.keys(this.loadedConfigurations);
  }

  /**
   * Get validation error keys
   */
  getValidationErrorKeys(): string[] {
    return this.validationResults?.validationErrors ? 
           Object.keys(this.validationResults.validationErrors) : [];
  }

  /**
   * Get category color for chips
   */
  getCategoryColor(category: string): 'primary' | 'accent' | 'warn' | undefined {
    switch (category?.toUpperCase()) {
      case 'EASY': return 'primary';
      case 'MEDIUM': return 'accent';
      case 'HARD': return 'warn';
      case 'LEGENDARY': return 'warn';
      default: return 'primary';
    }
  }
}