import { Injectable, signal, computed, effect } from '@angular/core';

export type MenuMode = 'static' | 'overlay' | 'slim';

export interface LayoutConfig {
  menuMode: MenuMode;
}

interface LayoutState {
  staticMenuDesktopInactive: boolean;
  overlayMenuActive: boolean;
  mobileMenuActive: boolean;
  menuHoverActive: boolean;
  activePath: string | null;
}

const STORAGE_KEY = 'sam-menu-mode';

@Injectable({ providedIn: 'root' })
export class LayoutService {
  layoutConfig = signal<LayoutConfig>({
    menuMode: (localStorage.getItem(STORAGE_KEY) as MenuMode | null) ?? 'static',
  });

  layoutState = signal<LayoutState>({
    staticMenuDesktopInactive: false,
    overlayMenuActive: false,
    mobileMenuActive: false,
    menuHoverActive: false,
    activePath: null,
  });

  isSidebarActive = computed(
    () => this.layoutState().overlayMenuActive || this.layoutState().mobileMenuActive,
  );

  isOverlay = computed(() => this.layoutConfig().menuMode === 'overlay');
  isSlim = computed(() => this.layoutConfig().menuMode === 'slim');
  isStatic = computed(() => this.layoutConfig().menuMode === 'static');

  constructor() {
    effect(() => {
      localStorage.setItem(STORAGE_KEY, this.layoutConfig().menuMode);
    });
  }

  onMenuToggle(): void {
    if (this.isOverlay()) {
      this.layoutState.update((s) => ({ ...s, overlayMenuActive: !s.overlayMenuActive }));
    } else if (this.isStatic()) {
      if (this.isDesktop()) {
        this.layoutState.update((s) => ({
          ...s,
          staticMenuDesktopInactive: !s.staticMenuDesktopInactive,
        }));
      } else {
        this.layoutState.update((s) => ({ ...s, mobileMenuActive: !s.mobileMenuActive }));
      }
    } else {
      // slim: toggle mobile only
      if (!this.isDesktop()) {
        this.layoutState.update((s) => ({ ...s, mobileMenuActive: !s.mobileMenuActive }));
      }
    }
  }

  hideOverlayMenu(): void {
    this.layoutState.update((s) => ({
      ...s,
      overlayMenuActive: false,
      mobileMenuActive: false,
    }));
  }

  setMenuMode(mode: MenuMode): void {
    this.layoutConfig.update((c) => ({ ...c, menuMode: mode }));
    // Reset sidebar state on mode change
    this.layoutState.update((s) => ({
      ...s,
      staticMenuDesktopInactive: false,
      overlayMenuActive: false,
      mobileMenuActive: false,
    }));
  }

  isDesktop(): boolean {
    return window.innerWidth > 991;
  }

  isMobile(): boolean {
    return !this.isDesktop();
  }
}
