import { Component, Input, OnInit } from '@angular/core';
import { AsyncPipe, CommonModule } from '@angular/common';
import { FormControl, FormBuilder, FormGroup, Validators, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { SheetMusicService } from '../../services/sheetMusic.service';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatRadioModule } from '@angular/material/radio';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { ActivatedRoute, Router } from '@angular/router';
import { map, Observable, startWith } from 'rxjs';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { Musician, SheetMusic } from '../../model/datamodels';

@Component({
  selector: 'app-sheetmusic-add-edit',
  imports: [
    CommonModule,
    MatInputModule,
    MatSelectModule,
    MatRadioModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatAutocompleteModule,
    MatButtonModule,
    MatIconModule,
    FormsModule,
    ReactiveFormsModule,
    AsyncPipe
  ],
  templateUrl: './sheetMusic-add-edit.component.html',
  styleUrl: './sheetMusic-add-edit.component.scss'
})
export class SheetMusicAddEditComponent implements OnInit {

  sheetForm: FormGroup;
  @Input() data!: SheetMusic;

  composerOptions: Musician[] = [
    { id: '1', name: 'Hans Zimmer' },
    { id: '2', name: 'Frank Bernaerts' },
    { id: '3', name: 'Anton Gälle' }
  ];

  filteredComposers!: Observable<Musician[]>;

  composerControl = new FormControl<string | Musician>('', {
    nonNullable: true,
  });

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
      publisher: ['', Validators.required],
      composer: this.composerControl,
      genre: ['', Validators.required],
      type: ['']
    });
  }

  async ngOnInit(): Promise<void> {
    const id = this.route.snapshot.paramMap.get('id')!;
    if (id) {
      await this.sheetMusicService.loadSheet(id).subscribe({
        next: (val: any) => {
          this.data = val;

          this.sheetForm.patchValue(this.data);

          var preselectedComposer = this._filter(this.data.composer?.name).pop();
          if (preselectedComposer) {
            this.composerControl.setValue(preselectedComposer);
          }
        }
      });
    }

    this.filteredComposers = this.composerControl.valueChanges.pipe(
      startWith(''),
      map(value => {
        const name = typeof value === 'string' ? value : value?.name;
        return name ? this._filter(name as string) : this.composerOptions.slice();
      }),
    );

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

  private _filter(name?: string): Musician[] {
    if (name) {
      const filterValue = name.toLowerCase();

      return this.composerOptions.filter(option => option.name.toLowerCase().includes(filterValue));
    } else {
      return this.composerOptions;
    }
  }

  displayFn(musician: Musician): string {
    return musician && musician.name ? musician.name : '';
  }

}
