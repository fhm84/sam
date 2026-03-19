import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { ConfirmationService, MessageService } from 'primeng/api';

import { Collections } from './collections';
import { CollectionsApiService } from '../../core/api';
import { TranslationService } from '../../core/translation.service';
import { LayoutPreferenceService } from '../../core/layout-preference.service';

function paginatedOf<T>(data: T[], totalCount = data.length) {
  return of({ data, totalCount, page: 0, size: data.length });
}

describe('Collections', () => {
  let component: Collections;
  let collectionsApi: { find: ReturnType<typeof vi.fn>; delete: ReturnType<typeof vi.fn> };
  let layoutPref: { getViewMode: ReturnType<typeof vi.fn>; setViewMode: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    collectionsApi = {
      find: vi.fn().mockReturnValue(paginatedOf([])),
      delete: vi.fn().mockReturnValue(of(undefined)),
    };
    layoutPref = {
      getViewMode: vi.fn().mockReturnValue('cards'),
      setViewMode: vi.fn(),
    };

    await TestBed.overrideComponent(Collections, {
      set: {
        imports: [],
        template: '<div></div>',
        providers: [ConfirmationService],
      },
    })
      .configureTestingModule({
        imports: [Collections],
        providers: [
          provideNoopAnimations(),
          provideRouter([]),
          MessageService,
          { provide: CollectionsApiService, useValue: collectionsApi },
          { provide: LayoutPreferenceService, useValue: layoutPref },
          {
            provide: TranslationService,
            useValue: { t: (k: string) => k, version: { subscribe: () => {} } },
          },
        ],
      })
      .compileComponents();

    const fixture = TestBed.createComponent(Collections);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ── buildFilter ────────────────────────────────────────

  it('buildFilter includes page, size and name filter', () => {
    (component as any).currentPage = 2;
    (component as any).rows = 20;
    (component as any).nameFilter = 'Bach';
    const f = (component as any).buildFilter();
    expect(f.page).toBe(2);
    expect(f.size).toBe(20);
    expect(f.name).toBe('Bach');
  });

  it('buildFilter includes type filter when set', () => {
    (component as any).typeFilter.set('SETLIST');
    const f = (component as any).buildFilter();
    expect(f.type).toBe('SETLIST');
  });

  it('buildFilter omits type when null', () => {
    (component as any).typeFilter.set(null);
    const f = (component as any).buildFilter();
    expect(f.type).toBeUndefined();
  });

  // ── onTypeFilterChange ─────────────────────────────────

  it('onTypeFilterChange sets type and resets page', () => {
    (component as any).currentPage = 5;
    collectionsApi.find.mockClear();

    (component as any).onTypeFilterChange('FOLDER');

    expect((component as any).typeFilter()).toBe('FOLDER');
    expect((component as any).currentPage).toBe(0);
    expect(collectionsApi.find).toHaveBeenCalledOnce();
  });

  it('onTypeFilterChange clears type when null passed', () => {
    (component as any).typeFilter.set('SETLIST');
    (component as any).onTypeFilterChange(null);
    expect((component as any).typeFilter()).toBeNull();
  });

  // ── clearFilters ───────────────────────────────────────

  it('clearFilters resets type and page', () => {
    (component as any).typeFilter.set('FOLDER');
    (component as any).currentPage = 3;

    (component as any).clearFilters();

    expect((component as any).typeFilter()).toBeNull();
    expect((component as any).currentPage).toBe(0);
  });

  // ── onPageChange ───────────────────────────────────────

  it('onPageChange computes page from first/rows', () => {
    collectionsApi.find.mockClear();
    (component as any).onPageChange({ first: 40, rows: 20 });
    expect((component as any).currentPage).toBe(2);
    expect(collectionsApi.find).toHaveBeenCalledOnce();
  });

  it('onPageChange updates rows', () => {
    (component as any).onPageChange({ first: 0, rows: 50 });
    expect((component as any).rows).toBe(50);
  });

  // ── formatDate ─────────────────────────────────────────

  it('formatDate returns empty string for falsy value', () => {
    expect((component as any).formatDate(undefined)).toBe('');
    expect((component as any).formatDate(null)).toBe('');
  });

  it('formatDate returns a non-empty string for a valid date', () => {
    const result = (component as any).formatDate(new Date('2024-06-15'));
    expect(typeof result).toBe('string');
    expect(result.length).toBeGreaterThan(0);
  });
});
