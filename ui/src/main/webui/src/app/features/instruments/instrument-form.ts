import { Component, EventEmitter, inject, Input, OnChanges, Output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { Button } from 'primeng/button';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { TranslationService } from '../../core/translation.service';
import { InstrumentsApiService } from '../../core/api';
import { convertEmptyStringsToNull } from '../../shared/utils/object.utils';
import { CreateInstrument, Instrument, InstrumentTransposing } from '../../model/datamodels';
import { map, Observable } from 'rxjs';

@Component({
  selector: 'app-instrument-form',
  imports: [ReactiveFormsModule, InputText, Select, Button, TranslatePipe],
  templateUrl: './instrument-form.html',
  styleUrl: './instrument-form.scss',
})
export class InstrumentForm implements OnChanges {
  protected readonly t = inject(TranslationService);
  private readonly api = inject(InstrumentsApiService);

  @Input() instrument: Instrument | null = null;
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  protected saving = false;

  protected readonly transpositionOptions: string[] = ['C', 'D', 'Eb', 'F', 'G', 'A', 'Ab', 'Bb'];

  protected readonly form = new FormGroup({
    id: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    displayName: new FormControl('', { nonNullable: true }),
    transposition: new FormControl<InstrumentTransposing | null>(null),
  });

  get isEdit(): boolean {
    return this.instrument !== null;
  }

  ngOnChanges(): void {
    if (this.instrument) {
      this.form.patchValue({
        id: this.instrument.id ?? '',
        name: this.instrument.name,
        displayName: this.instrument.displayName ?? '',
        transposition: this.instrument.transposition ?? null,
      });
      this.form.controls.id.disable();
    } else {
      this.form.reset();
      this.form.controls.id.enable();
    }
  }

  protected onSave(): void {
    if (this.form.invalid) return;

    this.saving = true;
    const raw = this.form.getRawValue();
    const payload = convertEmptyStringsToNull(raw);

    const request$: Observable<void> = this.isEdit
      ? this.api.update(this.instrument!.id!, payload as Instrument)
      : this.api.create(payload as CreateInstrument).pipe(map(() => {}));

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
