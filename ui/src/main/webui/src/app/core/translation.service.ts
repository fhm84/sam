import { Injectable, inject, signal, computed } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { PrimeNG } from 'primeng/config';
import { firstValueFrom } from 'rxjs';

export type Locale = 'en' | 'de';

const STORAGE_KEY = 'sam-locale';
const SUPPORTED_LOCALES: Locale[] = ['en', 'de'];

@Injectable({ providedIn: 'root' })
export class TranslationService {
  private readonly http = inject(HttpClient);
  private readonly primeng = inject(PrimeNG);

  readonly locale = signal<Locale>(this.detectLocale());
  private readonly translations = signal<Record<string, unknown>>({});

  /** Version counter that bumps on every locale load to invalidate pipes */
  readonly version = signal(0);

  readonly localeLabel = computed(() => (this.locale() === 'en' ? 'DE' : 'EN'));

  /**
   * Look up a dotted key path (e.g. "nav.sheets") in the loaded translations.
   * Falls back to the key itself if not found.
   */
  t(key: string): string {
    // Touch version so computed/pipe consumers re-evaluate
    this.version();
    const parts = key.split('.');
    let current: unknown = this.translations();
    for (const part of parts) {
      if (current && typeof current === 'object' && part in current) {
        current = (current as Record<string, unknown>)[part];
      } else {
        return key;
      }
    }
    return typeof current === 'string' ? current : key;
  }

  /**
   * Switch to a new locale. Loads the JSON, updates PrimeNG, persists preference.
   */
  async setLocale(locale: Locale): Promise<void> {
    const data = await firstValueFrom(this.http.get<Record<string, unknown>>(`/i18n/${locale}.json`));
    this.translations.set(data);
    this.locale.set(locale);
    this.version.update((v) => v + 1);
    localStorage.setItem(STORAGE_KEY, locale);
    document.documentElement.lang = locale;

    if (data['primeng'] && typeof data['primeng'] === 'object') {
      this.primeng.setTranslation(data['primeng'] as Record<string, unknown>);
    }
  }

  /**
   * Toggle between EN and DE.
   */
  async toggleLocale(): Promise<void> {
    const next: Locale = this.locale() === 'en' ? 'de' : 'en';
    await this.setLocale(next);
  }

  /**
   * Load the initial locale. Called via APP_INITIALIZER.
   */
  initialize(): Promise<void> {
    return this.setLocale(this.locale());
  }

  private detectLocale(): Locale {
    const stored = localStorage.getItem(STORAGE_KEY) as Locale | null;
    if (stored && SUPPORTED_LOCALES.includes(stored)) return stored;
    const browserLang = navigator.language.split('-')[0] as Locale;
    return SUPPORTED_LOCALES.includes(browserLang) ? browserLang : 'en';
  }
}
