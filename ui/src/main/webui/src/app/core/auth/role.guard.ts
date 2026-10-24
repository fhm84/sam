import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Lets the user through when they hold one of the given roles, otherwise sends them home. Runs
 * below `authGuard`, so the user is logged in by then; the server still enforces every role.
 */
export function roleGuard(...roles: string[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    return auth.hasAnyRole(roles) || inject(Router).parseUrl('/');
  };
}
