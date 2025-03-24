import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';

import { HeaderComponent } from './components/header/header.component';

@Component({
  selector: 'app-root',
  imports: [
    HeaderComponent,
    CommonModule,
    RouterOutlet
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {

  title = 'sam';

}
