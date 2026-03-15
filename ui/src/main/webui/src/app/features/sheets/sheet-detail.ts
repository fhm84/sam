import { Component, computed, EventEmitter, HostBinding, inject, Input, OnChanges, Output, signal } from '@angular/core';
import { TableModule } from 'primeng/table';
import { Dialog } from 'primeng/dialog';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Panel } from 'primeng/panel';
import { Select } from 'primeng/select';
import { Tabs, TabList, Tab, TabPanels, TabPanel } from 'primeng/tabs';
import { Tooltip } from 'primeng/tooltip';
import { Tag } from 'primeng/tag';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { SheetsApiService, InstrumentationsApiService } from '../../core/api';
import { Attachment, AttachmentType, CoverageSnapshotSummary, DownloadFormat, Instrumentation, SheetMusic } from '../../model/datamodels';
import { ATTACHMENT_TYPES, DIFFICULTY_LEVELS } from '../../shared/constants';
import { DocumentHandler } from '../../shared/base/document-handler';
import { InstrumentationForm } from './instrumentation-form';
import { InstrumentationDocuments, DocToggleEvent, DocsLoadedEvent } from './instrumentation-documents';
import { EnrichmentDialog } from './enrichment-dialog/enrichment-dialog';

@Component({
  selector: 'app-sheet-detail',
  imports: [
    TableModule,
    Dialog,
    ConfirmDialog,
    Button,
    Panel,
    Select,
    Tabs,
    TabList,
    Tab,
    TabPanels,
    TabPanel,
    Tooltip,
    TranslatePipe,
    FormsModule,
    InstrumentationForm,
    Tag,
    InstrumentationDocuments,
    EnrichmentDialog,
  ],
  providers: [ConfirmationService],
  templateUrl: './sheet-detail.html',
  styleUrl: './sheet-detail.scss',
})
export class SheetDetail extends DocumentHandler implements OnChanges {
  @Input() mode: 'compact' | 'full' = 'compact';

  @HostBinding('class.mode-full')
  get isFullMode() {
    return this.mode === 'full';
  }
  private readonly sheetsApi = inject(SheetsApiService);
  private readonly instrumentationsApi = inject(InstrumentationsApiService);

  @Input({ required: true }) sheetId!: string;
  @Input() autoEnrich = false;
  @Input() coverage: CoverageSnapshotSummary | null = null;
  @Input() ensembleName?: string;
  @Output() edit = new EventEmitter<SheetMusic>();
  @Output() deleted = new EventEmitter<void>();

  protected readonly sheet = signal<SheetMusic | null>(null);
  protected readonly instrumentations = signal<Instrumentation[]>([]);
  protected readonly loading = signal(true);
  protected readonly instrumentationsLoading = signal(true);

  protected instrumentationDialogVisible = false;
  protected editingInstrumentation: Instrumentation | null = null;

  // Enrichment dialog state
  protected enrichDialogVisible = false;
  protected readonly showEnrichPrompt = signal(false);

  // Relink dialog state
  protected relinkDialogVisible = false;
  protected relinkingDoc: Attachment | null = null;
  protected relinkTargetType: 'sheet' | 'instrumentation' = 'sheet';
  protected relinkInstrumentationId: string | null = null;
  protected relinkAttachmentType: AttachmentType | null = null;

  protected readonly attachmentTypeOptions = computed(() =>
    ATTACHMENT_TYPES.map((v) => ({ label: this.t.t(`uploads.attachmentTypes.${v}`), value: v }))
  );

  protected readonly instrumentationOptions = computed(() =>
    this.instrumentations().map((i) => ({
      label: i.instrument?.name + (i.partLabel ? ` (${i.partLabel})` : ''),
      value: i.id!,
    }))
  );

  // Row expansion state for the instrumentations table
  protected expandedRows: { [key: string]: boolean } = {};

  // Cache of loaded docs per instrumentation (instrId → Attachment[])
  protected readonly instrDocsCache = signal<Map<string, Attachment[]>>(new Map());

  // Selection state (instrId → Set of selected doc ids)
  protected readonly selectedDocMap = signal<Map<string, Set<string>>>(new Map());

  // Flat list of all selected {doc, instrId} entries
  protected readonly allSelectedDocs = computed(() => {
    const result: { doc: Attachment; instrId: string }[] = [];
    for (const [instrId, docIds] of this.selectedDocMap()) {
      const docs = this.instrDocsCache().get(instrId) ?? [];
      for (const doc of docs) {
        if (docIds.has(doc.id!)) result.push({ doc, instrId });
      }
    }
    return result;
  });

  // True when every selected doc is a PDF (enables "Merge PDF" button)
  protected readonly allSelectedArePdf = computed(() => {
    const selected = this.allSelectedDocs();
    return selected.length > 0 && selected.every(({ doc }) => doc.mimeType === 'application/pdf');
  });

  // All attachment types present across loaded instrumentation docs
  protected readonly availableTypes = computed(() => {
    const types = new Set<AttachmentType>();
    for (const docs of this.instrDocsCache().values()) {
      for (const doc of docs) {
        if (doc.type) types.add(doc.type);
      }
    }
    return [...types];
  });

  protected getDocumentBasePath(): string {
    return this.documentsApi.forSheets(this.sheetId);
  }

  ngOnChanges(): void {
    if (this.sheetId) {
      this.loadSheet();
      this.loadInstrumentations();
      this.loadDocuments();
    }
  }

  protected openEnrichDialog(): void {
    this.showEnrichPrompt.set(false);
    this.enrichDialogVisible = true;
  }

  protected dismissEnrichPrompt(): void {
    this.showEnrichPrompt.set(false);
  }

  protected onEnriched(): void {
    this.enrichDialogVisible = false;
    this.loadSheet();
  }

  protected onEdit(): void {
    const s = this.sheet();
    if (s) {
      this.edit.emit(s);
    }
  }

  protected openNewInstrumentation(): void {
    this.editingInstrumentation = null;
    this.instrumentationDialogVisible = true;
  }

  protected openEditInstrumentation(instr: Instrumentation): void {
    this.editingInstrumentation = { ...instr };
    this.instrumentationDialogVisible = true;
  }

  protected confirmDeleteInstrumentation(instr: Instrumentation): void {
    const instrumentName = instr.instrument?.name ?? '';
    this.confirmationService.confirm({
      message: this.t.t('sheets.instrumentations.delete.confirm').replace('{instrument}', instrumentName),
      header: this.t.t('sheets.instrumentations.delete.header'),
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.instrumentationsApi.delete(this.sheetId, instr.id!).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: this.t.t('sheets.instrumentations.messages.deleted'),
            });
            this.loadInstrumentations();
          },
          error: () => {},
        });
      },
    });
  }

  protected onInstrumentationSaved(): void {
    this.instrumentationDialogVisible = false;
    const key = this.editingInstrumentation
      ? 'sheets.instrumentations.messages.updated'
      : 'sheets.instrumentations.messages.created';
    this.messageService.add({ severity: 'success', summary: this.t.t(key) });
    this.loadInstrumentations();
  }

  protected pct(val?: number): string {
    return Math.round((val ?? 0) * 100) + '%';
  }

  protected coverageIcon(d: { missingRequired?: boolean; score?: number }): string {
    if (d.missingRequired) return 'pi pi-times-circle';
    if ((d.score ?? 0) >= 0.85) return 'pi pi-check-circle';
    if ((d.score ?? 0) > 0) return 'pi pi-minus-circle';
    return 'pi pi-circle';
  }

  protected difficultyLevelKey(grade: number): string {
    return DIFFICULTY_LEVELS[grade - 1] ?? '';
  }

  protected toggleFavorite(): void {
    const s = this.sheet();
    if (!s?.id) return;
    const wasFavorite = s.favorite;
    this.sheet.update((current) => (current ? { ...current, favorite: !wasFavorite } : current));
    const req = wasFavorite ? this.sheetsApi.unfavorite(s.id) : this.sheetsApi.favorite(s.id);
    req.subscribe({
      error: () => this.sheet.update((current) => (current ? { ...current, favorite: wasFavorite } : current)),
    });
  }

  // --- Relink document ---

  protected openRelinkDialog(doc: Attachment): void {
    this.relinkingDoc = doc;
    this.relinkAttachmentType = doc.type ?? null;
    this.relinkTargetType = 'sheet';
    this.relinkInstrumentationId = null;
    this.relinkDialogVisible = true;
  }

  protected saveRelink(): void {
    if (!this.relinkingDoc?.id) return;
    const instrId =
      this.relinkTargetType === 'instrumentation' ? (this.relinkInstrumentationId ?? undefined) : undefined;
    this.documentsApi
      .linkToSheet(this.relinkingDoc.id, this.sheetId, instrId, this.relinkAttachmentType ?? undefined)
      .subscribe({
        next: () => {
          this.relinkDialogVisible = false;
          this.messageService.add({ severity: 'success', summary: this.t.t('sheets.detail.relinkDocument.success') });
          this.loadDocuments();
          this.loadInstrumentations();
        },
        error: () => {},
      });
  }

  // --- Instrumentation document handlers ---

  protected onDocsLoaded(event: DocsLoadedEvent): void {
    this.instrDocsCache.update((m) => new Map(m).set(event.instrId, event.docs));
  }

  protected onToggleDoc(event: DocToggleEvent): void {
    this.selectedDocMap.update((m) => {
      const next = new Map(m);
      const ids = new Set(next.get(event.instrId) ?? []);
      if (event.selected) ids.add(event.docId);
      else ids.delete(event.docId);
      next.set(event.instrId, ids);
      return next;
    });
  }

  protected getSelectedIdsForInstr(instrId: string): Set<string> {
    return this.selectedDocMap().get(instrId) ?? new Set();
  }

  protected instrDocCount(instrId: string): number {
    return this.instrDocsCache().get(instrId)?.length ?? 0;
  }

  protected instrDocBadges(instrId: string): { icon: string; type: AttachmentType; count: number }[] {
    const docs = this.instrDocsCache().get(instrId) ?? [];
    const map = new Map<AttachmentType, number>();
    for (const doc of docs) {
      const t = (doc.type ?? 'UNSPECIFIED') as AttachmentType;
      map.set(t, (map.get(t) ?? 0) + 1);
    }
    return [...map.entries()].map(([type, count]) => ({
      icon: this.attachmentTypeIcon(type),
      type,
      count,
    }));
  }

  protected attachmentTypeIcon(type: AttachmentType): string {
    switch (type) {
      case 'PART':
        return 'pi pi-file';
      case 'FULL_SCORE':
        return 'pi pi-book';
      case 'AUDIO':
        return 'pi pi-volume-up';
      case 'MIDI':
        return 'pi pi-play';
      case 'ANNOTATIONS':
        return 'pi pi-pencil';
      case 'IMAGE':
      case 'COVER':
        return 'pi pi-image';
      case 'EXTERNAL_LINK':
        return 'pi pi-link';
      case 'MUSIC_XML':
        return 'pi pi-code';
      case 'LYRICS':
        return 'pi pi-align-left';
      case 'ANALYSIS':
        return 'pi pi-chart-bar';
      default:
        return 'pi pi-paperclip';
    }
  }

  // --- Batch download ---

  protected selectAllByType(type: AttachmentType): void {
    this.selectedDocMap.update((m) => {
      const next = new Map(m);
      for (const [instrId, docs] of this.instrDocsCache()) {
        const matching = docs.filter((d) => d.type === type).map((d) => d.id!);
        if (matching.length > 0) {
          const existing = new Set(next.get(instrId) ?? []);
          for (const id of matching) existing.add(id);
          next.set(instrId, existing);
        }
      }
      return next;
    });
  }

  protected clearSelection(): void {
    this.selectedDocMap.set(new Map());
  }

  protected downloadSelected(): void {
    this.triggerBatchDownload('ZIP');
  }

  protected mergePdfSelected(): void {
    this.triggerBatchDownload('MERGED_PDF');
  }

  private sanitizeFilename(title: string): string {
    return title
      .replace(/[/\\:*?"<>|]/g, '')
      .trim()
      .replace(/\s+/g, '_')
      .replace(/_+/g, '_') || 'documents';
  }

  private triggerBatchDownload(format: DownloadFormat): void {
    const ids = this.allSelectedDocs().map(({ doc }) => doc.id!);
    const baseName = this.sanitizeFilename(this.sheet()?.title ?? 'documents');
    const filename = format === 'MERGED_PDF' ? `${baseName}.pdf` : `${baseName}.zip`;
    this.documentsApi.downloadBatchByIds(ids, format, baseName).subscribe({
      next: (response) => {
        const blob = response.body!;
        const disposition = response.headers.get('Content-Disposition');
        const serverFilename = disposition?.match(/filename\*?=(?:utf-8'')?([^;]+)/i)?.[1];
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = serverFilename ? decodeURIComponent(serverFilename) : filename;
        a.click();
        URL.revokeObjectURL(url);
      },
    });
  }

  reloadSheet(): void {
    this.loadSheet();
  }

  private loadSheet(): void {
    this.loading.set(true);
    this.sheetsApi.load(this.sheetId).subscribe({
      next: (sheet) => {
        this.sheet.set(sheet);
        this.loading.set(false);
        if (this.autoEnrich) {
          this.autoEnrich = false;
          this.showEnrichPrompt.set(true);
        }
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  private loadInstrumentations(): void {
    this.instrumentationsLoading.set(true);
    this.expandedRows = {};
    this.instrDocsCache.set(new Map());
    this.selectedDocMap.set(new Map());
    this.instrumentationsApi.list(this.sheetId).subscribe({
      next: (items) => {
        this.instrumentations.set(items);
        const cache = new Map<string, Attachment[]>();
        for (const instr of items) {
          if (instr.id) cache.set(instr.id, instr.attachments ?? []);
        }
        this.instrDocsCache.set(cache);
        this.instrumentationsLoading.set(false);
      },
      error: () => {
        this.instrumentationsLoading.set(false);
      },
    });
  }
}
