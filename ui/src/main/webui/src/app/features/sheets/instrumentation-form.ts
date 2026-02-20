import { Component, EventEmitter, inject, Input, OnChanges, OnInit, Output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Select } from 'primeng/select';
import { InputText } from 'primeng/inputtext';
import { Textarea } from 'primeng/textarea';
import { Button } from 'primeng/button';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { TranslationService } from '../../core/translation.service';
import { InstrumentationsApiService, InstrumentsApiService } from '../../core/api';
import { convertEmptyStringsToNull } from '../../shared/utils/object.utils';
import { Clef, CreateInstrumentation, Instrument, Instrumentation, NotationType } from '../../model/datamodels';
import { map, Observable } from 'rxjs';

interface InstrumentOption {
  label: string;
  value: string;
}

@Component({
  selector: 'app-instrumentation-form',
  imports: [ReactiveFormsModule, Select, InputText, Textarea, Button, TranslatePipe],
  templateUrl: './instrumentation-form.html',
  styleUrl: './instrumentation-form.scss',
})
export class InstrumentationForm implements OnChanges, OnInit {
  protected readonly t = inject(TranslationService);
  private readonly api = inject(InstrumentationsApiService);
  private readonly instrumentsApi = inject(InstrumentsApiService);

  @Input({ required: true }) sheetId!: string;
  @Input() instrumentation: Instrumentation | null = null;
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  protected saving = false;
  protected readonly instrumentOptions = signal<InstrumentOption[]>([]);

  protected readonly clefOptions: Clef[] = ['TREBLE', 'ALTO', 'TENOR', 'BASS'];
  protected readonly notationTypeOptions: NotationType[] = [
    'STANDARD',
    'TABLATURE',
    'PERCUSSION',
    'LEAD_SHEET',
    'GRAPHIC',
  ];

  protected readonly form = new FormGroup({
    instrumentId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    partLabel: new FormControl('', { nonNullable: true }),
    clef: new FormControl<Clef | null>(null),
    notationType: new FormControl<NotationType | null>(null),
    notes: new FormControl('', { nonNullable: true }),
  });

  get isEdit(): boolean {
    return this.instrumentation !== null;
  }

  ngOnInit(): void {
    this.instrumentsApi.find({ size: 1000 }).subscribe((res) => {
      this.instrumentOptions.set(
        (res.data ?? []).map((i: Instrument) => ({ label: i.name, value: i.id! })),
      );
    });
  }

  ngOnChanges(): void {
    if (this.instrumentation) {
      this.form.patchValue({
        instrumentId: this.instrumentation.instrument?.id ?? '',
        partLabel: this.instrumentation.partLabel ?? '',
        clef: this.instrumentation.clef ?? null,
        notationType: this.instrumentation.notationType ?? null,
        notes: this.instrumentation.notes ?? '',
      });
    } else {
      this.form.reset();
    }
  }

  protected onSave(): void {
    if (this.form.invalid) return;

    this.saving = true;
    const raw = this.form.getRawValue();
    const payload: CreateInstrumentation = convertEmptyStringsToNull({
      instrumentId: raw.instrumentId,
      partLabel: raw.partLabel || undefined,
      clef: raw.clef ?? undefined,
      notationType: raw.notationType ?? undefined,
      notes: raw.notes || undefined,
    });

    const request$: Observable<void> = this.isEdit
      ? this.api.update(this.sheetId, this.instrumentation!.id!, {
          ...this.instrumentation!,
          instrument: { id: raw.instrumentId, name: '' },
          partLabel: payload.partLabel,
          clef: payload.clef,
          notationType: payload.notationType,
          notes: payload.notes,
        })
      : this.api.create(this.sheetId, payload).pipe(map(() => {}));

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
