import { Component, inject, Input } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { FloatLabel } from 'primeng/floatlabel';
import { InputText } from 'primeng/inputtext';
import { Select } from 'primeng/select';
import { Button } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { InstrumentsApiService } from '../../core/api';
import { convertEmptyStringsToNull } from '../../shared/utils/object.utils';
import { CreateInstrument, Instrument, InstrumentTransposing } from '../../model/datamodels';
import { map, Observable } from 'rxjs';
import { BaseForm } from '../../shared/base/base-form';

@Component({
  selector: 'app-instrument-form',
  imports: [ReactiveFormsModule, FloatLabel, InputText, Select, Button, Tooltip, TranslatePipe],
  templateUrl: './instrument-form.html',
  styleUrl: './instrument-form.scss',
})
export class InstrumentForm extends BaseForm<Instrument> {
  private readonly api = inject(InstrumentsApiService);

  @Input() instrument: Instrument | null = null;

  protected readonly transpositionOptions: string[] = ['C', 'D', 'Eb', 'F', 'G', 'A', 'Ab', 'Bb'];

  readonly form = new FormGroup({
    id: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    displayName: new FormControl('', { nonNullable: true }),
    transposition: new FormControl<InstrumentTransposing | null>(null),
  });

  getEntity = () => this.instrument;

  patchFormValues(i: Instrument): void {
    this.form.patchValue({
      id: i.id ?? '',
      name: i.name,
      displayName: i.displayName ?? '',
      transposition: i.transposition ?? null,
    });
    this.form.controls.id.disable();
  }

  protected override resetForm(): void {
    this.form.reset();
    this.form.controls.id.enable();
  }

  buildSaveRequest(): Observable<void> {
    const payload = convertEmptyStringsToNull(this.form.getRawValue());
    return this.isEdit
      ? this.api.update(this.instrument!.id!, payload as Instrument)
      : this.api.create(payload as CreateInstrument).pipe(map(() => {}));
  }
}
