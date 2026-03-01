import {
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Subject, debounceTime } from 'rxjs';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { Dialog } from 'primeng/dialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { ProgressBar } from 'primeng/progressbar';
import { FileUpload, FileSelectEvent } from 'primeng/fileupload';
import { Toolbar } from 'primeng/toolbar';
import { Select } from 'primeng/select';
import { FloatLabel } from 'primeng/floatlabel';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { InputText } from 'primeng/inputtext';
import { Tag } from 'primeng/tag';
import { Tooltip } from 'primeng/tooltip';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { TranslationService } from '../../core/translation.service';
import { DocumentsApiService, UploadProgress } from '../../core/api/documents-api.service';
import { SheetsApiService } from '../../core/api/sheets-api.service';
import { InstrumentationsApiService } from '../../core/api/instrumentations-api.service';
import {
  AttachmentType,
  DocumentDownload,
  Instrumentation,
  SheetMusicSearchResult,
} from '../../model/datamodels';
import { formatSize } from '../../shared/utils/format.utils';
import { ATTACHMENT_TYPES } from '../../shared/constants';

interface ActiveUpload {
  filename: string;
  progress: number;
}

/** Single picker dialog serves both "set upload target" and "assign existing doc" */
type PickerContext = 'upload' | 'assign';

@Component({
  selector: 'app-uploads',
  imports: [
    FormsModule,
    TableModule,
    ConfirmDialog,
    Dialog,
    Button,
    ProgressBar,
    FileUpload,
    Toolbar,
    Select,
    FloatLabel,
    IconField,
    InputIcon,
    InputText,
    Tag,
    Tooltip,
    TranslatePipe,
  ],
  providers: [ConfirmationService],
  templateUrl: './uploads.html',
  styleUrl: './uploads.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Uploads implements OnInit {
  protected readonly t = inject(TranslationService);
  private readonly documentsApi = inject(DocumentsApiService);
  private readonly sheetsApi = inject(SheetsApiService);
  private readonly instrumentationsApi = inject(InstrumentationsApiService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);
  private readonly destroyRef = inject(DestroyRef);

  // ── Unlinked documents ────────────────────────────────────────────
  protected readonly documents = signal<DocumentDownload[]>([]);
  protected readonly totalRecords = signal(0);
  protected readonly loading = signal(true);
  protected readonly activeUploads = signal<ActiveUpload[]>([]);

  // ── Upload target (persists while page is open) ───────────────────
  protected readonly uploadTargetSheet = signal<SheetMusicSearchResult | null>(null);
  protected readonly uploadTargetInstr = signal<Instrumentation | null>(null);
  protected readonly uploadTargetType = signal<AttachmentType | null>(null);
  protected readonly uploadTargetLabel = computed(() => {
    const sheet = this.uploadTargetSheet();
    if (!sheet) return null;
    const instr = this.uploadTargetInstr();
    if (instr) {
      return `${sheet.title} › ${instr.instrument.displayName || instr.instrument.name}${instr.partLabel ? ` (${instr.partLabel})` : ''}`;
    }
    return sheet.title;
  });

  // ── Shared picker dialog ──────────────────────────────────────────
  protected readonly pickerVisible = signal(false);
  protected pickerContext: PickerContext = 'upload';
  protected readonly pickerDoc = signal<DocumentDownload | null>(null);

  // Picker search
  protected pickerQuery = '';
  private pickerPage = 0;
  protected pickerRows = 10;
  private readonly pickerSearch$ = new Subject<string>();
  protected readonly pickerResults = signal<SheetMusicSearchResult[]>([]);
  protected readonly pickerTotal = signal(0);
  protected readonly pickerLoading = signal(false);

  // Picker selection
  protected readonly pickerSelectedSheet = signal<SheetMusicSearchResult | null>(null);
  protected readonly pickerInstrs = signal<Instrumentation[]>([]);
  protected readonly pickerInstrsLoading = signal(false);
  protected pickerInstrModel: Instrumentation | null = null;
  protected readonly pickerSelectedInstr = signal<Instrumentation | null>(null);
  protected readonly pickerInstrOptions = computed(() =>
    this.pickerInstrs().map((i) => ({
      label: this.instrLabel(i),
      value: i,
    })),
  );
  protected pickerTypeModel: AttachmentType | null = null;
  protected readonly pickerTypeOptions = computed(() =>
    ATTACHMENT_TYPES.map((t) => ({
      label: this.t.t(`uploads.attachmentTypes.${t}`),
      value: t,
    })),
  );

  protected readonly assigning = signal(false);

  private readonly topLevelPath = this.documentsApi.forTopLevel();

  ngOnInit(): void {
    this.loadDocuments();
    this.pickerSearch$
      .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.doPickerSearch());
  }

  // ── Upload target ─────────────────────────────────────────────────

  protected openUploadPicker(): void {
    this.openPicker('upload', null);
  }

  protected clearUploadTarget(): void {
    this.uploadTargetSheet.set(null);
    this.uploadTargetInstr.set(null);
    this.uploadTargetType.set(null);
  }

  // ── Assign existing doc ───────────────────────────────────────────

  protected openAssignPicker(doc: DocumentDownload): void {
    this.openPicker('assign', doc);
  }

  // ── Picker dialog ─────────────────────────────────────────────────

  private openPicker(context: PickerContext, doc: DocumentDownload | null): void {
    this.pickerContext = context;
    this.pickerDoc.set(doc);
    this.pickerQuery = '';
    this.pickerPage = 0;
    this.pickerSelectedSheet.set(null);
    this.pickerInstrs.set([]);
    this.pickerInstrModel = null;
    this.pickerSelectedInstr.set(null);
    this.pickerTypeModel = null;
    this.pickerVisible.set(true);
    this.doPickerSearch();
  }

  protected onPickerSearchInput(event: Event): void {
    this.pickerPage = 0;
    this.pickerQuery = (event.target as HTMLInputElement).value;
    this.pickerSearch$.next(this.pickerQuery);
  }

  protected onPickerLazyLoad(event: TableLazyLoadEvent): void {
    this.pickerPage = Math.floor((event.first ?? 0) / (event.rows ?? this.pickerRows));
    this.pickerRows = event.rows ?? this.pickerRows;
    this.doPickerSearch();
  }

  protected selectPickerSheet(sheet: SheetMusicSearchResult): void {
    this.pickerSelectedSheet.set(sheet);
    this.pickerInstrModel = null;
    this.pickerSelectedInstr.set(null);
    this.pickerInstrs.set([]);
    this.pickerInstrsLoading.set(true);
    this.instrumentationsApi.list(sheet.id!).subscribe({
      next: (list) => {
        this.pickerInstrs.set(list);
        this.pickerInstrsLoading.set(false);
      },
      error: () => this.pickerInstrsLoading.set(false),
    });
  }

  protected isPickerSheetSelected(sheet: SheetMusicSearchResult): boolean {
    return this.pickerSelectedSheet()?.id === sheet.id;
  }

  protected onPickerInstrChange(event: { value: Instrumentation | null }): void {
    this.pickerSelectedInstr.set(event.value);
  }

  protected onPickerInstrClear(): void {
    this.pickerInstrModel = null;
    this.pickerSelectedInstr.set(null);
  }

  protected confirmPicker(): void {
    const sheet = this.pickerSelectedSheet();
    if (!sheet) return;
    const instr = this.pickerSelectedInstr();

    const type = this.pickerTypeModel ?? undefined;

    if (this.pickerContext === 'upload') {
      this.uploadTargetSheet.set(sheet);
      this.uploadTargetInstr.set(instr);
      this.uploadTargetType.set(type ?? null);
      this.pickerVisible.set(false);
      return;
    }

    // assign context
    const doc = this.pickerDoc();
    if (!doc) return;
    this.assigning.set(true);
    this.documentsApi.linkToSheet(doc.id!, sheet.id!, instr?.id ?? undefined, type).subscribe({
      next: () => {
        this.assigning.set(false);
        this.pickerVisible.set(false);
        this.messageService.add({
          severity: 'success',
          summary: this.t.t('uploads.messages.assigned'),
        });
        this.loadDocuments();
      },
      error: () => {
        this.assigning.set(false);
        this.messageService.add({ severity: 'error', summary: this.t.t('uploads.messages.error') });
      },
    });
  }

  private doPickerSearch(): void {
    this.pickerLoading.set(true);
    this.sheetsApi
      .find({ query: this.pickerQuery || undefined, page: this.pickerPage, size: this.pickerRows })
      .subscribe({
        next: (res) => {
          this.pickerResults.set(res.data ?? []);
          this.pickerTotal.set(res.totalCount ?? 0);
          this.pickerLoading.set(false);
        },
        error: () => this.pickerLoading.set(false),
      });
  }

  // ── File upload ───────────────────────────────────────────────────

  protected onSelect(event: FileSelectEvent): void {
    for (const file of event.files) {
      this.uploadFile(file);
    }
  }

  private uploadFile(file: File): void {
    const sheet = this.uploadTargetSheet();
    const instr = this.uploadTargetInstr();
    const basePath = sheet
      ? instr
        ? this.documentsApi.forInstrumentations(sheet.id!, instr.id!)
        : this.documentsApi.forSheets(sheet.id!)
      : this.topLevelPath;

    // When linking to a sheet the backend requires a type to create the attachment.
    // Fall back to UNSPECIFIED if the user did not make a selection.
    const type: AttachmentType | undefined = sheet
      ? (this.uploadTargetType() ?? 'UNSPECIFIED')
      : undefined;

    const upload: ActiveUpload = { filename: file.name, progress: 0 };
    this.activeUploads.update((list) => [...list, upload]);

    this.documentsApi.upload(basePath, file, type).subscribe({
      next: (event: UploadProgress) => {
        if (event.type === 'progress') {
          upload.progress = event.progress ?? 0;
          this.activeUploads.update((list) => [...list]);
        }
        if (event.type === 'complete') {
          this.activeUploads.update((list) => list.filter((u) => u !== upload));
          this.messageService.add({
            severity: 'success',
            summary: this.t.t('uploads.messages.uploaded'),
          });
          this.loadDocuments();
        }
      },
      error: () => {
        this.activeUploads.update((list) => list.filter((u) => u !== upload));
        this.messageService.add({ severity: 'error', summary: this.t.t('uploads.messages.error') });
      },
    });
  }

  // ── Table actions ─────────────────────────────────────────────────

  protected onDownload(doc: DocumentDownload): void {
    this.documentsApi.download(this.topLevelPath, doc.id!).subscribe({
      next: (response) => {
        const blob = response.body!;
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = doc.filename ?? 'download';
        a.click();
        URL.revokeObjectURL(url);
      },
    });
  }

  protected confirmDelete(doc: DocumentDownload): void {
    this.confirmationService.confirm({
      message: this.t.t('uploads.delete.confirm'),
      header: this.t.t('uploads.delete.header'),
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.documentsApi.delete(this.topLevelPath, doc.id!).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: this.t.t('uploads.messages.deleted'),
            });
            this.loadDocuments();
          },
        });
      },
    });
  }

  protected formatSize = formatSize;

  private instrLabel(i: Instrumentation): string {
    const name = i.instrument.displayName || i.instrument.name;
    const part = i.partLabel ? `${i.partLabel} ` : '';
    const transposition = i.instrument.transposition ? ` · ${i.instrument.transposition}` : '';
    return `${part}${name}${transposition}`;
  }

  protected truncateChecksum(checksum?: string): string {
    if (!checksum) return '';
    return checksum.substring(0, 12) + '…';
  }

  private loadDocuments(): void {
    this.loading.set(true);
    this.documentsApi.listUnlinked().subscribe({
      next: (res) => {
        this.documents.set(res.data ?? []);
        this.totalRecords.set(res.totalCount ?? 0);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
