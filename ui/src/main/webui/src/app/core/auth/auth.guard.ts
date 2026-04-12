import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { map, take } from 'rxjs';

export const authGuard: CanActivateFn = () => {
  const oidc = inject(OidcSecurityService);

  return oidc.isAuthenticated$.pipe(
    take(1),
    map((result) => {
      if (result.isAuthenticated) {
        return true;
      }
      oidc.authorize();
      return false;
    }),
  );
};
