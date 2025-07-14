import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-section-title',
  template: `
    <h2 class="section-title" [ngClass]="{'centered': centered}">
      <ng-content></ng-content>
    </h2>
  `,
  styles: [`
    .section-title {
      font-size: 20px;
      font-weight: 500;
      margin: 0 0 16px;
      color: #333;
      position: relative;
      padding-left: 16px;
    }

    .section-title::before {
      content: '';
      position: absolute;
      left: 0;
      top: 0;
      height: 100%;
      width: 4px;
      background-color: #1976d2;
      border-radius: 2px;
    }

    .centered {
      text-align: center;
      padding-left: 0;
      padding-bottom: 16px;
    }

    .centered::before {
      left: 50%;
      top: auto;
      bottom: 0;
      transform: translateX(-50%);
      height: 4px;
      width: 60px;
    }
  `],
  standalone: false
})
export class SectionTitleComponent {
  @Input() centered: boolean = false;
}
