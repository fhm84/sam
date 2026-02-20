import { Component, EventEmitter, inject, Input, OnChanges, Output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { InputText } from 'primeng/inputtext';
import { InputNumber } from 'primeng/inputnumber';
import { Button } from 'primeng/button';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { TranslationService } from '../../core/translation.service';
import { MusiciansApiService } from '../../core/api';
import { convertEmptyStringsToNull } from '../../shared/utils/object.utils';
import { Musician } from '../../model/datamodels';
import { map, Observable } from 'rxjs';

@Component({
  selector: 'app-musician-form',
  imports: [ReactiveFormsModule, InputText, InputNumber, Button, TranslatePipe],
  templateUrl: './musician-form.html',
  styleUrl: './musician-form.scss',
})
export class MusicianForm implements OnChanges {
  protected readonly t = inject(TranslationService);
  private readonly api = inject(MusiciansApiService);

  @Input() musician: Musician | null = null;
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  protected saving = false;

  protected readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    ipi: new FormControl('', { nonNullable: true }),
    birthYear: new FormControl<number | null>(null),
    deathYear: new FormControl<number | null>(null),
  });

  get isEdit(): boolean {
    return this.musician !== null;
  }

  ngOnChanges(): void {
    if (this.musician) {
      this.form.patchValue({
        name: this.musician.name,
        ipi: this.musician.ipi ?? '',
        birthYear: this.musician.birthYear ?? null,
        deathYear: this.musician.deathYear ?? null,
      });
    } else {
      this.form.reset();
    }
  }

  protected onSave(): void {
    if (this.form.invalid) return;

    this.saving = true;
    const raw = this.form.getRawValue();
    const payload = convertEmptyStringsToNull(raw);

    const request$: Observable<void> = this.isEdit
      ? this.api.update(this.musician!.id!, payload as Musician)
      : this.api.create(payload as Musician).pipe(map(() => {}));

    request$.subscribe({
      next: () => {
        this.saving = false;
        this.saved.emit();
      },
      error: () => {
        this.saving = false;
      },
    });
  }
}
