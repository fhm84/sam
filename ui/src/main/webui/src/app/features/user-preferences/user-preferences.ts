import { ChangeDetectionStrategy, Component, inject, signal, WritableSignal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SelectButton } from 'primeng/selectbutton';
import { Tooltip } from 'primeng/tooltip';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { LayoutPreferenceService } from '../../core/layout-preference.service';
import { ThemeService } from '../../core/theme.service';
import { Locale, TranslationService } from '../../core/translation.service';
import { ColorSchemeKey, ColorSchemeService } from '../../core/color-scheme.service';

@Component({
  selector: 'app-user-preferences',
  imports: [FormsModule, SelectButton, Tooltip, TranslatePipe],
  templateUrl: './user-preferences.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserPreferences {
  private readonly layoutPrefs = inject(LayoutPreferenceService);
  protected readonly theme = inject(ThemeService);
  protected readonly i18n = inject(TranslationService);
  protected readonly colorScheme = inject(ColorSchemeService);

  protected readonly layoutOptions = [
    { icon: 'pi pi-th-large', value: 'cards', labelKey: 'userPreferences.cards' },
    { icon: 'pi pi-list', value: 'list', labelKey: 'userPreferences.list' },
  ];

  protected readonly themeOptions = [
    { icon: 'pi pi-sun', value: 'light', labelKey: 'userPreferences.light' },
    { icon: 'pi pi-moon', value: 'dark', labelKey: 'userPreferences.dark' },
  ];

  protected readonly languageOptions = [
    { label: 'EN', value: 'en', labelKey: 'userPreferences.languageEn' },
    { label: 'DE', value: 'de', labelKey: 'userPreferences.languageDe' },
  ];

  protected readonly sheetsLayout = signal(this.layoutPrefs.getViewMode('sheets'));
  protected readonly collectionsLayout = signal(this.layoutPrefs.getViewMode('collections'));
  protected readonly ensemblesLayout = signal(this.layoutPrefs.getViewMode('ensembles'));

  protected save(page: string, layout: WritableSignal<'list' | 'cards'>): void {
    this.layoutPrefs.setViewMode(page, layout());
  }

  protected setTheme(value: 'light' | 'dark'): void {
    if (this.theme.dark() !== (value === 'dark')) {
      this.theme.toggle();
    }
  }

  protected setLanguage(value: Locale): void {
    this.i18n.setLocale(value);
  }

  protected applyColorScheme(key: ColorSchemeKey): void {
    this.colorScheme.apply(key);
  }
}
