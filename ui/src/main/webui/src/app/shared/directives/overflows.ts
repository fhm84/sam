import { AfterViewInit, Directive, ElementRef, OnDestroy, inject, signal } from '@angular/core';

/**
 * Tells whether the element's content is cut off (e.g. by `line-clamp-2`), and keeps that up to
 * date when the element is resized. Use as `<span appOverflows #clamp="overflows">`, then
 * `clamp.overflowing()`.
 */
@Directive({ selector: '[appOverflows]', exportAs: 'overflows' })
export class Overflows implements AfterViewInit, OnDestroy {
  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
  private observer?: ResizeObserver;

  readonly overflowing = signal(false);

  ngAfterViewInit(): void {
    this.check();
    if (typeof ResizeObserver !== 'undefined') {
      this.observer = new ResizeObserver(() => this.check());
      this.observer.observe(this.element);
    }
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }

  private check(): void {
    // The standard CSS line-clamp drops the hidden lines from layout, so scrollHeight can't tell.
    // Measure an invisible, unclamped copy at the same width instead (+1 for sub-pixel rounding).
    const copy = this.element.cloneNode(true) as HTMLElement;
    copy.removeAttribute('id');
    Object.assign(copy.style, {
      position: 'absolute',
      visibility: 'hidden',
      pointerEvents: 'none',
      width: `${this.element.clientWidth}px`,
      display: 'block',
      overflow: 'visible',
      maxHeight: 'none',
      webkitLineClamp: 'unset',
      lineClamp: 'none',
    });
    this.element.parentElement?.appendChild(copy);
    this.overflowing.set(copy.offsetHeight > this.element.clientHeight + 1);
    copy.remove();
  }
}
