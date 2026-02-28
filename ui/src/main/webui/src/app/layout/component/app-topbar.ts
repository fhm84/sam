import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { Breadcrumb } from 'primeng/breadcrumb';
import { Menu } from 'primeng/menu';
import { Tooltip } from 'primeng/tooltip';
import { MenuItem } from 'primeng/api';
import { LayoutService, MenuMode } from '../service/layout.service';
import { ThemeService } from '../../core/theme.service';
import { TranslationService } from '../../core/translation.service';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';

@Component({
  selector: 'app-topbar',
  imports: [RouterLink, Breadcrumb, Menu, Tooltip, TranslatePipe],
  templateUrl: './app-topbar.html',
  styleUrl: './app-topbar.scss',
})
export class AppTopbar {
  protected readonly layoutService = inject(LayoutService);
  protected readonly theme = inject(ThemeService);
  protected readonly i18n = inject(TranslationService);
  private readonly router = inject(Router);

  private readonly routeMap: Record<string, string[]> = {
    '/sheets': ['nav.sheets'],
    '/collections': ['nav.collections'],
    '/uploads': ['nav.uploads'],
    '/musicians': ['nav.musicians'],
    '/admin/ensembles': ['nav.admin.title', 'nav.admin.ensembles'],
    '/admin/instruments': ['nav.admin.title', 'nav.admin.instruments'],
    '/admin/configuration': ['nav.admin.title', 'nav.admin.configuration'],
    '/user/preferences': ['nav.userPreferences'],
  };

  protected readonly breadcrumbItems = computed<MenuItem[]>(() => {
    void this.i18n.version();
    const path = this.layoutService.layoutState().activePath ?? this.router.url;
    const match = Object.keys(this.routeMap).find((k) => path.startsWith(k));
    if (!match) return [];
    return this.routeMap[match].map((key) => ({ label: this.i18n.t(key) }));
  });

  protected readonly breadcrumbHome: MenuItem = { icon: 'pi pi-home', routerLink: '/' };

  protected readonly darkIcon = computed(() => (this.theme.dark() ? 'pi pi-sun' : 'pi pi-moon'));

  protected readonly menuModeItems = computed<MenuItem[]>(() => {
    void this.i18n.version();
    const modes: MenuMode[] = ['static', 'overlay', 'slim'];
    return modes.map((mode) => ({
      label: this.i18n.t(`layout.${mode}`),
      icon:
        mode === 'static'
          ? 'pi pi-sidebar'
          : mode === 'overlay'
            ? 'pi pi-align-justify'
            : 'pi pi-bars',
      command: () => this.layoutService.setMenuMode(mode),
    }));
  });

  protected readonly userMenuItems = computed<MenuItem[]>(() => {
    void this.i18n.version();
    return [
      {
        label: this.i18n.t('nav.userPreferences'),
        icon: 'pi pi-sliders-h',
        command: () => this.router.navigate(['/user/preferences']),
      },
      { separator: true },
      { label: this.i18n.t('nav.logout'), icon: 'pi pi-sign-out', command: () => {} },
    ];
  });
}
