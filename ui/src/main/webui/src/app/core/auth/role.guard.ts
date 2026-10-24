import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

/**
 * Lets the user through when they hold one of the given roles, otherwise sends them home. Use as
 * `canMatch`, so a lazy area isn't even downloaded for users who may not open it. Runs below
 * `authGuard`, so the user is logged in by then; the server still enforces every role.
 */
export function roleGuard(roles: readonly string[]): CanMatchFn {
  return () => inject(AuthService).hasAnyRole(roles) || inject(Router).parseUrl('/');
}
