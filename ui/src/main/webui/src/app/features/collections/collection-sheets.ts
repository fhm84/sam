import { Component, DestroyRef, inject, Input, OnChanges, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subject, debounceTime } from 'rxjs';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { Dialog } from 'primeng/dialog';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { FloatLabel } from 'primeng/floatlabel';
import { Tooltip } from 'primeng/tooltip';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { TranslationService } from '../../core/translation.service';
import { CollectionsApiService, SheetsApiService } from '../../core/api';
import { CollectionSheet, SheetMusic, SheetMusicSearchResult } from '../../model/datamodels';

@Component({
  selector: 'app-collection-sheets',
  imports: [
    RouterLink,
    ReactiveFormsModule,
    TableModule,
    Dialog,
    ConfirmDialog,
    Button,
    InputText,
    IconField,
    InputIcon,
    FloatLabel,
    Tooltip,
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

  // ── Collection sheets table ────────────────────────────
  protected readonly sheets = signal<CollectionSheet[]>([]);
  protected readonly totalRecords = signal(0);
  protected readonly loading = signal(true);
  protected rows = 25;
  protected currentPage = 0;

  // ── Add dialog ─────────────────────────────────────────
  protected addDialogVisible = false;
  protected selectedSheet: SheetMusicSearchResult | null = null;
  protected saving = false;

  protected readonly searchResults = signal<SheetMusicSearchResult[]>([]);
  protected readonly searchTotal = signal(0);
  protected readonly searchLoading = signal(false);
  protected searchRows = 10;
  protected searchPage = 0;
  private currentQuery = '';
  private readonly searchSubject = new Subject<string>();

  // ── Edit dialog ────────────────────────────────────────
  protected editDialogVisible = false;
  protected editingSheet: CollectionSheet | null = null;

  // ── Preview dialog ─────────────────────────────────────
  protected previewDialogVisible = false;
  protected readonly previewSheet = signal<SheetMusic | null>(null);
  protected readonly previewLoading = signal(false);

  // ── Shared form (identifier only) ─────────────────────
  protected readonly identifierForm = new FormGroup({
    identifier: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
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
      this.loadSheets();
    }
  }

  // ── Add ───────────────────────────────────────────────
  protected openAdd(): void {
    this.selectedSheet = null;
    this.currentQuery = '';
    this.searchPage = 0;
    this.identifierForm.reset();
    this.searchResults.set([]);
    this.addDialogVisible = true;
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

  protected get canAdd(): boolean {
    return !!this.selectedSheet && this.identifierForm.valid;
  }

  protected onAdd(): void {
    if (!this.canAdd) return;
    const payload = {
      identifier: this.identifierForm.controls.identifier.value,
      sheetId: this.selectedSheet!.id!,
    };
    this.saving = true;
    this.collectionsApi.addSheet(this.collectionId, payload).subscribe({
      next: () => {
        this.saving = false;
        this.addDialogVisible = false;
        this.messageService.add({
          severity: 'success',
          summary: this.t.t('collections.sheets.messages.added'),
        });
        this.loadSheets();
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
  protected openEdit(sheet: CollectionSheet): void {
    this.editingSheet = sheet;
    this.identifierForm.reset({ identifier: sheet.identifier });
    this.editDialogVisible = true;
  }

  protected onUpdate(): void {
    if (this.identifierForm.invalid || !this.editingSheet) return;
    const payload = {
      identifier: this.identifierForm.controls.identifier.value,
      sheetId: this.editingSheet.sheetId!,
    };
    this.saving = true;
    this.collectionsApi.updateSheet(this.collectionId, this.editingSheet.id!, payload).subscribe({
      next: () => {
        this.saving = false;
        this.editDialogVisible = false;
        this.messageService.add({
          severity: 'success',
          summary: this.t.t('collections.sheets.messages.updated'),
        });
        this.loadSheets();
      },
      error: () => {
        this.saving = false;
      },
    });
  }

  // ── Remove ────────────────────────────────────────────
  protected confirmRemove(sheet: CollectionSheet): void {
    this.confirmationService.confirm({
      message: this.t
        .t('collections.sheets.delete.confirm')
        .replace('{title}', sheet.title ?? ''),
      header: this.t.t('collections.sheets.delete.header'),
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.collectionsApi.removeSheet(this.collectionId, sheet.id!).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: this.t.t('collections.sheets.messages.removed'),
            });
            this.loadSheets();
          },
          error: () => {},
        });
      },
    });
  }

  // ── Collection table ──────────────────────────────────
  protected onLazyLoad(event: TableLazyLoadEvent): void {
    this.currentPage = Math.floor((event.first ?? 0) / (event.rows ?? this.rows));
    this.rows = event.rows ?? this.rows;
    this.loadSheets();
  }

  private loadSheets(): void {
    this.loading.set(true);
    this.collectionsApi
      .listSheets(this.collectionId, { page: this.currentPage, size: this.rows })
      .subscribe({
        next: (res) => {
          this.sheets.set(res.data ?? []);
          this.totalRecords.set(res.totalCount ?? 0);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
        },
      });
  }
}
