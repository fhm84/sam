import { TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { ConfirmationService, MessageService } from 'primeng/api';

import { CollectionSheets } from './collection-sheets';
import { CollectionsApiService, SheetsApiService } from '../../core/api';
import { TranslationService } from '../../core/translation.service';
import { CollectionItem, SheetMusicSearchResult } from '../../model/datamodels';

function paginatedOf<T>(data: T[], totalCount = data.length) {
  return of({ data, totalCount, page: 0, size: data.length });
}

function makeSearchResult(id: string, title = 'Test Sheet'): SheetMusicSearchResult {
  return { id, title } as any;
}

function makeCollectionItem(id: string, sheetId: string, identifier = 'A1'): CollectionItem {
  return { id, sheetId, identifier, title: 'Some Sheet', type: 'SHEET' };
}

describe('CollectionSheets', () => {
  let component: CollectionSheets;
  let collectionsApi: {
    listItems: ReturnType<typeof vi.fn>;
    addItem: ReturnType<typeof vi.fn>;
    updateItem: ReturnType<typeof vi.fn>;
    removeItem: ReturnType<typeof vi.fn>;
  };
  let sheetsApi: { find: ReturnType<typeof vi.fn>; load: ReturnType<typeof vi.fn> };
  let confirmationService: ConfirmationService;

  beforeEach(async () => {
    collectionsApi = {
      listItems: vi.fn().mockReturnValue(paginatedOf([])),
      addItem: vi.fn().mockReturnValue(of(undefined)),
      updateItem: vi.fn().mockReturnValue(of(undefined)),
      removeItem: vi.fn().mockReturnValue(of(undefined)),
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

  // ── canAddSheet ──────────────────────────────────────────

  it('canAddSheet is false when no sheet selected', () => {
    (component as any).selectedSheet = null;
    expect((component as any).canAddSheet).toBe(false);
  });

  it('canAddSheet is true when a sheet is selected', () => {
    (component as any).selectedSheet = makeSearchResult('s1');
    expect((component as any).canAddSheet).toBe(true);
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

  // ── openAddSheet ───────────────────────────────────────

  it('openAddSheet resets state and shows dialog', () => {
    (component as any).selectedSheet = makeSearchResult('s1');
    (component as any).identifierForm.controls.identifier.setValue('X');
    sheetsApi.find.mockClear();

    (component as any).openAddSheet();

    expect((component as any).selectedSheet).toBeNull();
    expect((component as any).identifierForm.controls.identifier.value).toBe('');
    expect((component as any).addSheetDialogVisible).toBe(true);
    expect(sheetsApi.find).toHaveBeenCalledOnce();
  });

  it('openAddSheet resets createAnother to false', () => {
    (component as any).createAnother = true;
    (component as any).openAddSheet();
    expect((component as any).createAnother).toBe(false);
  });

  // ── openEdit ───────────────────────────────────────────

  it('openEdit populates form and shows edit dialog', () => {
    const item = makeCollectionItem('cs1', 's1', '2b');
    (component as any).openEdit(item);
    expect((component as any).editingItem).toBe(item);
    expect((component as any).identifierForm.controls.identifier.value).toBe('2b');
    expect((component as any).editDialogVisible).toBe(true);
  });

  // ── onAddSheet ───────────────────────────────────────────

  it('onAddSheet does nothing when canAddSheet is false', () => {
    (component as any).selectedSheet = null;
    (component as any).onAddSheet();
    expect(collectionsApi.addItem).not.toHaveBeenCalled();
  });

  it('onAddSheet calls addItem and reloads on success', () => {
    (component as any).selectedSheet = makeSearchResult('s2');
    (component as any).identifierForm.controls.identifier.setValue('3c');
    collectionsApi.listItems.mockClear();

    (component as any).onAddSheet();

    expect(collectionsApi.addItem).toHaveBeenCalledWith('col-1', {
      type: 'SHEET',
      identifier: '3c',
      sheetId: 's2',
    });
    expect((component as any).addSheetDialogVisible).toBe(false);
    expect(collectionsApi.listItems).toHaveBeenCalled();
  });

  // ── createAnother ──────────────────────────────────────

  it('onAddSheet with createAnother=true keeps dialog open and resets selection and identifier', () => {
    (component as any).addSheetDialogVisible = true;
    (component as any).selectedSheet = makeSearchResult('s3');
    (component as any).identifierForm.controls.identifier.setValue('5');
    (component as any).createAnother = true;

    (component as any).onAddSheet();

    expect((component as any).addSheetDialogVisible).toBe(true);
    expect((component as any).selectedSheet).toBeNull();
    expect((component as any).identifierForm.controls.identifier.value).toBe('');
  });

  it('onAddSheet with createAnother=true still calls addItem and reloads items', () => {
    (component as any).selectedSheet = makeSearchResult('s3');
    (component as any).identifierForm.controls.identifier.setValue('5');
    (component as any).createAnother = true;
    collectionsApi.listItems.mockClear();

    (component as any).onAddSheet();

    expect(collectionsApi.addItem).toHaveBeenCalledWith('col-1', {
      type: 'SHEET',
      identifier: '5',
      sheetId: 's3',
    });
    expect(collectionsApi.listItems).toHaveBeenCalled();
  });

  it('onAddSheet with createAnother=false closes dialog as normal', () => {
    (component as any).selectedSheet = makeSearchResult('s4');
    (component as any).identifierForm.controls.identifier.setValue('6');
    (component as any).createAnother = false;

    (component as any).onAddSheet();

    expect((component as any).addSheetDialogVisible).toBe(false);
  });

  // ── onUpdate ───────────────────────────────────────────

  it('onUpdate does nothing when text item form is invalid', () => {
    (component as any).editingItem = {
      id: 'cs1',
      identifier: 'A1',
      type: 'TEXT',
      textContent: '',
    } as CollectionItem;
    (component as any).textForm.reset({ identifier: 'A1', textContent: '' });

    (component as any).onUpdate();

    expect(collectionsApi.updateItem).not.toHaveBeenCalled();
  });

  it('onUpdate does nothing when editingItem is null', () => {
    (component as any).editingItem = null;
    (component as any).onUpdate();
    expect(collectionsApi.updateItem).not.toHaveBeenCalled();
  });

  it('onUpdate calls updateItem with correct payload', () => {
    const item = makeCollectionItem('cs1', 's1', '1a');
    (component as any).editingItem = item;
    (component as any).identifierForm.reset({ identifier: '2b' });
    collectionsApi.listItems.mockClear();

    (component as any).onUpdate();

    expect(collectionsApi.updateItem).toHaveBeenCalledWith('col-1', 'cs1', {
      ...item,
      identifier: '2b',
    });
    expect((component as any).editDialogVisible).toBe(false);
    expect(collectionsApi.listItems).toHaveBeenCalled();
  });

  // ── confirmRemove ──────────────────────────────────────

  it('confirmRemove calls confirm and removeItem on accept', () => {
    const item = makeCollectionItem('cs1', 's1');
    vi.spyOn(confirmationService, 'confirm').mockImplementation(({ accept }: any) => accept?.());
    collectionsApi.listItems.mockClear();

    (component as any).confirmRemove(item);

    expect(collectionsApi.removeItem).toHaveBeenCalledWith('col-1', 'cs1');
    expect(collectionsApi.listItems).toHaveBeenCalled();
  });

  // ── loadItems on input change ───────────────────────────

  it('ngOnChanges triggers loadItems with collectionId', () => {
    collectionsApi.listItems.mockClear();
    component.collectionId = 'col-2';
    component.ngOnChanges();
    expect(collectionsApi.listItems).toHaveBeenCalledWith(
      'col-2',
      expect.any(Object),
      expect.any(Boolean),
    );
  });
});
