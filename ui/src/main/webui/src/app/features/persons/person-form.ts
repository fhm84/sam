import { ChangeDetectionStrategy, Component, inject, Input, OnChanges } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { FloatLabel } from '@openng/optimus-ui/floatlabel';
import { InputText } from '@openng/optimus-ui/inputtext';
import { InputNumber } from '@openng/optimus-ui/inputnumber';
import { Button } from '@openng/optimus-ui/button';
import { Tooltip } from '@openng/optimus-ui/tooltip';
import { map, Observable } from 'rxjs';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { PersonsApiService } from '../../core/api';
import { convertEmptyStringsToNull } from '../../shared/utils/object.utils';
import { Person } from '../../model/datamodels';
import { BaseForm } from '../../shared/base/base-form';

@Component({
  selector: 'app-person-form',
  imports: [ReactiveFormsModule, FloatLabel, InputText, InputNumber, Button, Tooltip, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './person-form.html',
})
export class PersonForm extends BaseForm<Person, Person> implements OnChanges {
  private readonly api = inject(PersonsApiService);

  @Input() person: Person | null = null;

  readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    ipi: new FormControl('', { nonNullable: true }),
    birthYear: new FormControl<number | null>(null),
    deathYear: new FormControl<number | null>(null),
  });

  getEntity = () => this.person;

  patchFormValues(p: Person): void {
    this.form.patchValue({
      name: p.name,
      ipi: p.ipi ?? '',
      birthYear: p.birthYear ?? null,
      deathYear: p.deathYear ?? null,
    });
  }

  buildSaveRequest(): Observable<Person> {
    const raw = convertEmptyStringsToNull(this.form.getRawValue()) as Record<string, unknown>;
    const payload: Person = {
      name: raw['name'] as string,
      ipi: raw['ipi'] as string | undefined,
      birthYear: raw['birthYear'] as number | undefined,
      deathYear: raw['deathYear'] as number | undefined,
    };
    return this.isEdit
      ? this.api.update(this.person!.id!, payload).pipe(map(() => ({ ...payload, id: this.person!.id }) as Person))
      : this.api.create(payload);
  }
}
