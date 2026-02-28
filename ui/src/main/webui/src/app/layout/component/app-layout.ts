import { Component, computed, effect, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NgClass } from '@angular/common';
import { Toast } from 'primeng/toast';
import { LayoutService } from '../service/layout.service';
import { AppTopbar } from './app-topbar';
import { AppSidebar } from './app-sidebar';

@Component({
  selector: 'app-layout',
  imports: [NgClass, RouterOutlet, Toast, AppTopbar, AppSidebar],
  templateUrl: './app-layout.html',
  styleUrl: './app-layout.scss',
})
export class AppLayout {
  protected readonly layoutService = inject(LayoutService);

  protected readonly containerClass = computed(() => {
    const config = this.layoutService.layoutConfig();
    const state = this.layoutService.layoutState();
    return {
      'layout-static': config.menuMode === 'static',
      'layout-overlay': config.menuMode === 'overlay',
      'layout-slim': config.menuMode === 'slim',
      'layout-static-inactive': state.staticMenuDesktopInactive && config.menuMode === 'static',
      'layout-overlay-active': state.overlayMenuActive,
      'layout-mobile-active': state.mobileMenuActive,
    };
  });

  constructor() {
    effect(() => {
      if (this.layoutService.layoutState().mobileMenuActive) {
        document.body.classList.add('blocked-scroll');
      } else {
        document.body.classList.remove('blocked-scroll');
      }
    });
  }

  protected hideMenu(): void {
    this.layoutService.hideOverlayMenu();
  }
}
