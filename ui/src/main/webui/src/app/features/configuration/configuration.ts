import { Component, ChangeDetectionStrategy } from '@angular/core';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-configuration',
  imports: [TranslatePipe],
  changeDetection: ChangeDetectionStrategy.Eager,
  template: `<h1>{{ 'configuration.title' | translate }}</h1>`,
})
export class Configuration {}
