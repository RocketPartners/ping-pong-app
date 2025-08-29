import { Component, OnInit, OnDestroy, ViewChild, ElementRef, Input, OnChanges, SimpleChanges } from '@angular/core';
import { Subject, takeUntil } from 'rxjs';
import { AchievementService } from '../../../_services/achievement.service';
import { AlertService } from '../../../_services/alert.services';
import { AchievementDependency, PlayerDependencyTree, Achievement } from '../../../_models/achievement';
import { select, selectAll, tree, hierarchy, zoom, zoomIdentity } from 'd3';

interface TreeNode {
  id: string;
  name: string;
  category: string;
  isCompleted: boolean;
  children?: TreeNode[];
  x?: number;
  y?: number;
  depth?: number;
}

@Component({
  selector: 'app-dependency-tree',
  templateUrl: './dependency-tree.component.html',
  styleUrls: ['./dependency-tree.component.scss'],
  standalone: false
})
export class DependencyTreeComponent implements OnInit, OnDestroy, OnChanges {
  @ViewChild('treeContainer', { static: true }) treeContainer!: ElementRef;
  @Input() playerId: string = '';
  @Input() selectedAchievementId: string = '';
  
  private destroy$ = new Subject<void>();
  private svg: any;
  private g: any;
  private tree: any;
  private zoom: any;
  
  // Component state
  loading = false;
  dependencyTree: PlayerDependencyTree | null = null;
  achievements: Achievement[] = [];
  
  // Tree configuration
  private width = 800;
  private height = 600;
  private margin = { top: 20, right: 90, bottom: 30, left: 90 };
  
  constructor(
    private achievementService: AchievementService,
    private alertService: AlertService
  ) {}

  ngOnInit(): void {
    this.initializeSvg();
    if (this.playerId) {
      this.loadDependencyTree();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['playerId'] && this.playerId) {
      this.loadDependencyTree();
    }
    if (changes['selectedAchievementId'] && this.selectedAchievementId) {
      this.highlightAchievement(this.selectedAchievementId);
    }
  }

  /**
   * Initialize SVG canvas
   */
  private initializeSvg(): void {
    const element = this.treeContainer.nativeElement;
    
    // Clear any existing SVG
    select(element).selectAll('*').remove();
    
    this.svg = select(element)
      .append('svg')
      .attr('width', this.width + this.margin.left + this.margin.right)
      .attr('height', this.height + this.margin.top + this.margin.bottom);
    
    // Add zoom behavior
    this.zoom = zoom()
      .scaleExtent([0.1, 3])
      .on('zoom', (event) => {
        this.g.attr('transform', event.transform);
      });
    
    this.svg.call(this.zoom);
    
    this.g = this.svg.append('g')
      .attr('transform', `translate(${this.margin.left},${this.margin.top})`);
    
    // Initialize tree layout
    this.tree = tree().size([this.height - this.margin.top - this.margin.bottom, 
                                this.width - this.margin.left - this.margin.right]);
  }

  /**
   * Load dependency tree data
   */
  loadDependencyTree(): void {
    if (!this.playerId) return;
    
    this.loading = true;
    
    this.achievementService.getPlayerDependencyTree(this.playerId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (tree) => {
          this.dependencyTree = tree;
          this.loadAchievements();
        },
        error: (error) => {
          console.error('Error loading dependency tree:', error);
          this.alertService.error('Failed to load dependency tree');
          this.loading = false;
        }
      });
  }

  /**
   * Load achievements for reference
   */
  private loadAchievements(): void {
    this.achievementService.getAdminAchievementsList()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (achievements) => {
          this.achievements = achievements;
          this.renderTree();
          this.loading = false;
        },
        error: (error) => {
          console.error('Error loading achievements:', error);
          this.alertService.error('Failed to load achievements');
          this.loading = false;
        }
      });
  }

  /**
   * Render the dependency tree
   */
  private renderTree(): void {
    if (!this.dependencyTree || !this.achievements.length) return;
    
    // Convert dependency data to tree structure
    const treeData = this.buildTreeData();
    
    // Create hierarchy
    const root = hierarchy(treeData);
    const treeWithPositions = this.tree(root);
    
    // Clear existing elements
    this.g.selectAll('.link').remove();
    this.g.selectAll('.node').remove();
    
    // Add links
    const links = this.g.selectAll('.link')
      .data(treeWithPositions.descendants().slice(1))
      .enter().append('path')
      .attr('class', 'link')
      .attr('d', (d: any) => {
        return `M${d.y},${d.x}C${(d.y + d.parent.y) / 2},${d.x} ${(d.y + d.parent.y) / 2},${d.parent.x} ${d.parent.y},${d.parent.x}`;
      });
    
    // Add nodes
    const nodes = this.g.selectAll('.node')
      .data(treeWithPositions.descendants())
      .enter().append('g')
      .attr('class', (d: any) => `node ${d.data.isCompleted ? 'completed' : 'incomplete'}`)
      .attr('transform', (d: any) => `translate(${d.y},${d.x})`);
    
    // Add circles for nodes
    nodes.append('circle')
      .attr('r', 8)
      .attr('class', (d: any) => `category-${d.data.category.toLowerCase()}`)
      .on('click', (event: any, d: any) => this.onNodeClick(d))
      .on('mouseover', (event: any, d: any) => this.showTooltip(event, d))
      .on('mouseout', () => this.hideTooltip());
    
    // Add labels
    nodes.append('text')
      .attr('dy', '.35em')
      .attr('x', (d: any) => d.children || d._children ? -13 : 13)
      .style('text-anchor', (d: any) => d.children || d._children ? 'end' : 'start')
      .text((d: any) => this.truncateText(d.data.name, 15))
      .attr('class', 'node-label');
    
    // Add legend
    this.addLegend();
  }

  /**
   * Build tree data structure from dependencies
   */
  private buildTreeData(): TreeNode {
    // This is a simplified version - you'd need to implement proper tree building logic
    // based on your actual dependency structure
    const achievementMap = new Map(this.achievements.map(a => [a.id, a]));
    
    // Find root nodes (achievements with no dependencies)
    const rootAchievements = this.achievements.filter(a => 
      !this.dependencyTree?.dependencies.some((dep: any) => dep.dependentId === a.id)
    );
    
    if (rootAchievements.length === 0) {
      // Fallback: use first achievement as root
      const firstAchievement = this.achievements[0];
      return {
        id: firstAchievement.id,
        name: firstAchievement.name,
        category: firstAchievement.category,
        isCompleted: false,
        children: []
      };
    }
    
    // Build tree starting from first root
    return this.buildNodeRecursive(rootAchievements[0], achievementMap, new Set());
  }

  /**
   * Build tree node recursively
   */
  private buildNodeRecursive(achievement: Achievement, achievementMap: Map<string, Achievement>, visited: Set<string>): TreeNode {
    if (visited.has(achievement.id)) {
      return {
        id: achievement.id,
        name: achievement.name + ' (circular)',
        category: achievement.category,
        isCompleted: false,
        children: []
      };
    }
    
    visited.add(achievement.id);
    
    const dependents = this.dependencyTree?.dependencies
      .filter((dep: any) => dep.dependencyId === achievement.id)
      .map((dep: any) => achievementMap.get(dep.dependentId))
      .filter((a: any) => a !== undefined) as Achievement[];
    
    const children: TreeNode[] = dependents.map(dep => 
      this.buildNodeRecursive(dep, achievementMap, new Set(visited))
    );
    
    return {
      id: achievement.id,
      name: achievement.name,
      category: achievement.category,
      isCompleted: false, // You'd determine this from player progress
      children: children.length > 0 ? children : undefined
    };
  }

  /**
   * Handle node click
   */
  private onNodeClick(node: any): void {
    // Emit event or navigate to achievement details
    console.log('Clicked node:', node.data);
  }

  /**
   * Show tooltip on hover
   */
  private showTooltip(event: MouseEvent, node: any): void {
    // Implement tooltip display logic
    const tooltip = select('body').append('div')
      .attr('class', 'tree-tooltip')
      .style('opacity', 0);
    
    tooltip.transition()
      .duration(200)
      .style('opacity', .9);
    
    tooltip.html(`<strong>${node.data.name}</strong><br/>Category: ${node.data.category}<br/>Status: ${node.data.isCompleted ? 'Completed' : 'Incomplete'}`)
      .style('left', (event.pageX + 10) + 'px')
      .style('top', (event.pageY - 28) + 'px');
  }

  /**
   * Hide tooltip
   */
  private hideTooltip(): void {
    selectAll('.tree-tooltip').remove();
  }

  /**
   * Add legend to the visualization
   */
  private addLegend(): void {
    const legend = this.svg.append('g')
      .attr('class', 'legend')
      .attr('transform', `translate(${this.width - 150}, 20)`);
    
    const legendData = [
      { color: '#4caf50', label: 'Easy', class: 'category-easy' },
      { color: '#2196f3', label: 'Medium', class: 'category-medium' },
      { color: '#9c27b0', label: 'Hard', class: 'category-hard' },
      { color: '#ffc107', label: 'Legendary', class: 'category-legendary' }
    ];
    
    const legendItems = legend.selectAll('.legend-item')
      .data(legendData)
      .enter().append('g')
      .attr('class', 'legend-item')
      .attr('transform', (d: any, i: any) => `translate(0, ${i * 20})`);
    
    legendItems.append('circle')
      .attr('r', 6)
      .attr('fill', (d: any) => d.color);
    
    legendItems.append('text')
      .attr('x', 15)
      .attr('y', 4)
      .text((d: any) => d.label)
      .style('font-size', '12px');
  }

  /**
   * Highlight specific achievement
   */
  private highlightAchievement(achievementId: string): void {
    // Remove existing highlights
    this.g.selectAll('.node').classed('highlighted', false);
    
    // Add highlight to specific node
    this.g.selectAll('.node')
      .filter((d: any) => d.data.id === achievementId)
      .classed('highlighted', true);
  }

  /**
   * Truncate text for display
   */
  private truncateText(text: string, maxLength: number): string {
    return text.length > maxLength ? text.substring(0, maxLength) + '...' : text;
  }

  /**
   * Reset zoom and center the tree
   */
  resetView(): void {
    this.svg.transition().duration(750)
      .call(this.zoom.transform, zoomIdentity);
  }

  /**
   * Export tree as SVG
   */
  exportSvg(): void {
    const svgElement = this.treeContainer.nativeElement.querySelector('svg');
    const serializer = new XMLSerializer();
    const svgString = serializer.serializeToString(svgElement);
    
    const blob = new Blob([svgString], { type: 'image/svg+xml' });
    const url = URL.createObjectURL(blob);
    
    const link = document.createElement('a');
    link.href = url;
    link.download = 'dependency-tree.svg';
    link.click();
    
    URL.revokeObjectURL(url);
  }
}