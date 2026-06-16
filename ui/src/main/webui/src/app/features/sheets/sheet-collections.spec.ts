import { TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { ConfirmationService, MessageService } from 'primeng/api';

import { SheetCollections } from './sheet-collections';
import { CollectionsApiService } from '../../core/api';
import { TranslationService } from '../../core/translation.service';
import { SheetCollection } from '../../model/datamodels';

function paginatedOf<T>(data: T[], totalCount = data.length) {
  return of({ data, totalCount, page: 0, size: data.length });
}

function makeCollection(overrides: Partial<SheetCollection> = {}): SheetCollection {
  return { id: crypto.randomUUID() as any, name: 'Test Collection', type: 'FOLDER', ...overrides };
}

describe('SheetCollections', () => {
  let component: SheetCollections;
  let collectionsApi: { find: ReturnType<typeof vi.fn>; getCollectionsForSheet: ReturnType<typeof vi.fn>; addItem: ReturnType<typeof vi.fn>; removeItem: ReturnType<typeof vi.fn> };
  let messageService: MessageService;
  let confirmationService: ConfirmationService;

  beforeEach(async () => {
    collectionsApi = {
      find: vi.fn().mockReturnValue(paginatedOf([])),
      getCollectionsForSheet: vi.fn().mockReturnValue(paginatedOf([])),
      addItem: vi.fn().mockReturnValue(of(undefined)),
      removeItem: vi.fn().mockReturnValue(of(undefined)),
    };

    await TestBed.overrideComponent(SheetCollections, {
      set: {
        imports: [ReactiveFormsModule],
        template: '<div></div>',
        providers: [ConfirmationService],
      },
    })
      .configureTestingModule({
        imports: [SheetCollections],
        providers: [
          provideNoopAnimations(),
          provideRouter([]),
          MessageService,
          { provide: CollectionsApiService, useValue: collectionsApi },
          {
            provide: TranslationService,
            useValue: { t: (k: string) => k, version: { subscribe: () => {} } },
          },
        ],
      })
      .compileComponents();

    const fixture = TestBed.createComponent(SheetCollections);
    component = fixture.componentInstance;
    component.sheetId = 'sheet-123';
    fixture.detectChanges();
    messageService = TestBed.inject(MessageService);
    confirmationService = (component as any).confirmationService as ConfirmationService;
  });

  // ── canAdd ─────────────────────────────────────────────

  it('canAdd is false when no collection selected and form empty', () => {
    expect((component as any).canAdd).toBe(false);
  });

  it('canAdd is false when collection selected but identifier empty', () => {
    (component as any).selectedCollection = makeCollection();
    (component as any).identifierForm.controls.identifier.setValue('');
    expect((component as any).canAdd).toBe(false);
  });

  it('canAdd is true when collection selected and identifier filled', () => {
    (component as any).selectedCollection = makeCollection();
    (component as any).identifierForm.controls.identifier.setValue('1a');
    expect((component as any).canAdd).toBe(true);
  });

  // ── isSelected ─────────────────────────────────────────

  it('isSelected returns true for the currently selected collection', () => {
    const col = makeCollection({ id: '42' as any });
    (component as any).selectedCollection = col;
    expect((component as any).isSelected(col)).toBe(true);
  });

  it('isSelected returns false for a different collection', () => {
    const col1 = makeCollection({ id: '1' as any });
    const col2 = makeCollection({ id: '2' as any });
    (component as any).selectedCollection = col1;
    expect((component as any).isSelected(col2)).toBe(false);
  });

  it('isSelected returns false when nothing is selected', () => {
    (component as any).selectedCollection = null;
    expect((component as any).isSelected(makeCollection())).toBe(false);
  });

  // ── openAdd ────────────────────────────────────────────

  it('openAdd resets selection and form, then calls find()', () => {
    (component as any).selectedCollection = makeCollection();
    (component as any).identifierForm.controls.identifier.setValue('X');
    collectionsApi.find.mockClear();

    component.openAdd();

    expect((component as any).selectedCollection).toBeNull();
    expect((component as any).identifierForm.controls.identifier.value).toBe('');
    expect((component as any).addDialogVisible).toBe(true);
    expect(collectionsApi.find).toHaveBeenCalledOnce();
  });

  // ── doSearch filters out already-added collections ──────

  it('doSearch excludes collections already in membership list', () => {
    const existing = makeCollection({ id: 'existing-id' as any });
    const other = makeCollection({ id: 'other-id' as any });
    (component as any).collections.set([existing]);
    collectionsApi.find.mockReturnValue(paginatedOf([existing, other]));

    (component as any).doSearch();

    const results: SheetCollection[] = (component as any).searchResults();
    expect(results.map((c: SheetCollection) => c.id)).not.toContain('existing-id');
    expect(results.map((c: SheetCollection) => c.id)).toContain('other-id');
  });

  // ── onAdd ──────────────────────────────────────────────

  it('onAdd does nothing when canAdd is false', () => {
    (component as any).selectedCollection = null;
    (component as any).onAdd();
    expect(collectionsApi.addItem).not.toHaveBeenCalled();
  });

  it('onAdd calls addItem with correct payload and reloads on success', () => {
    const col = makeCollection({ id: 'col-999' as any });
    (component as any).selectedCollection = col;
    (component as any).identifierForm.controls.identifier.setValue('2b');
    collectionsApi.getCollectionsForSheet.mockReturnValue(paginatedOf([]));

    (component as any).onAdd();

    expect(collectionsApi.addItem).toHaveBeenCalledWith('col-999', {
      type: 'SHEET',
      identifier: '2b',
      sheetId: 'sheet-123',
    });
    expect((component as any).addDialogVisible).toBe(false);
    expect(collectionsApi.getCollectionsForSheet).toHaveBeenCalled();
  });

  // ── confirmRemove ─────────────────────────────────────

  it('confirmRemove does nothing when collection has no sheets entry', () => {
    const col = makeCollection({ items: [] });
    const confirmSpy = vi.spyOn(confirmationService, 'confirm');
    (component as any).confirmRemove(col);
    expect(confirmSpy).not.toHaveBeenCalled();
  });

  it('confirmRemove calls confirm and removeItem on accept', () => {
    const col = makeCollection({
      id: 'col-1' as any,
      items: [{ id: 'cs-1' as any, sheetId: 'sheet-123' as any }] as any,
    });
    vi.spyOn(confirmationService, 'confirm').mockImplementation(({ accept }: any) => accept?.());
    collectionsApi.getCollectionsForSheet.mockReturnValue(paginatedOf([]));

    (component as any).confirmRemove(col);

    expect(collectionsApi.removeItem).toHaveBeenCalledWith('col-1', 'cs-1');
  });

  // ── loadCollections on input change ───────────────────

  it('loadCollections is called with sheetId on ngOnChanges', () => {
    collectionsApi.getCollectionsForSheet.mockClear();
    component.sheetId = 'new-sheet';
    component.ngOnChanges();
    expect(collectionsApi.getCollectionsForSheet).toHaveBeenCalledWith('new-sheet', expect.any(Object));
  });

  it('collections signal is populated from API response', () => {
    const col = makeCollection();
    collectionsApi.getCollectionsForSheet.mockReturnValue(paginatedOf([col]));
    component.ngOnChanges();
    const cols: SheetCollection[] = (component as any).collections();
    expect(cols).toHaveLength(1);
    expect(cols[0].id).toBe(col.id);
  });
});
