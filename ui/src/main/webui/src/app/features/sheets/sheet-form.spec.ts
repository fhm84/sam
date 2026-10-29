import { TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { MessageService } from '@openng/optimus-ui/api';

import { SheetForm } from './sheet-form';
import { SheetsApiService, PersonsApiService, CollectionsApiService } from '../../core/api';
import { TranslationService } from '../../core/translation.service';
import { Person, SheetMusic } from '../../model/datamodels';

function makePerson(id: string, name: string): Person {
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
  let personsApi: { find: ReturnType<typeof vi.fn> };
  let collectionsApi: { find: ReturnType<typeof vi.fn> };
  let sheetsApi: { create: ReturnType<typeof vi.fn>; update: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    personsApi = { find: vi.fn().mockReturnValue(paginatedOf([])) };
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
          { provide: PersonsApiService, useValue: personsApi },
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

  it('patchFormValues maps the composer person', () => {
    const sheet = makeSheet({ composer: makePerson('m1', 'Bach') });
    (component as any).patchFormValues(sheet);
    expect(component.form.controls.composer.value?.id).toBe('m1');
  });

  it('patchFormValues sets composer to null when no composer', () => {
    const sheet = makeSheet({ composer: undefined });
    (component as any).patchFormValues(sheet);
    expect(component.form.controls.composer.value).toBeNull();
  });

  // ── person search ────────────────────────────────────

  it('onPersonSearch asks the server for matching persons', () => {
    personsApi.find.mockReturnValue(paginatedOf([makePerson('m1', 'Brahms')]));

    component.onPersonSearch({ query: ' brah ' } as any);

    expect(personsApi.find).toHaveBeenCalledWith({ name: 'brah', size: 20 });
    expect((component as any).personSuggestions().map((m: Person) => m.name)).toEqual(['Brahms']);
  });

  // ── onPersonCreated ──────────────────────────────────

  it('onPersonCreated sets the composer when target is composer', () => {
    (component as any).personDialogTarget = 'composer';
    const newPerson = makePerson('m5', 'Schubert');

    component.onPersonCreated(newPerson);

    expect(component.form.controls.composer.value?.id).toBe('m5');
    expect((component as any).showPersonDialog).toBe(false);
  });

  it('onPersonCreated sets the arranger when target is arranger', () => {
    (component as any).personDialogTarget = 'arranger';
    const newPerson = makePerson('m6', 'Liszt');

    component.onPersonCreated(newPerson);

    expect(component.form.controls.arranger.value?.id).toBe('m6');
    expect(component.form.controls.composer.value).toBeNull();
    expect((component as any).showPersonDialog).toBe(false);
  });

  it('onPersonCreated closes the person dialog', () => {
    (component as any).showPersonDialog = true;
    component.onPersonCreated(makePerson('m7', 'Handel'));
    expect((component as any).showPersonDialog).toBe(false);
  });

  // ── openPersonDialog ─────────────────────────────────

  it('openPersonDialog sets target and shows dialog', () => {
    component.openPersonDialog('arranger');
    expect((component as any).personDialogTarget).toBe('arranger');
    expect((component as any).showPersonDialog).toBe(true);
  });
});
