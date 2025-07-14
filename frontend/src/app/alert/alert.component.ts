import {Component, OnDestroy, OnInit} from '@angular/core';
import {Subscription} from 'rxjs';

import {AlertService} from "../_services/alert.services";

@Component({
  selector: 'alert', templateUrl: 'alert.component.html',
  standalone: false
})
export class AlertComponent implements OnInit, OnDestroy {
  alert: any;
  private subscription!: Subscription;

  constructor(private alertService: AlertService) {
  }

  ngOnInit() {
    this.subscription = this.alertService.onAlert()
      .subscribe(alert => {
        switch (alert?.type) {
          case 'success':
            alert.cssClass = 'alert alert-success';
            break;
          case 'error':
            alert.cssClass = 'alert alert-danger';
            break;
        }

        this.alert = alert;
      });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }
}
