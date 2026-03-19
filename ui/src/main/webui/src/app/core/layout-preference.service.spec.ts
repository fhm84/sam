import { TestBed } from '@angular/core/testing';
import { LayoutPreferenceService } from './layout-preference.service';

describe('LayoutPreferenceService', () => {
  let service: LayoutPreferenceService;

  beforeEach(() => {
    localStorage.clear();

    TestBed.configureTestingModule({ providers: [LayoutPreferenceService] });
    service = TestBed.inject(LayoutPreferenceService);
  });

  // ── getViewMode ────────────────────────────────────────

  it('returns "cards" when nothing is stored', () => {
    expect(service.getViewMode('sheets')).toBe('cards');
  });

  it('returns stored "list" value', () => {
    localStorage.setItem('sam.layout.sheets', 'list');
    expect(service.getViewMode('sheets')).toBe('list');
  });

  it('returns stored "cards" value', () => {
    localStorage.setItem('sam.layout.sheets', 'cards');
    expect(service.getViewMode('sheets')).toBe('cards');
  });

  it('returns "cards" for an unrecognised stored value', () => {
    localStorage.setItem('sam.layout.sheets', 'grid');
    expect(service.getViewMode('sheets')).toBe('cards');
  });

  it('uses page-specific key so pages do not interfere', () => {
    localStorage.setItem('sam.layout.sheets', 'list');
    expect(service.getViewMode('musicians')).toBe('cards');
  });

  // ── setViewMode ────────────────────────────────────────

  it('persists "list" in localStorage', () => {
    service.setViewMode('sheets', 'list');
    expect(localStorage.getItem('sam.layout.sheets')).toBe('list');
  });

  it('persists "cards" in localStorage', () => {
    service.setViewMode('sheets', 'cards');
    expect(localStorage.getItem('sam.layout.sheets')).toBe('cards');
  });

  it('round-trips: setViewMode then getViewMode returns same value', () => {
    service.setViewMode('musicians', 'list');
    expect(service.getViewMode('musicians')).toBe('list');
  });
});
