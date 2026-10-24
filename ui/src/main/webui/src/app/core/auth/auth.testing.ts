import { Provider, computed, signal } from '@angular/core';
import { AuthService } from './auth.service';
import { Roles, WRITE_ROLES } from './roles';

/**
 * Test stand-in for `AuthService` that holds the given roles, so component specs need no OIDC
 * setup. Defaults to the music librarian, who sees every write action.
 */
export function provideAuthStub(...roles: string[]): Provider {
  const held = signal(roles.length > 0 ? roles : [Roles.MUSIC_LIBRARIAN]);
  const hasRole = (role: string) => held().includes(role);
  const hasAnyRole = (wanted: readonly string[]) => wanted.some(hasRole);
  return {
    provide: AuthService,
    useValue: {
      hasRole,
      hasAnyRole,
      isAdmin: computed(() => hasRole(Roles.ADMIN)),
      canWrite: computed(() => hasAnyRole(WRITE_ROLES)),
      displayName: signal(''),
      isAuthenticated: signal(true),
    },
  };
}
