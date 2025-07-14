// src/app/_config/chart-config.ts
import {Chart, registerables} from 'chart.js';

export function initializeChartJs(): void {
  // Register all Chart.js components
  Chart.register(...registerables);

  // Set global defaults
  Chart.defaults.font.family = "'Roboto', 'Helvetica Neue', 'Arial', sans-serif";
  Chart.defaults.font.size = 12;
  Chart.defaults.color = 'rgba(0, 0, 0, 0.6)';
  Chart.defaults.elements.point.radius = 3;
  Chart.defaults.elements.point.hoverRadius = 5;
  Chart.defaults.elements.line.tension = 0.2;
  Chart.defaults.plugins.tooltip.backgroundColor = 'rgba(0, 0, 0, 0.8)';
  Chart.defaults.plugins.tooltip.padding = 10;
  Chart.defaults.plugins.tooltip.cornerRadius = 4;
  Chart.defaults.plugins.tooltip.titleFont = {weight: 'bold'};
  Chart.defaults.plugins.legend.labels.usePointStyle = true;
  Chart.defaults.plugins.legend.labels.padding = 15;
  Chart.defaults.responsive = true;
  Chart.defaults.maintainAspectRatio = false;
}
