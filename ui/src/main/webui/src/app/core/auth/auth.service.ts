import { Injectable, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { OidcSecurityService } from 'angular-auth-oidc-client';
import { map } from 'rxjs';

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

  readonly displayName = computed(() => {
    const ud = this.userData();
    if (!ud) return '';
    return ud.name ?? ud.preferred_username ?? ud.email ?? '';
  });

  readonly isAdmin = computed(() => this.hasRole('admin'));

  hasRole(role: string): boolean {
    return this.userData()?.realm_access?.roles?.includes(role) ?? false;
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
