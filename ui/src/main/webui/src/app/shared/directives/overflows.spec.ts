import { Component, viewChild } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Overflows } from './overflows';

@Component({
  imports: [Overflows],
  template: `<span appOverflows #clamp="overflows">text</span>`,
})
class Host {
  readonly clamp = viewChild.required<Overflows>('clamp');
}

describe('Overflows', () => {
  function render(naturalHeight: number, clientHeight: number): Overflows {
    // Layout sizes are 0 in jsdom: the unclamped copy reports naturalHeight, the element clientHeight
    Object.defineProperty(HTMLElement.prototype, 'offsetHeight', { configurable: true, value: naturalHeight });
    Object.defineProperty(HTMLElement.prototype, 'clientHeight', { configurable: true, value: clientHeight });
    const fixture = TestBed.createComponent(Host);
    fixture.detectChanges();
    return fixture.componentInstance.clamp();
  }

  afterEach(() => {
    delete (HTMLElement.prototype as Partial<Record<'offsetHeight' | 'clientHeight', number>>).offsetHeight;
    delete (HTMLElement.prototype as Partial<Record<'offsetHeight' | 'clientHeight', number>>).clientHeight;
  });

  it('reports content that is cut off', () => {
    expect(render(60, 38).overflowing()).toBe(true);
  });

  it('reports content that fits', () => {
    expect(render(38, 38).overflowing()).toBe(false);
  });
});
