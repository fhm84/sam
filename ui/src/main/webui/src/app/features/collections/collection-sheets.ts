import { Component, DestroyRef, ElementRef, inject, Input, OnChanges, OnInit, signal, ViewChild } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subject, debounceTime } from 'rxjs';
import { TableLazyLoadEvent, TableModule, TableRowReorderEvent } from 'primeng/table';
import { Dialog } from 'primeng/dialog';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Textarea } from 'primeng/textarea';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { FloatLabel } from 'primeng/floatlabel';
import { Tooltip } from 'primeng/tooltip';
import { Checkbox } from 'primeng/checkbox';
import { Tag } from 'primeng/tag';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { TranslationService } from '../../core/translation.service';
import { CollectionsApiService, SheetsApiService } from '../../core/api';
import { CollectionItem, CollectionItemType, SheetMusic, SheetMusicSearchResult } from '../../model/datamodels';
import { DIFFICULTY_LEVELS } from '../../shared/constants';

@Component({
  selector: 'app-collection-sheets',
  imports: [
    RouterLink,
    FormsModule,
    ReactiveFormsModule,
    TableModule,
    Dialog,
    ConfirmDialog,
    Button,
    InputText,
    Textarea,
    IconField,
    InputIcon,
    FloatLabel,
    Tooltip,
    Checkbox,
    Tag,
    TranslatePipe,
  ],
  providers: [ConfirmationService],
  templateUrl: './collection-sheets.html',
  styleUrl: './collection-sheets.scss',
})
export class CollectionSheets implements OnInit, OnChanges {
  protected readonly t = inject(TranslationService);
  private readonly collectionsApi = inject(CollectionsApiService);
  private readonly sheetsApi = inject(SheetsApiService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);
  private readonly destroyRef = inject(DestroyRef);

  @Input({ required: true }) collectionId!: string;

  // ── Items table ────────────────────────────────────────
  protected readonly items = signal<CollectionItem[]>([]);
  protected readonly totalRecords = signal(0);
  protected readonly loading = signal(false);
  protected readonly myPartsOnly = signal(false);
  protected rows = 1000;
  protected currentPage = 0;

  // ── Add sheet dialog ───────────────────────────────────
  protected addSheetDialogVisible = false;
  protected selectedSheet: SheetMusicSearchResult | null = null;
  protected saving = false;
  protected createAnother = false;

  protected readonly searchResults = signal<SheetMusicSearchResult[]>([]);
  protected readonly searchTotal = signal(0);
  protected readonly searchLoading = signal(false);
  protected searchRows = 10;
  protected searchPage = 0;
  private currentQuery = '';
  private readonly searchSubject = new Subject<string>();

  // ── Add text block dialog ──────────────────────────────
  protected addTextDialogVisible = false;

  // ── Edit dialog ────────────────────────────────────────
  protected editDialogVisible = false;
  protected editingItem: CollectionItem | null = null;

  // ── Preview dialog ─────────────────────────────────────
  protected previewDialogVisible = false;
  protected readonly previewSheet = signal<SheetMusic | null>(null);
  protected readonly previewLoading = signal(false);

  // ── Attachment upload ──────────────────────────────────
  @ViewChild('fileInput') private fileInputRef!: ElementRef<HTMLInputElement>;
  protected uploadingItem: CollectionItem | null = null;
  protected uploadingAttachment = false;

  // ── Shared identifier form ─────────────────────────────
  protected readonly identifierForm = new FormGroup({
    identifier: new FormControl('', { nonNullable: true }),
  });

  // ── Text block form ────────────────────────────────────
  protected readonly textForm = new FormGroup({
    identifier: new FormControl('', { nonNullable: true }),
    textContent: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  ngOnInit(): void {
    this.searchSubject
      .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe((query) => {
        this.currentQuery = query;
        this.selectedSheet = null;
        this.searchPage = 0;
        this.doSearch();
      });
  }

  ngOnChanges(): void {
    if (this.collectionId) {
      this.loadItems();
    }
  }

  protected isSheet(item: CollectionItem): boolean {
    return item.type === 'SHEET';
  }

  // ── Add sheet ──────────────────────────────────────────
  protected openAddSheet(): void {
    this.selectedSheet = null;
    this.currentQuery = '';
    this.searchPage = 0;
    this.createAnother = false;
    this.identifierForm.reset();
    this.searchResults.set([]);
    this.addSheetDialogVisible = true;
    this.doSearch();
  }

  protected onSearchInput(event: Event): void {
    this.searchSubject.next((event.target as HTMLInputElement).value);
  }

  protected onSearchLazyLoad(event: TableLazyLoadEvent): void {
    this.searchPage = Math.floor((event.first ?? 0) / (event.rows ?? this.searchRows));
    this.searchRows = event.rows ?? this.searchRows;
    this.doSearch();
  }

  protected selectSheet(sheet: SheetMusicSearchResult): void {
    this.selectedSheet = sheet;
  }

  protected isSelected(sheet: SheetMusicSearchResult): boolean {
    return this.selectedSheet?.id === sheet.id;
  }

  protected difficultyLevelKey(grade: number): string {
    return DIFFICULTY_LEVELS[grade - 1] ?? '';
  }

  protected get canAddSheet(): boolean {
    return !!this.selectedSheet;
  }

  protected onAddSheet(): void {
    if (!this.canAddSheet) return;
    const payload = {
      type: 'SHEET' as CollectionItemType,
      identifier: this.identifierForm.controls.identifier.value,
      sheetId: this.selectedSheet!.id!,
    };
    this.saving = true;
    this.collectionsApi.addItem(this.collectionId, payload).subscribe({
      next: () => {
        this.saving = false;
        this.messageService.add({
          severity: 'success',
          summary: this.t.t('collections.items.messages.sheetAdded'),
        });
        this.loadItems();
        if (this.createAnother) {
          this.selectedSheet = null;
          this.identifierForm.reset();
        } else {
          this.addSheetDialogVisible = false;
        }
      },
      error: () => {
        this.saving = false;
      },
    });
  }

  private doSearch(): void {
    this.searchLoading.set(true);
    this.sheetsApi
      .find({ query: this.currentQuery || undefined, page: this.searchPage, size: this.searchRows })
      .subscribe({
        next: (res) => {
          this.searchResults.set(res.data ?? []);
          this.searchTotal.set(res.totalCount ?? 0);
          this.searchLoading.set(false);
        },
        error: () => {
          this.searchLoading.set(false);
        },
      });
  }

  // ── Add text block ─────────────────────────────────────
  protected openAddTextBlock(): void {
    this.textForm.reset();
    this.addTextDialogVisible = true;
  }

  protected onAddTextBlock(): void {
    if (this.textForm.invalid) return;
    const payload = {
      type: 'TEXT' as CollectionItemType,
      identifier: this.textForm.controls.identifier.value,
      textContent: this.textForm.controls.textContent.value,
    };
    this.saving = true;
    this.collectionsApi.addItem(this.collectionId, payload).subscribe({
      next: () => {
        this.saving = false;
        this.addTextDialogVisible = false;
        this.messageService.add({
          severity: 'success',
          summary: this.t.t('collections.items.messages.textBlockAdded'),
        });
        this.loadItems();
      },
      error: () => {
        this.saving = false;
      },
    });
  }

  // ── Preview ───────────────────────────────────────────
  protected openPreview(sheet: SheetMusicSearchResult, event: MouseEvent): void {
    event.stopPropagation();
    this.previewSheet.set(null);
    this.previewLoading.set(true);
    this.previewDialogVisible = true;
    this.sheetsApi.load(sheet.id!).subscribe({
      next: (s) => {
        this.previewSheet.set(s);
        this.previewLoading.set(false);
      },
      error: () => {
        this.previewLoading.set(false);
      },
    });
  }

  // ── Edit ──────────────────────────────────────────────
  protected openEdit(item: CollectionItem): void {
    this.editingItem = item;
    if (item.type === 'SHEET') {
      this.identifierForm.reset({ identifier: item.identifier });
    } else {
      this.textForm.reset({ identifier: item.identifier, textContent: item.textContent ?? '' });
    }
    this.editDialogVisible = true;
  }

  protected onUpdate(): void {
    if (!this.editingItem) return;
    const isSheet = this.editingItem.type === 'SHEET';
    if (!isSheet && this.textForm.invalid) return;

    const payload: CollectionItem = isSheet
      ? { ...this.editingItem, identifier: this.identifierForm.controls.identifier.value }
      : {
          ...this.editingItem,
          identifier: this.textForm.controls.identifier.value,
          textContent: this.textForm.controls.textContent.value,
        };

    this.saving = true;
    this.collectionsApi.updateItem(this.collectionId, this.editingItem.id!, payload).subscribe({
      next: () => {
        this.saving = false;
        this.editDialogVisible = false;
        this.messageService.add({
          severity: 'success',
          summary: this.t.t('collections.items.messages.updated'),
        });
        this.loadItems();
      },
      error: () => {
        this.saving = false;
      },
    });
  }

  // ── Remove ────────────────────────────────────────────
  protected confirmRemove(item: CollectionItem): void {
    const label = item.type === 'SHEET' ? (item.title ?? '') : (item.textContent?.substring(0, 40) ?? '');
    this.confirmationService.confirm({
      message: this.t
        .t('collections.items.delete.confirm')
        .replace('{title}', label),
      header: this.t.t('collections.items.delete.header'),
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.collectionsApi.removeItem(this.collectionId, item.id!).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: this.t.t('collections.items.messages.removed'),
            });
            this.loadItems();
          },
          error: () => {},
        });
      },
    });
  }

  // ── Attachment ────────────────────────────────────────
  protected triggerAttachmentUpload(item: CollectionItem): void {
    this.uploadingItem = item;
    this.fileInputRef.nativeElement.value = '';
    this.fileInputRef.nativeElement.click();
  }

  protected onFileSelected(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file || !this.uploadingItem) return;
    const item = this.uploadingItem;
    this.uploadingAttachment = true;
    this.collectionsApi.uploadAttachment(this.collectionId, item.id!, file).subscribe({
      next: (attachment) => {
        this.uploadingAttachment = false;
        this.items.update((list) =>
          list.map((i) => (i.id === item.id ? { ...i, attachment } : i)),
        );
        this.messageService.add({
          severity: 'success',
          summary: this.t.t('collections.items.messages.attachmentUploaded'),
        });
      },
      error: () => {
        this.uploadingAttachment = false;
      },
    });
  }

  protected confirmRemoveAttachment(item: CollectionItem): void {
    this.confirmationService.confirm({
      message: this.t.t('collections.items.attachment.removeConfirm'),
      header: this.t.t('collections.items.attachment.removeHeader'),
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.collectionsApi.removeAttachment(this.collectionId, item.id!).subscribe({
          next: () => {
            this.items.update((list) =>
              list.map((i) => (i.id === item.id ? { ...i, attachment: undefined } : i)),
            );
            this.messageService.add({
              severity: 'success',
              summary: this.t.t('collections.items.messages.attachmentRemoved'),
            });
          },
          error: () => {},
        });
      },
    });
  }

  // ── Items table ───────────────────────────────────────
  protected onLazyLoad(event: TableLazyLoadEvent): void {
    this.currentPage = Math.floor((event.first ?? 0) / (event.rows ?? this.rows));
    this.rows = event.rows ?? this.rows;
    this.loadItems();
  }

  protected onRowReorder(_event: TableRowReorderEvent): void {
    // PrimeNG already reordered this.value in-place before emitting this event.
    // The emitted dragIndex/dropIndex are PrimeNG-internal and must not be used
    // to re-apply a splice. Just snapshot the already-correct array to trigger
    // signal reactivity, then sync the new order to the backend.
    const reordered = [...this.items()];
    this.items.set(reordered);
    const orderedIds = reordered.map((i) => i.id!);
    this.collectionsApi.reorderItems(this.collectionId, orderedIds).subscribe({
      error: () => {
        this.loadItems();
        this.messageService.add({
          severity: 'error',
          summary: this.t.t('collections.items.messages.reorderFailed'),
        });
      },
    });
  }

  protected toggleMyPartsOnly(): void {
    this.myPartsOnly.update((v) => !v);
    this.currentPage = 0;
    this.loadItems();
  }

  private loadItems(): void {
    this.loading.set(true);
    this.collectionsApi
      .listItems(this.collectionId, { page: this.currentPage, size: this.rows }, this.myPartsOnly())
      .subscribe({
        next: (res) => {
          this.items.set(res.data ?? []);
          this.totalRecords.set(res.totalCount ?? 0);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
        },
      });
  }
}
