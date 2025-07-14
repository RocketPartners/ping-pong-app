// src/app/achievement/achievement-detail/achievement-detail.service.ts

import {Injectable} from '@angular/core';
import {Overlay, OverlayRef} from '@angular/cdk/overlay';
import {ComponentPortal} from '@angular/cdk/portal';
import {AchievementDTO} from '../../_models/achievement';
import {AchievementDetailComponent} from './achievement-detail.component';

@Injectable({
  providedIn: 'root'
})
export class AchievementDetailService {
  private overlayRef: OverlayRef | null = null;

  constructor(private overlay: Overlay) {
  }

  open(achievementData: AchievementDTO): void {
    // Close any existing drawer
    this.close();

    // Create the overlay
    this.overlayRef = this.overlay.create({
      hasBackdrop: true,
      backdropClass: 'achievement-drawer-backdrop',
      positionStrategy: this.overlay
        .position()
        .global()
        .right('0')
        .top('0')
        .bottom('0'),
      scrollStrategy: this.overlay.scrollStrategies.block()
    });

    // Create the component portal
    const componentPortal = new ComponentPortal(AchievementDetailComponent);

    // Attach the portal to the overlay
    const componentRef = this.overlayRef.attach(componentPortal);

    // Pass data to the component
    componentRef.instance.achievementData = achievementData;
    componentRef.instance.overlayRef = this.overlayRef;

    // Handle backdrop clicks
    this.overlayRef.backdropClick().subscribe(() => this.close());
  }

  close(): void {
    if (this.overlayRef) {
      this.overlayRef.dispose();
      this.overlayRef = null;
    }
  }
}
