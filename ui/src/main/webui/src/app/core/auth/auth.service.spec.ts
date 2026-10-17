import { TestBed } from '@angular/core/testing';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { of } from 'rxjs';
import { OidcClaimsConfig } from './auth.config';
import { AuthService } from './auth.service';

describe('AuthService.hasRole', () => {
  function create(
    roleClaimPath: string,
    accessToken: Record<string, unknown>,
    idToken: Record<string, unknown> = {},
  ): AuthService {
    TestBed.configureTestingModule({
      providers: [
        {
          provide: OidcSecurityService,
          useValue: {
            isAuthenticated$: of({ isAuthenticated: true }),
            userData$: of({ userData: null }),
            getPayloadFromAccessToken: () => of(accessToken),
            getPayloadFromIdToken: () => of(idToken),
          },
        },
      ],
    });
    TestBed.inject(OidcClaimsConfig).roleClaimPath = roleClaimPath;
    return TestBed.inject(AuthService);
  }

  it('reads Keycloak realm roles from the access token', () => {
    const auth = create('realm_access.roles', { realm_access: { roles: ['admin'] } });

    expect(auth.hasRole('admin')).toBe(true);
    expect(auth.isAdmin()).toBe(true);
    expect(auth.hasRole('music_librarian')).toBe(false);
  });

  it('falls back to the ID token when the access token lacks the claim (Pocket ID groups)', () => {
    const auth = create('groups', { scp: ['openid', 'groups'] }, { groups: ['music_librarian'] });

    expect(auth.hasRole('music_librarian')).toBe(true);
    expect(auth.isAdmin()).toBe(false);
  });

  it('has no roles when neither token carries the claim', () => {
    const auth = create('groups', {}, {});

    expect(auth.hasRole('admin')).toBe(false);
  });
});
