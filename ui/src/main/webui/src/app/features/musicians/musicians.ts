import { Component } from '@angular/core';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-musicians',
  imports: [TranslatePipe],
  template: `<h1>{{ 'musicians.title' | translate }}</h1>`,
})
export class Musicians {}
