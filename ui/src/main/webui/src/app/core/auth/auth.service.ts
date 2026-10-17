import { Injectable, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { map } from 'rxjs';
import { OidcClaimsConfig } from './auth.config';

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

  // Decoded token payloads holding the roles. Keycloak: realm_access.roles in the access token
  // (userinfo does not return it by default). Pocket ID: groups, only in the ID token.
  private readonly accessTokenPayload = toSignal<Record<string, unknown> | null>(
    this.oidc.getPayloadFromAccessToken(),
    { initialValue: null },
  );
  private readonly idTokenPayload = toSignal<Record<string, unknown> | null>(
    this.oidc.getPayloadFromIdToken(),
    { initialValue: null },
  );

  private readonly roles = computed(() => {
    const value =
      this.claimAt(this.accessTokenPayload()) ?? this.claimAt(this.idTokenPayload());
    return Array.isArray(value) ? (value as string[]) : [];
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

  readonly isAdmin = computed(() => this.hasRole('admin'));

  hasRole(role: string): boolean {
    return this.roles().includes(role);
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
