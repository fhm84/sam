import { Component, inject, signal, computed } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { Drawer } from 'primeng/drawer';
import { Tooltip } from 'primeng/tooltip';
import { ThemeService } from './core/theme.service';
import { TranslationService } from './core/translation.service';
import { TranslatePipe } from './shared/pipes/translate.pipe';

interface NavItem {
  labelKey: string;
  icon: string;
  route?: string;
  children?: { labelKey: string; icon: string; route: string }[];
}

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, Drawer, Tooltip, TranslatePipe],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  protected readonly theme = inject(ThemeService);
  protected readonly i18n = inject(TranslationService);
  protected sidebarCollapsed = signal(false);
  protected drawerVisible = signal(false);

  protected readonly darkIcon = computed(() => (this.theme.dark() ? 'pi pi-sun' : 'pi pi-moon'));

  protected readonly navigation: NavItem[] = [
    { labelKey: 'nav.sheets', icon: 'pi pi-file', route: '/sheets' },
    { labelKey: 'nav.collections', icon: 'pi pi-folder', route: '/collections' },
    { labelKey: 'nav.uploads', icon: 'pi pi-upload', route: '/uploads' },
    { labelKey: 'nav.musicians', icon: 'pi pi-user', route: '/musicians' },
  ];

  protected readonly adminNavigation: NavItem[] = [
    {
      labelKey: 'nav.admin.title',
      icon: 'pi pi-cog',
      children: [
        { labelKey: 'nav.admin.ensembles', icon: 'pi pi-users', route: '/admin/ensembles' },
        { labelKey: 'nav.admin.instruments', icon: 'pi pi-sliders-h', route: '/admin/instruments' },
        { labelKey: 'nav.admin.configuration', icon: 'pi pi-wrench', route: '/admin/configuration' },
      ],
    },
  ];

  toggleSidebar() {
    this.sidebarCollapsed.update((v) => !v);
  }

  openDrawer() {
    this.drawerVisible.set(true);
  }
}
