import { Component, effect, ElementRef, inject, OnDestroy, OnInit } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter, Subject, takeUntil } from 'rxjs';
import { LayoutService } from '../service/layout.service';
import { AppMenu } from './app-menu';

@Component({
  selector: 'app-sidebar',
  imports: [AppMenu],
  template: `
    <div class="layout-sidebar">
      <app-menu />
    </div>
  `,
})
export class AppSidebar implements OnInit, OnDestroy {
  private readonly layoutService = inject(LayoutService);
  private readonly router = inject(Router);
  private readonly el = inject(ElementRef);

  private outsideClickListener: ((e: MouseEvent) => void) | null = null;
  private readonly destroy$ = new Subject<void>();

  constructor() {
    effect(() => {
      const state = this.layoutService.layoutState();
      const shouldBind =
        (this.layoutService.isDesktop() && state.overlayMenuActive) ||
        (!this.layoutService.isDesktop() && state.mobileMenuActive);
      if (shouldBind) {
        this.bindOutsideClickListener();
      } else {
        this.unbindOutsideClickListener();
      }
    });
  }

  ngOnInit(): void {
    this.router.events
      .pipe(
        filter((e) => e instanceof NavigationEnd),
        takeUntil(this.destroy$),
      )
      .subscribe((e) => {
        this.onRouteChange((e as NavigationEnd).urlAfterRedirects);
      });
    this.onRouteChange(this.router.url);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.unbindOutsideClickListener();
  }

  private onRouteChange(path: string): void {
    this.layoutService.layoutState.update((s) => ({
      ...s,
      activePath: path,
      overlayMenuActive: false,
      mobileMenuActive: false,
      menuHoverActive: false,
    }));
  }

  private bindOutsideClickListener(): void {
    if (!this.outsideClickListener) {
      this.outsideClickListener = (event: MouseEvent) => {
        if (this.isOutsideClicked(event)) {
          this.layoutService.hideOverlayMenu();
        }
      };
      document.addEventListener('click', this.outsideClickListener);
    }
  }

  private unbindOutsideClickListener(): void {
    if (this.outsideClickListener) {
      document.removeEventListener('click', this.outsideClickListener);
      this.outsideClickListener = null;
    }
  }

  private isOutsideClicked(event: MouseEvent): boolean {
    const menuBtn = document.querySelector('.layout-menu-button');
    const sidebar = this.el.nativeElement as HTMLElement;
    return !(
      sidebar.isSameNode(event.target as Node) ||
      sidebar.contains(event.target as Node) ||
      menuBtn?.isSameNode(event.target as Node) ||
      menuBtn?.contains(event.target as Node)
    );
  }
}
