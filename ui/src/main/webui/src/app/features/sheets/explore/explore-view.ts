import { ChangeDetectionStrategy, Component, DestroyRef, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Button } from 'primeng/button';
import { Tag } from 'primeng/tag';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { TranslationService } from '../../../core/translation.service';
import { ExploreShelfRow } from './explore-shelf-row';
import { SheetsApiService } from '../../../core/api';
import { ExploreShelves, SheetMusicSearchResult, TagCount } from '../../../model/datamodels';
import { formatDuration } from '../../../shared/utils/format.utils';

@Component({
  selector: 'app-explore-view',
  imports: [Button, Tag, TranslatePipe, ExploreShelfRow],
  templateUrl: './explore-view.html',
  styleUrl: './explore-view.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExploreView implements OnInit, OnChanges {
  @Input() ensembleId: string | null = null;
  @Output() tagSelected = new EventEmitter<string>();
  @Output() openSheet = new EventEmitter<SheetMusicSearchResult>();

  protected readonly t = inject(TranslationService);
  private readonly api = inject(SheetsApiService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly formatDuration = formatDuration;

  protected readonly shelves = signal<ExploreShelves | null>(null);
  protected readonly loadingShelves = signal(true);
  protected readonly surprise = signal<SheetMusicSearchResult | null>(null);
  protected readonly loadingSurprise = signal(true);

  protected readonly shelfList = computed<{ key: string; items: SheetMusicSearchResult[] | undefined }[]>(() => {
    const shelves = this.shelves();
    return [
      // Needs-attention is ensemble-relative; without a selected ensemble the backend
      // always returns it empty, so don't render an empty shelf in that case.
      ...(this.ensembleId ? [{ key: 'needsAttention', items: shelves?.needsAttention }] : []),
      { key: 'crowdPleasers', items: shelves?.crowdPleasers },
      { key: 'hiddenGems', items: shelves?.hiddenGems },
      { key: 'quickFillers', items: shelves?.quickFillers },
      { key: 'bigFinishes', items: shelves?.bigFinishes },
      { key: 'recentlyAdded', items: shelves?.recentlyAdded },
    ];
  });

  ngOnInit(): void {
    this.loadShelves();
    this.loadSurprise();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['ensembleId'] && !changes['ensembleId'].firstChange) {
      this.loadShelves();
      this.loadSurprise();
    }
  }

  protected shuffle(): void {
    this.loadSurprise();
  }

  protected onTagClick(tag: TagCount): void {
    if (tag.tag) {
      this.tagSelected.emit(tag.tag);
    }
  }

  protected onCardClick(sheet: SheetMusicSearchResult): void {
    this.openSheet.emit(sheet);
  }

  protected tagWeightClass(count: number | undefined, maxCount: number): string {
    const ratio = maxCount > 0 ? (count ?? 0) / maxCount : 0;
    if (ratio >= 0.75) return 'tag-xl';
    if (ratio >= 0.5) return 'tag-lg';
    if (ratio >= 0.25) return 'tag-md';
    return 'tag-sm';
  }

  protected maxTagCount(tagCloud: TagCount[] | undefined): number {
    return (tagCloud ?? []).reduce((max, t) => Math.max(max, t.count ?? 0), 1);
  }

  private loadShelves(): void {
    this.loadingShelves.set(true);
    this.api
      .explore(this.ensembleId ?? undefined)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (shelves) => {
          this.shelves.set(shelves);
          this.loadingShelves.set(false);
        },
        error: () => this.loadingShelves.set(false),
      });
  }

  private loadSurprise(): void {
    this.loadingSurprise.set(true);
    this.api
      .surprise(this.ensembleId ?? undefined)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (pick) => {
          this.surprise.set(pick);
          this.loadingSurprise.set(false);
        },
        error: () => this.loadingSurprise.set(false),
      });
  }
}
