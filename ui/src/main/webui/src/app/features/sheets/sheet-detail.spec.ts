import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { ConfirmationService, MessageService } from 'primeng/api';

import { SheetDetail } from './sheet-detail';
import { SheetsApiService, InstrumentationsApiService } from '../../core/api';
import { DocumentsApiService } from '../../core/api/documents-api.service';
import { TranslationService } from '../../core/translation.service';
import { Attachment, AttachmentType, Instrumentation } from '../../model/datamodels';

function makeInstr(id: string, name: string, partLabel?: string): Instrumentation {
  return { id, instrument: { id: 'x', name } as any, partLabel } as any;
}

function makeDoc(id: string, type: AttachmentType, mimeType = 'application/pdf'): Attachment {
  return { id, type, mimeType } as any;
}

describe('SheetDetail', () => {
  let component: SheetDetail;
  let sheetsApi: { load: ReturnType<typeof vi.fn>; favorite: ReturnType<typeof vi.fn>; unfavorite: ReturnType<typeof vi.fn> };
  let instrumentationsApi: { list: ReturnType<typeof vi.fn>; delete: ReturnType<typeof vi.fn> };
  let documentsApi: { forSheets: ReturnType<typeof vi.fn>; list: ReturnType<typeof vi.fn>; linkToSheet: ReturnType<typeof vi.fn>; downloadBatchByIds: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    sheetsApi = {
      load: vi.fn().mockReturnValue(of({ id: 's1', title: 'Test Sheet' })),
      favorite: vi.fn().mockReturnValue(of(undefined)),
      unfavorite: vi.fn().mockReturnValue(of(undefined)),
    };
    instrumentationsApi = {
      list: vi.fn().mockReturnValue(of([])),
      delete: vi.fn().mockReturnValue(of(undefined)),
    };
    documentsApi = {
      forSheets: vi.fn().mockReturnValue(''),
      list: vi.fn().mockReturnValue(of({ data: [], totalCount: 0 })),
      linkToSheet: vi.fn().mockReturnValue(of(undefined)),
      downloadBatchByIds: vi.fn(),
    };

    await TestBed.overrideComponent(SheetDetail, {
      set: {
        imports: [],
        template: '<div></div>',
        providers: [ConfirmationService],
      },
    })
      .configureTestingModule({
        imports: [SheetDetail],
        providers: [
          provideNoopAnimations(),
          provideRouter([]),
          MessageService,
          { provide: SheetsApiService, useValue: sheetsApi },
          { provide: InstrumentationsApiService, useValue: instrumentationsApi },
          { provide: DocumentsApiService, useValue: documentsApi },
          {
            provide: TranslationService,
            useValue: { t: (k: string) => k, version: { subscribe: () => {} } },
          },
        ],
      })
      .compileComponents();

    const fixture = TestBed.createComponent(SheetDetail);
    component = fixture.componentInstance;
    component.sheetId = 's1';
    fixture.detectChanges();
  });

  // ── pct ───────────────────────────────────────────────

  it('pct converts decimal to percentage string', () => {
    expect((component as any).pct(0.5)).toBe('50%');
    expect((component as any).pct(1)).toBe('100%');
    expect((component as any).pct(0)).toBe('0%');
  });

  it('pct treats undefined as 0', () => {
    expect((component as any).pct(undefined)).toBe('0%');
  });

  // ── coverageIcon ───────────────────────────────────────

  it('coverageIcon returns times-circle when missingRequired', () => {
    expect((component as any).coverageIcon({ missingRequired: true, score: 1 })).toBe('pi pi-times-circle');
  });

  it('coverageIcon returns check-circle for score >= 0.85', () => {
    expect((component as any).coverageIcon({ score: 0.85 })).toBe('pi pi-check-circle');
    expect((component as any).coverageIcon({ score: 1.0 })).toBe('pi pi-check-circle');
  });

  it('coverageIcon returns minus-circle for partial score', () => {
    expect((component as any).coverageIcon({ score: 0.5 })).toBe('pi pi-minus-circle');
    expect((component as any).coverageIcon({ score: 0.84 })).toBe('pi pi-minus-circle');
  });

  it('coverageIcon returns empty circle for zero score', () => {
    expect((component as any).coverageIcon({ score: 0 })).toBe('pi pi-circle');
    expect((component as any).coverageIcon({})).toBe('pi pi-circle');
  });

  // ── difficultyLevelKey ─────────────────────────────────

  it('difficultyLevelKey returns correct key for grade 1–6', () => {
    expect((component as any).difficultyLevelKey(1)).toBe('VERY_EASY');
    expect((component as any).difficultyLevelKey(3)).toBe('MEDIUM');
    expect((component as any).difficultyLevelKey(6)).toBe('VERY_DIFFICULT');
  });

  it('difficultyLevelKey returns empty string for out-of-range grade', () => {
    expect((component as any).difficultyLevelKey(0)).toBe('');
    expect((component as any).difficultyLevelKey(7)).toBe('');
  });

  // ── attachmentTypeIcon ─────────────────────────────────

  it('attachmentTypeIcon returns correct icons', () => {
    expect((component as any).attachmentTypeIcon('PART')).toBe('pi pi-file');
    expect((component as any).attachmentTypeIcon('FULL_SCORE')).toBe('pi pi-book');
    expect((component as any).attachmentTypeIcon('AUDIO')).toBe('pi pi-volume-up');
    expect((component as any).attachmentTypeIcon('MIDI')).toBe('pi pi-play');
    expect((component as any).attachmentTypeIcon('ANNOTATIONS')).toBe('pi pi-pencil');
    expect((component as any).attachmentTypeIcon('IMAGE')).toBe('pi pi-image');
    expect((component as any).attachmentTypeIcon('COVER')).toBe('pi pi-image');
    expect((component as any).attachmentTypeIcon('EXTERNAL_LINK')).toBe('pi pi-link');
    expect((component as any).attachmentTypeIcon('MUSIC_XML')).toBe('pi pi-code');
    expect((component as any).attachmentTypeIcon('LYRICS')).toBe('pi pi-align-left');
    expect((component as any).attachmentTypeIcon('ANALYSIS')).toBe('pi pi-chart-bar');
  });

  it('attachmentTypeIcon falls back to paperclip for unknown type', () => {
    expect((component as any).attachmentTypeIcon('OTHER')).toBe('pi pi-paperclip');
    expect((component as any).attachmentTypeIcon('UNSPECIFIED')).toBe('pi pi-paperclip');
  });

  // ── isFullMode ─────────────────────────────────────────

  it('isFullMode is false by default', () => {
    expect((component as any).isFullMode).toBe(false);
  });

  it('isFullMode is true when mode is full', () => {
    component.mode = 'full';
    expect((component as any).isFullMode).toBe(true);
  });

  // ── sanitizeFilename ───────────────────────────────────

  it('sanitizeFilename strips forbidden characters', () => {
    expect((component as any).sanitizeFilename('Hello/World:Test?')).toBe('HelloWorldTest');
  });

  it('sanitizeFilename collapses multiple spaces to single underscore', () => {
    expect((component as any).sanitizeFilename('My  Song')).toBe('My_Song');
  });

  it('sanitizeFilename returns "documents" for empty or whitespace title', () => {
    expect((component as any).sanitizeFilename('')).toBe('documents');
    expect((component as any).sanitizeFilename('   ')).toBe('documents');
  });

  // ── instrumentationOptions ─────────────────────────────

  it('instrumentationOptions maps instrumentations to label/value pairs', () => {
    (component as any).instrumentations.set([
      makeInstr('i1', 'Flute'),
      makeInstr('i2', 'Trumpet', '3rd'),
    ]);
    const opts = (component as any).instrumentationOptions();
    expect(opts).toEqual([
      { label: 'Flute', value: 'i1' },
      { label: 'Trumpet (3rd)', value: 'i2' },
    ]);
  });

  // ── alsoLinkOptions ────────────────────────────────────

  it('alsoLinkOptions excludes the source instrumentation', () => {
    (component as any).instrumentations.set([
      makeInstr('i1', 'Flute'),
      makeInstr('i2', 'Oboe'),
      makeInstr('i3', 'Trumpet'),
    ]);
    (component as any).alsoLinkSourceInstrId = 'i2';
    const opts = (component as any).alsoLinkOptions;
    expect(opts.map((o: any) => o.id)).toEqual(['i1', 'i3']);
  });

  // ── alsoLinkIsSelected / toggleAlsoLink ────────────────

  it('alsoLinkIsSelected returns false initially', () => {
    expect((component as any).alsoLinkIsSelected('i1')).toBe(false);
  });

  it('toggleAlsoLink adds an id to the selection', () => {
    (component as any).toggleAlsoLink('i1');
    expect((component as any).alsoLinkIsSelected('i1')).toBe(true);
  });

  it('toggleAlsoLink removes an already-selected id', () => {
    (component as any).toggleAlsoLink('i1');
    (component as any).toggleAlsoLink('i1');
    expect((component as any).alsoLinkIsSelected('i1')).toBe(false);
  });

  it('toggleAlsoLink keeps other ids intact', () => {
    (component as any).toggleAlsoLink('i1');
    (component as any).toggleAlsoLink('i2');
    (component as any).toggleAlsoLink('i1');
    expect((component as any).alsoLinkIsSelected('i2')).toBe(true);
  });

  // ── instrDocCount ──────────────────────────────────────

  it('instrDocCount returns 0 for unknown instrumentation', () => {
    expect((component as any).instrDocCount('unknown')).toBe(0);
  });

  it('instrDocCount returns the cached doc count', () => {
    (component as any).instrDocsCache.set(new Map([['i1', [makeDoc('d1', 'PART'), makeDoc('d2', 'FULL_SCORE')]]]));
    expect((component as any).instrDocCount('i1')).toBe(2);
  });

  // ── instrDocBadges ─────────────────────────────────────

  it('instrDocBadges groups documents by type with counts', () => {
    (component as any).instrDocsCache.set(new Map([
      ['i1', [makeDoc('d1', 'PART'), makeDoc('d2', 'PART'), makeDoc('d3', 'FULL_SCORE')]],
    ]));
    const badges = (component as any).instrDocBadges('i1');
    const byType = Object.fromEntries(badges.map((b: any) => [b.type, b.count]));
    expect(byType['PART']).toBe(2);
    expect(byType['FULL_SCORE']).toBe(1);
  });

  it('instrDocBadges uses correct icon for each type', () => {
    (component as any).instrDocsCache.set(new Map([['i1', [makeDoc('d1', 'AUDIO')]]]));
    const badges = (component as any).instrDocBadges('i1');
    expect(badges[0].icon).toBe('pi pi-volume-up');
  });

  it('instrDocBadges returns empty array for empty cache', () => {
    expect((component as any).instrDocBadges('missing')).toEqual([]);
  });

  // ── availableTypes ─────────────────────────────────────

  it('availableTypes collects distinct types from all cached docs', () => {
    (component as any).instrDocsCache.set(new Map([
      ['i1', [makeDoc('d1', 'PART')]],
      ['i2', [makeDoc('d2', 'PART'), makeDoc('d3', 'FULL_SCORE')]],
    ]));
    const types = (component as any).availableTypes();
    expect(types).toContain('PART');
    expect(types).toContain('FULL_SCORE');
    expect(types).toHaveLength(2);
  });

  // ── allSelectedArePdf ──────────────────────────────────

  it('allSelectedArePdf is false when nothing is selected', () => {
    expect((component as any).allSelectedArePdf()).toBe(false);
  });

  it('allSelectedArePdf is true when all selected docs are PDF', () => {
    (component as any).instrDocsCache.set(new Map([['i1', [makeDoc('d1', 'PART', 'application/pdf')]]]));
    (component as any).selectedDocMap.set(new Map([['i1', new Set(['d1'])]]));
    expect((component as any).allSelectedArePdf()).toBe(true);
  });

  it('allSelectedArePdf is false when a non-PDF is selected', () => {
    (component as any).instrDocsCache.set(new Map([
      ['i1', [makeDoc('d1', 'PART', 'application/pdf'), makeDoc('d2', 'AUDIO', 'audio/mp3')]],
    ]));
    (component as any).selectedDocMap.set(new Map([['i1', new Set(['d1', 'd2'])]]));
    expect((component as any).allSelectedArePdf()).toBe(false);
  });

  // ── selectAllByType ────────────────────────────────────

  it('selectAllByType adds matching docs across all instrumentations', () => {
    (component as any).instrDocsCache.set(new Map([
      ['i1', [makeDoc('d1', 'PART'), makeDoc('d2', 'FULL_SCORE')]],
      ['i2', [makeDoc('d3', 'PART')]],
    ]));
    (component as any).selectAllByType('PART');
    const map: Map<string, Set<string>> = (component as any).selectedDocMap();
    expect(map.get('i1')?.has('d1')).toBe(true);
    expect(map.get('i1')?.has('d2')).toBe(false);
    expect(map.get('i2')?.has('d3')).toBe(true);
  });

  it('selectAllByType merges with existing selection', () => {
    (component as any).instrDocsCache.set(new Map([['i1', [makeDoc('d1', 'PART'), makeDoc('d2', 'FULL_SCORE')]]]));
    (component as any).selectedDocMap.set(new Map([['i1', new Set(['d2'])]]));
    (component as any).selectAllByType('PART');
    const set = (component as any).selectedDocMap().get('i1');
    expect(set.has('d1')).toBe(true);
    expect(set.has('d2')).toBe(true);
  });

  // ── clearSelection ─────────────────────────────────────

  it('clearSelection empties the selection map', () => {
    (component as any).instrDocsCache.set(new Map([['i1', [makeDoc('d1', 'PART')]]]));
    (component as any).selectedDocMap.set(new Map([['i1', new Set(['d1'])]]));
    (component as any).clearSelection();
    expect((component as any).selectedDocMap().size).toBe(0);
  });

  // ── getSelectedIdsForInstr ─────────────────────────────

  it('getSelectedIdsForInstr returns empty Set for unknown instrumentation', () => {
    expect((component as any).getSelectedIdsForInstr('unknown').size).toBe(0);
  });

  it('getSelectedIdsForInstr returns the correct Set', () => {
    (component as any).selectedDocMap.set(new Map([['i1', new Set(['d1', 'd2'])]]));
    const ids = (component as any).getSelectedIdsForInstr('i1');
    expect(ids.has('d1')).toBe(true);
    expect(ids.has('d2')).toBe(true);
  });

  // ── onDocsLoaded / onToggleDoc ─────────────────────────

  it('onDocsLoaded adds docs to the cache', () => {
    (component as any).onDocsLoaded({ instrId: 'i1', docs: [makeDoc('d1', 'PART')] });
    expect((component as any).instrDocsCache().get('i1')).toHaveLength(1);
  });

  it('onToggleDoc adds doc to selection when selected=true', () => {
    (component as any).onToggleDoc({ instrId: 'i1', docId: 'd1', selected: true });
    expect((component as any).selectedDocMap().get('i1')?.has('d1')).toBe(true);
  });

  it('onToggleDoc removes doc from selection when selected=false', () => {
    (component as any).selectedDocMap.set(new Map([['i1', new Set(['d1'])]]));
    (component as any).onToggleDoc({ instrId: 'i1', docId: 'd1', selected: false });
    expect((component as any).selectedDocMap().get('i1')?.has('d1')).toBe(false);
  });
});
