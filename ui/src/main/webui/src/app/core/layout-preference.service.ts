import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class LayoutPreferenceService {
  getViewMode(page: string): 'list' | 'cards' {
    const stored = localStorage.getItem(`sam.layout.${page}`);
    return stored === 'list' || stored === 'cards' ? stored : 'cards';
  }

  setViewMode(page: string, mode: 'list' | 'cards'): void {
    localStorage.setItem(`sam.layout.${page}`, mode);
  }
}
