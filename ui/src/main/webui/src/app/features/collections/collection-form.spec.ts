import { TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { CollectionForm } from './collection-form';
import { CollectionsApiService } from '../../core/api';
import { TranslationService } from '../../core/translation.service';
import { SheetCollection } from '../../model/datamodels';

function makeCollection(overrides: Partial<SheetCollection> = {}): SheetCollection {
  return { id: '1' as any, name: 'My Folder', type: 'FOLDER', description: 'desc', ...overrides };
}

describe('CollectionForm', () => {
  let component: CollectionForm;
  let collectionsApi: { create: ReturnType<typeof vi.fn>; update: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    collectionsApi = {
      create: vi.fn().mockReturnValue(of({ id: '99' })),
      update: vi.fn().mockReturnValue(of(undefined)),
    };

    await TestBed.overrideComponent(CollectionForm, {
      set: {
        imports: [ReactiveFormsModule],
        template: '<div></div>',
        providers: [],
      },
    })
      .configureTestingModule({
        imports: [CollectionForm],
        providers: [
          provideNoopAnimations(),
          { provide: CollectionsApiService, useValue: collectionsApi },
          {
            provide: TranslationService,
            useValue: { t: (k: string) => k, version: { subscribe: () => {} } },
          },
        ],
      })
      .compileComponents();

    const fixture = TestBed.createComponent(CollectionForm);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ── isEdit ─────────────────────────────────────────────

  it('isEdit is false when no collection is passed', () => {
    component.collection = null;
    expect((component as any).isEdit).toBe(false);
  });

  it('isEdit is true when a collection is passed', () => {
    component.collection = makeCollection();
    expect((component as any).isEdit).toBe(true);
  });

  // ── isSetlist ──────────────────────────────────────────

  it('isSetlist is false when type is FOLDER', () => {
    component.form.controls.type.setValue('FOLDER');
    expect((component as any).isSetlist).toBe(false);
  });

  it('isSetlist is true when type is SETLIST', () => {
    component.form.controls.type.setValue('SETLIST');
    expect((component as any).isSetlist).toBe(true);
  });

  it('isSetlist is false when type is null', () => {
    component.form.controls.type.setValue(null);
    expect((component as any).isSetlist).toBe(false);
  });

  // ── patchFormValues ────────────────────────────────────

  it('patchFormValues populates form fields from collection', () => {
    const col = makeCollection({
      name: 'Spring Concert',
      description: 'Annual event',
      type: 'SETLIST',
      date: '2024-05-01' as any,
    });
    (component as any).patchFormValues(col);
    expect(component.form.controls.name.value).toBe('Spring Concert');
    expect(component.form.controls.description.value).toBe('Annual event');
    expect(component.form.controls.type.value).toBe('SETLIST');
    expect(component.form.controls.date.value).toBeInstanceOf(Date);
  });

  it('patchFormValues sets date to null when collection has no date', () => {
    const col = makeCollection({ date: undefined });
    (component as any).patchFormValues(col);
    expect(component.form.controls.date.value).toBeNull();
  });

  it('patchFormValues sets description to empty string when absent', () => {
    const col = makeCollection({ description: undefined });
    (component as any).patchFormValues(col);
    expect(component.form.controls.description.value).toBe('');
  });

  // ── type → date reactive clearing ─────────────────────

  it('clears date when type changes away from SETLIST', async () => {
    // set up as a SETLIST with a date
    component.form.controls.type.setValue('SETLIST');
    component.form.controls.date.setValue(new Date('2024-06-01'));

    // switch to FOLDER — subscription in ngOnInit should clear the date
    component.form.controls.type.setValue('FOLDER');

    expect(component.form.controls.date.value).toBeNull();
  });

  it('does not clear date when type is set to SETLIST', () => {
    component.form.controls.type.setValue('SETLIST');
    component.form.controls.date.setValue(new Date('2024-06-01'));
    component.form.controls.type.setValue('SETLIST');
    expect(component.form.controls.date.value).not.toBeNull();
  });
});
