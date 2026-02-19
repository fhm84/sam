import { Component } from '@angular/core';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-configuration',
  imports: [TranslatePipe],
  template: `<h1>{{ 'configuration.title' | translate }}</h1>`,
})
export class Configuration {}
