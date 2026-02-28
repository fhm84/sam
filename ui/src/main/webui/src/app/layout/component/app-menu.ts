import { Component, computed, inject, OnInit } from '@angular/core';
import { MenuItem } from 'primeng/api';
import { TranslationService } from '../../core/translation.service';
import { AppMenuitem } from './app-menuitem';

@Component({
  selector: 'app-menu',
  imports: [AppMenuitem],
  template: `
    <ul class="layout-menu">
      @for (item of model(); track item.label) {
        @if (!item.separator) {
          <li app-menuitem [item]="item" [root]="true"></li>
        } @else {
          <li class="menu-separator"></li>
        }
      }
    </ul>
  `,
})
export class AppMenu {
  private readonly i18n = inject(TranslationService);

  protected readonly model = computed<MenuItem[]>(() => {
    // Depend on translation version so model rebuilds on locale change
    void this.i18n.version();
    return [
      {
        label: this.i18n.t('nav.main'),
        items: [
          { label: this.i18n.t('nav.sheets'), icon: 'pi pi-file', routerLink: ['/sheets'] },
          {
            label: this.i18n.t('nav.collections'),
            icon: 'pi pi-folder',
            routerLink: ['/collections'],
          },
          { label: this.i18n.t('nav.uploads'), icon: 'pi pi-upload', routerLink: ['/uploads'] },
          { label: this.i18n.t('nav.musicians'), icon: 'pi pi-user', routerLink: ['/musicians'] },
        ],
      },
      {
        label: this.i18n.t('nav.admin.title'),
        items: [
          {
            label: this.i18n.t('nav.admin.ensembles'),
            icon: 'pi pi-users',
            routerLink: ['/admin/ensembles'],
          },
          {
            label: this.i18n.t('nav.admin.instruments'),
            icon: 'pi pi-microphone',
            routerLink: ['/admin/instruments'],
          },
          {
            label: this.i18n.t('nav.admin.configuration'),
            icon: 'pi pi-wrench',
            routerLink: ['/admin/configuration'],
          },
        ],
      },
    ];
  });
}
