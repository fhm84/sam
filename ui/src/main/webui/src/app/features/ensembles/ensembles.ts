import { Component } from '@angular/core';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-ensembles',
  imports: [TranslatePipe],
  template: `<h1>{{ 'ensembles.title' | translate }}</h1>`,
})
export class Ensembles {}
