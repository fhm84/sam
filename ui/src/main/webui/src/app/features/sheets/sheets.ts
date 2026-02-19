import { Component } from '@angular/core';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-sheets',
  imports: [TranslatePipe],
  template: `<h1>{{ 'sheets.title' | translate }}</h1>`,
})
export class Sheets {}
