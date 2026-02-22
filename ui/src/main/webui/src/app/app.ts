import { Component, computed, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Toast } from 'primeng/toast';
import { ThemeService } from './core/theme.service';
import { TranslationService } from './core/translation.service';
import { TranslatePipe } from './shared/pipes/translate.pipe';
import { Sidebar } from './layout/sidebar';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Toast, TranslatePipe, Sidebar],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly theme = inject(ThemeService);
  protected readonly i18n = inject(TranslationService);
  protected readonly sidebarCollapsed = signal(false);
  protected readonly drawerVisible = signal(false);
  protected readonly darkIcon = computed(() => (this.theme.dark() ? 'pi pi-sun' : 'pi pi-moon'));

  protected toggleSidebar(): void {
    this.sidebarCollapsed.update((v) => !v);
  }

  protected openDrawer(): void {
    this.drawerVisible.set(true);
  }
}
