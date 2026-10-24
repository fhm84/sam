import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import { roleGuard } from './role.guard';
import { Roles, WRITE_ROLES } from './roles';

describe('roleGuard', () => {
  function runGuard(userRoles: string[], required: string[]): boolean | UrlTree {
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: { hasAnyRole: (roles: readonly string[]) => roles.some((r) => userRoles.includes(r)) },
        },
      ],
    });
    return TestBed.runInInjectionContext(
      () =>
        roleGuard(...required)({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot) as boolean | UrlTree,
    );
  }

  it('lets a user with one of the roles through', () => {
    expect(runGuard([Roles.MUSIC_LIBRARIAN], [...WRITE_ROLES])).toBe(true);
  });

  it('sends a user without the role home', () => {
    const result = runGuard([Roles.MUSIC_LIBRARIAN], [Roles.ADMIN]);

    expect(result).toBeInstanceOf(UrlTree);
    expect((result as UrlTree).toString()).toBe('/');
  });

  it('sends a user without any role home', () => {
    expect(runGuard([], [...WRITE_ROLES])).toBeInstanceOf(UrlTree);
  });
});
