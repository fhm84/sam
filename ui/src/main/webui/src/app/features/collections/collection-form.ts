import { Component, DestroyRef, inject, Input, OnInit } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { FloatLabel } from 'primeng/floatlabel';
import { InputText } from 'primeng/inputtext';
import { Textarea } from 'primeng/textarea';
import { Select } from 'primeng/select';
import { DatePicker } from 'primeng/datepicker';
import { Button } from 'primeng/button';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { CollectionsApiService } from '../../core/api';
import { convertEmptyStringsToNull } from '../../shared/utils/object.utils';
import { CollectionType, SheetCollection } from '../../model/datamodels';
import { map, Observable } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BaseForm } from '../../shared/base/base-form';

@Component({
  selector: 'app-collection-form',
  imports: [ReactiveFormsModule, FloatLabel, InputText, Textarea, Select, DatePicker, Button, TranslatePipe],
  templateUrl: './collection-form.html',
})
export class CollectionForm extends BaseForm<SheetCollection> implements OnInit {
  private readonly api = inject(CollectionsApiService);
  private readonly destroyRef = inject(DestroyRef);

  @Input() collection: SheetCollection | null = null;

  protected readonly typeOptions: { label: string; value: CollectionType }[] = [
    { label: 'Folder', value: 'FOLDER' },
    { label: 'Setlist', value: 'SETLIST' },
  ];

  readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    description: new FormControl('', { nonNullable: true }),
    type: new FormControl<CollectionType | null>(null),
    date: new FormControl<Date | null>(null),
  });

  protected get isSetlist(): boolean {
    return this.form.controls.type.value === 'SETLIST';
  }

  ngOnInit(): void {
    this.form.controls.type.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((type) => {
        if (type !== 'SETLIST') {
          this.form.controls.date.setValue(null);
        }
      });
  }

  getEntity = () => this.collection;

  patchFormValues(c: SheetCollection): void {
    this.form.patchValue({
      name: c.name,
      description: c.description ?? '',
      type: c.type ?? null,
      date: c.date ? new Date(c.date) : null,
    });
  }

  buildSaveRequest(): Observable<void> {
    const raw = this.form.getRawValue();
    const payload = convertEmptyStringsToNull({
      name: raw.name,
      description: raw.description,
      type: raw.type ?? undefined,
      date: raw.date ?? undefined,
    }) as SheetCollection;

    return this.isEdit
      ? this.api.update(this.collection!.id!, payload)
      : this.api.create(payload).pipe(map(() => {}));
  }
}
