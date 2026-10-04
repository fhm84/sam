import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { map, take } from 'rxjs';

/**
 * Where to go after the Keycloak login redirect. Tokens live in sessionStorage, so a new tab
 * starts unauthenticated and has to log in (silently, via the Keycloak SSO session); the login
 * returns to the app root, so the originally requested URL is remembered here — same tab, same
 * origin, so it survives the redirect.
 */
export const POST_LOGIN_URL_KEY = 'sam.postLoginUrl';

export const authGuard: CanActivateFn = (_route, state) => {
  const oidc = inject(OidcSecurityService);
  const router = inject(Router);

  return oidc.isAuthenticated$.pipe(
    take(1),
    map((result) => {
      if (result.isAuthenticated) {
        const target = readAndClear(POST_LOGIN_URL_KEY);
        return target && target !== state.url ? router.parseUrl(target) : true;
      }
      write(POST_LOGIN_URL_KEY, state.url);
      oidc.authorize();
      return false;
    }),
  );
};

function readAndClear(key: string): string | null {
  try {
    const value = sessionStorage.getItem(key);
    sessionStorage.removeItem(key);
    // Only ever navigate within the app
    return value && value.startsWith('/') && !value.startsWith('//') ? value : null;
  } catch {
    return null;
  }
}

function write(key: string, value: string): void {
  try {
    if (value && value !== '/') {
      sessionStorage.setItem(key, value);
    }
  } catch {
    // Storage unavailable (e.g. blocked): the login still works, it just lands on the home page
  }
}
