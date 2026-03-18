import { Component, inject, Input, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { FloatLabel } from 'primeng/floatlabel';
import { Select } from 'primeng/select';
import { InputNumber } from 'primeng/inputnumber';
import { Button } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { EnsemblesApiService, InstrumentsApiService } from '../../core/api';
import { convertEmptyStringsToNull } from '../../shared/utils/object.utils';
import { instrumentLabel } from '../../shared/utils/format.utils';
import { CreateVoiceOption, Instrument, VoiceOption, VoiceOptionType } from '../../model/datamodels';
import { map, Observable } from 'rxjs';
import { BaseForm } from '../../shared/base/base-form';
import { FETCH_ALL_SIZE } from '../../shared/constants';

interface InstrumentOption {
  label: string;
  value: string;
}

@Component({
  selector: 'app-voice-option-form',
  imports: [ReactiveFormsModule, FloatLabel, Select, InputNumber, Button, Tooltip, TranslatePipe],
  templateUrl: './voice-option-form.html',
  styleUrl: './voice-option-form.scss',
})
export class VoiceOptionForm extends BaseForm<VoiceOption> implements OnInit {
  private readonly api = inject(EnsemblesApiService);
  private readonly instrumentsApi = inject(InstrumentsApiService);

  @Input({ required: true }) ensembleId!: string;
  @Input({ required: true }) voiceId!: string;
  @Input() option: VoiceOption | null = null;

  protected readonly instrumentOptions = signal<InstrumentOption[]>([]);
  protected readonly typeOptions: VoiceOptionType[] = ['PRIMARY', 'ALTERNATE', 'FALLBACK'];

  readonly form = new FormGroup({
    instrumentId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    type: new FormControl<VoiceOptionType | null>(null),
    factor: new FormControl<number | null>(null),
  });

  getEntity = () => this.option;

  ngOnInit(): void {
    this.instrumentsApi.find({ size: FETCH_ALL_SIZE }).subscribe((res) => {
      this.instrumentOptions.set(
        (res.data ?? []).map((i: Instrument) => ({ label: instrumentLabel(i), value: i.id! })),
      );
    });
  }

  patchFormValues(o: VoiceOption): void {
    this.form.patchValue({
      instrumentId: o.instrumentId,
      type: o.type ?? null,
      factor: o.factor ?? null,
    });
  }

  buildSaveRequest(): Observable<void> {
    const payload = convertEmptyStringsToNull(this.form.getRawValue());
    return this.isEdit
      ? this.api.updateVoiceOption(this.ensembleId, this.voiceId, this.option!.id!, payload as VoiceOption)
      : this.api.createVoiceOption(this.ensembleId, this.voiceId, payload as CreateVoiceOption).pipe(map(() => {}));
  }
}
