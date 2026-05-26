import { Component, inject, Input } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { FloatLabel } from 'primeng/floatlabel';
import { InputText } from 'primeng/inputtext';
import { InputNumber } from 'primeng/inputnumber';
import { Select } from 'primeng/select';
import { AutoComplete } from 'primeng/autocomplete';
import { Button } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { InstrumentsApiService } from '../../core/api';
import { convertEmptyStringsToNull } from '../../shared/utils/object.utils';
import {
  Clef,
  CreateInstrument,
  Instrument,
  InstrumentFamily,
  InstrumentTransposing,
} from '../../model/datamodels';
import { computed } from '@angular/core';
import { map, Observable } from 'rxjs';
import { BaseForm } from '../../shared/base/base-form';

const FAMILIES: InstrumentFamily[] = ['BRASS', 'WOODWIND', 'STRING', 'PERCUSSION', 'KEYBOARD', 'VOICE', 'OTHER'];
const CLEFS: Clef[] = ['TREBLE', 'ALTO', 'TENOR', 'BASS'];

@Component({
  selector: 'app-instrument-form',
  imports: [ReactiveFormsModule, FloatLabel, InputText, InputNumber, Select, AutoComplete, Button, Tooltip, TranslatePipe],
  templateUrl: './instrument-form.html',
})
export class InstrumentForm extends BaseForm<Instrument> {
  private readonly api = inject(InstrumentsApiService);

  @Input() instrument: Instrument | null = null;

  protected readonly transpositionOptions: string[] = ['C', 'D', 'Eb', 'F', 'G', 'A', 'Ab', 'Bb'];

  protected readonly familyOptions = computed(() =>
    FAMILIES.map((f) => ({ label: this.t.t(`instruments.family.${f}`), value: f })),
  );

  protected readonly clefOptions = computed(() =>
    CLEFS.map((c) => ({ label: this.t.t(`instruments.clef.${c}`), value: c })),
  );

  readonly form = new FormGroup({
    id: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    displayName: new FormControl('', { nonNullable: true }),
    family: new FormControl<InstrumentFamily | null>(null),
    defaultClef: new FormControl<Clef | null>(null),
    aliases: new FormControl<string[]>([], { nonNullable: true }),
    catalogSection: new FormControl('', { nonNullable: true }),
    catalogPosition: new FormControl<number | null>(null),
    transposition: new FormControl<InstrumentTransposing | null>(null),
  });

  getEntity = () => this.instrument;

  patchFormValues(i: Instrument): void {
    this.form.patchValue({
      id: i.id ?? '',
      name: i.name,
      displayName: i.displayName ?? '',
      family: i.family ?? null,
      defaultClef: i.defaultClef ?? null,
      aliases: i.aliases ?? [],
      catalogSection: i.catalogSection ?? '',
      catalogPosition: i.catalogPosition ?? null,
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
