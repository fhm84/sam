import { Component, inject, Input } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { FloatLabel } from 'primeng/floatlabel';
import { InputText } from 'primeng/inputtext';
import { Textarea } from 'primeng/textarea';
import { Button } from 'primeng/button';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { EnsemblesApiService } from '../../core/api';
import { convertEmptyStringsToNull } from '../../shared/utils/object.utils';
import { CreateEnsemble, Ensemble } from '../../model/datamodels';
import { map, Observable } from 'rxjs';
import { BaseForm } from '../../shared/base/base-form';

@Component({
  selector: 'app-ensemble-form',
  imports: [ReactiveFormsModule, FloatLabel, InputText, Textarea, Button, TranslatePipe],
  templateUrl: './ensemble-form.html',
  styleUrl: './ensemble-form.scss',
})
export class EnsembleForm extends BaseForm<Ensemble> {
  private readonly api = inject(EnsemblesApiService);

  @Input() ensemble: Ensemble | null = null;

  readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    description: new FormControl('', { nonNullable: true }),
  });

  getEntity = () => this.ensemble;

  patchFormValues(e: Ensemble): void {
    this.form.patchValue({
      name: e.name,
      description: e.description ?? '',
    });
  }

  buildSaveRequest(): Observable<void> {
    const payload = convertEmptyStringsToNull(this.form.getRawValue());
    return this.isEdit
      ? this.api.update(this.ensemble!.id!, payload as Ensemble)
      : this.api.create(payload as CreateEnsemble).pipe(map(() => {}));
  }
}
