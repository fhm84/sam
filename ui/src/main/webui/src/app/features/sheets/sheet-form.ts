import { Component, inject, Input, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { FloatLabel } from 'primeng/floatlabel';
import { InputText } from 'primeng/inputtext';
import { InputNumber } from 'primeng/inputnumber';
import { Textarea } from 'primeng/textarea';
import { Select } from 'primeng/select';
import { Button } from 'primeng/button';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { SheetsApiService, MusiciansApiService } from '../../core/api';
import { convertEmptyStringsToNull } from '../../shared/utils/object.utils';
import { CreateSheetMusic, Musician, SheetMusic } from '../../model/datamodels';
import { map, Observable } from 'rxjs';
import { BaseForm } from '../../shared/base/base-form';
import { FETCH_ALL_SIZE } from '../../shared/constants';

@Component({
  selector: 'app-sheet-form',
  imports: [ReactiveFormsModule, FloatLabel, InputText, InputNumber, Textarea, Select, Button, TranslatePipe],
  templateUrl: './sheet-form.html',
  styleUrl: './sheet-form.scss',
})
export class SheetForm extends BaseForm<SheetMusic, SheetMusic> implements OnInit {
  private readonly api = inject(SheetsApiService);
  private readonly musiciansApi = inject(MusiciansApiService);

  @Input() sheet: SheetMusic | null = null;

  protected readonly musicians = signal<Musician[]>([]);

  readonly form = new FormGroup({
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

  getEntity = () => this.sheet;

  ngOnInit(): void {
    this.musiciansApi.find({ size: FETCH_ALL_SIZE }).subscribe((res) => {
      this.musicians.set(res.data ?? []);
    });
  }

  patchFormValues(s: SheetMusic): void {
    this.form.patchValue({
      title: s.title,
      subtitle: s.subtitle ?? '',
      composerId: s.composer?.id ?? null,
      arrangerId: s.arranger?.id ?? null,
      genre: s.genre ?? '',
      yearOfComposition: s.yearOfComposition ?? null,
      publisher: s.publisher ?? '',
      difficultyLevel: s.difficultyLevel ?? '',
      edition: s.edition ?? '',
      copyright: s.copyright ?? '',
      additionalNotes: s.additionalNotes ?? '',
    });
  }

  buildSaveRequest(): Observable<SheetMusic> {
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

    return this.isEdit
      ? this.api.update(this.sheet!.id!, { ...payload, id: this.sheet!.id } as SheetMusic).pipe(
          map(() => ({ ...payload, id: this.sheet!.id }) as SheetMusic),
        )
      : this.api.create(payload);
  }
}
