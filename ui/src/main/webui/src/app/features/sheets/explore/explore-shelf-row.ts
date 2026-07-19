import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { TranslationService } from '../../../core/translation.service';

/**
 * Horizontal shelf with Netflix-style paging: chevron buttons scroll one
 * viewport width at a time and hide at the ends; the native scrollbar is
 * hidden (touch/trackpad scrolling still works).
 */
@Component({
  selector: 'app-explore-shelf-row',
  templateUrl: './explore-shelf-row.html',
  styleUrl: './explore-shelf-row.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExploreShelfRow implements AfterViewInit {
  @ViewChild('scroller') private scroller!: ElementRef<HTMLElement>;

  protected readonly t = inject(TranslationService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly canScrollPrev = signal(false);
  protected readonly canScrollNext = signal(false);

  ngAfterViewInit(): void {
    const el = this.scroller.nativeElement;
    const observer = new ResizeObserver(() => this.updateArrows());
    observer.observe(el);
    this.destroyRef.onDestroy(() => observer.disconnect());
    this.updateArrows();
  }

  protected page(direction: -1 | 1): void {
    const el = this.scroller.nativeElement;
    // Page by slightly less than the visible width so the last partly
    // visible card of the previous page stays as an anchor.
    el.scrollBy({ left: direction * el.clientWidth * 0.9, behavior: 'smooth' });
  }

  protected updateArrows(): void {
    const el = this.scroller.nativeElement;
    this.canScrollPrev.set(el.scrollLeft > 1);
    this.canScrollNext.set(el.scrollLeft + el.clientWidth < el.scrollWidth - 1);
  }
}
