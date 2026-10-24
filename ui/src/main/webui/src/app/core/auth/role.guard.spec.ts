import { TestBed } from '@angular/core/testing';
import { UrlTree, provideRouter } from '@angular/router';
import { provideAuthStub } from './auth.testing';
import { roleGuard } from './role.guard';
import { AREA_ROLES, Roles } from './roles';

describe('roleGuard', () => {
  function runGuard(userRoles: string[], required: readonly string[]): boolean | UrlTree {
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideAuthStub(userRoles)] });
    // The guard reads none of the router's arguments
    const guard = roleGuard(required) as unknown as () => boolean | UrlTree;
    return TestBed.runInInjectionContext(guard);
  }

  it('lets a user with one of the roles through', () => {
    expect(runGuard([Roles.MUSIC_LIBRARIAN], AREA_ROLES.uploads)).toBe(true);
  });

  it('sends a user without the role home', () => {
    const result = runGuard([Roles.MUSIC_LIBRARIAN], AREA_ROLES.eventLogs);

    expect(result).toBeInstanceOf(UrlTree);
    expect((result as UrlTree).toString()).toBe('/');
  });

  it('sends a user without any role home', () => {
    expect(runGuard([], AREA_ROLES.sheetForms)).toBeInstanceOf(UrlTree);
  });
});
