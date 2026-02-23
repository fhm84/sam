import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import { Subject, debounceTime } from 'rxjs';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { SelectButton } from 'primeng/selectbutton';
import { Select } from 'primeng/select';
import { Badge } from 'primeng/badge';
import { Paginator } from 'primeng/paginator';
import { Drawer } from 'primeng/drawer';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { TranslationService } from '../../core/translation.service';
import { LayoutPreferenceService } from '../../core/layout-preference.service';
import { SheetsApiService } from '../../core/api';
import { Genre, SheetMusic, SheetMusicSearchResult } from '../../model/datamodels';
import { GENRES, STYLES } from '../../shared/constants';
import { SheetDetail } from './sheet-detail';

@Component({
  selector: 'app-sheets',
  imports: [
    TableModule,
    ConfirmDialog,
Button,
    InputText,
    IconField,
    InputIcon,
    SelectButton,
    Select,
    Badge,
    Paginator,
    FormsModule,
    TranslatePipe,
    SheetDetail,
    Drawer,
  ],
  providers: [ConfirmationService],
  templateUrl: './sheets.html',
  styleUrl: './sheets.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Sheets implements OnInit {
  private static readonly DRAWER_BREAKPOINT = '(min-width: 960px)';

  protected readonly t = inject(TranslationService);
  private readonly api = inject(SheetsApiService);
  private readonly router = inject(Router);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly layoutPref = inject(LayoutPreferenceService);

  protected readonly ALL_LETTERS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'.split('');

  protected readonly sheets = signal<SheetMusicSearchResult[]>([]);
  protected readonly totalRecords = signal(0);
  protected readonly loading = signal(true);
  protected readonly viewMode = signal<'list' | 'cards'>('cards');
  protected readonly filterPanelOpen = signal(false);
  protected readonly genreOptions = computed(() =>
    GENRES.map((g) => ({ label: this.t.t(`sheets.genres.${g}`), value: g })),
  );

  protected readonly styleOptions = computed(() =>
    STYLES.map((s) => ({ label: this.t.t(`sheets.styles.${s}`), value: s })),
  );

  protected readonly selectedGenre = signal<Genre | null>(null);
  protected readonly selectedLetter = signal<string | null>(null);
  protected readonly availableLetters = signal<string[]>([]);
  protected readonly hasTextSearch = signal(false);
  protected readonly activeFilterCount = computed(
    () => (this.selectedGenre() ? 1 : 0) + (this.selectedLetter() ? 1 : 0),
  );
  protected rows = 20;

  protected detailDrawerVisible = false;
  protected selectedSheetId: string | null = null;

  protected readonly viewOptions = [
    { icon: 'pi pi-th-large', value: 'cards' },
    { icon: 'pi pi-list', value: 'list' },
  ];

  private currentPage = 0;
  private searchFilter = '';
  private readonly filterSubject = new Subject<string>();

  ngOnInit(): void {
    this.filterSubject
      .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
      .subscribe((value) => {
        this.searchFilter = value;
        this.hasTextSearch.set(!!value);
        if (value) {
          this.selectedLetter.set(null);
        }
        this.currentPage = 0;
        this.loadData();
      });

    this.viewMode.set(this.layoutPref.getViewMode('sheets'));
    this.loadAvailableLetters();
    this.loadData();
  }

  protected onLazyLoad(event: TableLazyLoadEvent): void {
    this.currentPage = (event.first ?? 0) / (event.rows ?? this.rows);
    this.rows = event.rows ?? this.rows;
    this.loadData();
  }

  protected onPageChange(event: { first?: number; rows?: number }): void {
    this.currentPage = (event.first ?? 0) / (event.rows ?? this.rows);
    this.rows = event.rows ?? this.rows;
    this.loadData();
  }

  protected onFilter(event: Event): void {
    this.filterSubject.next((event.target as HTMLInputElement).value);
  }

  protected onViewModeChange(): void {
    this.layoutPref.setViewMode('sheets', this.viewMode());
    this.currentPage = 0;
    this.loadData();
  }

  protected toggleFilterPanel(): void {
    this.filterPanelOpen.update((v) => !v);
  }

  protected onGenreChange(genre: Genre | null): void {
    this.selectedGenre.set(genre);
    this.selectedLetter.set(null);
    this.currentPage = 0;
    this.loadAvailableLetters();
    this.loadData();
  }

  protected clearFilters(): void {
    this.selectedGenre.set(null);
    this.selectedLetter.set(null);
    this.currentPage = 0;
    this.loadAvailableLetters();
    this.loadData();
  }

  protected onLetterSelect(letter: string): void {
    this.selectedLetter.update((current) => (current === letter ? null : letter));
    this.currentPage = 0;
    this.loadData();
  }

  private loadAvailableLetters(): void {
    this.api
      .getAvailableLetters(this.selectedGenre() ?? undefined)
      .subscribe((letters) => this.availableLetters.set(letters));
  }

  protected openNew(): void {
    this.router.navigate(['/sheets/new']);
  }

  protected openEdit(sheet: SheetMusic): void {
    this.router.navigate(['/sheets', sheet.id, 'edit']);
  }

  protected confirmDelete(sheet: SheetMusic): void {
    this.confirmationService.confirm({
      message: this.t.t('sheets.delete.confirm').replace('{title}', sheet.title),
      header: this.t.t('sheets.delete.header'),
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.api.delete(sheet.id!).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: this.t.t('sheets.messages.deleted'),
            });
            this.loadData();
          },
          error: () => {},
        });
      },
    });
  }

  protected openDetail(sheet: SheetMusicSearchResult): void {
    if (window.matchMedia(Sheets.DRAWER_BREAKPOINT).matches) {
      this.selectedSheetId = sheet.id!;
      this.detailDrawerVisible = true;
    } else {
      this.router.navigate(['/sheets', sheet.id]);
    }
  }

  protected onDetailEdit(sheet: SheetMusic): void {
    this.router.navigate(['/sheets', sheet.id, 'edit']);
  }

  protected onDetailDeleted(): void {
    this.detailDrawerVisible = false;
    this.selectedSheetId = null;
    this.loadData();
  }

  private loadData(): void {
    this.loading.set(true);
    this.api
      .find({
        page: this.currentPage,
        size: this.rows,
        query: this.searchFilter || undefined,
        genre: this.selectedGenre() || undefined,
        titleStartsWith: this.selectedLetter() || undefined,
      })
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
