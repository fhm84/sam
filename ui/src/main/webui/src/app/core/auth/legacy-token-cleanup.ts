/**
 * Tokens used to be stored in localStorage. Since they moved to sessionStorage, nothing reads the
 * old entries any more — but they'd stay in every user's browser indefinitely, including the
 * refresh token. Remove them once at startup. Entries are recognised by their content (the
 * angular-auth-oidc-client state object), not by key, because the key contains the client ID,
 * which differs per deployment.
 */
export function removeLegacyLocalStorageTokens(storage: Storage | undefined = safeLocalStorage()): void {
  if (!storage) return;
  try {
    const stale: string[] = [];
    for (let i = 0; i < storage.length; i++) {
      const key = storage.key(i);
      if (key && isOidcClientState(storage.getItem(key))) {
        stale.push(key);
      }
    }
    stale.forEach((key) => storage.removeItem(key));
  } catch {
    // Storage blocked or unavailable: nothing to clean up
  }
}

function isOidcClientState(value: string | null): boolean {
  if (!value || !value.startsWith('{')) return false;
  try {
    const parsed = JSON.parse(value) as Record<string, unknown>;
    return 'authWellKnownEndPoints' in parsed || 'authnResult' in parsed;
  } catch {
    return false;
  }
}

function safeLocalStorage(): Storage | undefined {
  try {
    return localStorage;
  } catch {
    return undefined;
  }
}
