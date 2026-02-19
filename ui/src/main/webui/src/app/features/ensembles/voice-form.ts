import { Component, EventEmitter, inject, Input, OnChanges, Output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { InputText } from 'primeng/inputtext';
import { InputNumber } from 'primeng/inputnumber';
import { Checkbox } from 'primeng/checkbox';
import { Button } from 'primeng/button';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { TranslationService } from '../../core/translation.service';
import { EnsemblesApiService } from '../../core/api';
import { convertEmptyStringsToNull } from '../../shared/utils/object.utils';
import { CreateEnsembleVoice, EnsembleVoice } from '../../model/datamodels';
import { map, Observable } from 'rxjs';

@Component({
  selector: 'app-voice-form',
  imports: [ReactiveFormsModule, InputText, InputNumber, Checkbox, Button, TranslatePipe],
  templateUrl: './voice-form.html',
  styleUrl: './voice-form.scss',
})
export class VoiceForm implements OnChanges {
  protected readonly t = inject(TranslationService);
  private readonly api = inject(EnsemblesApiService);

  @Input({ required: true }) ensembleId!: string;
  @Input() voice: EnsembleVoice | null = null;
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  protected saving = false;

  protected readonly form = new FormGroup({
    label: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    weight: new FormControl<number | null>(null),
    required: new FormControl(false, { nonNullable: true }),
  });

  get isEdit(): boolean {
    return this.voice !== null;
  }

  ngOnChanges(): void {
    if (this.voice) {
      this.form.patchValue({
        label: this.voice.label,
        weight: this.voice.weight ?? null,
        required: this.voice.required ?? false,
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
      ? this.api.updateVoice(this.ensembleId, this.voice!.id!, payload as EnsembleVoice)
      : this.api.createVoice(this.ensembleId, payload as CreateEnsembleVoice).pipe(map(() => {}));

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
