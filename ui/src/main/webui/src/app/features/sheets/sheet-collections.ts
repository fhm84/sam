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
import { Tag } from 'primeng/tag';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { TranslationService } from '../../core/translation.service';
import { CollectionsApiService } from '../../core/api';
import { SheetCollection } from '../../model/datamodels';

@Component({
  selector: 'app-sheet-collections',
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
    Tag,
    TranslatePipe,
  ],
  providers: [ConfirmationService],
  templateUrl: './sheet-collections.html',
  styleUrl: './sheet-collections.scss',
})
export class SheetCollections implements OnInit, OnChanges {
  protected readonly t = inject(TranslationService);
  private readonly collectionsApi = inject(CollectionsApiService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);
  private readonly destroyRef = inject(DestroyRef);

  @Input({ required: true }) sheetId!: string;

  // ── Membership table ───────────────────────────────────
  protected readonly collections = signal<SheetCollection[]>([]);
  protected readonly loading = signal(true);

  // ── Add dialog ─────────────────────────────────────────
  protected addDialogVisible = false;
  protected saving = false;
  protected selectedCollection: SheetCollection | null = null;

  protected readonly searchResults = signal<SheetCollection[]>([]);
  protected readonly searchTotal = signal(0);
  protected readonly searchLoading = signal(false);
  protected searchRows = 10;
  protected searchPage = 0;
  private currentQuery = '';
  private readonly searchSubject = new Subject<string>();

  protected readonly identifierForm = new FormGroup({
    identifier: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
  });

  protected get canAdd(): boolean {
    return !!this.selectedCollection && this.identifierForm.valid;
  }

  protected isSelected(col: SheetCollection): boolean {
    return this.selectedCollection?.id === col.id;
  }

  ngOnInit(): void {
    this.searchSubject
      .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe((query) => {
        this.currentQuery = query;
        this.selectedCollection = null;
        this.searchPage = 0;
        this.doSearch();
      });
  }

  ngOnChanges(): void {
    if (this.sheetId) {
      this.loadCollections();
    }
  }

  private loadCollections(): void {
    this.loading.set(true);
    this.collectionsApi.getCollectionsForSheet(this.sheetId, { page: 0, size: 100 }).subscribe({
      next: (res) => {
        this.collections.set(res.data ?? []);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  // ── Add ───────────────────────────────────────────────
  public openAdd(): void {
    this.selectedCollection = null;
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

  protected selectCollection(col: SheetCollection): void {
    this.selectedCollection = col;
  }

  private doSearch(): void {
    this.searchLoading.set(true);
    const currentIds = new Set(this.collections().map((c) => c.id?.toString()));
    this.collectionsApi
      .find({ name: this.currentQuery || undefined, page: this.searchPage, size: this.searchRows })
      .subscribe({
        next: (res) => {
          this.searchResults.set((res.data ?? []).filter((c) => !currentIds.has(c.id?.toString())));
          this.searchTotal.set(res.totalCount ?? 0);
          this.searchLoading.set(false);
        },
        error: () => {
          this.searchLoading.set(false);
        },
      });
  }

  protected onAdd(): void {
    if (!this.canAdd) return;
    const payload = {
      type: 'SHEET' as const,
      identifier: this.identifierForm.controls.identifier.value,
      sheetId: this.sheetId as any,
    };
    this.saving = true;
    this.collectionsApi.addItem(this.selectedCollection!.id!.toString(), payload).subscribe({
      next: () => {
        this.saving = false;
        this.addDialogVisible = false;
        this.messageService.add({
          severity: 'success',
          summary: this.t.t('collections.items.messages.added'),
        });
        this.loadCollections();
      },
      error: () => {
        this.saving = false;
      },
    });
  }

  // ── Remove ────────────────────────────────────────────
  protected confirmRemove(collection: SheetCollection): void {
    const collectionSheetId = collection.items?.[0]?.id?.toString();
    if (!collectionSheetId) return;
    this.confirmationService.confirm({
      message: this.t
        .t('sheets.collections.remove.confirm')
        .replace('{name}', collection.name ?? ''),
      header: this.t.t('sheets.collections.remove.header'),
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.collectionsApi.removeItem(collection.id!.toString(), collectionSheetId).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: this.t.t('collections.items.messages.removed'),
            });
            this.loadCollections();
          },
          error: () => {},
        });
      },
    });
  }
}
