import { Component, Input } from '@angular/core';
import { SheetMusic } from '../model/sheetMusic';

@Component({
  selector: 'app-sheetMusic-details',
  imports: [],
  templateUrl: './sheetMusic-details.component.html',
  styleUrl: './sheetMusic-details.component.less'
})
export class SheetMusicDetailsComponent {

  @Input() sheetMusic!: SheetMusic;

}
