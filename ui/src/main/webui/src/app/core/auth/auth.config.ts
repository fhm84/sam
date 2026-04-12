import { HttpClient } from '@angular/common/http';
import { StsConfigHttpLoader } from 'angular-auth-oidc-client';
import { map } from 'rxjs';

interface OidcConfig {
  issuerUrl: string;
  clientId: string;
}

export function authConfigLoader(httpClient: HttpClient): StsConfigHttpLoader {
  return new StsConfigHttpLoader(
    httpClient.get<OidcConfig>('/oidc-config.json').pipe(
      map((config) => ({
        authority: config.issuerUrl,
        clientId: config.clientId,
        redirectUrl: window.location.origin,
        responseType: 'code',
        scope: 'openid profile email',
        silentRenew: true,
        useRefreshToken: true,
        postLogoutRedirectUri: window.location.origin,
        secureRoutes: ['/api'],
        ignoreNonceAfterRefresh: true,
      })),
    ),
  );
}
