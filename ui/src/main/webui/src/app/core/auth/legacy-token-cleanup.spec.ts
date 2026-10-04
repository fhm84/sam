import { removeLegacyLocalStorageTokens } from './legacy-token-cleanup';

describe('removeLegacyLocalStorageTokens', () => {
  beforeEach(() => localStorage.clear());

  it('removes oidc client state left over from localStorage token storage', () => {
    localStorage.setItem('0-sam-ui', JSON.stringify({ authnResult: { refresh_token: 'r' } }));
    localStorage.setItem('0-other-client', JSON.stringify({ authWellKnownEndPoints: {} }));

    removeLegacyLocalStorageTokens();

    expect(localStorage.getItem('0-sam-ui')).toBeNull();
    expect(localStorage.getItem('0-other-client')).toBeNull();
  });

  it('keeps unrelated preferences', () => {
    localStorage.setItem('sam-locale', 'de');
    localStorage.setItem('sam.layout.sheets', 'list');
    localStorage.setItem('some-json', JSON.stringify({ theme: 'dark' }));

    removeLegacyLocalStorageTokens();

    expect(localStorage.getItem('sam-locale')).toBe('de');
    expect(localStorage.getItem('sam.layout.sheets')).toBe('list');
    expect(localStorage.getItem('some-json')).not.toBeNull();
  });

  it('copes with invalid JSON', () => {
    localStorage.setItem('broken', '{not json');

    expect(() => removeLegacyLocalStorageTokens()).not.toThrow();
    expect(localStorage.getItem('broken')).toBe('{not json');
  });
});
