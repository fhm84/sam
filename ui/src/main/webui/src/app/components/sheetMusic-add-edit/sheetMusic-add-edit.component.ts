import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { SheetMusicService } from '../../services/sheetMusic.service';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatRadioModule } from '@angular/material/radio';
import { ActivatedRoute, Router } from '@angular/router';
import { Instrumentation, SheetMusic } from '../../model/datamodels';
import { MusicianAutocompleteComponent } from '../musician-autocomplete/musician-autocomplete.component';
import { MatCardModule } from '@angular/material/card';
import { MatExpansionModule } from '@angular/material/expansion';
import { InstrumentationListComponent } from '../instrumentation-list/instrumentation-list.component';

@Component({
  selector: 'app-sheetmusic-add-edit',
  imports: [
    CommonModule,
    MatInputModule,
    MatSelectModule,
    MatRadioModule,
    MatCardModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatButtonModule,
    MatIconModule,
    FormsModule,
    ReactiveFormsModule,
    MusicianAutocompleteComponent,
    InstrumentationListComponent
  ],
  templateUrl: './sheetMusic-add-edit.component.html',
  styleUrl: './sheetMusic-add-edit.component.scss'
})
export class SheetMusicAddEditComponent implements OnInit {

  sheetForm: FormGroup;
  @Input() data!: SheetMusic;
  initialComposer?: string;
  initialArranger?: string;
  instrumentations: Instrumentation[] = [];

  // TODO: this should be loaded from backend!
  genre: string[] = [
    'Polka',
    'Marsch',
    'Konzertmarsch',
    'Walzer'
  ];

  constructor(
    private sheetMusicService: SheetMusicService,
    private formBuilder: FormBuilder,
    private route: ActivatedRoute,
    private router: Router
  ) {
    this.sheetForm = this.formBuilder.group({
      title: ['', Validators.required],
      subtitle: [''],
      publisher: [''],
      publisherIpi: [''],
      composer: [''],
      arranger: [''],
      genre: [''],
      difficultyLevel: [''],
      yearOfComposition: [''],
      edition: [''],
      copyright: [''],
      rating: [''],
      iswc: [''],
      gemaWorkNumber: [''],
      additionalNotes: ['']
    });
  }

  async ngOnInit(): Promise<void> {
    const id = this.route.snapshot.paramMap.get('id')!;
    if (id) {
      await this.sheetMusicService.loadSheet(id).subscribe({
        next: (val: any) => {
          this.data = val;

          // Patch the form with the loaded data
          this.sheetForm.patchValue(this.data);

          // Set the initial composer value (auto-complete component)
          this.initialComposer = this.data.composer?.name;

          // Set the initial arranger value (auto-complete component)
          this.initialArranger = this.data.arranger?.name;

          // Load instrumentations for the sheet music
          this.sheetMusicService.getInstrumentationsForSheet(id).subscribe({
            next: (instrumentations: Instrumentation[]) => {
              this.instrumentations = instrumentations;
            },
            error: (err: any) => {
              console.error('Error loading instrumentations:', err);
            }
          });
        }
      });
    }
  }

  onSubmit() {
    if (this.sheetForm.valid) {
      if (this.data?.id) {
        this.sheetMusicService
          .updateSheet(this.data.id, this.sheetForm.value)
          .subscribe({
            next: (val: any) => {
              alert('Sheet details updated!');
              this.router.navigate(['/sheets']);
            },
            error: (err: any) => {
              console.error(err);
              alert("Error while updating the sheetMusic!");
            },
          });
      } else {
        this.sheetMusicService.addSheet(this.sheetForm.value).subscribe({
          next: (val: any) => {
            alert('Sheet added successfully!');
            this.sheetForm.reset();
            this.router.navigate(['/sheets']);
          },
          error: (err: any) => {
            console.error(err);
            alert("Error while adding the sheetMusic!");
          },
        });
      }
    }
  }

  cancel(): void {
    this.router.navigate(['/sheets']);
  }

}
