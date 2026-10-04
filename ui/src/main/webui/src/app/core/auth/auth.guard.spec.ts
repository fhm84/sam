import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { Observable, firstValueFrom, of } from 'rxjs';
import { vi } from 'vitest';
import { POST_LOGIN_URL_KEY, authGuard } from './auth.guard';

describe('authGuard', () => {
  let authorize: ReturnType<typeof vi.fn>;

  function runGuard(isAuthenticated: boolean, url: string): Promise<boolean | UrlTree> {
    authorize = vi.fn();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: OidcSecurityService,
          useValue: { isAuthenticated$: of({ isAuthenticated }), authorize },
        },
      ],
    });
    return TestBed.runInInjectionContext(() =>
      firstValueFrom(
        authGuard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot) as Observable<boolean | UrlTree>,
      ),
    );
  }

  beforeEach(() => sessionStorage.clear());

  it('lets authenticated users through', async () => {
    expect(await runGuard(true, '/sheets')).toBe(true);
  });

  it('starts the login and remembers the requested deep link', async () => {
    expect(await runGuard(false, '/sheets/123')).toBe(false);

    expect(authorize).toHaveBeenCalledOnce();
    expect(sessionStorage.getItem(POST_LOGIN_URL_KEY)).toBe('/sheets/123');
  });

  it('does not remember the root URL', async () => {
    await runGuard(false, '/');

    expect(sessionStorage.getItem(POST_LOGIN_URL_KEY)).toBeNull();
  });

  it('after login, redirects to the remembered deep link once', async () => {
    sessionStorage.setItem(POST_LOGIN_URL_KEY, '/sheets/123');

    const result = await runGuard(true, '/');

    expect(TestBed.inject(Router).serializeUrl(result as UrlTree)).toBe('/sheets/123');
    expect(sessionStorage.getItem(POST_LOGIN_URL_KEY)).toBeNull();
  });

  it('ignores a remembered URL that would leave the app', async () => {
    sessionStorage.setItem(POST_LOGIN_URL_KEY, '//evil.example/phish');

    expect(await runGuard(true, '/')).toBe(true);
  });
});
