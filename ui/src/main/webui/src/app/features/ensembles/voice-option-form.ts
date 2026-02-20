import { Component, EventEmitter, inject, Input, OnChanges, OnInit, Output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Select } from 'primeng/select';
import { InputNumber } from 'primeng/inputnumber';
import { Button } from 'primeng/button';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { TranslationService } from '../../core/translation.service';
import { EnsemblesApiService, InstrumentsApiService } from '../../core/api';
import { convertEmptyStringsToNull } from '../../shared/utils/object.utils';
import { CreateVoiceOption, Instrument, VoiceOption, VoiceOptionType } from '../../model/datamodels';
import { map, Observable } from 'rxjs';

interface InstrumentOption {
  label: string;
  value: string;
}

@Component({
  selector: 'app-voice-option-form',
  imports: [ReactiveFormsModule, Select, InputNumber, Button, TranslatePipe],
  templateUrl: './voice-option-form.html',
  styleUrl: './voice-option-form.scss',
})
export class VoiceOptionForm implements OnChanges, OnInit {
  protected readonly t = inject(TranslationService);
  private readonly api = inject(EnsemblesApiService);
  private readonly instrumentsApi = inject(InstrumentsApiService);

  @Input({ required: true }) ensembleId!: string;
  @Input({ required: true }) voiceId!: string;
  @Input() option: VoiceOption | null = null;
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  protected saving = false;
  protected readonly instrumentOptions = signal<InstrumentOption[]>([]);

  protected readonly typeOptions: VoiceOptionType[] = ['PRIMARY', 'ALTERNATE', 'FALLBACK'];

  protected readonly form = new FormGroup({
    instrumentId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    type: new FormControl<VoiceOptionType | null>(null),
    factor: new FormControl<number | null>(null),
  });

  get isEdit(): boolean {
    return this.option !== null;
  }

  ngOnInit(): void {
    this.instrumentsApi.find({ size: 1000 }).subscribe((res) => {
      this.instrumentOptions.set(
        (res.data ?? []).map((i: Instrument) => ({ label: i.name, value: i.id! })),
      );
    });
  }

  ngOnChanges(): void {
    if (this.option) {
      this.form.patchValue({
        instrumentId: this.option.instrumentId,
        type: this.option.type ?? null,
        factor: this.option.factor ?? null,
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
      ? this.api.updateVoiceOption(this.ensembleId, this.voiceId, this.option!.id!, payload as VoiceOption)
      : this.api.createVoiceOption(this.ensembleId, this.voiceId, payload as CreateVoiceOption).pipe(map(() => {}));

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
