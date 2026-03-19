import { TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { ConfirmationService, MessageService } from 'primeng/api';

import { CollectionSheets } from './collection-sheets';
import { CollectionsApiService, SheetsApiService } from '../../core/api';
import { TranslationService } from '../../core/translation.service';
import { CollectionSheet, SheetMusicSearchResult } from '../../model/datamodels';

function paginatedOf<T>(data: T[], totalCount = data.length) {
  return of({ data, totalCount, page: 0, size: data.length });
}

function makeSearchResult(id: string, title = 'Test Sheet'): SheetMusicSearchResult {
  return { id, title } as any;
}

function makeCollectionSheet(id: string, sheetId: string, identifier = 'A1'): CollectionSheet {
  return { id, sheetId, identifier, title: 'Some Sheet' } as any;
}

describe('CollectionSheets', () => {
  let component: CollectionSheets;
  let collectionsApi: {
    listSheets: ReturnType<typeof vi.fn>;
    addSheet: ReturnType<typeof vi.fn>;
    updateSheet: ReturnType<typeof vi.fn>;
    removeSheet: ReturnType<typeof vi.fn>;
  };
  let sheetsApi: { find: ReturnType<typeof vi.fn>; load: ReturnType<typeof vi.fn> };
  let confirmationService: ConfirmationService;

  beforeEach(async () => {
    collectionsApi = {
      listSheets: vi.fn().mockReturnValue(paginatedOf([])),
      addSheet: vi.fn().mockReturnValue(of(undefined)),
      updateSheet: vi.fn().mockReturnValue(of(undefined)),
      removeSheet: vi.fn().mockReturnValue(of(undefined)),
    };
    sheetsApi = {
      find: vi.fn().mockReturnValue(paginatedOf([])),
      load: vi.fn().mockReturnValue(of({ id: 's1', title: 'T', instrumentations: [] })),
    };

    await TestBed.overrideComponent(CollectionSheets, {
      set: {
        imports: [ReactiveFormsModule],
        template: '<div></div>',
        providers: [ConfirmationService],
      },
    })
      .configureTestingModule({
        imports: [CollectionSheets],
        providers: [
          provideNoopAnimations(),
          provideRouter([]),
          MessageService,
          { provide: CollectionsApiService, useValue: collectionsApi },
          { provide: SheetsApiService, useValue: sheetsApi },
          {
            provide: TranslationService,
            useValue: { t: (k: string) => k, version: { subscribe: () => {} } },
          },
        ],
      })
      .compileComponents();

    const fixture = TestBed.createComponent(CollectionSheets);
    component = fixture.componentInstance;
    component.collectionId = 'col-1';
    fixture.detectChanges();
    confirmationService = (component as any).confirmationService as ConfirmationService;
  });

  // ── canAdd ─────────────────────────────────────────────

  it('canAdd is false when no sheet selected', () => {
    (component as any).selectedSheet = null;
    expect((component as any).canAdd).toBe(false);
  });

  it('canAdd is false when sheet selected but identifier empty', () => {
    (component as any).selectedSheet = makeSearchResult('s1');
    (component as any).identifierForm.controls.identifier.setValue('');
    expect((component as any).canAdd).toBe(false);
  });

  it('canAdd is true when sheet selected and identifier filled', () => {
    (component as any).selectedSheet = makeSearchResult('s1');
    (component as any).identifierForm.controls.identifier.setValue('1a');
    expect((component as any).canAdd).toBe(true);
  });

  // ── isSelected ─────────────────────────────────────────

  it('isSelected returns true for the selected sheet', () => {
    const sheet = makeSearchResult('s1');
    (component as any).selectedSheet = sheet;
    expect((component as any).isSelected(sheet)).toBe(true);
  });

  it('isSelected returns false for a different sheet', () => {
    (component as any).selectedSheet = makeSearchResult('s1');
    expect((component as any).isSelected(makeSearchResult('s2'))).toBe(false);
  });

  it('isSelected returns false when nothing is selected', () => {
    (component as any).selectedSheet = null;
    expect((component as any).isSelected(makeSearchResult('s1'))).toBe(false);
  });

  // ── difficultyLevelKey ─────────────────────────────────

  it('difficultyLevelKey returns the correct key for grade 1', () => {
    expect((component as any).difficultyLevelKey(1)).toBe('VERY_EASY');
  });

  it('difficultyLevelKey returns the correct key for grade 6', () => {
    expect((component as any).difficultyLevelKey(6)).toBe('VERY_DIFFICULT');
  });

  it('difficultyLevelKey returns empty string for out-of-range grade', () => {
    expect((component as any).difficultyLevelKey(0)).toBe('');
    expect((component as any).difficultyLevelKey(7)).toBe('');
  });

  // ── openAdd ────────────────────────────────────────────

  it('openAdd resets state and shows dialog', () => {
    (component as any).selectedSheet = makeSearchResult('s1');
    (component as any).identifierForm.controls.identifier.setValue('X');
    sheetsApi.find.mockClear();

    (component as any).openAdd();

    expect((component as any).selectedSheet).toBeNull();
    expect((component as any).identifierForm.controls.identifier.value).toBe('');
    expect((component as any).addDialogVisible).toBe(true);
    expect(sheetsApi.find).toHaveBeenCalledOnce();
  });

  // ── openEdit ───────────────────────────────────────────

  it('openEdit populates form and shows edit dialog', () => {
    const sheet = makeCollectionSheet('cs1', 's1', '2b');
    (component as any).openEdit(sheet);
    expect((component as any).editingSheet).toBe(sheet);
    expect((component as any).identifierForm.controls.identifier.value).toBe('2b');
    expect((component as any).editDialogVisible).toBe(true);
  });

  // ── onAdd ──────────────────────────────────────────────

  it('onAdd does nothing when canAdd is false', () => {
    (component as any).selectedSheet = null;
    (component as any).onAdd();
    expect(collectionsApi.addSheet).not.toHaveBeenCalled();
  });

  it('onAdd calls addSheet and reloads on success', () => {
    (component as any).selectedSheet = makeSearchResult('s2');
    (component as any).identifierForm.controls.identifier.setValue('3c');
    collectionsApi.listSheets.mockClear();

    (component as any).onAdd();

    expect(collectionsApi.addSheet).toHaveBeenCalledWith('col-1', { identifier: '3c', sheetId: 's2' });
    expect((component as any).addDialogVisible).toBe(false);
    expect(collectionsApi.listSheets).toHaveBeenCalled();
  });

  // ── onUpdate ───────────────────────────────────────────

  it('onUpdate does nothing when form is invalid', () => {
    (component as any).editingSheet = makeCollectionSheet('cs1', 's1');
    (component as any).identifierForm.controls.identifier.setValue('');
    (component as any).onUpdate();
    expect(collectionsApi.updateSheet).not.toHaveBeenCalled();
  });

  it('onUpdate does nothing when editingSheet is null', () => {
    (component as any).editingSheet = null;
    (component as any).onUpdate();
    expect(collectionsApi.updateSheet).not.toHaveBeenCalled();
  });

  it('onUpdate calls updateSheet with correct payload', () => {
    const sheet = makeCollectionSheet('cs1', 's1', '1a');
    (component as any).editingSheet = sheet;
    (component as any).identifierForm.reset({ identifier: '2b' });
    collectionsApi.listSheets.mockClear();

    (component as any).onUpdate();

    expect(collectionsApi.updateSheet).toHaveBeenCalledWith('col-1', 'cs1', { identifier: '2b', sheetId: 's1' });
    expect((component as any).editDialogVisible).toBe(false);
    expect(collectionsApi.listSheets).toHaveBeenCalled();
  });

  // ── confirmRemove ──────────────────────────────────────

  it('confirmRemove calls confirm and removeSheet on accept', () => {
    const sheet = makeCollectionSheet('cs1', 's1');
    vi.spyOn(confirmationService, 'confirm').mockImplementation(({ accept }: any) => accept?.());
    collectionsApi.listSheets.mockClear();

    (component as any).confirmRemove(sheet);

    expect(collectionsApi.removeSheet).toHaveBeenCalledWith('col-1', 'cs1');
    expect(collectionsApi.listSheets).toHaveBeenCalled();
  });

  // ── loadSheets on input change ─────────────────────────

  it('ngOnChanges triggers loadSheets with collectionId', () => {
    collectionsApi.listSheets.mockClear();
    component.collectionId = 'col-2';
    component.ngOnChanges();
    expect(collectionsApi.listSheets).toHaveBeenCalledWith('col-2', expect.any(Object));
  });
});
