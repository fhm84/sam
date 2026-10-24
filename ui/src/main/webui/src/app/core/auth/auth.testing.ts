import { Provider, computed, signal } from '@angular/core';
import { AuthService } from './auth.service';
import { Roles, WRITE_ROLES } from './roles';

type RoleChecks = Pick<AuthService, 'hasRole' | 'hasAnyRole' | 'isAdmin' | 'canWrite'>;

/**
 * Test stand-in for `AuthService` holding exactly `roles`, so component specs need no OIDC setup.
 * Defaults to the music librarian, who sees every write action; pass `[]` for a plain musician.
 */
export function provideAuthStub(roles: readonly string[] = [Roles.MUSIC_LIBRARIAN]): Provider {
  const hasRole = (role: string) => roles.includes(role);
  const hasAnyRole = (wanted: readonly string[]) => wanted.some(hasRole);
  const checks: RoleChecks = {
    hasRole,
    hasAnyRole,
    isAdmin: computed(() => hasRole(Roles.ADMIN)),
    canWrite: computed(() => hasAnyRole(WRITE_ROLES)),
  };
  return {
    provide: AuthService,
    useValue: { ...checks, displayName: signal(''), isAuthenticated: signal(true) },
  };
}
