import { TestBed } from '@angular/core/testing';
import { EventTypes, OidcSecurityService, PublicEventsService } from 'angular-auth-oidc-client';
import { of, throwError } from 'rxjs';
import { OidcClaimsConfig } from './auth.config';
import { AuthService } from './auth.service';

describe('AuthService.hasRole', () => {
  function create(
    roleClaimPath: string,
    accessToken: Record<string, unknown>,
    idToken: Record<string, unknown> = {},
    roleGroups: Record<string, string> = {},
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
    const claims = TestBed.inject(OidcClaimsConfig);
    claims.roleClaimPath = roleClaimPath;
    claims.roleGroups = roleGroups;
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

  it('maps SAM-specific groups to roles and ignores other apps’ groups', () => {
    const roleGroups = { admin: 'sam-admin', music_librarian: 'sam-music-librarian' };
    const librarian = create('groups', {}, { groups: ['sam-member', 'sam-music-librarian'] }, roleGroups);

    expect(librarian.hasRole('music_librarian')).toBe(true);
    expect(librarian.isAdmin()).toBe(false);

    TestBed.resetTestingModule();
    const otherAppsAdmin = create('groups', {}, { groups: ['admin', 'music_librarian'] }, roleGroups);

    expect(otherAppsAdmin.isAdmin()).toBe(false);
    expect(otherAppsAdmin.hasRole('music_librarian')).toBe(false);
  });

  it('has no roles when neither token carries the claim', () => {
    const auth = create('groups', {}, {});

    expect(auth.hasRole('admin')).toBe(false);
  });

  it('lets the music librarian and the admin write, nobody else', () => {
    expect(create('groups', {}, { groups: ['music_librarian'] }).canWrite()).toBe(true);
    TestBed.resetTestingModule();
    expect(create('groups', {}, { groups: ['admin'] }).canWrite()).toBe(true);
    TestBed.resetTestingModule();
    expect(create('groups', {}, { groups: ['musician'] }).canWrite()).toBe(false);
  });
});

describe('AuthService after a token refresh', () => {
  it('re-reads the roles when the library stores new tokens', () => {
    let idToken: Record<string, unknown> = { groups: ['music_librarian'] };
    TestBed.configureTestingModule({
      providers: [
        {
          provide: OidcSecurityService,
          useValue: {
            isAuthenticated$: of({ isAuthenticated: true }),
            userData$: of({ userData: null }),
            getPayloadFromAccessToken: () => of({}),
            getPayloadFromIdToken: () => of(idToken),
          },
        },
      ],
    });
    TestBed.inject(OidcClaimsConfig).roleClaimPath = 'groups';
    const auth = TestBed.inject(AuthService);
    expect(auth.canWrite()).toBe(true);

    // An admin removed the user from the librarian group; the next silent refresh brings it
    idToken = { groups: [] };
    TestBed.inject(PublicEventsService).fireEvent(EventTypes.NewAuthenticationResult, {
      isAuthenticated: true,
      isRenewProcess: true,
    });

    expect(auth.canWrite()).toBe(false);
    expect(auth.hasRole('music_librarian')).toBe(false);
  });

  it('treats an undecodable token as no roles and recovers with the next one', () => {
    let idToken: Record<string, unknown> | 'broken' = { groups: ['admin'] };
    TestBed.configureTestingModule({
      providers: [
        {
          provide: OidcSecurityService,
          useValue: {
            isAuthenticated$: of({ isAuthenticated: true }),
            userData$: of({ userData: null }),
            getPayloadFromAccessToken: () => of({}),
            getPayloadFromIdToken: () =>
              idToken === 'broken' ? throwError(() => new SyntaxError('Unexpected token')) : of(idToken),
          },
        },
      ],
    });
    TestBed.inject(OidcClaimsConfig).roleClaimPath = 'groups';
    const auth = TestBed.inject(AuthService);
    const events = TestBed.inject(PublicEventsService);
    const refresh = () =>
      events.fireEvent(EventTypes.NewAuthenticationResult, { isAuthenticated: true, isRenewProcess: true });

    idToken = 'broken';
    refresh();
    expect(auth.isAdmin()).toBe(false);

    idToken = { groups: ['admin'] };
    refresh();
    expect(auth.isAdmin()).toBe(true);
  });

  it('ignores other library events', () => {
    let idToken: Record<string, unknown> = { groups: ['admin'] };
    TestBed.configureTestingModule({
      providers: [
        {
          provide: OidcSecurityService,
          useValue: {
            isAuthenticated$: of({ isAuthenticated: true }),
            userData$: of({ userData: null }),
            getPayloadFromAccessToken: () => of({}),
            getPayloadFromIdToken: () => of(idToken),
          },
        },
      ],
    });
    TestBed.inject(OidcClaimsConfig).roleClaimPath = 'groups';
    const auth = TestBed.inject(AuthService);

    idToken = { groups: [] };
    TestBed.inject(PublicEventsService).fireEvent(EventTypes.SilentRenewStarted, null);

    expect(auth.isAdmin()).toBe(true);
  });
});
