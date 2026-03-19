import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { ConfirmationService, MessageService } from 'primeng/api';

import { Instruments } from './instruments';
import { InstrumentsApiService } from '../../core/api';
import { TranslationService } from '../../core/translation.service';

function paginatedOf<T>(data: T[], totalCount = data.length) {
  return of({ data, totalCount, page: 0, size: data.length });
}

describe('Instruments', () => {
  let component: Instruments;
  let instrumentsApi: { find: ReturnType<typeof vi.fn>; delete: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    instrumentsApi = {
      find: vi.fn().mockReturnValue(paginatedOf([])),
      delete: vi.fn().mockReturnValue(of(undefined)),
    };

    await TestBed.overrideComponent(Instruments, {
      set: {
        imports: [],
        template: '<div></div>',
        providers: [ConfirmationService],
      },
    })
      .configureTestingModule({
        imports: [Instruments],
        providers: [
          provideNoopAnimations(),
          MessageService,
          { provide: InstrumentsApiService, useValue: instrumentsApi },
          {
            provide: TranslationService,
            useValue: { t: (k: string) => k, version: { subscribe: () => {} } },
          },
        ],
      })
      .compileComponents();

    const fixture = TestBed.createComponent(Instruments);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ── buildFilter ────────────────────────────────────────

  it('buildFilter includes page, size and name filter', () => {
    (component as any).currentPage = 1;
    (component as any).rows = 15;
    (component as any).nameFilter = 'Flute';
    const f = (component as any).buildFilter();
    expect(f.page).toBe(1);
    expect(f.size).toBe(15);
    expect(f.name).toBe('Flute');
  });

  it('buildFilter includes transposition when set', () => {
    (component as any).transpositionFilter.set('Bb');
    const f = (component as any).buildFilter();
    expect(f.transposition).toBe('Bb');
  });

  it('buildFilter omits transposition when null', () => {
    (component as any).transpositionFilter.set(null);
    const f = (component as any).buildFilter();
    expect(f.transposition).toBeUndefined();
  });

  // ── onTranspositionFilterChange ────────────────────────

  it('onTranspositionFilterChange sets transposition and resets page', () => {
    (component as any).currentPage = 3;
    instrumentsApi.find.mockClear();

    (component as any).onTranspositionFilterChange('Eb');

    expect((component as any).transpositionFilter()).toBe('Eb');
    expect((component as any).currentPage).toBe(0);
    expect(instrumentsApi.find).toHaveBeenCalledOnce();
  });

  it('onTranspositionFilterChange clears when null passed', () => {
    (component as any).transpositionFilter.set('F');
    (component as any).onTranspositionFilterChange(null);
    expect((component as any).transpositionFilter()).toBeNull();
  });

  // ── clearFilters ───────────────────────────────────────

  it('clearFilters resets transposition and page', () => {
    (component as any).transpositionFilter.set('G');
    (component as any).currentPage = 2;
    instrumentsApi.find.mockClear();

    (component as any).clearFilters();

    expect((component as any).transpositionFilter()).toBeNull();
    expect((component as any).currentPage).toBe(0);
    expect(instrumentsApi.find).toHaveBeenCalledOnce();
  });
});
