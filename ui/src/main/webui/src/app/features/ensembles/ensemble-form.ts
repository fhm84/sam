import { Component, EventEmitter, inject, Input, OnChanges, Output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { InputText } from 'primeng/inputtext';
import { Textarea } from 'primeng/textarea';
import { Button } from 'primeng/button';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { TranslationService } from '../../core/translation.service';
import { EnsemblesApiService } from '../../core/api';
import { convertEmptyStringsToNull } from '../../shared/utils/object.utils';
import { CreateEnsemble, Ensemble } from '../../model/datamodels';
import { map, Observable } from 'rxjs';

@Component({
  selector: 'app-ensemble-form',
  imports: [ReactiveFormsModule, InputText, Textarea, Button, TranslatePipe],
  templateUrl: './ensemble-form.html',
  styleUrl: './ensemble-form.scss',
})
export class EnsembleForm implements OnChanges {
  protected readonly t = inject(TranslationService);
  private readonly api = inject(EnsemblesApiService);

  @Input() ensemble: Ensemble | null = null;
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  protected saving = false;

  protected readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    description: new FormControl('', { nonNullable: true }),
  });

  get isEdit(): boolean {
    return this.ensemble !== null;
  }

  ngOnChanges(): void {
    if (this.ensemble) {
      this.form.patchValue({
        name: this.ensemble.name,
        description: this.ensemble.description ?? '',
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
      ? this.api.update(this.ensemble!.id!, payload as Ensemble)
      : this.api.create(payload as CreateEnsemble).pipe(map(() => {}));

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
