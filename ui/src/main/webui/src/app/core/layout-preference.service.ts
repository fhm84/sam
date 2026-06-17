import { Injectable } from '@angular/core';

type DefaultMode = 'list' | 'cards';

@Injectable({ providedIn: 'root' })
export class LayoutPreferenceService {
  getViewMode<T extends string = DefaultMode>(
    page: string,
    validModes: readonly T[] = ['list', 'cards'] as unknown as readonly T[],
    defaultMode: T = 'cards' as T,
  ): T {
    const stored = localStorage.getItem(`sam.layout.${page}`);
    return stored != null && (validModes as readonly string[]).includes(stored) ? (stored as T) : defaultMode;
  }

  setViewMode<T extends string>(page: string, mode: T): void {
    localStorage.setItem(`sam.layout.${page}`, mode);
  }
}
