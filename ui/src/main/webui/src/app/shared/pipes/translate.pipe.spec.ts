import { TestBed } from '@angular/core/testing';
import { TranslatePipe } from './translate.pipe';
import { TranslationService } from '../../core/translation.service';

describe('TranslatePipe', () => {
  let pipe: TranslatePipe;
  let translationService: { t: ReturnType<typeof vi.fn>; version: { subscribe: () => void } };

  beforeEach(() => {
    translationService = {
      t: vi.fn((key: string) => key),
      version: { subscribe: () => {} },
    };

    TestBed.configureTestingModule({
      providers: [
        TranslatePipe,
        { provide: TranslationService, useValue: translationService },
      ],
    });

    pipe = TestBed.inject(TranslatePipe);
  });

  it('calls TranslationService.t with the given key', () => {
    pipe.transform('nav.sheets');
    expect(translationService.t).toHaveBeenCalledWith('nav.sheets');
  });

  it('returns the translated string from the service', () => {
    translationService.t.mockReturnValue('Noten');
    expect(pipe.transform('nav.sheets')).toBe('Noten');
  });

  it('returns the key when service returns the key (fallback)', () => {
    translationService.t.mockReturnValue('nav.missing');
    expect(pipe.transform('nav.missing')).toBe('nav.missing');
  });
});
