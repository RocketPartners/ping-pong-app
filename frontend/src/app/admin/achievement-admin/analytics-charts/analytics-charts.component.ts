import { Component, OnInit, OnDestroy, ViewChild, ElementRef, Input, OnChanges, SimpleChanges, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { Subject, takeUntil } from 'rxjs';
import { AchievementService } from '../../../_services/achievement.service';
import { AlertService } from '../../../_services/alert.services';
import { AnalyticsSummary, AchievementAnalytics } from '../../../_models/achievement';
import { select, selectAll, pie, arc, scaleBand, scaleLinear, scalePoint, max, axisBottom, axisLeft, line, curveMonotoneX } from 'd3';

interface ChartData {
  label: string;
  value: number;
  category?: string;
  color?: string;
}

@Component({
  selector: 'app-analytics-charts',
  templateUrl: './analytics-charts.component.html',
  styleUrls: ['./analytics-charts.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AnalyticsChartsComponent implements OnInit, OnDestroy, OnChanges {
  @ViewChild('completionChart', { static: true }) completionChart!: ElementRef;
  @ViewChild('difficultyChart', { static: true }) difficultyChart!: ElementRef;
  @ViewChild('trendsChart', { static: true }) trendsChart!: ElementRef;
  @ViewChild('performanceChart', { static: true }) performanceChart!: ElementRef;
  
  @Input() analyticsData: AnalyticsSummary | null = null;
  @Input() refreshTrigger: number = 0;
  
  private destroy$ = new Subject<void>();
  
  // Component state
  loading = false;
  chartData: {
    completion: ChartData[];
    difficulty: ChartData[];
    trends: ChartData[];
    performance: ChartData[];
  } = {
    completion: [],
    difficulty: [],
    trends: [],
    performance: []
  };
  
  // Chart dimensions
  private chartWidth = 350;
  private chartHeight = 250;
  private margin = { top: 20, right: 20, bottom: 40, left: 40 };
  
  constructor(
    private achievementService: AchievementService,
    private alertService: AlertService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    if (this.analyticsData) {
      this.processAnalyticsData();
      this.renderAllCharts();
    } else {
      this.loadAnalyticsData();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['analyticsData'] && this.analyticsData) {
      this.processAnalyticsData();
      this.renderAllCharts();
    }
    if (changes['refreshTrigger']) {
      this.loadAnalyticsData();
    }
  }

  /**
   * Load analytics data from service
   */
  private loadAnalyticsData(): void {
    this.loading = true;
    
    this.achievementService.getAnalyticsSummary()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.analyticsData = data;
          this.processAnalyticsData();
          this.renderAllCharts();
          this.loading = false;
          this.cdr.markForCheck();
        },
        error: (error) => {
          console.error('Error loading analytics data:', error);
          this.alertService.error('Failed to load analytics data');
          this.loading = false;
          this.cdr.markForCheck();
        }
      });
  }

  /**
   * Process analytics data into chart format
   */
  private processAnalyticsData(): void {
    if (!this.analyticsData) return;
    
    // Completion Rate Distribution
    this.chartData.completion = Object.entries(this.analyticsData.completionRateDistribution || {})
      .map(([range, count]) => ({
        label: range,
        value: count,
        color: this.getCompletionRateColor(range)
      }));
    
    // Difficulty Distribution
    this.chartData.difficulty = Object.entries(this.analyticsData.difficultyDistribution || {})
      .map(([difficulty, count]) => ({
        label: this.formatDifficultyLabel(difficulty),
        value: count,
        color: this.getDifficultyColor(difficulty)
      }));
    
    // Recent Trends
    this.chartData.trends = Object.entries(this.analyticsData.recentTrends || {})
      .map(([period, count]) => ({
        label: period,
        value: count,
        color: '#2196f3'
      }));
    
    // Performance Data (custom metrics)
    this.chartData.performance = [
      { label: 'Total Achievements', value: this.analyticsData.totalAchievements || 0, color: '#4caf50' },
      { label: 'Active Players', value: this.analyticsData.totalPlayersWithProgress || 0, color: '#2196f3' },
      { label: 'Completions Today', value: this.analyticsData.completionsToday || 0, color: '#ff9800' },
      { label: 'Avg Completion Rate', value: Math.round(this.analyticsData.averageCompletionRate || 0), color: '#9c27b0' }
    ];
  }

  /**
   * Render all charts
   */
  private renderAllCharts(): void {
    this.renderDonutChart(this.completionChart.nativeElement, this.chartData.completion, 'Completion Rate Distribution');
    this.renderBarChart(this.difficultyChart.nativeElement, this.chartData.difficulty, 'Difficulty Distribution');
    this.renderLineChart(this.trendsChart.nativeElement, this.chartData.trends, 'Recent Trends');
    this.renderMetricsChart(this.performanceChart.nativeElement, this.chartData.performance, 'Key Metrics');
  }

  /**
   * Render donut chart
   */
  private renderDonutChart(element: HTMLElement, data: ChartData[], title: string): void {
    select(element).selectAll('*').remove();
    
    const width = this.chartWidth;
    const height = this.chartHeight;
    const radius = Math.min(width, height) / 2 - 20;
    
    const svg = select(element)
      .append('svg')
      .attr('width', width)
      .attr('height', height)
      .append('g')
      .attr('transform', `translate(${width / 2}, ${height / 2})`);
    
    // Add title
    svg.append('text')
      .attr('text-anchor', 'middle')
      .attr('y', -height / 2 + 15)
      .style('font-size', '14px')
      .style('font-weight', 'bold')
      .style('fill', 'var(--on-surface)')
      .text(title);
    
    if (data.length === 0) {
      svg.append('text')
        .attr('text-anchor', 'middle')
        .attr('y', 0)
        .style('fill', 'var(--on-surface-variant)')
        .text('No data available');
      return;
    }
    
    const pieGen = pie<ChartData>()
      .value(d => d.value)
      .sort(null);
    
    const arcGen = arc<any>()
      .innerRadius(radius * 0.4)
      .outerRadius(radius);
    
    const arcs = svg.selectAll('.arc')
      .data(pieGen(data))
      .enter().append('g')
      .attr('class', 'arc');
    
    arcs.append('path')
      .attr('d', arcGen)
      .attr('fill', d => d.data.color || '#2196f3')
      .attr('stroke', 'var(--surface)')
      .attr('stroke-width', 2)
      .on('mouseover', (event, d) => this.showTooltip(event, `${d.data.label}: ${d.data.value}`))
      .on('mouseout', () => this.hideTooltip());
    
    // Add labels
    arcs.append('text')
      .attr('transform', d => `translate(${arcGen.centroid(d)})`)
      .attr('text-anchor', 'middle')
      .style('font-size', '11px')
      .style('fill', 'white')
      .text(d => d.data.value > 0 ? d.data.value : '');
  }

  /**
   * Render bar chart
   */
  private renderBarChart(element: HTMLElement, data: ChartData[], title: string): void {
    select(element).selectAll('*').remove();
    
    const width = this.chartWidth;
    const height = this.chartHeight;
    const innerWidth = width - this.margin.left - this.margin.right;
    const innerHeight = height - this.margin.top - this.margin.bottom;
    
    const svg = select(element)
      .append('svg')
      .attr('width', width)
      .attr('height', height);
    
    const g = svg.append('g')
      .attr('transform', `translate(${this.margin.left}, ${this.margin.top})`);
    
    // Add title
    svg.append('text')
      .attr('x', width / 2)
      .attr('y', 15)
      .attr('text-anchor', 'middle')
      .style('font-size', '14px')
      .style('font-weight', 'bold')
      .style('fill', 'var(--on-surface)')
      .text(title);
    
    if (data.length === 0) {
      g.append('text')
        .attr('x', innerWidth / 2)
        .attr('y', innerHeight / 2)
        .attr('text-anchor', 'middle')
        .style('fill', 'var(--on-surface-variant)')
        .text('No data available');
      return;
    }
    
    const xScale = scaleBand()
      .domain(data.map(d => d.label))
      .range([0, innerWidth])
      .padding(0.1);
    
    const yScale = scaleLinear()
      .domain([0, max(data, d => d.value) || 0])
      .range([innerHeight, 0]);
    
    // Add axes
    g.append('g')
      .attr('transform', `translate(0, ${innerHeight})`)
      .call(axisBottom(xScale))
      .selectAll('text')
      .style('fill', 'var(--on-surface-variant)')
      .style('font-size', '11px');
    
    g.append('g')
      .call(axisLeft(yScale).ticks(5))
      .selectAll('text')
      .style('fill', 'var(--on-surface-variant)')
      .style('font-size', '11px');
    
    // Add bars
    g.selectAll('.bar')
      .data(data)
      .enter().append('rect')
      .attr('class', 'bar')
      .attr('x', d => xScale(d.label) || 0)
      .attr('y', d => yScale(d.value))
      .attr('width', xScale.bandwidth())
      .attr('height', d => innerHeight - yScale(d.value))
      .attr('fill', d => d.color || '#2196f3')
      .on('mouseover', (event, d) => this.showTooltip(event, `${d.label}: ${d.value}`))
      .on('mouseout', () => this.hideTooltip());
  }

  /**
   * Render line chart
   */
  private renderLineChart(element: HTMLElement, data: ChartData[], title: string): void {
    select(element).selectAll('*').remove();
    
    const width = this.chartWidth;
    const height = this.chartHeight;
    const innerWidth = width - this.margin.left - this.margin.right;
    const innerHeight = height - this.margin.top - this.margin.bottom;
    
    const svg = select(element)
      .append('svg')
      .attr('width', width)
      .attr('height', height);
    
    const g = svg.append('g')
      .attr('transform', `translate(${this.margin.left}, ${this.margin.top})`);
    
    // Add title
    svg.append('text')
      .attr('x', width / 2)
      .attr('y', 15)
      .attr('text-anchor', 'middle')
      .style('font-size', '14px')
      .style('font-weight', 'bold')
      .style('fill', 'var(--on-surface)')
      .text(title);
    
    if (data.length === 0) {
      g.append('text')
        .attr('x', innerWidth / 2)
        .attr('y', innerHeight / 2)
        .attr('text-anchor', 'middle')
        .style('fill', 'var(--on-surface-variant)')
        .text('No data available');
      return;
    }
    
    const xScale = scalePoint()
      .domain(data.map(d => d.label))
      .range([0, innerWidth]);
    
    const yScale = scaleLinear()
      .domain([0, max(data, d => d.value) || 0])
      .range([innerHeight, 0]);
    
    // Add axes
    g.append('g')
      .attr('transform', `translate(0, ${innerHeight})`)
      .call(axisBottom(xScale))
      .selectAll('text')
      .style('fill', 'var(--on-surface-variant)')
      .style('font-size', '11px');
    
    g.append('g')
      .call(axisLeft(yScale).ticks(5))
      .selectAll('text')
      .style('fill', 'var(--on-surface-variant)')
      .style('font-size', '11px');
    
    // Add line
    const lineGen = line<ChartData>()
      .x(d => xScale(d.label) || 0)
      .y(d => yScale(d.value))
      .curve(curveMonotoneX);
    
    g.append('path')
      .datum(data)
      .attr('fill', 'none')
      .attr('stroke', '#2196f3')
      .attr('stroke-width', 2)
      .attr('d', lineGen);
    
    // Add dots
    g.selectAll('.dot')
      .data(data)
      .enter().append('circle')
      .attr('class', 'dot')
      .attr('cx', d => xScale(d.label) || 0)
      .attr('cy', d => yScale(d.value))
      .attr('r', 4)
      .attr('fill', '#2196f3')
      .on('mouseover', (event, d) => this.showTooltip(event, `${d.label}: ${d.value}`))
      .on('mouseout', () => this.hideTooltip());
  }

  /**
   * Render metrics chart (horizontal bars)
   */
  private renderMetricsChart(element: HTMLElement, data: ChartData[], title: string): void {
    select(element).selectAll('*').remove();
    
    const width = this.chartWidth;
    const height = this.chartHeight;
    const innerWidth = width - this.margin.left - this.margin.right;
    const innerHeight = height - this.margin.top - this.margin.bottom;
    
    const svg = select(element)
      .append('svg')
      .attr('width', width)
      .attr('height', height);
    
    const g = svg.append('g')
      .attr('transform', `translate(${this.margin.left}, ${this.margin.top})`);
    
    // Add title
    svg.append('text')
      .attr('x', width / 2)
      .attr('y', 15)
      .attr('text-anchor', 'middle')
      .style('font-size', '14px')
      .style('font-weight', 'bold')
      .style('fill', 'var(--on-surface)')
      .text(title);
    
    if (data.length === 0) {
      g.append('text')
        .attr('x', innerWidth / 2)
        .attr('y', innerHeight / 2)
        .attr('text-anchor', 'middle')
        .style('fill', 'var(--on-surface-variant)')
        .text('No data available');
      return;
    }
    
    const yScale = scaleBand()
      .domain(data.map(d => d.label))
      .range([0, innerHeight])
      .padding(0.1);
    
    const xScale = scaleLinear()
      .domain([0, max(data, d => d.value) || 0])
      .range([0, innerWidth]);
    
    // Add bars
    g.selectAll('.metric-bar')
      .data(data)
      .enter().append('rect')
      .attr('class', 'metric-bar')
      .attr('y', d => yScale(d.label) || 0)
      .attr('x', 0)
      .attr('height', yScale.bandwidth())
      .attr('width', d => xScale(d.value))
      .attr('fill', d => d.color || '#2196f3')
      .on('mouseover', (event, d) => this.showTooltip(event, `${d.label}: ${d.value}`))
      .on('mouseout', () => this.hideTooltip());
    
    // Add labels
    g.selectAll('.metric-label')
      .data(data)
      .enter().append('text')
      .attr('class', 'metric-label')
      .attr('y', d => (yScale(d.label) || 0) + yScale.bandwidth() / 2)
      .attr('x', 5)
      .attr('dy', '0.35em')
      .style('font-size', '11px')
      .style('fill', 'white')
      .style('font-weight', 'bold')
      .text(d => d.label);
    
    // Add values
    g.selectAll('.metric-value')
      .data(data)
      .enter().append('text')
      .attr('class', 'metric-value')
      .attr('y', d => (yScale(d.label) || 0) + yScale.bandwidth() / 2)
      .attr('x', d => xScale(d.value) - 5)
      .attr('dy', '0.35em')
      .attr('text-anchor', 'end')
      .style('font-size', '11px')
      .style('fill', 'white')
      .style('font-weight', 'bold')
      .text(d => d.value);
  }

  /**
   * Show tooltip
   */
  private showTooltip(event: MouseEvent, text: string): void {
    const tooltip = select('body').append('div')
      .attr('class', 'chart-tooltip')
      .style('opacity', 0);
    
    tooltip.transition()
      .duration(200)
      .style('opacity', .9);
    
    tooltip.html(text)
      .style('left', (event.pageX + 10) + 'px')
      .style('top', (event.pageY - 28) + 'px');
  }

  /**
   * Hide tooltip
   */
  private hideTooltip(): void {
    selectAll('.chart-tooltip').remove();
  }

  /**
   * Get color for completion rate ranges
   */
  private getCompletionRateColor(range: string): string {
    if (range.includes('80-100')) return '#4caf50';
    if (range.includes('60-80')) return '#8bc34a';
    if (range.includes('40-60')) return '#ffc107';
    if (range.includes('20-40')) return '#ff9800';
    return '#f44336';
  }

  /**
   * Get color for difficulty levels
   */
  private getDifficultyColor(difficulty: string): string {
    switch (difficulty.toUpperCase()) {
      case 'VERY_EASY': return '#4caf50';
      case 'EASY': return '#8bc34a';
      case 'MODERATE': return '#ffc107';
      case 'HARD': return '#ff9800';
      case 'VERY_HARD': return '#f44336';
      default: return '#2196f3';
    }
  }

  /**
   * Format difficulty labels
   */
  private formatDifficultyLabel(difficulty: string): string {
    return difficulty.replace(/_/g, ' ').toLowerCase()
      .replace(/\b\w/g, l => l.toUpperCase());
  }

  /**
   * Export all charts as SVG
   */
  exportCharts(): void {
    const svgElements = [
      this.completionChart.nativeElement.querySelector('svg'),
      this.difficultyChart.nativeElement.querySelector('svg'),
      this.trendsChart.nativeElement.querySelector('svg'),
      this.performanceChart.nativeElement.querySelector('svg')
    ];
    
    svgElements.forEach((svg, index) => {
      if (svg) {
        const serializer = new XMLSerializer();
        const svgString = serializer.serializeToString(svg);
        
        const blob = new Blob([svgString], { type: 'image/svg+xml' });
        const url = URL.createObjectURL(blob);
        
        const link = document.createElement('a');
        link.href = url;
        link.download = `analytics-chart-${index + 1}.svg`;
        link.click();
        
        URL.revokeObjectURL(url);
      }
    });
  }

  /**
   * Refresh all charts
   */
  refresh(): void {
    this.loadAnalyticsData();
  }
}