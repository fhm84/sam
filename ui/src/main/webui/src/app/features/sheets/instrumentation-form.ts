import { Component, computed, inject, Input, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { FloatLabel } from 'primeng/floatlabel';
import { Select } from 'primeng/select';
import { InputText } from 'primeng/inputtext';
import { Textarea } from 'primeng/textarea';
import { Button } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { InstrumentationsApiService, InstrumentsApiService } from '../../core/api';
import { convertEmptyStringsToNull } from '../../shared/utils/object.utils';
import { instrumentLabel } from '../../shared/utils/format.utils';
import { Clef, CreateInstrumentation, Instrument, Instrumentation, NotationType, PhysicalCondition } from '../../model/datamodels';
import { map, Observable } from 'rxjs';
import { BaseForm } from '../../shared/base/base-form';
import { CLEFS, FETCH_ALL_SIZE, NOTATION_TYPES, PHYSICAL_CONDITIONS } from '../../shared/constants';

interface InstrumentOption {
  label: string;
  value: string;
}

@Component({
  selector: 'app-instrumentation-form',
  imports: [ReactiveFormsModule, FloatLabel, Select, InputText, Textarea, Button, Tooltip, TranslatePipe],
  templateUrl: './instrumentation-form.html',
  styleUrl: './instrumentation-form.scss',
})
export class InstrumentationForm extends BaseForm<Instrumentation> implements OnInit {
  private readonly api = inject(InstrumentationsApiService);
  private readonly instrumentsApi = inject(InstrumentsApiService);

  @Input({ required: true }) sheetId!: string;
  @Input() instrumentation: Instrumentation | null = null;

  protected readonly instrumentOptions = signal<InstrumentOption[]>([]);

  protected readonly clefOptions = computed(() =>
    CLEFS.map((c) => ({ label: this.t.t(`instruments.clef.${c}`), value: c })),
  );
  protected readonly notationTypeOptions = computed(() =>
    NOTATION_TYPES.map((n) => ({ label: this.t.t(`instruments.notationType.${n}`), value: n })),
  );
  protected readonly physicalConditionOptions = computed(() =>
    PHYSICAL_CONDITIONS.map((c) => ({ label: this.t.t(`sheets.instrumentations.physicalCondition.${c}`), value: c })),
  );

  readonly form = new FormGroup({
    instrumentId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    partLabel: new FormControl('', { nonNullable: true }),
    clef: new FormControl<Clef | null>(null),
    notationType: new FormControl<NotationType | null>(null),
    notes: new FormControl('', { nonNullable: true }),
    physicalLocation: new FormControl('', { nonNullable: true }),
    physicalCondition: new FormControl<PhysicalCondition | null>(null),
  });

  getEntity = () => this.instrumentation;

  ngOnInit(): void {
    this.instrumentsApi.find({ size: FETCH_ALL_SIZE }).subscribe((res) => {
      this.instrumentOptions.set(
        (res.data ?? []).map((i: Instrument) => ({ label: instrumentLabel(i), value: i.id! })),
      );
    });
  }

  patchFormValues(inst: Instrumentation): void {
    this.form.patchValue({
      instrumentId: inst.instrument?.id ?? '',
      partLabel: inst.partLabel ?? '',
      clef: inst.clef ?? null,
      notationType: inst.notationType ?? null,
      notes: inst.notes ?? '',
      physicalLocation: inst.physicalLocation ?? '',
      physicalCondition: inst.physicalCondition ?? null,
    });
  }

  buildSaveRequest(): Observable<void> {
    const raw = this.form.getRawValue();
    const payload: CreateInstrumentation = convertEmptyStringsToNull({
      instrumentId: raw.instrumentId,
      partLabel: raw.partLabel || undefined,
      clef: raw.clef ?? undefined,
      notationType: raw.notationType ?? undefined,
      notes: raw.notes || undefined,
      physicalLocation: raw.physicalLocation || undefined,
      physicalCondition: raw.physicalCondition ?? undefined,
    });

    return this.isEdit
      ? this.api.update(this.sheetId, this.instrumentation!.id!, {
          ...this.instrumentation!,
          instrument: { id: raw.instrumentId, name: '' },
          partLabel: payload.partLabel,
          clef: payload.clef,
          notationType: payload.notationType,
          notes: payload.notes,
          physicalLocation: payload.physicalLocation,
          physicalCondition: payload.physicalCondition,
        })
      : this.api.create(this.sheetId, payload).pipe(map(() => {}));
  }
}
