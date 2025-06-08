import { Component, Input, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDivider } from '@angular/material/divider';
import { MatButtonModule } from '@angular/material/button';
import { Instrumentation, SheetMusic } from '../../model/datamodels';
import { SheetMusicService } from '../../services/sheetMusic.service';
import { MatGridListModule } from '@angular/material/grid-list';
import { CommonModule } from '@angular/common';
import { InstrumentationListComponent } from '../instrumentation-list/instrumentation-list.component';

@Component({
  selector: 'app-sheetMusic-details',
  imports: [
    CommonModule,
    MatGridListModule,
    MatDivider,
    MatButtonModule,
    InstrumentationListComponent
  ],
  templateUrl: './sheetMusic-details.component.html',
  styleUrl: './sheetMusic-details.component.scss'
})
export class SheetMusicDetailsComponent implements OnInit {

  @Input() data!: SheetMusic;
  instrumentations: Instrumentation[] = [];

  constructor(
    private sheetMusicService: SheetMusicService,
    private route: ActivatedRoute,
    private router: Router) { }

  async ngOnInit(): Promise<void> {
    const id = this.route.snapshot.paramMap.get('id')!;
    if (id) {
      await this.sheetMusicService.loadSheet(id).subscribe({
        next: (val: any) => {
          this.data = val;

          this.instrumentations = [
            { instrumentName: 'Trumpet', partLabel: 'Trompete 1', transposition: 'Bb', clef: 'TREBLE', notationType: 'STANDARD' },
            { instrumentName: 'Trombone', partLabel: 'Posaune', transposition: 'C', clef: 'BASS', notationType: 'STANDARD' }
          ];
          // Load instrumentations for the sheet music
          /*this.sheetMusicService.getInstrumentationsForSheet(id).subscribe({
            next: (instrumentations: Instrumentation[]) => {
              this.instrumentations = instrumentations;
            },
            error: (err: any) => {
              console.error('Error loading instrumentations:', err);
            }
          });*/
        }
      });
    }
  }

  goBack(): void {
    this.router.navigate(['/sheets']); // Navigate back to the list or previous page
  }

}
