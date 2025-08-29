import { Component, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { Router } from '@angular/router';

@Component({
  selector: 'app-achievement-admin',
  templateUrl: './achievement-admin.component.html',
  styleUrls: ['./achievement-admin.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AchievementAdminComponent implements OnInit {

  constructor(private router: Router) { }

  ngOnInit(): void {
  }

  /**
   * Navigate to different admin sections
   */
  navigateToSection(section: string): void {
    this.router.navigate(['/admin', section]);
  }
}