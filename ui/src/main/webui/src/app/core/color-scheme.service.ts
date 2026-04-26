import { effect, inject, Injectable, signal } from '@angular/core';
import { definePreset, Theme } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';
import { ThemeService } from './theme.service';

export type ColorSchemeKey = 'indigo' | 'blue' | 'emerald' | 'amber' | 'rose' | 'slate';

export interface SchemeConfig {
  label: string;
  swatch: string;
}

export const SCHEME_CONFIGS: Record<ColorSchemeKey, SchemeConfig> = {
  indigo:  { label: 'Indigo',  swatch: '#6366f1' },
  blue:    { label: 'Blue',    swatch: '#3b82f6' },
  emerald: { label: 'Emerald', swatch: '#10b981' },
  amber:   { label: 'Amber',   swatch: '#f59e0b' },
  rose:    { label: 'Rose',    swatch: '#f43f5e' },
  slate:   { label: 'Slate',   swatch: '#64748b' },
};

export const COLOR_SCHEME_KEYS: ColorSchemeKey[] = ['indigo', 'blue', 'emerald', 'amber', 'rose', 'slate'];

const CSS_VARS: Record<ColorSchemeKey, { accentLight: string; accentDark: string; activeBgLight: string; activeBgDark: string }> = {
  indigo:  { accentLight: '#3730a3', accentDark: '#a5b4fc', activeBgLight: 'rgba(67,56,202,0.08)',   activeBgDark: 'rgba(165,180,252,0.12)' },
  blue:    { accentLight: '#1d4ed8', accentDark: '#93c5fd', activeBgLight: 'rgba(29,78,216,0.08)',   activeBgDark: 'rgba(147,197,253,0.12)' },
  emerald: { accentLight: '#065f46', accentDark: '#6ee7b7', activeBgLight: 'rgba(6,95,70,0.08)',     activeBgDark: 'rgba(110,231,183,0.12)' },
  amber:   { accentLight: '#92400e', accentDark: '#fcd34d', activeBgLight: 'rgba(146,64,14,0.08)',   activeBgDark: 'rgba(252,211,77,0.12)'  },
  rose:    { accentLight: '#9f1239', accentDark: '#fda4af', activeBgLight: 'rgba(159,18,57,0.08)',   activeBgDark: 'rgba(253,164,175,0.12)' },
  slate:   { accentLight: '#334155', accentDark: '#94a3b8', activeBgLight: 'rgba(51,65,85,0.08)',    activeBgDark: 'rgba(148,163,184,0.12)' },
};

const STORAGE_KEY = 'sam-color-scheme';

function buildPreset(paletteName: string): object {
  const shades = [50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 950];
  const primary = Object.fromEntries(shades.map((s) => [s, `{${paletteName}.${s}}`]));
  return definePreset(Aura, { semantic: { primary } });
}

const PRESETS: Record<ColorSchemeKey, object> = Object.fromEntries(
  COLOR_SCHEME_KEYS.map((k) => [k, buildPreset(k)]),
) as Record<ColorSchemeKey, object>;

@Injectable({ providedIn: 'root' })
export class ColorSchemeService {
  private readonly themeService = inject(ThemeService);

  readonly configs = SCHEME_CONFIGS;
  readonly keys = COLOR_SCHEME_KEYS;
  readonly current = signal<ColorSchemeKey>('indigo');

  constructor() {
    // Re-apply CSS vars whenever dark mode or scheme changes
    effect(() => {
      this.applyCssVars(this.current(), this.themeService.dark());
    });
  }

  initialize(): void {
    const stored = localStorage.getItem(STORAGE_KEY) as ColorSchemeKey | null;
    const key = stored && COLOR_SCHEME_KEYS.includes(stored) ? stored : 'indigo';
    this.current.set(key);
    Theme.setPreset(PRESETS[key]);
    this.applyCssVars(key, this.themeService.dark());
  }

  apply(key: ColorSchemeKey): void {
    this.current.set(key);
    Theme.setPreset(PRESETS[key]);
    localStorage.setItem(STORAGE_KEY, key);
  }

  private applyCssVars(key: ColorSchemeKey, dark: boolean): void {
    const vars = CSS_VARS[key];
    const el = document.documentElement;
    el.style.setProperty('--sam-accent', dark ? vars.accentDark : vars.accentLight);
    el.style.setProperty('--sam-active-bg', dark ? vars.activeBgDark : vars.activeBgLight);
  }
}
