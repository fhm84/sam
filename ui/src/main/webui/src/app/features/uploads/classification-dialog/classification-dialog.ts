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
import { Dialog } from 'primeng/dialog';
import { Button } from 'primeng/button';
import { Select } from 'primeng/select';
import { FloatLabel } from 'primeng/floatlabel';
import { InputText } from 'primeng/inputtext';
import { InputNumber } from 'primeng/inputnumber';
import { Tag } from 'primeng/tag';
import { Divider } from 'primeng/divider';
import { SelectButton } from 'primeng/selectbutton';
import { MessageService } from 'primeng/api';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { TranslationService } from '../../../core/translation.service';
import { DocumentsApiService } from '../../../core/api/documents-api.service';
import { DocumentPreviewService } from '../../../core/document-preview.service';
import {
  AttachmentType,
  ClassificationApplyRequest,
  ClassificationApplyResult,
  Clef,
  DocumentDownload,
  Genre,
  NotationType,
  SheetClassification,
} from '../../../model/datamodels';
import { ATTACHMENT_TYPES, GENRES } from '../../../shared/constants';

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
    Tag,
    Divider,
    SelectButton,
    TranslatePipe,
  ],
  templateUrl: './classification-dialog.html',
  styleUrl: './classification-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ClassificationDialog implements OnDestroy {
  protected readonly t = inject(TranslationService);
  private readonly documentsApi = inject(DocumentsApiService);
  private readonly previewService = inject(DocumentPreviewService);
  private readonly messageService = inject(MessageService);
  private readonly destroyRef = inject(DestroyRef);

  readonly doc = input.required<DocumentDownload>();
  readonly visible = input(false);
  readonly visibleChange = output<boolean>();
  readonly applied = output<ClassificationApplyResult>();

  // ── Preview ────────────────────────────────────────────────────────
  protected readonly previewUrl = signal<SafeResourceUrl | null>(null);
  protected readonly previewLoading = signal(false);
  private currentBlobUrl: string | null = null;

  // ── Classification phase ───────────────────────────────────────────
  protected readonly phase = signal<'loading' | 'review' | 'error'>('loading');
  protected readonly classification = signal<SheetClassification | null>(null);
  protected readonly applying = signal(false);

  // ── Form modes ─────────────────────────────────────────────────────
  protected sheetMode: SheetMode = 'new';
  protected composerMode: PersonMode = 'none';
  protected arrangerMode: PersonMode = 'none';
  protected instrMode: InstrMode = 'none';

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

  protected readonly clefOptions: { label: string; value: Clef }[] = [
    { label: 'Treble', value: 'TREBLE' },
    { label: 'Alto', value: 'ALTO' },
    { label: 'Tenor', value: 'TENOR' },
    { label: 'Bass', value: 'BASS' },
  ];

  protected readonly notationOptions: { label: string; value: NotationType }[] = [
    { label: 'Standard', value: 'STANDARD' },
    { label: 'Tablature', value: 'TABLATURE' },
    { label: 'Percussion', value: 'PERCUSSION' },
    { label: 'Lead Sheet', value: 'LEAD_SHEET' },
    { label: 'Graphic', value: 'GRAPHIC' },
  ];

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
      label: `${ic.name} (${Math.round((ic.score ?? 0) * 100)}%)`,
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
    this.sheetTitle = s.title ?? '';
    this.sheetSubtitle = s.subtitle ?? '';
    this.sheetPublisher = s.publisher ?? '';
    this.sheetGenre = (s.genre as Genre) ?? null;
    this.sheetYear = s.yearOfComposition ?? null;
    this.sheetEdition = s.edition ?? '';
    this.sheetIswc = s.iswc ?? '';

    if (s.composerId) {
      this.composerMode = 'existing';
      this.composerName = c.composer ?? '';
    } else if (s.composerName) {
      this.composerMode = 'new';
      this.composerName = s.composerName;
    } else {
      this.composerMode = 'none';
      this.composerName = '';
    }

    if (s.arrangerId) {
      this.arrangerMode = 'existing';
      this.arrangerName = c.arranger ?? '';
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
    if (this.sheetMode === 'new' && !this.sheetTitle.trim()) return false;
    if (this.composerMode === 'new' && !this.composerName.trim()) return false;
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
      req.sheetId = s.sheetId;
    } else {
      req.title = this.sheetTitle.trim();
      if (this.sheetSubtitle.trim()) req.subtitle = this.sheetSubtitle.trim();
      if (this.sheetPublisher.trim()) req.publisher = this.sheetPublisher.trim();
      if (this.sheetGenre) req.genre = this.sheetGenre;
      if (this.sheetYear) req.yearOfComposition = this.sheetYear;
      if (this.sheetEdition.trim()) req.edition = this.sheetEdition.trim();
      if (this.sheetIswc.trim()) req.iswc = this.sheetIswc.trim();
    }

    if (this.composerMode === 'existing') req.composerId = s.composerId;
    else if (this.composerMode === 'new') req.composerName = this.composerName.trim();

    if (this.arrangerMode === 'existing') req.arrangerId = s.arrangerId;
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
    this.applying.set(true);
    this.documentsApi.apply(this.doc().id!, req).subscribe({
      next: (result) => {
        this.applying.set(false);
        this.messageService.add({
          severity: 'success',
          summary: this.t.t('classification.messages.applied'),
          detail,
          life: 6000,
        });
        this.applied.emit(result);
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
    const c = this.classification()!;
    const sheetTitle =
      this.sheetMode === 'existing' ? (c.matchedSheetTitle ?? '') : this.sheetTitle;
    const composerName =
      this.composerMode === 'existing'
        ? c.composer
        : this.composerMode === 'new'
          ? this.composerName
          : null;
    const parts = [sheetTitle];
    if (composerName) parts.push(`— ${composerName}`);
    if (this.instrMode !== 'none') {
      const instrName =
        this.instrMode === 'existing'
          ? (c.instrumentCandidates?.find((ic) => ic.id === this.instrId)?.name ?? '')
          : this.instrName;
      if (instrName) parts.push(`· ${instrName}${this.instrPartLabel ? ` (${this.instrPartLabel})` : ''}`);
    }
    return parts.join(' ');
  }

  protected close(): void {
    this.visibleChange.emit(false);
  }

  protected retry(): void {
    this.startAll();
  }
}
