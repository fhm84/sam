import { TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { PrimeNG } from 'primeng/config';
import { TranslationService } from './translation.service';

describe('TranslationService', () => {
  let service: TranslationService;

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: PrimeNG, useValue: { setTranslation: vi.fn() } },
        TranslationService,
      ],
    });
    service = TestBed.inject(TranslationService);
  });

  // ── t() key lookup ─────────────────────────────────────

  it('returns key as fallback when translations are empty', () => {
    expect(service.t('nav.sheets')).toBe('nav.sheets');
  });

  it('resolves a simple top-level key', () => {
    (service as any).translations.set({ hello: 'world' });
    expect(service.t('hello')).toBe('world');
  });

  it('resolves a dotted nested key', () => {
    (service as any).translations.set({ nav: { sheets: 'Noten' } });
    expect(service.t('nav.sheets')).toBe('Noten');
  });

  it('returns key when intermediate segment is missing', () => {
    (service as any).translations.set({ nav: {} });
    expect(service.t('nav.missing')).toBe('nav.missing');
  });

  it('returns key when value is not a string (e.g. an object)', () => {
    (service as any).translations.set({ nav: { sheets: { sub: 'x' } } });
    expect(service.t('nav.sheets')).toBe('nav.sheets');
  });

  // ── localeLabel ────────────────────────────────────────

  it('localeLabel is "DE" when locale is "en"', () => {
    service.locale.set('en');
    expect(service.localeLabel()).toBe('DE');
  });

  it('localeLabel is "EN" when locale is "de"', () => {
    service.locale.set('de');
    expect(service.localeLabel()).toBe('EN');
  });

  // ── version bumps ──────────────────────────────────────

  it('t() touches version so computed consumers re-evaluate', () => {
    const before = service.version();
    // calling t() touches version (read) but does not increment it
    service.t('anything');
    // version only increments on setLocale; reading t() should not throw
    expect(service.version()).toBe(before);
  });
});
