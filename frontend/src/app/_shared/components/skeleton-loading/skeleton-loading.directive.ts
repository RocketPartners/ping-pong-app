import { Directive, ElementRef, Input, OnInit, Renderer2 } from '@angular/core';

@Directive({
  selector: '[appSkeleton]',
  standalone: true
})
export class SkeletonLoadingDirective implements OnInit {
  @Input() appSkeleton: 'text' | 'circle' | 'rect' = 'text';
  @Input() width: string = '100%';
  @Input() height: string = '16px';
  @Input() rounded: boolean = true;
  @Input() animation: 'pulse' | 'wave' | 'none' = 'pulse';

  constructor(private el: ElementRef, private renderer: Renderer2) {
  }

  ngOnInit(): void {
    // Set basic skeleton styles
    this.renderer.addClass(this.el.nativeElement, 'skeleton-loading');
    this.renderer.addClass(this.el.nativeElement, `skeleton-${this.appSkeleton}`);
    
    if (this.animation !== 'none') {
      this.renderer.addClass(this.el.nativeElement, `skeleton-animation-${this.animation}`);
    }
    
    if (this.rounded) {
      this.renderer.addClass(this.el.nativeElement, 'skeleton-rounded');
    }
    
    // Set dimensions
    this.renderer.setStyle(this.el.nativeElement, 'width', this.width);
    this.renderer.setStyle(this.el.nativeElement, 'height', this.height);
    
    // Apply shape-specific styles
    switch (this.appSkeleton) {
      case 'circle':
        this.renderer.setStyle(this.el.nativeElement, 'border-radius', '50%');
        break;
      case 'rect':
        if (this.rounded) {
          this.renderer.setStyle(this.el.nativeElement, 'border-radius', '4px');
        }
        break;
      case 'text':
        this.renderer.setStyle(this.el.nativeElement, 'border-radius', '2px');
        break;
    }
    
    // Hide any existing content
    this.renderer.setStyle(this.el.nativeElement, 'color', 'transparent');
    this.renderer.setStyle(this.el.nativeElement, 'background-color', 'var(--skeleton-background, rgba(0, 0, 0, 0.11))');
    
    // Add animation
    if (this.animation === 'pulse') {
      this.renderer.setStyle(this.el.nativeElement, 'animation', 'skeleton-pulse 1.5s ease-in-out infinite');
    } else if (this.animation === 'wave') {
      this.renderer.setStyle(this.el.nativeElement, 'position', 'relative');
      this.renderer.setStyle(this.el.nativeElement, 'overflow', 'hidden');
      
      // Create wave overlay
      const wave = this.renderer.createElement('div');
      this.renderer.addClass(wave, 'skeleton-wave');
      this.renderer.setStyle(wave, 'position', 'absolute');
      this.renderer.setStyle(wave, 'top', '0');
      this.renderer.setStyle(wave, 'left', '0');
      this.renderer.setStyle(wave, 'right', '0');
      this.renderer.setStyle(wave, 'bottom', '0');
      this.renderer.setStyle(wave, 'animation', 'skeleton-wave 1.6s linear infinite');
      this.renderer.setStyle(wave, 'background', 'linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.4), transparent)');
      this.renderer.setStyle(wave, 'transform', 'translateX(-100%)');
      
      this.renderer.appendChild(this.el.nativeElement, wave);
    }
  }
}