import { Component } from '@angular/core';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-uploads',
  imports: [TranslatePipe],
  template: `<h1>{{ 'uploads.title' | translate }}</h1>`,
})
export class Uploads {}
