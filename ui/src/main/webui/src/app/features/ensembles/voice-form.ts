import { Component, inject, Input } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { FloatLabel } from 'primeng/floatlabel';
import { InputText } from 'primeng/inputtext';
import { InputNumber } from 'primeng/inputnumber';
import { Checkbox } from 'primeng/checkbox';
import { Button } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { EnsemblesApiService } from '../../core/api';
import { convertEmptyStringsToNull } from '../../shared/utils/object.utils';
import { CreateEnsembleVoice, EnsembleVoice } from '../../model/datamodels';
import { map, Observable } from 'rxjs';
import { BaseForm } from '../../shared/base/base-form';

@Component({
  selector: 'app-voice-form',
  imports: [ReactiveFormsModule, FloatLabel, InputText, InputNumber, Checkbox, Button, Tooltip, TranslatePipe],
  templateUrl: './voice-form.html',
})
export class VoiceForm extends BaseForm<EnsembleVoice> {
  private readonly api = inject(EnsemblesApiService);

  @Input({ required: true }) ensembleId!: string;
  @Input() voice: EnsembleVoice | null = null;

  readonly form = new FormGroup({
    label: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    weight: new FormControl<number | null>(null),
    required: new FormControl(false, { nonNullable: true }),
  });

  getEntity = () => this.voice;

  patchFormValues(v: EnsembleVoice): void {
    this.form.patchValue({
      label: v.label,
      weight: v.weight ?? null,
      required: v.required ?? false,
    });
  }

  buildSaveRequest(): Observable<void> {
    const payload = convertEmptyStringsToNull(this.form.getRawValue());
    return this.isEdit
      ? this.api.updateVoice(this.ensembleId, this.voice!.id!, payload as EnsembleVoice)
      : this.api.createVoice(this.ensembleId, payload as CreateEnsembleVoice).pipe(map(() => {}));
  }
}
