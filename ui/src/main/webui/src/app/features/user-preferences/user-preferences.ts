import { ChangeDetectionStrategy, Component, inject, signal, WritableSignal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SelectButton } from 'primeng/selectbutton';
import { Tooltip } from 'primeng/tooltip';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { LayoutPreferenceService } from '../../core/layout-preference.service';

@Component({
  selector: 'app-user-preferences',
  imports: [FormsModule, SelectButton, Tooltip, TranslatePipe],
  templateUrl: './user-preferences.html',
  styleUrl: './user-preferences.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UserPreferences {
  private readonly layoutPrefs = inject(LayoutPreferenceService);

  protected readonly layoutOptions = [
    { icon: 'pi pi-th-large', value: 'cards', labelKey: 'userPreferences.cards' },
    { icon: 'pi pi-list', value: 'list', labelKey: 'userPreferences.list' },
  ];

  protected readonly sheetsLayout = signal(this.layoutPrefs.getViewMode('sheets'));
  protected readonly collectionsLayout = signal(this.layoutPrefs.getViewMode('collections'));
  protected readonly ensemblesLayout = signal(this.layoutPrefs.getViewMode('ensembles'));

  protected save(page: string, layout: WritableSignal<'list' | 'cards'>): void {
    this.layoutPrefs.setViewMode(page, layout());
  }
}
