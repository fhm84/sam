import { Component, computed, inject, input, model } from '@angular/core';
import { RouterLink, RouterLinkActive, Router } from '@angular/router';
import { Drawer } from 'primeng/drawer';
import { Menu } from 'primeng/menu';
import { Tooltip } from 'primeng/tooltip';
import { MenuItem } from 'primeng/api';
import { TranslationService } from '../core/translation.service';
import { TranslatePipe } from '../shared/pipes/translate.pipe';

interface NavItem {
  labelKey: string;
  icon: string;
  route?: string;
  children?: { labelKey: string; icon: string; route: string }[];
}

@Component({
  selector: 'app-sidebar',
  imports: [RouterLink, RouterLinkActive, Drawer, Menu, Tooltip, TranslatePipe],
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss',
  host: {
    '[class.collapsed]': 'collapsed()',
  },
})
export class Sidebar {
  private readonly router = inject(Router);
  protected readonly i18n = inject(TranslationService);

  readonly collapsed = input<boolean>(false);
  readonly drawerVisible = model<boolean>(false);

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

  protected readonly userMenuItems = computed<MenuItem[]>(() => [
    {
      label: this.i18n.t('nav.userPreferences'),
      icon: 'pi pi-sliders-h',
      command: () => this.router.navigate(['/user/preferences']),
    },
    { separator: true },
    {
      label: this.i18n.t('nav.logout'),
      icon: 'pi pi-sign-out',
      command: () => {},
    },
  ]);

  protected closeDrawer(): void {
    this.drawerVisible.set(false);
  }
}
