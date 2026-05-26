import { Component, computed, DestroyRef, inject, Input, OnInit } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { FloatLabel } from 'primeng/floatlabel';
import { InputText } from 'primeng/inputtext';
import { Textarea } from 'primeng/textarea';
import { Select } from 'primeng/select';
import { DatePicker } from 'primeng/datepicker';
import { Button } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { CollectionsApiService } from '../../core/api';
import { convertEmptyStringsToNull } from '../../shared/utils/object.utils';
import { CollectionType, CollectionVisibility, SheetCollection } from '../../model/datamodels';
import { map, Observable } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { BaseForm } from '../../shared/base/base-form';
import { TranslationService } from '../../core/translation.service';

const VISIBILITIES: CollectionVisibility[] = ['WHOLE_ENSEMBLE', 'ADMINS_ONLY', 'PRIVATE'];

export const COVER_COLORS = [
  '#ef4444', '#f97316', '#eab308', '#22c55e',
  '#3b82f6', '#8b5cf6', '#ec4899', '#6b7280',
];

@Component({
  selector: 'app-collection-form',
  imports: [ReactiveFormsModule, FloatLabel, InputText, Textarea, Select, DatePicker, Button, Tooltip, TranslatePipe],
  templateUrl: './collection-form.html',
})
export class CollectionForm extends BaseForm<SheetCollection> implements OnInit {
  private readonly api = inject(CollectionsApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly i18n = inject(TranslationService);

  @Input() collection: SheetCollection | null = null;

  protected readonly coverColors = COVER_COLORS;

  protected readonly typeOptions = computed<{ label: string; value: CollectionType }[]>(() => {
    void this.i18n.version();
    return [
      { label: this.i18n.t('collections.types.FOLDER'), value: 'FOLDER' },
      { label: this.i18n.t('collections.types.SETLIST'), value: 'SETLIST' },
    ];
  });

  protected readonly visibilityOptions = computed(() =>
    VISIBILITIES.map((v) => ({ label: this.i18n.t(`collections.visibility.${v}`), value: v })),
  );

  readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    description: new FormControl('', { nonNullable: true }),
    type: new FormControl<CollectionType | null>(null),
    date: new FormControl<Date | null>(null),
    visibility: new FormControl<CollectionVisibility | null>(null),
    coverColor: new FormControl<string | null>(null),
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
      visibility: c.visibility ?? null,
      coverColor: c.coverColor ?? null,
    });
  }

  selectCoverColor(color: string): void {
    const current = this.form.controls.coverColor.value;
    this.form.controls.coverColor.setValue(current === color ? null : color);
  }

  private toLocalDateString(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  buildSaveRequest(): Observable<void> {
    const raw = this.form.getRawValue();
    const payload = convertEmptyStringsToNull({
      name: raw.name,
      description: raw.description,
      type: raw.type ?? undefined,
      date: raw.date ? this.toLocalDateString(raw.date) : undefined,
      visibility: raw.visibility ?? undefined,
      coverColor: raw.coverColor ?? undefined,
    }) as SheetCollection;

    return this.isEdit
      ? this.api.update(this.collection!.id!, payload)
      : this.api.create(payload).pipe(map(() => {}));
  }
}
