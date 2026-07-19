import { TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { MessageService } from 'primeng/api';

import { SheetForm } from './sheet-form';
import { SheetsApiService, MusiciansApiService, CollectionsApiService } from '../../core/api';
import { TranslationService } from '../../core/translation.service';
import { Musician, SheetMusic } from '../../model/datamodels';

function makeMusician(id: string, name: string): Musician {
  return { id, name } as any;
}

function makeSheet(overrides: Partial<SheetMusic> = {}): SheetMusic {
  return { id: 's1' as any, title: 'Test', genre: 'MARCH', ...overrides } as any;
}

function paginatedOf<T>(data: T[]) {
  return of({ data, totalCount: data.length, page: 0, size: data.length });
}

describe('SheetForm', () => {
  let component: SheetForm;
  let musiciansApi: { find: ReturnType<typeof vi.fn> };
  let collectionsApi: { find: ReturnType<typeof vi.fn> };
  let sheetsApi: { create: ReturnType<typeof vi.fn>; update: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    musiciansApi = { find: vi.fn().mockReturnValue(paginatedOf([])) };
    collectionsApi = { find: vi.fn().mockReturnValue(paginatedOf([])) };
    sheetsApi = {
      create: vi.fn().mockReturnValue(of(makeSheet())),
      update: vi.fn().mockReturnValue(of(undefined)),
    };

    await TestBed.overrideComponent(SheetForm, {
      set: {
        imports: [ReactiveFormsModule],
        template: '<div></div>',
        providers: [],
      },
    })
      .configureTestingModule({
        imports: [SheetForm],
        providers: [
          provideNoopAnimations(),
          MessageService,
          { provide: SheetsApiService, useValue: sheetsApi },
          { provide: MusiciansApiService, useValue: musiciansApi },
          { provide: CollectionsApiService, useValue: collectionsApi },
          {
            provide: TranslationService,
            useValue: { t: (k: string) => k, version: { subscribe: () => {} } },
          },
        ],
      })
      .compileComponents();

    const fixture = TestBed.createComponent(SheetForm);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ── isEdit ─────────────────────────────────────────────

  it('isEdit is false when no sheet is provided', () => {
    component.sheet = null;
    expect((component as any).isEdit).toBe(false);
  });

  it('isEdit is true when a sheet is provided', () => {
    component.sheet = makeSheet();
    expect((component as any).isEdit).toBe(true);
  });

  // ── patchFormValues ────────────────────────────────────

  it('patchFormValues fills title and genre', () => {
    const sheet = makeSheet({ title: 'Fanfare', genre: 'JAZZ', yearOfComposition: 1990 });
    (component as any).patchFormValues(sheet);
    expect(component.form.controls.title.value).toBe('Fanfare');
    expect(component.form.controls.genre.value).toBe('JAZZ');
    expect(component.form.controls.yearOfComposition.value).toBe(1990);
  });

  it('patchFormValues maps composer to composerId', () => {
    const sheet = makeSheet({ composer: makeMusician('m1', 'Bach') });
    (component as any).patchFormValues(sheet);
    expect(component.form.controls.composerId.value).toBe('m1');
  });

  it('patchFormValues sets composerId to null when no composer', () => {
    const sheet = makeSheet({ composer: undefined });
    (component as any).patchFormValues(sheet);
    expect(component.form.controls.composerId.value).toBeNull();
  });

  // ── onMusicianCreated ──────────────────────────────────

  it('onMusicianCreated adds musician to list sorted by name', () => {
    (component as any).musicians.set([makeMusician('m1', 'Brahms'), makeMusician('m2', 'Wagner')]);
    const newMusician = makeMusician('m3', 'Beethoven');

    component.onMusicianCreated(newMusician);

    const names = (component as any).musicians().map((m: Musician) => m.name);
    expect(names).toEqual(['Beethoven', 'Brahms', 'Wagner']);
  });

  it('onMusicianCreated sets composerId when target is composer', () => {
    (component as any).musicianDialogTarget = 'composer';
    const newMusician = makeMusician('m5', 'Schubert');

    component.onMusicianCreated(newMusician);

    expect(component.form.controls.composerId.value).toBe('m5');
    expect((component as any).showMusicianDialog).toBe(false);
  });

  it('onMusicianCreated sets arrangerId when target is arranger', () => {
    (component as any).musicianDialogTarget = 'arranger';
    const newMusician = makeMusician('m6', 'Liszt');

    component.onMusicianCreated(newMusician);

    expect(component.form.controls.arrangerId.value).toBe('m6');
    expect(component.form.controls.composerId.value).toBeNull();
    expect((component as any).showMusicianDialog).toBe(false);
  });

  it('onMusicianCreated closes the musician dialog', () => {
    (component as any).showMusicianDialog = true;
    component.onMusicianCreated(makeMusician('m7', 'Handel'));
    expect((component as any).showMusicianDialog).toBe(false);
  });

  // ── openMusicianDialog ─────────────────────────────────

  it('openMusicianDialog sets target and shows dialog', () => {
    component.openMusicianDialog('arranger');
    expect((component as any).musicianDialogTarget).toBe('arranger');
    expect((component as any).showMusicianDialog).toBe(true);
  });
});
