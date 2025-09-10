import { Component, OnInit, OnDestroy, ElementRef, Renderer2, PLATFORM_ID, Inject } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Subscription, interval } from 'rxjs';
import { filter } from 'rxjs/operators';
import { EasterEggService } from '../../../_services/easter-egg.service';
import { EasterEgg, EggType, EggClaimResult, EGG_TYPE_CONFIG, EasterEggStats } from '../../../_models/easter-egg';
import { Router, NavigationEnd } from '@angular/router';
import { AlertService } from '../../../_services/alert.services';
import { AccountService } from '../../../_services/account.service';

@Component({
  selector: 'app-easter-egg-display',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule
  ],
  template: `
    <!-- Active egg on current page -->
    <div 
      *ngIf="currentEgg && shouldShowOnCurrentPage && isEasterEggHuntingEnabled() && !isEggHiding"
      class="easter-egg-container"
      [style.position]="'fixed'"
      [style.z-index]="'9999'"
      [style.cursor]="'pointer'"
      (click)="attemptClaim($event)"
>

      <!-- Just the egg image - clean and mysterious -->
      <img class="egg-image" 
           [src]="getEggImagePath()"
           [alt]=""
           [style.width.px]="getEggSize()"
           [style.height.px]="getEggSize()" />
    </div>
    
    <!-- Subtle indicator when egg is available elsewhere -->
    <div 
      *ngIf="currentEgg && !shouldShowOnCurrentPage && isEasterEggHuntingEnabled()"
      class="egg-indicator"
      [attr.title]="getIndicatorTooltip()"
    >
      🥚 <span class="indicator-text">{{ getPageDisplayName(currentEgg.pageLocation) }}</span>
    </div>
  `,
  styles: [`
    .easter-egg-container {
      display: inline-block;
      user-select: none;
      cursor: pointer;
      transition: transform 0.2s ease;
      background: transparent !important;
      border: none !important;
      box-shadow: none !important;
    }

    .easter-egg-container:hover {
      transform: scale(1.1);
    }

    /* Clean egg image - no decorations */
    .egg-image {
      object-fit: contain;
      display: block;
      user-select: none;
      pointer-events: none;
    }

    /* Minimal animations */
    @keyframes claim-success {
      0% { transform: scale(1.1); opacity: 1; }
      50% { transform: scale(1.3); opacity: 0.8; }
      100% { transform: scale(1.5); opacity: 0; }
    }

    .claiming {
      animation: claim-success 0.6s ease-out forwards;
      pointer-events: none;
    }

    .egg-indicator {
      position: fixed;
      top: 20px;
      right: 20px;
      background: rgba(0, 0, 0, 0.8);
      color: white;
      padding: 8px 12px;
      border-radius: 20px;
      font-size: 12px;
      z-index: 999998;
      opacity: 0.8;
      animation: gentle-pulse 2s ease-in-out infinite;
      pointer-events: none;
    }

    .indicator-text {
      font-weight: 500;
      margin-left: 4px;
    }
  `]
})
export class EasterEggDisplayComponent implements OnInit, OnDestroy {
  currentEgg: EasterEgg | null = null;
  shouldShowOnCurrentPage: boolean = false;
  isClaiming: boolean = false;
  userStats: EasterEggStats | null = null;
  
  // Positioning and behavior tracking
  public isEggHiding: boolean = false;
  private eggPositioned: boolean = false; // Track if current egg has been positioned
  private selectedImagePath: string = ''; // Store selected image for current egg

  private subscriptions: Subscription = new Subscription();
  private refreshInterval: Subscription = new Subscription();

  constructor(
    private easterEggService: EasterEggService,
    private router: Router,
    private elementRef: ElementRef,
    private renderer: Renderer2,
    private alertService: AlertService,
    private accountService: AccountService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) {
      return;
    }

    // Subscribe to current egg updates
    this.subscriptions.add(
      this.easterEggService.currentEgg$.subscribe(egg => {
        console.log('🥚 Easter Egg Update:', egg);
        this.currentEgg = egg;
        
        // Subscribe to user stats for progressive sizing (only once)
        if (!this.userStats) {
          this.subscriptions.add(
            this.easterEggService.myStats$.subscribe(stats => {
              this.userStats = stats;
              console.log(`🥚 📊 User stats updated: ${stats?.totalEggsFound || 0} eggs found`);
            })
          );
          
          // Initialize user stats
          this.easterEggService.refreshMyStats();
        }
        // Reset interaction tracking for new egg
        this.isEggHiding = false;
        this.eggPositioned = false; // Reset positioning flag for new egg
        this.selectedImagePath = ''; // Reset selected image for new egg
        
        this.checkIfShouldShowOnCurrentPage();
        if (egg && !this.eggPositioned && this.shouldShowOnCurrentPage) {
          console.log('🥚 Positioning egg on page:', this.router.url);
          // Use longer delay and check for DOM readiness
          setTimeout(() => {
            this.positionEggWithRetry();
          }, 250);
        } else if (egg && this.eggPositioned && this.shouldShowOnCurrentPage) {
          console.log('🥚 Egg already positioned, skipping re-positioning');
        } else if (egg && !this.shouldShowOnCurrentPage) {
          console.log('🥚 Egg exists but not for current page:', this.router.url);
        } else {
          console.log('🥚 No active egg found');
        }
      })
    );

    // Initialize the service
    this.easterEggService.initialize();

    // Subscribe to route changes to update egg visibility
    this.subscriptions.add(
      this.router.events.pipe(
        filter(event => event instanceof NavigationEnd)
      ).subscribe(() => {
        console.log('🥚 Route changed, checking egg visibility');
        this.handleRouteChange();
      })
    );

    // Set up periodic refresh (every 30 seconds)
    this.refreshInterval = interval(30000).subscribe(() => {
      this.easterEggService.refreshCurrentEgg();
    });
  }

  private handleRouteChange(): void {
    if (!this.currentEgg) return;
    
    const wasShowing = this.shouldShowOnCurrentPage;
    this.checkIfShouldShowOnCurrentPage();
    
    // If egg should now be visible and wasn't before, position it
    if (this.shouldShowOnCurrentPage && !wasShowing) {
      console.log('🥚 Egg should now be visible on this page, positioning...');
      this.eggPositioned = false; // Reset positioning flag
      setTimeout(() => {
        if (this.shouldShowOnCurrentPage) { // Double-check in case route changed again
          this.positionEggWithRetry();
        }
      }, 100); // Small delay to ensure DOM is ready
    }
    // If egg should be repositioned on the same page (edge case)
    else if (this.shouldShowOnCurrentPage && wasShowing && !this.eggPositioned) {
      console.log('🥚 Re-positioning egg on same page');
      setTimeout(() => {
        if (this.shouldShowOnCurrentPage) {
          this.positionEggWithRetry();
        }
      }, 100);
    }
    // If egg was showing but shouldn't be now, it will be hidden by the template
    else if (!this.shouldShowOnCurrentPage && wasShowing) {
      console.log('🥚 Egg should no longer be visible on this page');
      this.eggPositioned = false; // Reset for next time
    }
  }

  private checkIfShouldShowOnCurrentPage(): void {
    if (!this.currentEgg) {
      this.shouldShowOnCurrentPage = false;
      return;
    }

    const currentRoute = this.router.url;
    const eggLocation = this.currentEgg.pageLocation;
    
    // Check if current page matches egg location
    let shouldShow = false;
    
    if (eggLocation === 'any') {
      // Show on any page
      shouldShow = true;
    } else if (eggLocation === 'home') {
      // Home page can be '/' or '/home'
      shouldShow = currentRoute === '/' || currentRoute === '/home' || currentRoute.startsWith('/home');
    } else {
      // Show on general pages only (not specific to object instances)
      const generalPages = [
        'leaderboard', 'achievements', 'statistics', 'match-builder', 
        'profile-settings', 'easter-egg-hunter'
      ];
      
      // Check if current route matches a general page
      if (generalPages.some(page => currentRoute.includes(page))) {
        shouldShow = currentRoute.includes(eggLocation);
      }
      
      // Exclude specific instance pages (like /player/username or /game/123)
      const isSpecificInstancePage = currentRoute.match(/\/(player|game)\/[^\/]+$/);
      if (isSpecificInstancePage) {
        shouldShow = false;
      }
    }
    
    console.log(`🥚 Route check - Current: "${currentRoute}", Egg Location: "${eggLocation}", Should Show: ${shouldShow}`);
    this.shouldShowOnCurrentPage = shouldShow;
  }

  private positionEggWithRetry(retryCount: number = 0): void {
    if (!this.currentEgg || !isPlatformBrowser(this.platformId)) return;
    
    const maxRetries = 3;
    const container = this.elementRef.nativeElement.querySelector('.easter-egg-container');
    
    if (!container && retryCount < maxRetries) {
      // Container not ready yet, retry after a short delay
      setTimeout(() => {
        this.positionEggWithRetry(retryCount + 1);
      }, 100);
      return;
    }
    
    if (!container) {
      console.warn('🥚 Container still not found after retries - egg may not be visible yet');
      return;
    }
    
    this.positionEggInMiddleScreen();
    this.eggPositioned = true; // Mark egg as positioned
  }

  private positionEggInMiddleScreen(): void {
    if (!this.currentEgg || !isPlatformBrowser(this.platformId)) return;

    const container = this.elementRef.nativeElement.querySelector('.easter-egg-container');
    if (!container) {
      console.warn('🥚 Container not found for positioning');
      return;
    }

    // Get document dimensions for positioning within page content
    const documentWidth = Math.max(document.documentElement.scrollWidth, window.innerWidth);
    const documentHeight = Math.max(document.documentElement.scrollHeight, window.innerHeight);
    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;
    
    // Ensure we have a reasonable minimum height to work with
    const effectiveHeight = Math.max(documentHeight, viewportHeight * 1.5);
    
    // Calculate middle 60% of the page content area
    const contentWidth = Math.min(documentWidth, viewportWidth) * 0.6;
    const contentHeight = effectiveHeight * 0.6;
    const startX = (Math.min(documentWidth, viewportWidth) - contentWidth) / 2; // Center horizontally
    const startY = effectiveHeight * 0.2; // Start 20% down the page
    
    // Random position within the middle content area
    const x = startX + Math.random() * contentWidth;
    const y = startY + Math.random() * contentHeight;
    
    console.log(`🥚 Positioning egg in page content at (${x.toFixed(1)}, ${y.toFixed(1)}) - Document height: ${documentHeight}px, Effective height: ${effectiveHeight}px`);
    
    // Apply positioning with absolute position relative to document
    this.renderer.setStyle(container, 'position', 'absolute');
    this.renderer.setStyle(container, 'left', `${x}px`);
    this.renderer.setStyle(container, 'top', `${y}px`);
    this.renderer.setStyle(container, 'z-index', '999999');
    
    // Ensure egg is fully visible and clean
    this.renderer.removeStyle(container, 'clip-path');
    this.renderer.removeStyle(container, 'border');
    this.renderer.removeStyle(container, 'background');
    this.renderer.removeStyle(container, 'filter');
  }




  claimEgg(): void {
    if (!this.currentEgg || this.isClaiming) return;

    this.isClaiming = true;
    
    // Add claiming animation class
    const container = this.elementRef.nativeElement.querySelector('.easter-egg-container');
    if (container) {
      this.renderer.addClass(container, 'claiming');
    }

    this.easterEggService.claimEgg(this.currentEgg.id).subscribe({
      next: (result: EggClaimResult) => {
        if (result.success) {
          console.log(`🥚 Egg claimed successfully!`);
          
          this.easterEggService.handleSuccessfulClaim(result);
          // Hide the egg immediately since it's been claimed
          this.currentEgg = null;
          this.shouldShowOnCurrentPage = false;
        } else {
          // Show error message but also hide the egg since someone else claimed it
          this.alertService.error(result.message);
          console.log(`🥚 Someone else claimed the egg: ${result.message}`);
          // Hide the egg since it's been claimed by someone else
          this.currentEgg = null;
          this.shouldShowOnCurrentPage = false;
        }
        this.isClaiming = false;
      },
      error: (error) => {
        console.error('Error claiming egg:', error);
        
        // Check if the error response contains claim result info
        if (error.error && error.error.message) {
          this.alertService.error(error.error.message);
          console.log(`🥚 Claim failed: ${error.error.message}`);
          // If someone beat us to it, hide the egg
          if (error.error.message.includes('beat you to it')) {
            this.currentEgg = null;
            this.shouldShowOnCurrentPage = false;
          }
        } else {
          this.alertService.error('Failed to claim easter egg');
        }
        
        this.isClaiming = false;
        // Remove claiming class on error
        if (container) {
          this.renderer.removeClass(container, 'claiming');
        }
      }
    });
  }


  getEggImagePath(): string {
    if (!this.currentEgg) return 'assets/images/easter-eggs/common-egg-01.png';
    
    // If we haven't selected an image for this egg yet, pick one randomly
    if (!this.selectedImagePath) {
      const config = EGG_TYPE_CONFIG[this.currentEgg.type];
      if (config && config.imageFilenames && config.imageFilenames.length > 0) {
        const randomIndex = Math.floor(Math.random() * config.imageFilenames.length);
        const selectedFilename = config.imageFilenames[randomIndex];
        this.selectedImagePath = `assets/images/easter-eggs/${selectedFilename}`;
        console.log(`🥚 Selected random image for ${this.currentEgg.type}: ${selectedFilename}`);
      } else {
        this.selectedImagePath = 'assets/images/easter-eggs/common-egg-01.png';
      }
    }
    
    return this.selectedImagePath;
  }

  getTooltipText(): string {
    if (!this.currentEgg) return '';
    return `${this.currentEgg.type} Easter Egg - Click to claim ${this.currentEgg.pointValue} points!`;
  }

  isEasterEggHuntingEnabled(): boolean {
    const player = this.accountService.playerValue?.player;
    return player?.easterEggHuntingEnabled ?? true;
  }

  // Enhanced visual effects methods
  getEggSize(): number {
    if (!this.currentEgg) return 20; // Half the original size
    
    const config = EGG_TYPE_CONFIG[this.currentEgg.type];
    const baseSizeWithType = 20 * (config?.sizeMultiplier || 1.0); // Half the original 40px base
    
    // Progressive scaling: Each egg found makes future eggs 1% smaller
    const userEggCount = this.userStats?.totalEggsFound || 0;
    const scalingFactor = Math.max(0.3, 1 - (userEggCount * 0.01)); // Minimum 30% size
    
    // Start at smaller size (15px base) for subtlety
    const progressiveSize = Math.max(10, baseSizeWithType * scalingFactor); // Minimum 10px
    
    return Math.round(progressiveSize);
  }

  getEggColor(): string {
    if (!this.currentEgg) return '#8BC34A';
    const config = EGG_TYPE_CONFIG[this.currentEgg.type];
    return config?.color || '#8BC34A';
  }

  getDisplayName(): string {
    if (!this.currentEgg) return 'Common Egg';
    const config = EGG_TYPE_CONFIG[this.currentEgg.type];
    return config?.displayName || 'Common Egg';
  }


  getIndicatorTooltip(): string {
    if (!this.currentEgg) return '';
    return `${this.currentEgg.type} Easter Egg available on ${this.getPageDisplayName(this.currentEgg.pageLocation)} page`;
  }

  getPageDisplayName(pageLocation: string): string {
    switch (pageLocation) {
      case 'home': return 'Home';
      case 'leaderboard': return 'Leaderboard';
      case 'achievements': return 'Achievements';
      case 'statistics': return 'Statistics';
      case 'match-builder': return 'Match Builder';
      case 'profile-settings': return 'Profile Settings';
      case 'easter-egg-hunter': return 'Easter Egg Hunter';
      case 'any': return 'Current';
      default: return pageLocation.charAt(0).toUpperCase() + pageLocation.slice(1);
    }
  }


  attemptClaim(event: MouseEvent): void {
    if (!this.currentEgg || this.isClaiming) return;
    
    console.log(`🥚 Claiming ${this.currentEgg.type} egg`);
    this.claimEgg();
  }

  // Removed dynamic behavior methods - eggs now stay in one position

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
    this.refreshInterval.unsubscribe();
  }
}