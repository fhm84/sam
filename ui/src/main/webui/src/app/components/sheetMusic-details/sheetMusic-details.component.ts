import { Component, Input } from '@angular/core';
import { SheetMusic } from '../../model/datamodels';

@Component({
  selector: 'app-sheetMusic-details',
  imports: [],
  templateUrl: './sheetMusic-details.component.html',
  styleUrl: './sheetMusic-details.component.scss'
})
export class SheetMusicDetailsComponent {

  @Input() sheetMusic!: SheetMusic;

}
