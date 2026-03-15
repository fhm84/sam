import { ChangeDetectionStrategy, Component, computed, effect, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from 'primeng/button';
import { Checkbox } from 'primeng/checkbox';
import { Dialog } from 'primeng/dialog';
import { Divider } from 'primeng/divider';
import { MessageService } from 'primeng/api';
import { Tag } from 'primeng/tag';
import { TranslatePipe } from '../../../shared/pipes/translate.pipe';
import { TranslationService } from '../../../core/translation.service';
import { SheetsApiService } from '../../../core/api';
import { DifficultyLevelKey, DIFFICULTY_LEVELS } from '../../../shared/constants';
import { SheetEnrichment, SheetMusic, Style } from '../../../model/datamodels';

@Component({
  selector: 'app-enrichment-dialog',
  imports: [Dialog, Button, Checkbox, Divider, Tag, FormsModule, TranslatePipe],
  templateUrl: './enrichment-dialog.html',
  styleUrl: './enrichment-dialog.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EnrichmentDialog {
  protected readonly t = inject(TranslationService);
  private readonly sheetsApi = inject(SheetsApiService);
  private readonly messageService = inject(MessageService);

  readonly sheet = input.required<SheetMusic>();
  readonly visible = input(false);
  readonly visibleChange = output<boolean>();
  readonly enriched = output<void>();

  protected readonly phase = signal<'loading' | 'review' | 'error'>('loading');
  protected readonly enrichment = signal<SheetEnrichment | null>(null);
  protected readonly applying = signal(false);

  // Per-field acceptance state
  protected acceptedTags = new Set<string>();
  protected acceptStyle = false;
  protected acceptDifficulty = false;
  protected acceptYear = false;
  protected acceptNotes = false;

  protected readonly hasSuggestions = computed(() => {
    const e = this.enrichment();
    if (!e) return false;
    return (
      (e.suggestedTags?.length ?? 0) > 0 ||
      e.suggestedStyle != null ||
      e.suggestedDifficultyLevel != null ||
      e.suggestedYearOfComposition != null ||
      e.suggestedAdditionalNotes != null
    );
  });

  protected readonly styleLabel = computed(() => {
    const s = this.enrichment()?.suggestedStyle;
    return s ? this.t.t(`sheets.styles.${s}`) : '';
  });

  protected readonly difficultyLabel = computed(() => {
    const grade = this.enrichment()?.suggestedDifficultyLevel;
    if (grade == null) return '';
    const key: DifficultyLevelKey = DIFFICULTY_LEVELS[grade - 1] ?? 'EASY';
    return this.t.t(`sheets.difficultyLevels.${key}`);
  });

  constructor() {
    effect(() => {
      if (this.visible()) {
        this.start();
      }
    });
  }

  private start(): void {
    this.phase.set('loading');
    this.enrichment.set(null);
    this.applying.set(false);
    this.resetAcceptState();
    const sheetId = this.sheet().id!;
    this.sheetsApi.enrich(sheetId).subscribe({
      next: (result) => {
        this.enrichment.set(result);
        this.prefill(result);
        this.phase.set('review');
      },
      error: () => {
        this.phase.set('error');
      },
    });
  }

  private resetAcceptState(): void {
    this.acceptedTags = new Set();
    this.acceptStyle = false;
    this.acceptDifficulty = false;
    this.acceptYear = false;
    this.acceptNotes = false;
  }

  private prefill(e: SheetEnrichment): void {
    // Pre-accept all suggested tags
    this.acceptedTags = new Set(e.suggestedTags ?? []);
    this.acceptStyle = e.suggestedStyle != null;
    this.acceptDifficulty = e.suggestedDifficultyLevel != null;
    this.acceptYear = e.suggestedYearOfComposition != null;
    this.acceptNotes = e.suggestedAdditionalNotes != null;
  }

  protected isTagAccepted(tag: string): boolean {
    return this.acceptedTags.has(tag);
  }

  protected toggleTag(tag: string, checked: boolean): void {
    if (checked) {
      this.acceptedTags.add(tag);
    } else {
      this.acceptedTags.delete(tag);
    }
  }

  protected canApply(): boolean {
    if (this.phase() !== 'review') return false;
    const e = this.enrichment();
    if (!e) return false;
    return (
      this.acceptedTags.size > 0 ||
      (this.acceptStyle && e.suggestedStyle != null) ||
      (this.acceptDifficulty && e.suggestedDifficultyLevel != null) ||
      (this.acceptYear && e.suggestedYearOfComposition != null) ||
      (this.acceptNotes && e.suggestedAdditionalNotes != null)
    );
  }

  protected apply(): void {
    if (!this.canApply()) return;
    const e = this.enrichment()!;
    const current = this.sheet();
    const updated: SheetMusic = { ...current };

    if (this.acceptedTags.size > 0) {
      const existing = new Set(current.tags ?? []);
      for (const tag of this.acceptedTags) existing.add(tag);
      updated.tags = [...existing];
    }
    if (this.acceptStyle && e.suggestedStyle != null) {
      updated.style = e.suggestedStyle as Style;
    }
    if (this.acceptDifficulty && e.suggestedDifficultyLevel != null) {
      updated.difficultyLevel = e.suggestedDifficultyLevel as any;
    }
    if (this.acceptYear && e.suggestedYearOfComposition != null) {
      updated.yearOfComposition = e.suggestedYearOfComposition;
    }
    if (this.acceptNotes && e.suggestedAdditionalNotes != null) {
      updated.additionalNotes = e.suggestedAdditionalNotes;
    }

    this.applying.set(true);
    this.sheetsApi.update(current.id!, updated).subscribe({
      next: () => {
        this.applying.set(false);
        this.messageService.add({
          severity: 'success',
          summary: this.t.t('enrichment.messages.applied'),
        });
        this.enriched.emit();
        this.close();
      },
      error: () => {
        this.applying.set(false);
        this.messageService.add({
          severity: 'error',
          summary: this.t.t('enrichment.messages.error'),
        });
      },
    });
  }

  protected retry(): void {
    this.start();
  }

  protected close(): void {
    this.visibleChange.emit(false);
  }
}
