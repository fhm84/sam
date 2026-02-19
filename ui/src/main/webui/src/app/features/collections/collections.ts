import { Component } from '@angular/core';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-collections',
  imports: [TranslatePipe],
  template: `<h1>{{ 'collections.title' | translate }}</h1>`,
})
export class Collections {}
