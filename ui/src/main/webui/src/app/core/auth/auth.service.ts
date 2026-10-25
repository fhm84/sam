import { Injectable, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { EventTypes, OidcSecurityService, PublicEventsService } from 'angular-auth-oidc-client';
import { Observable, catchError, filter, map, of, startWith, switchMap } from 'rxjs';
import { OidcClaimsConfig } from './auth.config';
import { Roles, WRITE_ROLES } from './roles';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly oidc = inject(OidcSecurityService);

  readonly isAuthenticated = toSignal(
    this.oidc.isAuthenticated$.pipe(map((r) => r.isAuthenticated)),
    { initialValue: false },
  );

  readonly userData = toSignal(
    this.oidc.userData$.pipe(map((r) => r.userData as UserData | null)),
    { initialValue: null },
  );

  private readonly claims = inject(OidcClaimsConfig);

  // Fires on start and whenever the library stores new tokens (login callback, silent refresh),
  // so roles follow group changes in the provider without a page reload.
  private readonly tokensChanged$ = inject(PublicEventsService)
    .registerForEvents()
    .pipe(
      filter((event) => event.type === EventTypes.NewAuthenticationResult),
      startWith(null),
    );

  // Decoded token payloads holding the roles. Keycloak: realm_access.roles in the access token
  // (userinfo does not return it by default). Pocket ID: groups, only in the ID token.
  private readonly accessTokenPayload = toSignal<Record<string, unknown> | null>(
    this.payloadOnEveryChange(() => this.oidc.getPayloadFromAccessToken()),
    { initialValue: null },
  );
  private readonly idTokenPayload = toSignal<Record<string, unknown> | null>(
    this.payloadOnEveryChange(() => this.oidc.getPayloadFromIdToken()),
    { initialValue: null },
  );

  /**
   * Reads a token payload now and after each token change. A token that can't be decoded counts
   * as no payload (no roles) for that round, instead of ending the stream and with it every
   * role check until a reload.
   */
  private payloadOnEveryChange(
    read: () => Observable<Record<string, unknown> | null>,
  ): Observable<Record<string, unknown> | null> {
    return this.tokensChanged$.pipe(switchMap(() => read().pipe(catchError(() => of(null)))));
  }

  private readonly roles = computed(() => {
    const value =
      this.claimAt(this.accessTokenPayload()) ?? this.claimAt(this.idTokenPayload());
    const claimed = Array.isArray(value) ? (value as string[]) : [];
    // Mapped groups: only SAM-specific groups grant a role, like the server's RoleGroupsAugmentor
    const mapping = Object.entries(this.claims.roleGroups);
    return mapping.length === 0
      ? claimed
      : mapping.filter(([, group]) => claimed.includes(group)).map(([role]) => role);
  });

  private claimAt(payload: Record<string, unknown> | null): unknown {
    return this.claims.roleClaimPath
      .split('.')
      .reduce<unknown>((node, key) => (node as Record<string, unknown> | null | undefined)?.[key], payload);
  }

  readonly displayName = computed(() => {
    const ud = this.userData();
    if (!ud) return '';
    return ud.name ?? ud.preferred_username ?? ud.email ?? '';
  });

  readonly isAdmin = computed(() => this.hasRole(Roles.ADMIN));

  /** May change the archive; hides create/edit/delete actions for everyone else. */
  readonly canWrite = computed(() => this.hasAnyRole(WRITE_ROLES));

  hasRole(role: string): boolean {
    return this.roles().includes(role);
  }

  hasAnyRole(roles: readonly string[]): boolean {
    return roles.some((role) => this.hasRole(role));
  }

  login(): void {
    this.oidc.authorize();
  }

  logout(): void {
    this.oidc.logoff().subscribe();
  }
}

export interface UserData {
  sub?: string;
  name?: string;
  preferred_username?: string;
  email?: string;
  realm_access?: { roles: string[] };
}
