import { Injectable } from '@angular/core';

export interface PerformanceMetric {
  name: string;
  value: number;
  timestamp: number;
  url?: string;
  metadata?: { [key: string]: any };
}

export interface ComponentLoadMetric {
  componentName: string;
  loadTime: number;
  timestamp: number;
}

@Injectable({
  providedIn: 'root'
})
export class PerformanceMonitorService {
  private metrics: PerformanceMetric[] = [];
  private componentLoadTimes: ComponentLoadMetric[] = [];
  private performanceObserver?: PerformanceObserver;

  constructor() {
    this.initializePerformanceObserver();
    this.measureInitialLoad();
  }

  /**
   * Initialize performance observer for monitoring
   */
  private initializePerformanceObserver(): void {
    if ('PerformanceObserver' in window) {
      this.performanceObserver = new PerformanceObserver((list) => {
        const entries = list.getEntries();
        
        entries.forEach(entry => {
          switch (entry.entryType) {
            case 'navigation':
              this.recordMetric('page_load_time', entry.duration);
              break;
            case 'resource':
              if (entry.name.includes('.js') || entry.name.includes('.css')) {
                this.recordMetric('resource_load_time', entry.duration, {
                  resourceName: entry.name,
                  resourceType: entry.name.includes('.js') ? 'javascript' : 'stylesheet'
                });
              }
              break;
            case 'paint':
              this.recordMetric(entry.name.replace('-', '_'), entry.startTime);
              break;
            case 'largest-contentful-paint':
              this.recordMetric('lcp', entry.startTime);
              break;
          }
        });
      });

      // Observe different types of performance entries
      try {
        this.performanceObserver.observe({ type: 'navigation', buffered: true });
        this.performanceObserver.observe({ type: 'resource', buffered: true });
        this.performanceObserver.observe({ type: 'paint', buffered: true });
        this.performanceObserver.observe({ type: 'largest-contentful-paint', buffered: true });
      } catch (error) {
        console.warn('Some performance observation types not supported:', error);
      }
    }
  }

  /**
   * Measure initial page load metrics
   */
  private measureInitialLoad(): void {
    if ('performance' in window && performance.timing) {
      const timing = performance.timing;
      
      // Core Web Vitals and loading metrics
      const loadCompleteTime = timing.loadEventEnd - timing.navigationStart;
      const domContentLoadedTime = timing.domContentLoadedEventEnd - timing.navigationStart;
      const firstByteTime = timing.responseStart - timing.navigationStart;
      
      this.recordMetric('total_load_time', loadCompleteTime);
      this.recordMetric('dom_content_loaded', domContentLoadedTime);
      this.recordMetric('time_to_first_byte', firstByteTime);
    }
  }

  /**
   * Record a performance metric
   */
  recordMetric(name: string, value: number, metadata?: { [key: string]: any }): void {
    const metric: PerformanceMetric = {
      name,
      value,
      timestamp: Date.now(),
      url: window.location.pathname,
      metadata
    };

    this.metrics.push(metric);

    // Log significant metrics to console in development
    if (!this.isProduction()) {
      if (value > 1000 || name.includes('error') || name.includes('slow')) {
        console.warn(`Performance Alert: ${name} = ${value.toFixed(2)}ms`, metadata);
      }
    }

    // Keep only last 100 metrics to prevent memory issues
    if (this.metrics.length > 100) {
      this.metrics = this.metrics.slice(-100);
    }
  }

  /**
   * Record component load time
   */
  recordComponentLoad(componentName: string, startTime: number): void {
    const loadTime = performance.now() - startTime;
    
    const metric: ComponentLoadMetric = {
      componentName,
      loadTime,
      timestamp: Date.now()
    };

    this.componentLoadTimes.push(metric);
    this.recordMetric(`component_load_${componentName.toLowerCase()}`, loadTime);

    // Keep only last 50 component metrics
    if (this.componentLoadTimes.length > 50) {
      this.componentLoadTimes = this.componentLoadTimes.slice(-50);
    }
  }

  /**
   * Measure async operation duration
   */
  measureAsyncOperation<T>(operationName: string, operation: Promise<T>): Promise<T> {
    const startTime = performance.now();
    
    return operation
      .then(result => {
        const duration = performance.now() - startTime;
        this.recordMetric(`async_${operationName}`, duration);
        return result;
      })
      .catch(error => {
        const duration = performance.now() - startTime;
        this.recordMetric(`async_${operationName}_error`, duration, { error: error.message });
        throw error;
      });
  }

  /**
   * Get performance summary
   */
  getPerformanceSummary(): {
    averageLoadTime: number;
    slowestComponents: ComponentLoadMetric[];
    recentMetrics: PerformanceMetric[];
    coreWebVitals: { [key: string]: number };
  } {
    const componentLoadTimes = this.componentLoadTimes.slice(-10);
    const avgLoadTime = componentLoadTimes.length > 0 
      ? componentLoadTimes.reduce((sum, metric) => sum + metric.loadTime, 0) / componentLoadTimes.length
      : 0;

    const slowestComponents = [...this.componentLoadTimes]
      .sort((a, b) => b.loadTime - a.loadTime)
      .slice(0, 5);

    const recentMetrics = this.metrics.slice(-20);

    // Extract Core Web Vitals
    const coreWebVitals: { [key: string]: number } = {};
    const lcpMetric = this.metrics.find(m => m.name === 'lcp');
    const fcpMetric = this.metrics.find(m => m.name === 'first_contentful_paint');
    
    if (lcpMetric) coreWebVitals.lcp = lcpMetric.value;
    if (fcpMetric) coreWebVitals.fcp = fcpMetric.value;

    return {
      averageLoadTime: avgLoadTime,
      slowestComponents,
      recentMetrics,
      coreWebVitals
    };
  }

  /**
   * Clear all metrics (useful for testing)
   */
  clearMetrics(): void {
    this.metrics = [];
    this.componentLoadTimes = [];
  }

  /**
   * Get all metrics for external analysis
   */
  getAllMetrics(): {
    metrics: PerformanceMetric[];
    componentLoadTimes: ComponentLoadMetric[];
  } {
    return {
      metrics: [...this.metrics],
      componentLoadTimes: [...this.componentLoadTimes]
    };
  }

  private isProduction(): boolean {
    return false; // Always false for debugging during development
  }

  /**
   * Cleanup resources
   */
  destroy(): void {
    if (this.performanceObserver) {
      this.performanceObserver.disconnect();
    }
  }
}