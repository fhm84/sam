import { TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { ConfirmationService, MessageService } from 'primeng/api';

import { EnsembleVoices } from './ensemble-voices';
import { EnsemblesApiService } from '../../core/api';
import { TranslationService } from '../../core/translation.service';
import { EnsembleVoice } from '../../model/datamodels';

function makeVoice(overrides: Partial<EnsembleVoice> = {}): EnsembleVoice {
  return { id: crypto.randomUUID() as any, label: 'Trumpet', weight: 1, required: false, ...overrides };
}

describe('EnsembleVoices', () => {
  let component: EnsembleVoices;
  let ensemblesApi: {
    listVoices: ReturnType<typeof vi.fn>;
    createVoice: ReturnType<typeof vi.fn>;
    updateVoice: ReturnType<typeof vi.fn>;
    deleteVoice: ReturnType<typeof vi.fn>;
  };
  let confirmationService: ConfirmationService;

  beforeEach(async () => {
    ensemblesApi = {
      listVoices: vi.fn().mockReturnValue(of([])),
      createVoice: vi.fn().mockReturnValue(of(makeVoice({ id: 'new-1' as any }))),
      updateVoice: vi.fn().mockReturnValue(of(undefined)),
      deleteVoice: vi.fn().mockReturnValue(of(undefined)),
    };

    await TestBed.overrideComponent(EnsembleVoices, {
      set: {
        imports: [ReactiveFormsModule],
        template: '<div></div>',
        providers: [ConfirmationService],
      },
    })
      .configureTestingModule({
        imports: [EnsembleVoices],
        providers: [
          provideNoopAnimations(),
          MessageService,
          { provide: EnsemblesApiService, useValue: ensemblesApi },
          {
            provide: TranslationService,
            useValue: { t: (k: string) => k, version: { subscribe: () => {} } },
          },
        ],
      })
      .compileComponents();

    const fixture = TestBed.createComponent(EnsembleVoices);
    component = fixture.componentInstance;
    component.ensembleId = 'ens-1';
    fixture.detectChanges();
    confirmationService = (component as any).confirmationService as ConfirmationService;
  });

  // ── openNewVoice ───────────────────────────────────────

  it('openNewVoice resets form and opens dialog', () => {
    (component as any).addForm.controls.label.setValue('Flute');
    (component as any).openNewVoice();
    expect((component as any).addForm.controls.label.value).toBe('');
    expect((component as any).addForm.controls.required.value).toBe(false);
    expect((component as any).addDialogVisible).toBe(true);
  });

  // ── openEditVoice ──────────────────────────────────────

  it('openEditVoice patches form, sets activeTab to 0, opens dialog', () => {
    const voice = makeVoice({ label: 'Clarinet', weight: 2, required: true });
    (component as any).openEditVoice(voice);
    expect((component as any).editForm.controls.weight.value).toBe(2);
    expect((component as any).editForm.controls.required.value).toBe(true);
    expect((component as any).activeTab).toBe(0);
    expect((component as any).editDialogVisible).toBe(true);
  });

  it('openEditVoice makes a copy so original is not mutated', () => {
    const voice = makeVoice({ label: 'Oboe' });
    (component as any).openEditVoice(voice);
    expect((component as any).editingVoice).not.toBe(voice);
    expect((component as any).editingVoice.label).toBe('Oboe');
  });

  // ── openOptions ────────────────────────────────────────

  it('openOptions patches form and sets activeTab to 1', () => {
    const voice = makeVoice({ weight: 3, required: false });
    (component as any).openOptions(voice);
    expect((component as any).editForm.controls.weight.value).toBe(3);
    expect((component as any).activeTab).toBe(1);
    expect((component as any).editDialogVisible).toBe(true);
  });

  // ── onAdd ──────────────────────────────────────────────

  it('onAdd does nothing when form is invalid (empty label)', () => {
    (component as any).addForm.controls.label.setValue('');
    (component as any).onAdd();
    expect(ensemblesApi.createVoice).not.toHaveBeenCalled();
  });

  it('onAdd calls createVoice and transitions to edit dialog on success', () => {
    (component as any).addForm.reset({ label: 'Tuba', weight: null, required: false });
    (component as any).onAdd();
    expect(ensemblesApi.createVoice).toHaveBeenCalledWith('ens-1', expect.objectContaining({ label: 'Tuba' }));
    expect((component as any).addDialogVisible).toBe(false);
    expect((component as any).editDialogVisible).toBe(true);
    expect((component as any).activeTab).toBe(1);
  });

  // ── onUpdate ───────────────────────────────────────────

  it('onUpdate does nothing when editingVoice is null', () => {
    (component as any).editingVoice = null;
    (component as any).onUpdate();
    expect(ensemblesApi.updateVoice).not.toHaveBeenCalled();
  });

  it('onUpdate calls updateVoice with correct payload and closes dialog', () => {
    const voice = makeVoice({ id: 'v1' as any, label: 'Horn' });
    (component as any).editingVoice = voice;
    (component as any).editForm.reset({ weight: 2, required: true });
    ensemblesApi.listVoices.mockClear();

    (component as any).onUpdate();

    expect(ensemblesApi.updateVoice).toHaveBeenCalledWith(
      'ens-1',
      'v1',
      expect.objectContaining({ label: 'Horn', weight: 2, required: true }),
    );
    expect((component as any).editDialogVisible).toBe(false);
    expect(ensemblesApi.listVoices).toHaveBeenCalled();
  });

  // ── confirmDeleteVoice ─────────────────────────────────

  it('confirmDeleteVoice calls deleteVoice on accept', () => {
    const voice = makeVoice({ id: 'v2' as any });
    vi.spyOn(confirmationService, 'confirm').mockImplementation(({ accept }: any) => accept?.());
    ensemblesApi.listVoices.mockClear();

    (component as any).confirmDeleteVoice(voice);

    expect(ensemblesApi.deleteVoice).toHaveBeenCalledWith('ens-1', 'v2');
    expect(ensemblesApi.listVoices).toHaveBeenCalled();
  });

  // ── loadVoices on input change ─────────────────────────

  it('ngOnChanges loads voices for the given ensembleId', () => {
    ensemblesApi.listVoices.mockClear();
    component.ensembleId = 'ens-2';
    component.ngOnChanges();
    expect(ensemblesApi.listVoices).toHaveBeenCalledWith('ens-2');
  });

  it('voices signal is populated from API response', () => {
    const voices = [makeVoice({ label: 'Flute' }), makeVoice({ label: 'Oboe' })];
    ensemblesApi.listVoices.mockReturnValue(of(voices));
    component.ngOnChanges();
    expect((component as any).voices()).toHaveLength(2);
  });
});
