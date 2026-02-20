import { Component, EventEmitter, inject, Input, OnChanges, OnInit, Output, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { InputText } from 'primeng/inputtext';
import { InputNumber } from 'primeng/inputnumber';
import { Textarea } from 'primeng/textarea';
import { Select } from 'primeng/select';
import { Button } from 'primeng/button';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { TranslationService } from '../../core/translation.service';
import { SheetsApiService, MusiciansApiService } from '../../core/api';
import { convertEmptyStringsToNull } from '../../shared/utils/object.utils';
import { CreateSheetMusic, Musician, SheetMusic } from '../../model/datamodels';
import { map, Observable } from 'rxjs';

@Component({
  selector: 'app-sheet-form',
  imports: [ReactiveFormsModule, InputText, InputNumber, Textarea, Select, Button, TranslatePipe],
  templateUrl: './sheet-form.html',
  styleUrl: './sheet-form.scss',
})
export class SheetForm implements OnChanges, OnInit {
  protected readonly t = inject(TranslationService);
  private readonly api = inject(SheetsApiService);
  private readonly musiciansApi = inject(MusiciansApiService);

  @Input() sheet: SheetMusic | null = null;
  @Output() saved = new EventEmitter<void>();
  @Output() cancelled = new EventEmitter<void>();

  protected saving = false;
  protected readonly musicians = signal<Musician[]>([]);

  protected readonly form = new FormGroup({
    title: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    subtitle: new FormControl('', { nonNullable: true }),
    composerId: new FormControl<string | null>(null),
    arrangerId: new FormControl<string | null>(null),
    genre: new FormControl('', { nonNullable: true }),
    yearOfComposition: new FormControl<number | null>(null),
    publisher: new FormControl('', { nonNullable: true }),
    difficultyLevel: new FormControl('', { nonNullable: true }),
    edition: new FormControl('', { nonNullable: true }),
    copyright: new FormControl('', { nonNullable: true }),
    additionalNotes: new FormControl('', { nonNullable: true }),
  });

  get isEdit(): boolean {
    return this.sheet !== null;
  }

  ngOnInit(): void {
    this.musiciansApi.find({ size: 1000 }).subscribe((res) => {
      this.musicians.set(res.data ?? []);
    });
  }

  ngOnChanges(): void {
    if (this.sheet) {
      this.form.patchValue({
        title: this.sheet.title,
        subtitle: this.sheet.subtitle ?? '',
        composerId: this.sheet.composer?.id ?? null,
        arrangerId: this.sheet.arranger?.id ?? null,
        genre: this.sheet.genre ?? '',
        yearOfComposition: this.sheet.yearOfComposition ?? null,
        publisher: this.sheet.publisher ?? '',
        difficultyLevel: this.sheet.difficultyLevel ?? '',
        edition: this.sheet.edition ?? '',
        copyright: this.sheet.copyright ?? '',
        additionalNotes: this.sheet.additionalNotes ?? '',
      });
    } else {
      this.form.reset();
    }
  }

  protected onSave(): void {
    if (this.form.invalid) return;

    this.saving = true;
    const raw = this.form.getRawValue();
    const allMusicians = this.musicians();

    const composer = raw.composerId
      ? allMusicians.find((m) => m.id === raw.composerId) ?? null
      : null;
    const arranger = raw.arrangerId
      ? allMusicians.find((m) => m.id === raw.arrangerId) ?? null
      : null;

    const payload: CreateSheetMusic = convertEmptyStringsToNull({
      title: raw.title,
      subtitle: raw.subtitle,
      composer: composer ? { id: composer.id, name: composer.name } : undefined,
      arranger: arranger ? { id: arranger.id, name: arranger.name } : undefined,
      genre: raw.genre,
      yearOfComposition: raw.yearOfComposition ?? undefined,
      publisher: raw.publisher,
      difficultyLevel: raw.difficultyLevel,
      edition: raw.edition,
      copyright: raw.copyright,
      additionalNotes: raw.additionalNotes,
    });

    const request$: Observable<void> = this.isEdit
      ? this.api.update(this.sheet!.id!, { ...payload, id: this.sheet!.id } as SheetMusic)
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
