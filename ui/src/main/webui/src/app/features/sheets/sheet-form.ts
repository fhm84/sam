import { Component, computed, inject, Input, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { FloatLabel } from 'primeng/floatlabel';
import { InputText } from 'primeng/inputtext';
import { InputNumber } from 'primeng/inputnumber';
import { Textarea } from 'primeng/textarea';
import { Select } from 'primeng/select';
import { AutoComplete } from 'primeng/autocomplete';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { Tooltip } from 'primeng/tooltip';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { SheetsApiService, MusiciansApiService } from '../../core/api';
import { convertEmptyStringsToNull } from '../../shared/utils/object.utils';
import { CreateSheetMusic, Genre, Musician, SheetMusic, Style } from '../../model/datamodels';
import { map, Observable } from 'rxjs';
import { BaseForm } from '../../shared/base/base-form';
import { DIFFICULTY_LEVELS, FETCH_ALL_SIZE, GENRES, STYLES } from '../../shared/constants';
import { MusicianForm } from '../musicians/musician-form';

@Component({
  selector: 'app-sheet-form',
  imports: [ReactiveFormsModule, FloatLabel, InputText, InputNumber, Textarea, Select, AutoComplete, Button, Dialog, Tooltip, TranslatePipe, MusicianForm],
  templateUrl: './sheet-form.html',
})
export class SheetForm extends BaseForm<SheetMusic, SheetMusic> implements OnInit {
  private readonly api = inject(SheetsApiService);
  private readonly musiciansApi = inject(MusiciansApiService);

  @Input() sheet: SheetMusic | null = null;

  protected readonly musicians = signal<Musician[]>([]);

  protected showMusicianDialog = false;
  protected musicianDialogTarget: 'composer' | 'arranger' = 'composer';

  protected readonly genreOptions = computed(() =>
    GENRES.map((g) => ({ label: this.t.t(`sheets.genres.${g}`), value: g })),
  );

  protected readonly styleOptions = computed(() =>
    STYLES.map((s) => ({ label: this.t.t(`sheets.styles.${s}`), value: s })),
  );

  protected readonly difficultyOptions = computed(() =>
    DIFFICULTY_LEVELS.map((key, i) => ({ label: this.t.t(`sheets.difficultyLevels.${key}`), value: i + 1 })),
  );

  readonly form = new FormGroup({
    title: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    subtitle: new FormControl('', { nonNullable: true }),
    composerId: new FormControl<string | null>(null),
    arrangerId: new FormControl<string | null>(null),
    genre: new FormControl<Genre | null>(null),
    style: new FormControl<Style | null>(null),
    yearOfComposition: new FormControl<number | null>(null),
    publisher: new FormControl('', { nonNullable: true }),
    difficultyLevel: new FormControl<number | null>(null),
    edition: new FormControl('', { nonNullable: true }),
    copyright: new FormControl('', { nonNullable: true }),
    additionalNotes: new FormControl('', { nonNullable: true }),
    tags: new FormControl<string[]>([], { nonNullable: true }),
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
      genre: s.genre ?? null,
      style: s.style ?? null,
      yearOfComposition: s.yearOfComposition ?? null,
      publisher: s.publisher ?? '',
      difficultyLevel: s.difficultyLevel ?? null,
      edition: s.edition ?? '',
      copyright: s.copyright ?? '',
      additionalNotes: s.additionalNotes ?? '',
      tags: s.tags ?? [],
    });
  }

  openMusicianDialog(target: 'composer' | 'arranger'): void {
    this.musicianDialogTarget = target;
    this.showMusicianDialog = true;
  }

  onMusicianCreated(musician: Musician): void {
    this.musicians.update((list) => [...list, musician].sort((a, b) => a.name.localeCompare(b.name)));
    if (this.musicianDialogTarget === 'composer') {
      this.form.patchValue({ composerId: musician.id! });
    } else {
      this.form.patchValue({ arrangerId: musician.id! });
    }
    this.showMusicianDialog = false;
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
      genre: raw.genre ?? undefined,
      style: raw.style ?? undefined,
      yearOfComposition: raw.yearOfComposition ?? undefined,
      publisher: raw.publisher,
      difficultyLevel: raw.difficultyLevel ?? undefined,
      edition: raw.edition,
      copyright: raw.copyright,
      additionalNotes: raw.additionalNotes,
      tags: raw.tags.length > 0 ? raw.tags : undefined,
    });

    return this.isEdit
      ? this.api.update(this.sheet!.id!, { ...payload, id: this.sheet!.id } as SheetMusic).pipe(
          map(() => ({ ...payload, id: this.sheet!.id }) as SheetMusic),
        )
      : this.api.create(payload);
  }
}
