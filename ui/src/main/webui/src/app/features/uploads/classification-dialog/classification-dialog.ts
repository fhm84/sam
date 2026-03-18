import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  effect,
  inject,
  input,
  OnDestroy,
  output,
  signal,
} from '@angular/core';
import { SafeResourceUrl } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { forkJoin, of, switchMap, map } from 'rxjs';
import { Dialog } from 'primeng/dialog';
import { Button } from 'primeng/button';
import { Select } from 'primeng/select';
import { FloatLabel } from 'primeng/floatlabel';
import { InputText } from 'primeng/inputtext';
import { InputNumber } from 'primeng/inputnumber';
import { Divider } from 'primeng/divider';
import { SelectButton } from 'primeng/selectbutton';
import { AutoComplete, AutoCompleteCompleteEvent, AutoCompleteSelectEvent } from 'primeng/autocomplete';
import { Checkbox } from 'primeng/checkbox';
import { MessageService } from 'primeng/api';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { TranslationService } from '../../../core/translation.service';
import { DocumentsApiService } from '../../../core/api/documents-api.service';
import { SheetsApiService } from '../../../core/api/sheets-api.service';
import { MusiciansApiService } from '../../../core/api/musicians-api.service';
import { InstrumentationsApiService } from '../../../core/api/instrumentations-api.service';
import { DocumentPreviewService } from '../../../core/document-preview.service';
import {
  AttachmentType,
  ClassificationApplyRequest,
  ClassificationApplyResult,
  Clef,
  DocumentDownload,
  Genre,
  Instrumentation,
  Musician,
  NotationType,
  SheetClassification,
  SheetMusicSearchResult,
} from '../../../model/datamodels';

export interface ClassificationAppliedEvent {
  result: ClassificationApplyResult;
  isNewSheet: boolean;
}
import { ATTACHMENT_TYPES, CLEFS, GENRES, NOTATION_TYPES } from '../../../shared/constants';
import { instrumentLabel } from '../../../shared/utils/format.utils';

type SheetMode = 'existing' | 'new';
type PersonMode = 'none' | 'existing' | 'new';
type InstrMode = 'none' | 'existing' | 'new';

@Component({
  selector: 'app-classification-dialog',
  imports: [
    FormsModule,
    Dialog,
    Button,
    Select,
    FloatLabel,
    InputText,
    InputNumber,
    Divider,
    SelectButton,
    AutoComplete,
    Checkbox,
    TranslatePipe,
  ],
  templateUrl: './classification-dialog.html',
  styleUrl: './classification-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ClassificationDialog implements OnDestroy {
  protected readonly t = inject(TranslationService);
  private readonly documentsApi = inject(DocumentsApiService);
  private readonly sheetsApi = inject(SheetsApiService);
  private readonly musiciansApi = inject(MusiciansApiService);
  private readonly instrumentationsApi = inject(InstrumentationsApiService);
  private readonly previewService = inject(DocumentPreviewService);
  private readonly messageService = inject(MessageService);
  private readonly destroyRef = inject(DestroyRef);

  readonly doc = input.required<DocumentDownload>();
  readonly visible = input(false);
  readonly visibleChange = output<boolean>();
  readonly applied = output<ClassificationAppliedEvent>();

  // ── Preview ────────────────────────────────────────────────────────
  protected readonly previewUrl = signal<SafeResourceUrl | null>(null);
  protected readonly previewLoading = signal(false);
  private currentBlobUrl: string | null = null;

  // ── Classification phase ───────────────────────────────────────────
  protected readonly phase = signal<'loading' | 'review' | 'error'>('loading');
  protected readonly classification = signal<SheetClassification | null>(null);
  protected readonly applying = signal(false);

  // ── Additional instrumentation links (existing sheet only) ────────
  protected readonly additionalInstrs = signal<Instrumentation[]>([]);
  protected readonly additionalInstrsLoading = signal(false);
  protected readonly additionalInstrIds = signal<Set<string>>(new Set());

  // ── Form modes ─────────────────────────────────────────────────────
  protected sheetMode: SheetMode = 'new';
  protected composerMode: PersonMode = 'none';
  protected arrangerMode: PersonMode = 'none';
  protected instrMode: InstrMode = 'none';

  // ── Autocomplete selections (for 'existing' modes) ─────────────────
  protected sheetSearchModel: SheetMusicSearchResult | null = null;
  protected readonly sheetSuggestions = signal<SheetMusicSearchResult[]>([]);

  protected composerModel: Musician | null = null;
  protected readonly composerSuggestions = signal<Musician[]>([]);

  protected arrangerModel: Musician | null = null;
  protected readonly arrangerSuggestions = signal<Musician[]>([]);

  // ── Sheet fields ───────────────────────────────────────────────────
  protected sheetTitle = '';
  protected sheetSubtitle = '';
  protected sheetPublisher = '';
  protected sheetGenre: Genre | null = null;
  protected sheetYear: number | null = null;
  protected sheetEdition = '';
  protected sheetIswc = '';

  // ── Composer / Arranger ───────────────────────────────────────────
  protected composerName = '';
  protected arrangerName = '';

  // ── Instrumentation ───────────────────────────────────────────────
  protected instrId: string | null = null;
  protected instrName = '';
  protected instrPartLabel = '';
  protected instrClef: Clef | null = null;
  protected instrNotation: NotationType | null = null;
  protected attachmentType: AttachmentType | null = 'UNSPECIFIED';

  // ── Select options ────────────────────────────────────────────────
  protected readonly sheetModeOptions = computed(() => [
    { label: this.t.t('classification.sheet.existingMode'), value: 'existing' as SheetMode },
    { label: this.t.t('classification.sheet.newMode'), value: 'new' as SheetMode },
  ]);

  protected readonly composerModeOptions = computed(() => [
    { label: this.t.t('classification.person.none'), value: 'none' as PersonMode },
    { label: this.t.t('classification.person.existing'), value: 'existing' as PersonMode },
    { label: this.t.t('classification.person.new'), value: 'new' as PersonMode },
  ]);

  protected readonly instrModeOptions = computed(() => [
    { label: this.t.t('classification.instr.none'), value: 'none' as InstrMode },
    { label: this.t.t('classification.instr.existing'), value: 'existing' as InstrMode },
    { label: this.t.t('classification.instr.new'), value: 'new' as InstrMode },
  ]);

  protected readonly genreOptions = computed(() =>
    GENRES.map((g) => ({ label: this.t.t(`sheets.genres.${g}`), value: g })),
  );

  protected readonly clefOptions = computed(() =>
    CLEFS.map((c) => ({ label: this.t.t(`instruments.clef.${c}`), value: c })),
  );

  protected readonly notationOptions = computed(() =>
    NOTATION_TYPES.map((n) => ({ label: this.t.t(`instruments.notationType.${n}`), value: n })),
  );

  protected readonly typeOptions = computed(() =>
    ATTACHMENT_TYPES.map((a) => ({
      label: this.t.t(`uploads.attachmentTypes.${a}`),
      value: a,
    })),
  );

  protected readonly instrumentCandidateOptions = computed(() => {
    const c = this.classification();
    if (!c?.instrumentCandidates?.length) return [];
    return c.instrumentCandidates.map((ic) => ({
      label: `${instrumentLabel({ name: ic.name ?? '', displayName: ic.displayName ?? undefined, transposition: ic.transposition ?? undefined })} (${Math.round((ic.score ?? 0) * 100)}%)`,
      value: ic.id ?? '',
    }));
  });

  protected readonly isPdf = computed(
    () => this.doc().mimeType === 'application/pdf',
  );

  constructor() {
    effect(() => {
      if (this.visible()) {
        this.startAll();
      }
    });
  }

  ngOnDestroy(): void {
    this.revokeBlobUrl();
  }

  // ── Internal ──────────────────────────────────────────────────────

  /** Kick off preview fetch and classification in parallel. */
  private startAll(): void {
    this.phase.set('loading');
    this.classification.set(null);
    this.applying.set(false);
    this.sheetSearchModel = null;
    this.sheetSuggestions.set([]);
    this.composerModel = null;
    this.composerSuggestions.set([]);
    this.arrangerModel = null;
    this.arrangerSuggestions.set([]);
    this.additionalInstrs.set([]);
    this.additionalInstrIds.set(new Set());
    this.loadPreview();
    this.runClassify();
  }

  private loadPreview(): void {
    this.revokeBlobUrl();
    this.previewLoading.set(true);

    this.previewService.load(this.doc().id!).subscribe({
      next: ({ safeUrl, rawUrl }) => {
        this.currentBlobUrl = rawUrl;
        this.previewUrl.set(safeUrl);
        this.previewLoading.set(false);
      },
      error: () => {
        // Non-fatal: preview just won't show
        this.previewLoading.set(false);
      },
    });
  }

  private runClassify(): void {
    this.documentsApi.classify(this.doc().id!).subscribe({
      next: (result) => {
        this.classification.set(result);
        this.prefillFromSuggested(result);
        this.phase.set('review');
      },
      error: () => {
        this.phase.set('error');
        this.messageService.add({
          severity: 'error',
          summary: this.t.t('classification.messages.classifyError'),
        });
      },
    });
  }

  private revokeBlobUrl(): void {
    if (this.currentBlobUrl) {
      URL.revokeObjectURL(this.currentBlobUrl);
      this.currentBlobUrl = null;
      this.previewUrl.set(null);
    }
  }

  private prefillFromSuggested(c: SheetClassification): void {
    const s = c.suggested;
    if (!s) return;

    this.sheetMode = s.sheetId ? 'existing' : 'new';
    if (s.sheetId) {
      this.sheetSearchModel = { id: s.sheetId, title: c.matchedSheetTitle ?? '' } as SheetMusicSearchResult;
      this.loadAdditionalInstrs(s.sheetId);
    }
    this.sheetTitle = s.title ?? '';
    this.sheetSubtitle = s.subtitle ?? '';
    this.sheetPublisher = s.publisher ?? '';
    this.sheetGenre = (s.genre as Genre) ?? null;
    this.sheetYear = s.yearOfComposition ?? null;
    this.sheetEdition = s.edition ?? '';
    this.sheetIswc = s.iswc ?? '';

    if (s.composerId) {
      this.composerMode = 'existing';
      this.composerModel = { id: s.composerId, name: c.composer ?? '' } as Musician;
    } else if (s.composerName) {
      this.composerMode = 'new';
      this.composerName = s.composerName;
    } else {
      this.composerMode = 'none';
      this.composerName = '';
    }

    if (s.arrangerId) {
      this.arrangerMode = 'existing';
      this.arrangerModel = { id: s.arrangerId, name: c.arranger ?? '' } as Musician;
    } else if (s.arrangerName) {
      this.arrangerMode = 'new';
      this.arrangerName = s.arrangerName;
    } else {
      this.arrangerMode = 'none';
      this.arrangerName = '';
    }

    if (s.instrumentId) {
      this.instrMode = 'existing';
      this.instrId = s.instrumentId;
    } else if (s.instrumentName) {
      this.instrMode = 'new';
      this.instrName = s.instrumentName;
      this.instrId = null;
    } else {
      this.instrMode = 'none';
      this.instrId = null;
      this.instrName = '';
    }

    this.instrPartLabel = s.partLabel ?? '';
    this.instrClef = s.clef ?? null;
    this.instrNotation = s.notationType ?? null;
    this.attachmentType = s.attachmentType ?? 'UNSPECIFIED';
  }

  // ── Public actions ─────────────────────────────────────────────────

  protected canApply(): boolean {
    if (this.phase() !== 'review') return false;
    if (this.sheetMode === 'existing' && !this.sheetSearchModel?.id) return false;
    if (this.sheetMode === 'new' && !this.sheetTitle.trim()) return false;
    if (this.composerMode === 'existing' && !this.composerModel?.id) return false;
    if (this.composerMode === 'new' && !this.composerName.trim()) return false;
    if (this.arrangerMode === 'existing' && !this.arrangerModel?.id) return false;
    if (this.arrangerMode === 'new' && !this.arrangerName.trim()) return false;
    if (this.instrMode === 'existing' && !this.instrId) return false;
    if (this.instrMode === 'new' && !this.instrName.trim()) return false;
    return true;
  }

  protected apply(): void {
    if (!this.canApply()) return;
    const c = this.classification()!;
    const s = c.suggested ?? {};
    const req: ClassificationApplyRequest = {};

    if (this.sheetMode === 'existing') {
      req.sheetId = this.sheetSearchModel?.id;
    } else {
      req.title = this.sheetTitle.trim();
      if (this.sheetSubtitle.trim()) req.subtitle = this.sheetSubtitle.trim();
      if (this.sheetPublisher.trim()) req.publisher = this.sheetPublisher.trim();
      if (this.sheetGenre) req.genre = this.sheetGenre;
      if (this.sheetYear) req.yearOfComposition = this.sheetYear;
      if (this.sheetEdition.trim()) req.edition = this.sheetEdition.trim();
      if (this.sheetIswc.trim()) req.iswc = this.sheetIswc.trim();
    }

    if (this.composerMode === 'existing') req.composerId = this.composerModel?.id;
    else if (this.composerMode === 'new') req.composerName = this.composerName.trim();

    if (this.arrangerMode === 'existing') req.arrangerId = this.arrangerModel?.id;
    else if (this.arrangerMode === 'new') req.arrangerName = this.arrangerName.trim();

    if (this.instrMode === 'existing') req.instrumentId = this.instrId ?? undefined;
    else if (this.instrMode === 'new') req.instrumentName = this.instrName.trim();

    if (this.instrMode !== 'none') {
      if (this.instrPartLabel.trim()) req.partLabel = this.instrPartLabel.trim();
      if (this.instrClef) req.clef = this.instrClef;
      if (this.instrNotation) req.notationType = this.instrNotation;
    }

    req.attachmentType = this.attachmentType ?? 'UNSPECIFIED';

    const detail = this.buildApplyDetail();
    const isNewSheet = this.sheetMode === 'new';
    const extraIds = [...this.additionalInstrIds()];
    const attachType = this.attachmentType ?? undefined;

    this.applying.set(true);
    this.documentsApi
      .apply(this.doc().id!, req)
      .pipe(
        switchMap((result) => {
          if (extraIds.length === 0) return of(result);
          const links = extraIds.map((id) =>
            this.documentsApi.linkToSheet(this.doc().id!, result.sheetId!, id, attachType),
          );
          return forkJoin(links).pipe(map(() => result));
        }),
      )
      .subscribe({
        next: (result) => {
          this.applying.set(false);
          this.messageService.add({
            severity: 'success',
            summary: this.t.t('classification.messages.applied'),
            detail,
            life: 6000,
          });
          this.applied.emit({ result, isNewSheet });
        },
        error: () => {
          this.applying.set(false);
          this.messageService.add({
            severity: 'error',
            summary: this.t.t('classification.messages.applyError'),
          });
        },
      });
  }

  private buildApplyDetail(): string {
    const sheetTitle =
      this.sheetMode === 'existing' ? (this.sheetSearchModel?.title ?? '') : this.sheetTitle;
    const composerName =
      this.composerMode === 'existing'
        ? (this.composerModel?.name ?? null)
        : this.composerMode === 'new'
          ? this.composerName
          : null;
    const parts = [sheetTitle];
    if (composerName) parts.push(`— ${composerName}`);
    if (this.instrMode !== 'none') {
      const instrName =
        this.instrMode === 'existing'
          ? (this.classification()?.instrumentCandidates?.find((ic) => ic.id === this.instrId)?.name ?? '')
          : this.instrName;
      if (instrName) parts.push(`· ${instrName}${this.instrPartLabel ? ` (${this.instrPartLabel})` : ''}`);
    }
    return parts.join(' ');
  }

  protected onSheetSearch(event: AutoCompleteCompleteEvent): void {
    this.sheetsApi.find({ query: event.query || undefined, size: 10 }).subscribe({
      next: (res) => this.sheetSuggestions.set(res.data ?? []),
      error: () => this.sheetSuggestions.set([]),
    });
  }

  protected onSheetSelect(event: AutoCompleteSelectEvent): void {
    const sheet = event.value as SheetMusicSearchResult;
    this.loadAdditionalInstrs(sheet.id!);
  }

  protected onSheetClear(): void {
    this.additionalInstrs.set([]);
    this.additionalInstrIds.set(new Set());
  }

  protected onSheetModeChange(mode: SheetMode): void {
    if (mode === 'new') {
      this.additionalInstrs.set([]);
      this.additionalInstrIds.set(new Set());
    }
  }

  protected additionalInstrIsSelected(id: string): boolean {
    return this.additionalInstrIds().has(id);
  }

  protected toggleAdditionalInstr(id: string): void {
    this.additionalInstrIds.update((set) => {
      const next = new Set(set);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  private loadAdditionalInstrs(sheetId: string): void {
    this.additionalInstrs.set([]);
    this.additionalInstrIds.set(new Set());
    this.additionalInstrsLoading.set(true);
    this.instrumentationsApi.list(sheetId).subscribe({
      next: (list) => {
        this.additionalInstrs.set(list);
        this.additionalInstrsLoading.set(false);
      },
      error: () => this.additionalInstrsLoading.set(false),
    });
  }

  protected onMusicianSearch(event: AutoCompleteCompleteEvent, role: 'composer' | 'arranger'): void {
    this.musiciansApi.find({ name: event.query || undefined, size: 10 }).subscribe({
      next: (res) => {
        if (role === 'composer') this.composerSuggestions.set(res.data ?? []);
        else this.arrangerSuggestions.set(res.data ?? []);
      },
      error: () => {
        if (role === 'composer') this.composerSuggestions.set([]);
        else this.arrangerSuggestions.set([]);
      },
    });
  }

  protected close(): void {
    this.visibleChange.emit(false);
  }

  protected retry(): void {
    this.startAll();
  }
}
