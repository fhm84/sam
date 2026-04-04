import { Component, inject, Input } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { FloatLabel } from 'primeng/floatlabel';
import { InputText } from 'primeng/inputtext';
import { InputNumber } from 'primeng/inputnumber';
import { Button } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { MusiciansApiService } from '../../core/api';
import { convertEmptyStringsToNull } from '../../shared/utils/object.utils';
import { Musician } from '../../model/datamodels';
import { map, Observable } from 'rxjs';
import { BaseForm } from '../../shared/base/base-form';

@Component({
  selector: 'app-musician-form',
  imports: [ReactiveFormsModule, FloatLabel, InputText, InputNumber, Button, Tooltip, TranslatePipe],
  templateUrl: './musician-form.html',
})
export class MusicianForm extends BaseForm<Musician, Musician> {
  private readonly api = inject(MusiciansApiService);

  @Input() musician: Musician | null = null;

  readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    ipi: new FormControl('', { nonNullable: true }),
    birthYear: new FormControl<number | null>(null),
    deathYear: new FormControl<number | null>(null),
  });

  getEntity = () => this.musician;

  patchFormValues(m: Musician): void {
    this.form.patchValue({
      name: m.name,
      ipi: m.ipi ?? '',
      birthYear: m.birthYear ?? null,
      deathYear: m.deathYear ?? null,
    });
  }

  buildSaveRequest(): Observable<Musician> {
    const payload = convertEmptyStringsToNull(this.form.getRawValue()) as Musician;
    return this.isEdit
      ? this.api.update(this.musician!.id!, payload).pipe(map(() => ({ ...payload, id: this.musician!.id }) as Musician))
      : this.api.create(payload);
  }
}
