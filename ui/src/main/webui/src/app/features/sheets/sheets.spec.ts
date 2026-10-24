import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { ConfirmationService, MessageService } from '@openng/optimus-ui/api';

import { Sheets } from './sheets';
import { SheetsApiService, EnsemblesApiService, InstrumentsApiService } from '../../core/api';
import { TranslationService } from '../../core/translation.service';
import { LayoutPreferenceService } from '../../core/layout-preference.service';
import { SheetMusic, SheetMusicSearchResult } from '../../model/datamodels';
import { provideAuthStub } from '../../core/auth/auth.testing';

function paginatedOf<T>(data: T[], totalCount = data.length) {
  return of({ data, totalCount, page: 0, size: data.length });
}

describe('Sheets', () => {
  let component: Sheets;
  let sheetsApi: { find: ReturnType<typeof vi.fn>; getAvailableLetters: ReturnType<typeof vi.fn> };
  let ensemblesApi: { find: ReturnType<typeof vi.fn>; getCoverageStatus: ReturnType<typeof vi.fn>; computeCoverage: ReturnType<typeof vi.fn> };
  let instrumentsApi: { find: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    sheetsApi = {
      find: vi.fn().mockReturnValue(paginatedOf([])),
      getAvailableLetters: vi.fn().mockReturnValue(of([])),
    };
    ensemblesApi = {
      find: vi.fn().mockReturnValue(paginatedOf([])),
      getCoverageStatus: vi.fn().mockReturnValue(of({})),
      computeCoverage: vi.fn().mockReturnValue(of({})),
    };
    instrumentsApi = {
      find: vi.fn().mockReturnValue(paginatedOf([])),
    };

    await TestBed.overrideComponent(Sheets, {
      set: {
        imports: [],
        template: '<div></div>',
        providers: [ConfirmationService],
      },
    })
      .configureTestingModule({
        imports: [Sheets],
        providers: [
          provideAuthStub(),
          provideNoopAnimations(),
          provideRouter([]),
          MessageService,
          { provide: SheetsApiService, useValue: sheetsApi },
          { provide: EnsemblesApiService, useValue: ensemblesApi },
          { provide: InstrumentsApiService, useValue: instrumentsApi },
          {
            provide: TranslationService,
            useValue: { t: (k: string) => k, version: { subscribe: () => {} } },
          },
          {
            provide: LayoutPreferenceService,
            useValue: { getViewMode: () => 'cards', setViewMode: vi.fn() },
          },
        ],
      })
      .compileComponents();

    const fixture = TestBed.createComponent(Sheets);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ── formatScore ────────────────────────────────────────

  it('formatScore rounds rank to nearest percent', () => {
    expect((component as any).formatScore(0.456)).toBe('46%');
  });

  it('formatScore caps at 100%', () => {
    expect((component as any).formatScore(1.5)).toBe('100%');
  });

  it('formatScore returns 0% for zero rank', () => {
    expect((component as any).formatScore(0)).toBe('0%');
  });

  // ── scoreSeverity ──────────────────────────────────────

  it('scoreSeverity returns good for rank >= 0.4', () => {
    expect((component as any).scoreSeverity(0.4)).toBe('good');
    expect((component as any).scoreSeverity(0.99)).toBe('good');
  });

  it('scoreSeverity returns fair for rank between 0.15 and 0.4', () => {
    expect((component as any).scoreSeverity(0.15)).toBe('fair');
    expect((component as any).scoreSeverity(0.39)).toBe('fair');
  });

  it('scoreSeverity returns low for rank below 0.15', () => {
    expect((component as any).scoreSeverity(0)).toBe('low');
    expect((component as any).scoreSeverity(0.14)).toBe('low');
  });

  // ── pct ───────────────────────────────────────────────

  it('pct formats decimal as percentage string', () => {
    expect((component as any).pct(0.75)).toBe('75%');
    expect((component as any).pct(1)).toBe('100%');
    expect((component as any).pct(0)).toBe('0%');
  });

  it('pct handles undefined as 0%', () => {
    expect((component as any).pct(undefined)).toBe('0%');
  });

  // ── formatComputedAt ───────────────────────────────────

  it('formatComputedAt returns empty string for null/undefined', () => {
    expect((component as any).formatComputedAt(null)).toBe('');
    expect((component as any).formatComputedAt(undefined)).toBe('');
  });

  it('formatComputedAt returns "just now" for less than 60 seconds ago', () => {
    const recent = new Date(Date.now() - 30_000).toISOString();
    expect((component as any).formatComputedAt(recent)).toBe('just now');
  });

  it('formatComputedAt returns minutes ago for < 1 hour', () => {
    const twoMinAgo = new Date(Date.now() - 2 * 60_000).toISOString();
    expect((component as any).formatComputedAt(twoMinAgo)).toBe('2m ago');
  });

  it('formatComputedAt returns hours ago for < 1 day', () => {
    const threeHoursAgo = new Date(Date.now() - 3 * 3_600_000).toISOString();
    expect((component as any).formatComputedAt(threeHoursAgo)).toBe('3h ago');
  });

  it('formatComputedAt returns days ago for >= 1 day', () => {
    const twoDaysAgo = new Date(Date.now() - 2 * 86_400_000).toISOString();
    expect((component as any).formatComputedAt(twoDaysAgo)).toBe('2d ago');
  });

  // ── totalAttachments ───────────────────────────────────

  it('totalAttachments returns 0 for a sheet with no attachments', () => {
    const sheet = { attachments: [], instrumentations: [] } as unknown as SheetMusic;
    expect((component as any).totalAttachments(sheet)).toBe(0);
  });

  it('totalAttachments counts sheet-level attachments', () => {
    const sheet = {
      attachments: [{}, {}],
      instrumentations: [],
    } as unknown as SheetMusic;
    expect((component as any).totalAttachments(sheet)).toBe(2);
  });

  it('totalAttachments sums instrumentation-level attachments', () => {
    const sheet = {
      attachments: [],
      instrumentations: [{ attachments: [{}] }, { attachments: [{}, {}] }],
    } as unknown as SheetMusic;
    expect((component as any).totalAttachments(sheet)).toBe(3);
  });

  it('totalAttachments combines both levels', () => {
    const sheet = {
      attachments: [{}],
      instrumentations: [{ attachments: [{}, {}] }],
    } as unknown as SheetMusic;
    expect((component as any).totalAttachments(sheet)).toBe(3);
  });

  it('totalAttachments handles undefined attachments gracefully', () => {
    const sheet = { instrumentations: [{ attachments: undefined }] } as unknown as SheetMusic;
    expect((component as any).totalAttachments(sheet)).toBe(0);
  });

  // ── activeFilterCount ──────────────────────────────────

  it('activeFilterCount is 0 with no filters active', () => {
    expect((component as any).activeFilterCount()).toBe(0);
  });

  it('activeFilterCount is 1 when genre is selected', () => {
    (component as any).selectedGenre.set('CLASSICAL');
    expect((component as any).activeFilterCount()).toBe(1);
  });

  it('activeFilterCount is 1 when letter is selected', () => {
    (component as any).selectedLetter.set('A');
    expect((component as any).activeFilterCount()).toBe(1);
  });

  it('activeFilterCount is 2 when both genre and letter are selected', () => {
    (component as any).selectedGenre.set('CLASSICAL');
    (component as any).selectedLetter.set('B');
    expect((component as any).activeFilterCount()).toBe(2);
  });

  // ── onPageChange ───────────────────────────────────────

  it('onPageChange correctly computes currentPage from first/rows', () => {
    sheetsApi.find.mockClear();
    (component as any).onPageChange({ first: 40, rows: 20 });
    expect((component as any).currentPage).toBe(2);
    expect(sheetsApi.find).toHaveBeenCalledOnce();
  });

  it('onPageChange resets to page 0 when first is 0', () => {
    (component as any).currentPage = 3;
    (component as any).onPageChange({ first: 0, rows: 20 });
    expect((component as any).currentPage).toBe(0);
  });

  it('onPageChange updates rows when provided', () => {
    (component as any).onPageChange({ first: 0, rows: 50 });
    expect((component as any).rows).toBe(50);
  });

  // ── filter resets page ─────────────────────────────────

  it('onGenreChange resets currentPage to 0', () => {
    (component as any).currentPage = 3;
    (component as any).onGenreChange('JAZZ' as any);
    expect((component as any).currentPage).toBe(0);
  });

  it('onLetterSelect toggles off the same letter', () => {
    (component as any).selectedLetter.set('C');
    (component as any).onLetterSelect('C');
    expect((component as any).selectedLetter()).toBeNull();
  });

  it('onLetterSelect sets a new letter', () => {
    (component as any).onLetterSelect('D');
    expect((component as any).selectedLetter()).toBe('D');
  });

  it('clearFilters resets genre, letter and page', () => {
    (component as any).selectedGenre.set('JAZZ' as any);
    (component as any).selectedLetter.set('A');
    (component as any).currentPage = 2;

    (component as any).clearFilters();

    expect((component as any).selectedGenre()).toBeNull();
    expect((component as any).selectedLetter()).toBeNull();
    expect((component as any).currentPage).toBe(0);
  });

  // ── instrument criteria ────────────────────────────────

  describe('instrument criteria', () => {
    beforeEach(() => {
      vi.useFakeTimers();
      sheetsApi.find.mockClear();
    });

    afterEach(() => vi.useRealTimers());

    const lastRequestedCriteria = () => sheetsApi.find.mock.lastCall?.[0].instrumentCriteria;

    it('loads instruments only when the filter panel is first opened', () => {
      expect(instrumentsApi.find).not.toHaveBeenCalled();

      (component as any).toggleFilterPanel();
      (component as any).toggleFilterPanel();
      (component as any).toggleFilterPanel();

      expect(instrumentsApi.find).toHaveBeenCalledTimes(1);
    });

    it('debounces edits into a single reload', () => {
      (component as any).addInstrumentCriterionRow();
      (component as any).updateInstrumentCriterionRow(0, { instrumentId: 'HORN' });
      (component as any).updateInstrumentCriterionRow(0, { count: 2 });
      (component as any).updateInstrumentCriterionRow(0, { count: 3 });
      expect(sheetsApi.find).not.toHaveBeenCalled();

      vi.advanceTimersByTime(300);

      expect(sheetsApi.find).toHaveBeenCalledTimes(1);
      expect(lastRequestedCriteria()).toEqual(['HORN:EQ:3']);
    });

    it('skips incomplete rows and edits that leave the criteria unchanged', () => {
      (component as any).addInstrumentCriterionRow();
      (component as any).updateInstrumentCriterionRow(0, { operator: 'GTE' });
      vi.advanceTimersByTime(300);
      expect(sheetsApi.find).not.toHaveBeenCalled();

      (component as any).updateInstrumentCriterionRow(0, { instrumentId: 'HORN', count: null });
      vi.advanceTimersByTime(300);
      expect(sheetsApi.find).not.toHaveBeenCalled();
      expect((component as any).activeFilterCount()).toBe(0);
    });

    it('reloads with the criteria once a row is complete', () => {
      (component as any).addInstrumentCriterionRow();
      (component as any).updateInstrumentCriterionRow(0, { instrumentId: 'OBOE', operator: 'LT' });
      vi.advanceTimersByTime(300);

      expect(lastRequestedCriteria()).toEqual(['OBOE:LT:1']);
      expect((component as any).activeFilterCount()).toBe(1);
    });

    it('reloads unchanged criteria again after a failed request', () => {
      sheetsApi.find.mockReturnValueOnce(throwError(() => new Error('offline')));
      (component as any).addInstrumentCriterionRow();
      (component as any).updateInstrumentCriterionRow(0, { instrumentId: 'HORN' });
      vi.advanceTimersByTime(300);
      expect(sheetsApi.find).toHaveBeenCalledTimes(1);

      // Re-applying the same value must retry instead of being skipped as "already requested"
      (component as any).updateInstrumentCriterionRow(0, { count: 1 });
      vi.advanceTimersByTime(300);

      expect(sheetsApi.find).toHaveBeenCalledTimes(2);
      expect(lastRequestedCriteria()).toEqual(['HORN:EQ:1']);
    });

    it('retries loading instruments on the next panel open after a failure', () => {
      instrumentsApi.find.mockReturnValueOnce(throwError(() => new Error('offline')));

      (component as any).toggleFilterPanel();
      (component as any).toggleFilterPanel();
      (component as any).toggleFilterPanel();

      expect(instrumentsApi.find).toHaveBeenCalledTimes(2);
    });
  });
});
