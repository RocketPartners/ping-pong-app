import {Component, ElementRef, Input, OnChanges, OnInit, SimpleChanges, ViewChild} from '@angular/core';

/**
 * Interface for chart data point
 */
export interface ChartDataPoint {
  timestamp: string;
  value: number;
  change?: number;
  additionalInfo?: { [key: string]: any };
}

@Component({
  selector: 'app-line-chart',
  template: `
    <div class="chart-container">
      <div *ngIf="loading" class="chart-loading">
        <mat-spinner [diameter]="30"></mat-spinner>
        <span>Loading chart data...</span>
      </div>

      <div *ngIf="!loading && (!data || data.length === 0)" class="no-data">
        <mat-icon>info</mat-icon>
        <span>No data available for the selected filters.</span>
      </div>

      <div *ngIf="!loading && data && data.length > 0" class="chart-content">
        <div class="y-axis">
          <div *ngFor="let label of yAxisLabels" class="axis-label">{{ label }}</div>
        </div>

        <div #chartArea class="chart-area">
        </div>

        <div class="x-axis">
          <div *ngFor="let label of xAxisLabels; let i = index"
               [style.left.%]="(i / (xAxisLabels.length - 1)) * 100"
               class="axis-label">
            {{ label }}
          </div>
        </div>
      </div>

      <div *ngIf="!loading && data && data.length > 0" class="chart-legend">
        <div class="legend-item">
          <div [style.background-color]="color" class="color-box"></div>
          <span>{{ title }}</span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .chart-container {
      height: 300px;
      position: relative;
      margin: 20px 0;
    }

    .chart-loading, .no-data {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100%;
      color: rgba(0, 0, 0, 0.5);
    }

    .chart-content {
      display: flex;
      height: 100%;
    }

    .y-axis {
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      padding-right: 10px;
      width: 50px;
    }

    .chart-area {
      flex: 1;
      position: relative;
      border-left: 1px solid rgba(0, 0, 0, 0.1);
      border-bottom: 1px solid rgba(0, 0, 0, 0.1);
    }

    .x-axis {
      height: 30px;
      position: relative;
      margin-top: 5px;
    }

    .axis-label {
      font-size: 12px;
      color: rgba(0, 0, 0, 0.6);
    }

    .x-axis .axis-label {
      position: absolute;
      transform: translateX(-50%) rotate(45deg);
      transform-origin: top left;
      white-space: nowrap;
    }

    .chart-legend {
      display: flex;
      justify-content: center;
      margin-top: 20px;
    }

    .legend-item {
      display: flex;
      align-items: center;
      margin: 0 10px;
    }

    .color-box {
      width: 12px;
      height: 12px;
      margin-right: 5px;
      border-radius: 2px;
    }

    /* Tooltip styles */
    .chart-tooltip {
      position: absolute;
      background-color: rgba(0, 0, 0, 0.8);
      color: white;
      padding: 8px 12px;
      border-radius: 4px;
      font-size: 12px;
      pointer-events: none;
      z-index: 1000;
      transition: opacity 0.2s;
    }

    .chart-point {
      position: absolute;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      transform: translate(-50%, -50%);
      cursor: pointer;
      z-index: 2;
    }
  `],
  standalone: false
})
export class LineChartComponent implements OnInit, OnChanges {
  @Input() data: ChartDataPoint[] = [];
  @Input() color: string = '#1976d2';
  @Input() title: string = 'Chart';
  @Input() invertYAxis: boolean = false; // True for rank (lower is better)
  @Input() loading: boolean = false;
  @ViewChild('chartArea') chartArea!: ElementRef<HTMLDivElement>;
  yAxisLabels: string[] = [];
  xAxisLabels: string[] = [];
  private tooltip: HTMLDivElement | null = null;

  constructor() {
  }

  @Input() yAxisFormatter: (value: number) => string = (value) => value.toString();

  @Input() xAxisFormatter: (timestamp: string) => string = (timestamp) => new Date(timestamp).toLocaleDateString();

  ngOnInit(): void {
    this.generateAxisLabels();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['data'] && !changes['data'].firstChange) {
      this.generateAxisLabels();
      setTimeout(() => this.renderChart(), 0);
    }
  }

  ngAfterViewInit(): void {
    if (this.data && this.data.length > 0) {
      setTimeout(() => this.renderChart(), 0);
    }
  }

  private generateAxisLabels(): void {
    if (!this.data || this.data.length === 0) {
      this.yAxisLabels = [];
      this.xAxisLabels = [];
      return;
    }

    // Generate Y-axis labels (5 steps)
    const values = this.data.map(d => d.value);
    const minValue = Math.min(...values);
    const maxValue = Math.max(...values);
    const range = maxValue - minValue;
    const step = range / 4; // 5 labels (0, 1, 2, 3, 4)

    this.yAxisLabels = Array.from({length: 5}, (_, i) => {
      const value = this.invertYAxis
        ? maxValue - (i * step) // For rank: top is minimum rank (1)
        : minValue + (i * step); // For ELO: bottom is minimum ELO
      return this.yAxisFormatter(Math.round(value));
    });

    // Generate X-axis labels (show fewer labels if many data points)
    const labelCount = Math.min(7, this.data.length);
    const step2 = Math.max(1, Math.floor(this.data.length / (labelCount - 1)));

    this.xAxisLabels = Array.from({length: labelCount}, (_, i) => {
      const index = i === labelCount - 1
        ? this.data.length - 1 // Last label should always be the last data point
        : i * step2;

      return this.xAxisFormatter(this.data[index].timestamp);
    });
  }

  private renderChart(): void {
    if (!this.chartArea || !this.data || this.data.length === 0) return;

    const container = this.chartArea.nativeElement;
    container.innerHTML = ''; // Clear previous content

    const width = container.clientWidth;
    const height = container.clientHeight;

    // Create SVG element
    const svgNS = "http://www.w3.org/2000/svg";
    const svg = document.createElementNS(svgNS, "svg");
    svg.setAttribute('width', '100%');
    svg.setAttribute('height', '100%');

    // Get min/max values for scaling
    const values = this.data.map(d => d.value);
    const minValue = Math.min(...values);
    const maxValue = Math.max(...values);

    // Create path for line
    const path = document.createElementNS(svgNS, "path");

    // Generate path data
    let pathData = '';

    // Create data points
    this.data.forEach((point, index) => {
      // Calculate x and y coordinates
      const x = (index / (this.data.length - 1)) * width;
      let y;

      if (this.invertYAxis) {
        // For rank (lower is better), invert the y-axis
        y = ((point.value - minValue) / (maxValue - minValue)) * height;
      } else {
        // For ELO (higher is better)
        y = height - ((point.value - minValue) / (maxValue - minValue)) * height;
      }

      // Add point to path
      if (index === 0) {
        pathData += `M ${x},${y} `;
      } else {
        pathData += `L ${x},${y} `;
      }

      // Create interactive point element
      const pointEl = document.createElement('div');
      pointEl.className = 'chart-point';
      pointEl.style.left = `${x}px`;
      pointEl.style.top = `${y}px`;
      pointEl.style.backgroundColor = this.color;

      // Store data for tooltip
      pointEl.dataset.value = point.value.toString();
      pointEl.dataset.timestamp = point.timestamp;
      if (point.change !== undefined) {
        pointEl.dataset.change = point.change.toString();
      }

      // Add any additional info to dataset
      if (point.additionalInfo) {
        Object.entries(point.additionalInfo).forEach(([key, value]) => {
          pointEl.dataset[key] = value.toString();
        });
      }

      // Add event listeners for tooltip
      pointEl.addEventListener('mouseenter', this.showTooltip.bind(this));
      pointEl.addEventListener('mouseleave', this.hideTooltip.bind(this));

      container.appendChild(pointEl);
    });

    // Set path attributes
    path.setAttribute('d', pathData);
    path.setAttribute('stroke', this.color);
    path.setAttribute('stroke-width', '2');
    path.setAttribute('fill', 'none');

    // Add path to SVG
    svg.appendChild(path);

    // Add grid lines
    for (let i = 1; i < 5; i++) {
      const line = document.createElementNS(svgNS, 'line');
      line.setAttribute('x1', '0');
      line.setAttribute('y1', (i * height / 5).toString());
      line.setAttribute('x2', width.toString());
      line.setAttribute('y2', (i * height / 5).toString());
      line.setAttribute('stroke', 'rgba(0, 0, 0, 0.1)');
      line.setAttribute('stroke-width', '1');
      svg.appendChild(line);
    }

    // Add SVG to container
    container.appendChild(svg);
  }

  private showTooltip(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    const value = target.dataset.value || '';
    const timestamp = target.dataset.timestamp
      ? new Date(target.dataset.timestamp).toLocaleDateString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric'
      })
      : '';
    const change = target.dataset.change;

    // Create tooltip content
    let content = `
      <div><strong>Date:</strong> ${timestamp}</div>
      <div><strong>${this.invertYAxis ? 'Rank' : 'Rating'}:</strong> ${this.invertYAxis ? '#' : ''}${value}</div>
    `;

    // Add change if available
    if (change) {
      const changeNum = parseFloat(change);
      const isPositive = !this.invertYAxis
        ? changeNum > 0  // For ELO, positive change is good
        : changeNum < 0; // For Rank, negative change is good (rank went down)

      content += `
        <div>
          <strong>Change:</strong>
          <span style="color: ${isPositive ? '#4caf50' : '#f44336'}">
            ${!this.invertYAxis ? (changeNum > 0 ? '+' : '') : (changeNum < 0 ? '+' : '-')}${Math.abs(changeNum)}
          </span>
        </div>
      `;
    }

    // Add any additional info from dataset
    Object.entries(target.dataset).forEach(([key, value]) => {
      if (!['value', 'timestamp', 'change'].includes(key) && value) {
        content += `<div><strong>${key.charAt(0).toUpperCase() + key.slice(1)}:</strong> ${value}</div>`;
      }
    });

    // Create or update tooltip
    if (!this.tooltip) {
      this.tooltip = document.createElement('div');
      this.tooltip.className = 'chart-tooltip';
      document.body.appendChild(this.tooltip);
    }

    this.tooltip.innerHTML = content;

    // Position tooltip
    const rect = target.getBoundingClientRect();
    this.tooltip.style.left = `${rect.left + window.scrollX}px`;
    this.tooltip.style.top = `${rect.top + window.scrollY - this.tooltip.offsetHeight - 10}px`;
    this.tooltip.style.opacity = '1';
  }

  private hideTooltip(): void {
    if (this.tooltip) {
      this.tooltip.style.opacity = '0';
      setTimeout(() => {
        if (this.tooltip) {
          document.body.removeChild(this.tooltip);
          this.tooltip = null;
        }
      }, 200);
    }
  }
}
