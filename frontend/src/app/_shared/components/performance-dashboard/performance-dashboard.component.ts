import { Component, OnInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { PerformanceMonitorService, PerformanceMetric, ComponentLoadMetric } from '../../../_services/performance-monitor.service';
import { interval, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

@Component({
  selector: 'app-performance-dashboard',
  templateUrl: './performance-dashboard.component.html',
  styleUrls: ['./performance-dashboard.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PerformanceDashboardComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  private componentStartTime = performance.now();

  // Performance data
  performanceSummary: any = null;
  recentMetrics: PerformanceMetric[] = [];
  slowestComponents: ComponentLoadMetric[] = [];
  coreWebVitals: { [key: string]: number } = {};

  // UI state
  loading = false;
  autoRefresh = true;

  constructor(
    private performanceMonitor: PerformanceMonitorService,
    private cdr: ChangeDetectorRef
  ) { }

  ngOnInit(): void {
    this.performanceMonitor.recordComponentLoad('PerformanceDashboardComponent', this.componentStartTime);
    this.loadPerformanceData();
    
    if (this.autoRefresh) {
      this.startAutoRefresh();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load performance data
   */
  loadPerformanceData(): void {
    this.performanceSummary = this.performanceMonitor.getPerformanceSummary();
    this.recentMetrics = this.performanceSummary.recentMetrics;
    this.slowestComponents = this.performanceSummary.slowestComponents;
    this.coreWebVitals = this.performanceSummary.coreWebVitals;
    this.cdr.markForCheck();
  }

  /**
   * Start auto refresh of performance data
   */
  private startAutoRefresh(): void {
    interval(5000) // Refresh every 5 seconds
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        this.loadPerformanceData();
      });
  }

  /**
   * Clear all performance metrics
   */
  clearMetrics(): void {
    this.performanceMonitor.clearMetrics();
    this.loadPerformanceData();
  }

  /**
   * Toggle auto refresh
   */
  toggleAutoRefresh(): void {
    this.autoRefresh = !this.autoRefresh;
    if (this.autoRefresh) {
      this.startAutoRefresh();
    }
  }

  /**
   * Get performance score based on Core Web Vitals
   */
  getPerformanceScore(): { score: number; rating: string; color: string } {
    const lcp = this.coreWebVitals.lcp || 0;
    const fcp = this.coreWebVitals.fcp || 0;
    
    let score = 100;
    let rating = 'Good';
    let color = 'success';

    // Score based on LCP (Largest Contentful Paint)
    if (lcp > 4000) {
      score -= 40;
      rating = 'Poor';
      color = 'error';
    } else if (lcp > 2500) {
      score -= 20;
      rating = 'Needs Improvement';
      color = 'warning';
    }

    // Score based on FCP (First Contentful Paint)
    if (fcp > 3000) {
      score -= 30;
      rating = 'Poor';
      color = 'error';
    } else if (fcp > 1800) {
      score -= 15;
      rating = 'Needs Improvement';
      color = 'warning';
    }

    // Adjust based on average component load time
    const avgLoadTime = this.performanceSummary?.averageLoadTime || 0;
    if (avgLoadTime > 1000) {
      score -= 20;
      if (rating === 'Good') {
        rating = 'Needs Improvement';
        color = 'warning';
      }
    }

    return { score: Math.max(0, score), rating, color };
  }

  /**
   * Format time in ms to readable format
   */
  formatTime(ms: number): string {
    if (ms < 1000) {
      return `${ms.toFixed(0)}ms`;
    } else if (ms < 60000) {
      return `${(ms / 1000).toFixed(1)}s`;
    } else {
      return `${(ms / 60000).toFixed(1)}m`;
    }
  }

  /**
   * Get metric display color based on performance
   */
  getMetricColor(metricName: string, value: number): string {
    const thresholds: { [key: string]: { warning: number; error: number } } = {
      'analytics_summary_load': { warning: 2000, error: 5000 },
      'component_load': { warning: 500, error: 1000 },
      'lcp': { warning: 2500, error: 4000 },
      'fcp': { warning: 1800, error: 3000 },
      'total_load_time': { warning: 3000, error: 6000 }
    };

    const threshold = thresholds[metricName] || thresholds['component_load'];
    
    if (value >= threshold.error) return 'error';
    if (value >= threshold.warning) return 'warning';
    return 'success';
  }

  /**
   * Export performance data for analysis
   */
  exportPerformanceData(): void {
    const allMetrics = this.performanceMonitor.getAllMetrics();
    const dataStr = JSON.stringify(allMetrics, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `performance-metrics-${new Date().toISOString().split('T')[0]}.json`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }
}