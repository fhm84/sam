import { Component } from '@angular/core';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-instruments',
  imports: [TranslatePipe],
  template: `<h1>{{ 'instruments.title' | translate }}</h1>`,
})
export class Instruments {}
