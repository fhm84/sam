import { ChangeDetectionStrategy, Component, computed, DestroyRef, inject, OnInit, signal, ViewChild } from '@angular/core';
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
import { Toolbar } from 'primeng/toolbar';
import { Tag } from 'primeng/tag';
import { Tooltip } from 'primeng/tooltip';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { TranslationService } from '../../core/translation.service';
import { LayoutPreferenceService } from '../../core/layout-preference.service';
import { EnsemblesApiService, SheetsApiService } from '../../core/api';
import {
  CoverageSnapshotSummary,
  Ensemble,
  EnsembleCoverageStatus,
  Genre,
  SearchResultMetrics,
  SheetMusic,
  SheetMusicSearchResult,
} from '../../model/datamodels';
import { GENRES, STYLES } from '../../shared/constants';
import { Dialog } from 'primeng/dialog';
import { SheetDetail } from './sheet-detail';
import { SheetCollections } from './sheet-collections';

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
    SheetCollections,
    Drawer,
    Dialog,
    Toolbar,
    Tag,
    Tooltip,
  ],
  providers: [ConfirmationService],
  templateUrl: './sheets.html',
  styleUrl: './sheets.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Sheets implements OnInit {
  private static readonly DRAWER_BREAKPOINT = '(min-width: 960px)';

  @ViewChild(SheetCollections) protected collectionsDialogPanel?: SheetCollections;

  protected readonly t = inject(TranslationService);
  private readonly api = inject(SheetsApiService);
  private readonly ensemblesApi = inject(EnsemblesApiService);
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
  protected readonly debugMode = signal(false);
  protected readonly activeFilterCount = computed(
    () => (this.selectedGenre() ? 1 : 0) + (this.selectedLetter() ? 1 : 0),
  );
  protected rows = 20;

  // ── Ensemble context ──────────────────────────────────
  protected readonly ensembles = signal<Ensemble[]>([]);
  protected readonly selectedEnsemble = signal<Ensemble | null>(null);
  protected readonly coverageStatus = signal<EnsembleCoverageStatus | null>(null);
  protected readonly coverageComputing = signal(false);

  protected detailDrawerVisible = false;
  protected selectedSheetId: string | null = null;
  protected collectionsDialogVisible = false;
  protected collectionsDialogSheetId: string | null = null;
  protected selectedSheetCoverage = signal<CoverageSnapshotSummary | null>(null);

  protected readonly viewOptions = [
    { icon: 'pi pi-th-large', value: 'cards' },
    { icon: 'pi pi-list', value: 'list' },
  ];

  protected currentPage = 0;
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
    this.loadEnsembles();
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

  protected toggleDebugMode(): void {
    this.debugMode.update((v) => !v);
  }

  protected formatScore(rank: number): string {
    return Math.min(Math.round(rank * 100), 100) + '%';
  }

  protected scoreSeverity(rank: number): 'good' | 'fair' | 'low' {
    if (rank >= 0.4) return 'good';
    if (rank >= 0.15) return 'fair';
    return 'low';
  }

  protected pct(val: number | undefined): string {
    return Math.round((val ?? 0) * 100) + '%';
  }

  protected metricsTooltip(m: SearchResultMetrics): string {
    const rows: [string, string][] = [
      ['FTS rank', m.ftsRank?.toFixed(3) ?? '–'],
      ['Title', this.pct(m.titleSimilarity)],
      ['Composer', this.pct(m.composerSimilarity)],
      ['Phonetic', m.phoneticMatch ? '✓' : '–'],
    ];
    const rowsHtml = rows
      .map(
        ([k, v]) =>
          `<div style="display:flex;justify-content:space-between;gap:1.5rem">` +
          `<span style="opacity:.65">${k}</span><strong>${v}</strong></div>`,
      )
      .join('');
    return `<div style="font-family:monospace;font-size:.75rem;line-height:1.7">${rowsHtml}</div>`;
  }

  protected coverageTooltip(c: CoverageSnapshotSummary): string {
    if (!c.details?.length) return '';
    const cells = c.details
      .map((d) => {
        const scoreColor = d.missingRequired
          ? '#e53e3e'
          : (d.score ?? 0) >= 0.85
            ? '#38a169'
            : (d.score ?? 0) > 0
              ? '#d97706'
              : '#718096';
        return (
          `<span style="opacity:.75;padding-right:1.5rem">${d.voiceLabel ?? '?'}</span>` +
          `<strong style="color:${scoreColor}">${d.missingRequired ? '✗' : this.pct(d.score)}</strong>`
        );
      })
      .join('');
    return (
      `<div style="display:grid;grid-template-columns:1fr auto;align-items:center;` +
      `font-family:monospace;font-size:.73rem;line-height:1.9">${cells}</div>`
    );
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

  protected openCollectionsDialog(sheet: SheetMusicSearchResult, event: MouseEvent): void {
    event.stopPropagation();
    this.collectionsDialogSheetId = sheet.id ?? null;
    this.collectionsDialogVisible = true;
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
      this.selectedSheetCoverage.set(sheet.coverage ?? null);
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

  protected toggleFavorite(sheet: SheetMusicSearchResult, event: Event): void {
    event.stopPropagation();
    const wasFavorite = sheet.favorite;
    this.sheets.update((list) => list.map((s) => (s.id === sheet.id ? { ...s, favorite: !wasFavorite } : s)));
    const req = wasFavorite ? this.api.unfavorite(sheet.id!) : this.api.favorite(sheet.id!);
    req.subscribe({
      error: () =>
        this.sheets.update((list) => list.map((s) => (s.id === sheet.id ? { ...s, favorite: wasFavorite } : s))),
    });
  }

  // ── Ensemble context ──────────────────────────────────

  private loadEnsembles(): void {
    this.ensemblesApi.find({ size: 200 }).subscribe((res) => this.ensembles.set(res.data ?? []));
  }

  protected onEnsembleChange(ensemble: Ensemble | null): void {
    this.selectedEnsemble.set(ensemble ?? null);
    this.coverageStatus.set(null);
    this.currentPage = 0;
    if (ensemble?.id) {
      this.loadCoverageStatus(ensemble.id);
    }
    this.loadData();
  }

  private loadCoverageStatus(ensembleId: string): void {
    this.ensemblesApi.getCoverageStatus(ensembleId).subscribe({
      next: (status) => this.coverageStatus.set(status),
      error: () => this.coverageStatus.set({}),
    });
  }

  protected computeCoverage(): void {
    const ensemble = this.selectedEnsemble();
    if (!ensemble?.id || this.coverageComputing()) return;
    this.coverageComputing.set(true);
    this.ensemblesApi.computeCoverage(ensemble.id).subscribe({
      next: (status) => {
        this.coverageStatus.set(status);
        this.coverageComputing.set(false);
        this.loadData();
      },
      error: () => this.coverageComputing.set(false),
    });
  }

  protected totalAttachments(sheet: SheetMusic): number {
    const sheetLevel = sheet.attachments?.length ?? 0;
    const instrLevel = (sheet.instrumentations ?? []).reduce(
      (sum, i) => sum + (i.attachments?.length ?? 0),
      0,
    );
    return sheetLevel + instrLevel;
  }

  protected attachmentTooltip(sheet: SheetMusic): string {
    const sheetLevel = sheet.attachments?.length ?? 0;
    const instrLevel = (sheet.instrumentations ?? []).reduce(
      (sum, i) => sum + (i.attachments?.length ?? 0),
      0,
    );
    if (sheetLevel === 0 && instrLevel === 0) return '';
    const parts: string[] = [];
    if (sheetLevel > 0)
      parts.push(this.t.t('sheets.columns.attachmentsTooltip.sheetLevel').replace('{{count}}', String(sheetLevel)));
    if (instrLevel > 0)
      parts.push(this.t.t('sheets.columns.attachmentsTooltip.instrLevel').replace('{{count}}', String(instrLevel)));
    return parts.join(', ');
  }

  protected formatComputedAt(date: Date | string | null | undefined): string {
    if (!date) return '';
    const d = new Date(date as string);
    const diff = Math.floor((Date.now() - d.getTime()) / 1000);
    if (diff < 60) return 'just now';
    if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
    if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
    return `${Math.floor(diff / 86400)}d ago`;
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
        ensemble: this.selectedEnsemble()?.id || undefined,
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
