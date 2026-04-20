import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { MessageService } from 'primeng/api';

import { CollectionDetailPage } from './collection-detail-page';
import { CollectionsApiService } from '../../core/api';
import { TranslationService } from '../../core/translation.service';
import { SheetCollection } from '../../model/datamodels';

function makeCollection(overrides: Partial<SheetCollection> = {}): SheetCollection {
  return { id: 'col-1' as any, name: 'Test', type: 'SETLIST', ...overrides };
}

describe('CollectionDetailPage', () => {
  let component: CollectionDetailPage;
  let collectionsApi: {
    load: ReturnType<typeof vi.fn>;
    downloadToc: ReturnType<typeof vi.fn>;
    downloadGemaSetlist: ReturnType<typeof vi.fn>;
    export: ReturnType<typeof vi.fn>;
  };
  let messageService: MessageService;

  const fakeBlobResponse = { body: new Blob(), headers: { get: () => null } };

  beforeEach(async () => {
    collectionsApi = {
      load: vi.fn().mockReturnValue(of(makeCollection())),
      downloadToc: vi.fn().mockReturnValue(of(fakeBlobResponse)),
      downloadGemaSetlist: vi.fn().mockReturnValue(of(fakeBlobResponse)),
      export: vi.fn().mockReturnValue(of(fakeBlobResponse)),
    };

    await TestBed.overrideComponent(CollectionDetailPage, {
      set: {
        imports: [],
        template: '<div></div>',
        providers: [],
      },
    })
      .configureTestingModule({
        imports: [CollectionDetailPage],
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

    const fixture = TestBed.createComponent(CollectionDetailPage);
    component = fixture.componentInstance;
    fixture.detectChanges(); // triggers ngOnInit (route param is null in test env)
    (component as any).collectionId.set('col-1'); // override after init
    messageService = TestBed.inject(MessageService);
  });

  // ── isSetlist ──────────────────────────────────────────

  it('isSetlist is true when collection type is SETLIST', () => {
    (component as any).collection.set(makeCollection({ type: 'SETLIST' }));
    expect((component as any).isSetlist).toBe(true);
  });

  it('isSetlist is false when collection type is FOLDER', () => {
    (component as any).collection.set(makeCollection({ type: 'FOLDER' }));
    expect((component as any).isSetlist).toBe(false);
  });

  it('isSetlist is false when collection is null', () => {
    (component as any).collection.set(null);
    expect((component as any).isSetlist).toBe(false);
  });

  it('isSetlist is false when collection has no type', () => {
    (component as any).collection.set(makeCollection({ type: undefined }));
    expect((component as any).isSetlist).toBe(false);
  });

  // ── generateGemaSetlist ────────────────────────────────

  it('generateGemaSetlist calls downloadGemaSetlist with the current collectionId', () => {
    (component as any).generateGemaSetlist();
    expect(collectionsApi.downloadGemaSetlist).toHaveBeenCalledWith('col-1');
  });

  it('generateGemaSetlist shows error toast when API call fails', () => {
    collectionsApi.downloadGemaSetlist.mockReturnValue(throwError(() => new Error('server error')));
    vi.spyOn(messageService, 'add');

    (component as any).generateGemaSetlist();

    expect(messageService.add).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'error' }),
    );
  });
});
