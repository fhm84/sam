import { Component, EventEmitter, inject, Input, OnChanges, Output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { InputText } from 'primeng/inputtext';
import { Textarea } from 'primeng/textarea';
import { Select } from 'primeng/select';
import { DatePicker } from 'primeng/datepicker';
import { Button } from 'primeng/button';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { TranslationService } from '../../core/translation.service';
import { CollectionsApiService } from '../../core/api';
import { convertEmptyStringsToNull } from '../../shared/utils/object.utils';
import { CollectionType, SheetCollection } from '../../model/datamodels';
import { map, Observable } from 'rxjs';

@Component({
  selector: 'app-collection-form',
  imports: [ReactiveFormsModule, InputText, Textarea, Select, DatePicker, Button, TranslatePipe],
  templateUrl: './collection-form.html',
  styleUrl: './collection-form.scss',
})
export class CollectionForm implements OnChanges {
  protected readonly t = inject(TranslationService);
  private readonly api = inject(CollectionsApiService);

  @Input() collection: SheetCollection | null = null;
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  protected saving = false;

  protected readonly typeOptions: { label: string; value: CollectionType }[] = [
    { label: 'Folder', value: 'FOLDER' },
    { label: 'Setlist', value: 'SETLIST' },
  ];

  protected readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    description: new FormControl('', { nonNullable: true }),
    type: new FormControl<CollectionType | null>(null),
    date: new FormControl<Date | null>(null),
  });

  get isEdit(): boolean {
    return this.collection !== null;
  }

  ngOnChanges(): void {
    if (this.collection) {
      this.form.patchValue({
        name: this.collection.name,
        description: this.collection.description ?? '',
        type: this.collection.type ?? null,
        date: this.collection.date ? new Date(this.collection.date) : null,
      });
    } else {
      this.form.reset();
    }
  }

  protected onSave(): void {
    if (this.form.invalid) return;

    this.saving = true;
    const raw = this.form.getRawValue();
    const payload = convertEmptyStringsToNull({
      name: raw.name,
      description: raw.description,
      type: raw.type ?? undefined,
      date: raw.date ?? undefined,
    }) as SheetCollection;

    const request$: Observable<void> = this.isEdit
      ? this.api.update(this.collection!.id!, payload)
      : this.api.create(payload).pipe(map(() => {}));

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
