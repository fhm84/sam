import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive } from '@angular/router';
import { NgClass } from '@angular/common';
import { filter } from 'rxjs/operators';
import { MenuItem } from 'primeng/api';
import { Tooltip } from 'primeng/tooltip';
import { LayoutService } from '../service/layout.service';

@Component({
  selector: '[app-menuitem]',
  imports: [NgClass, RouterLink, RouterLinkActive, Tooltip],
  templateUrl: './app-menuitem.html',
  host: {
    '[class.active-menuitem]': 'isActive()',
    '[class.layout-root-menuitem]': 'root()',
  },
})
export class AppMenuitem implements OnInit {
  protected readonly layoutService = inject(LayoutService);
  private readonly router = inject(Router);

  readonly item = input<MenuItem>({});
  readonly root = input<boolean>(false);
  readonly parentPath = input<string | null>(null);

  protected readonly isVisible = computed(() => this.item()?.['visible'] !== false);
  protected readonly hasChildren = computed(
    () => !!(this.item()?.items && this.item()!.items!.length > 0),
  );
  protected readonly hasRouterLink = computed(() => !!this.item()?.routerLink);

  protected readonly fullPath = computed(() => {
    const itemPath = this.item()?.['path'] as string | undefined;
    if (!itemPath) return this.parentPath();
    const parent = this.parentPath();
    return parent && !itemPath.startsWith(parent) ? parent + itemPath : itemPath;
  });

  protected readonly isActive = computed(() => {
    const activePath = this.layoutService.layoutState().activePath ?? '';
    const fp = this.fullPath();
    if (fp) return activePath.startsWith(fp);
    // For leaf items check routerLink directly
    const link = this.item()?.routerLink;
    if (Array.isArray(link) && link[0]) return activePath.startsWith(link[0] as string);
    return false;
  });

  protected readonly isCollapsed = computed(() => {
    const mode = this.layoutService.layoutConfig().menuMode;
    const state = this.layoutService.layoutState();
    return mode === 'slim' || (mode === 'static' && state.staticMenuDesktopInactive);
  });

  protected readonly submenuOpen = signal(false);

  ngOnInit(): void {
    this.router.events
      .pipe(filter((e) => e instanceof NavigationEnd))
      .subscribe(() => this.updateActiveState());
    this.updateActiveState();
  }

  private updateActiveState(): void {
    const link = this.item()?.routerLink;
    if (!Array.isArray(link) || !link[0]) return;
    const isActive = this.router.isActive(link[0] as string, {
      paths: 'subset',
      queryParams: 'ignored',
      matrixParams: 'ignored',
      fragment: 'ignored',
    });
    if (isActive) {
      const parent = this.parentPath();
      if (parent) {
        this.layoutService.layoutState.update((s) => ({ ...s, activePath: parent }));
      }
      this.submenuOpen.set(true);
    }
  }

  protected itemClick(event: Event): void {
    const item = this.item();
    if (item.disabled) {
      event.preventDefault();
      return;
    }
    if (item.command) {
      item.command({ originalEvent: event, item });
    }
    if (this.hasChildren()) {
      this.submenuOpen.update((v) => !v);
      if (!this.submenuOpen()) {
        this.layoutService.layoutState.update((s) => ({ ...s, activePath: this.parentPath() }));
      } else {
        this.layoutService.layoutState.update((s) => ({
          ...s,
          activePath: this.fullPath(),
          menuHoverActive: true,
        }));
      }
    } else {
      this.layoutService.layoutState.update((s) => ({
        ...s,
        overlayMenuActive: false,
        mobileMenuActive: false,
        menuHoverActive: false,
      }));
    }
  }
}
