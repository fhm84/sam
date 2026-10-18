import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { StsConfigHttpLoader } from 'angular-auth-oidc-client';
import { map } from 'rxjs';

interface OidcConfig {
  issuerUrl: string;
  clientId: string;
  // Optional so a static oidc-config.json without them still works (Keycloak defaults)
  scope?: string;
  roleClaimPath?: string;
  roleGroups?: Record<string, string>;
}

/** Provider-specific token layout, filled in once oidc-config.json has loaded. */
@Injectable({ providedIn: 'root' })
export class OidcClaimsConfig {
  // Dot-separated path to the roles array in the access token
  roleClaimPath = 'realm_access.roles';
  // SAM role → provider group (Pocket ID: sam-admin, …); empty when the claim holds SAM's roles
  roleGroups: Record<string, string> = {};
}

export function authConfigLoader(httpClient: HttpClient, claims: OidcClaimsConfig): StsConfigHttpLoader {
  return new StsConfigHttpLoader(
    httpClient.get<OidcConfig>('/oidc-config.json').pipe(
      map((config) => {
        claims.roleClaimPath = config.roleClaimPath ?? claims.roleClaimPath;
        claims.roleGroups = config.roleGroups ?? claims.roleGroups;
        return {
          authority: config.issuerUrl,
          clientId: config.clientId,
          redirectUrl: window.location.origin,
          responseType: 'code',
          scope: config.scope ?? 'openid profile email',
          silentRenew: true,
          useRefreshToken: true,
          postLogoutRedirectUri: window.location.origin,
          secureRoutes: ['/api'],
          ignoreNonceAfterRefresh: true,
        };
      }),
    ),
  );
}
