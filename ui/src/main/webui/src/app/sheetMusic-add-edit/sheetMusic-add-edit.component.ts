import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormControl, FormBuilder, FormGroup, Validators, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { SheetMusicService } from '../services/sheetMusic.service';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatRadioModule } from '@angular/material/radio';
import { MatDatepickerModule } from '@angular/material/datepicker';

@Component({
  selector: 'app-sheetmusic-add-edit',
  imports: [
    CommonModule,
    MatInputModule,
    MatSelectModule,
    MatRadioModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatButtonModule,
    MatIconModule,
    FormsModule,
    ReactiveFormsModule
  ],
  templateUrl: './sheetMusic-add-edit.component.html',
  styleUrl: './sheetMusic-add-edit.component.less'
})
export class SheetMusicAddEditComponent implements OnInit {

  sheetForm: FormGroup;

  // TODO: this should be loaded from backend!
  genre: string[] = [
    'Polka',
    'Marsch',
    'Konzertmarsch',
    'Walzer'
  ];

  constructor(
    private sheetMusicService: SheetMusicService,
    private dialogRef: MatDialogRef<SheetMusicAddEditComponent>,
    private formBuilder: FormBuilder,
    @Inject(MAT_DIALOG_DATA) public data: any,
  ) {
    this.sheetForm = this.formBuilder.group({
      title: ['', Validators.required],
      publisher: ['', Validators.required],
      composer: ['', Validators.required],
      genre: ['', Validators.required],
      type: [''],
      //aliases: this.formBuilder.array([this.formBuilder.control('')])
    });
  }

//  get aliases() { return this.sheetForm.get('aliases') as FormArray; }

//  addAlias() { this.aliases.push(this.formBuilder.control('')); }

  ngOnInit(): void {
    this.sheetForm.patchValue(this.data);
  }

  onSubmit() {
    if (this.sheetForm.valid) {
      if (this.data) {
        this.sheetMusicService
          .updateSheet(this.data.id, this.sheetForm.value)
          .subscribe({
            next: (val: any) => {
              alert('Sheet details updated!');
              this.dialogRef.close(true);
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
            this.dialogRef.close(true);
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
    this.dialogRef.close();
  }

}
