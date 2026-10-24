import { Component, computed, inject, OnInit, ChangeDetectionStrategy } from '@angular/core';
import { MenuItem } from '@openng/optimus-ui/api';
import { AuthService } from '../../core/auth/auth.service';
import { Roles, WRITE_ROLES } from '../../core/auth/roles';
import { TranslationService } from '../../core/translation.service';
import { AppMenuitem } from './app-menuitem';

@Component({
  selector: 'app-menu',
  imports: [AppMenuitem],
  changeDetection: ChangeDetectionStrategy.Eager,
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
  private readonly auth = inject(AuthService);

  protected readonly model = computed<MenuItem[]>(() => {
    // Depend on translation version so model rebuilds on locale change
    void this.i18n.version();
    return withRoles([
      {
        label: this.i18n.t('nav.main'),
        items: [
          { label: this.i18n.t('nav.sheets'), icon: 'pi pi-file', routerLink: ['/sheets'] },
          {
            label: this.i18n.t('nav.collections'),
            icon: 'pi pi-folder',
            routerLink: ['/collections'],
          },
          {
            label: this.i18n.t('nav.uploads'),
            icon: 'pi pi-upload',
            routerLink: ['/uploads'],
            roles: WRITE_ROLES,
          },
          { label: this.i18n.t('nav.musicians'), icon: 'pi pi-user', routerLink: ['/musicians'] },
          { label: this.i18n.t('nav.shares'), icon: 'pi pi-share-alt', routerLink: ['/shares'] },
          {
            label: this.i18n.t('nav.myParts'),
            icon: 'pi pi-book',
            routerLink: ['/my-parts'],
          },
        ],
      },
      {
        label: this.i18n.t('nav.admin.title'),
        items: [
          {
            label: this.i18n.t('nav.admin.ensembles'),
            icon: 'pi pi-users',
            routerLink: ['/admin/ensembles'],
            roles: WRITE_ROLES,
          },
          {
            label: this.i18n.t('nav.admin.instruments'),
            icon: 'pi pi-microphone',
            routerLink: ['/admin/instruments'],
            roles: WRITE_ROLES,
          },
          {
            label: this.i18n.t('nav.admin.configuration'),
            icon: 'pi pi-wrench',
            routerLink: ['/admin/configuration'],
            roles: [Roles.ADMIN],
          },
          {
            label: this.i18n.t('nav.admin.eventLogs'),
            icon: 'pi pi-list',
            routerLink: ['/admin/event-logs'],
            roles: [Roles.ADMIN],
          },
        ],
      },
    ], (roles) => this.auth.hasAnyRole(roles));
  });
}

/** A menu item that only users holding one of `roles` see; without `roles` everyone sees it. */
export type RoleMenuItem = MenuItem & { roles?: readonly string[]; items?: RoleMenuItem[] };

/**
 * Hides items whose `roles` the user doesn't hold, and groups left without a visible item. The
 * route guard enforces the same lists; the server enforces the roles themselves.
 */
export function withRoles(
  items: RoleMenuItem[],
  hasAnyRole: (roles: readonly string[]) => boolean,
): MenuItem[] {
  return items.map(({ roles, items: children, ...item }) => {
    const shown = roles === undefined || hasAnyRole(roles);
    if (!children) {
      return { ...item, visible: shown };
    }
    const filtered = withRoles(children, hasAnyRole);
    return { ...item, items: filtered, visible: shown && filtered.some((c) => c.visible !== false) };
  });
}
