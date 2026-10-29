import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { provideAuthStub } from '../../core/auth/auth.testing';
import { Roles } from '../../core/auth/roles';
import { TranslationService } from '../../core/translation.service';
import { Home } from './home';

describe('Home (rendered per role)', () => {
  function linksFor(roles: string[]): string[] {
    TestBed.configureTestingModule({
      imports: [Home],
      providers: [
        provideRouter([]),
        provideAuthStub(roles),
        { provide: TranslationService, useValue: { t: (k: string) => k } },
      ],
    });
    const fixture = TestBed.createComponent(Home);
    fixture.detectChanges();
    const anchors = fixture.nativeElement.querySelectorAll('a[href]') as NodeListOf<HTMLAnchorElement>;
    return Array.from(anchors, (a) => a.getAttribute('href') ?? '');
  }

  it('shows a plain musician only the reading areas', () => {
    const links = linksFor([]);

    expect(links).toEqual(['/sheets', '/collections', '/members', '/shares']);
  });

  it('shows the music librarian the upload and management areas, not the admin pages', () => {
    const links = linksFor([Roles.MUSIC_LIBRARIAN]);

    expect(links).toContain('/uploads');
    expect(links).toContain('/admin/ensembles');
    expect(links).toContain('/admin/instruments');
    expect(links).not.toContain('/admin/configuration');
    expect(links).not.toContain('/admin/event-logs');
  });

  it('shows the admin every area', () => {
    const links = linksFor([Roles.ADMIN]);

    expect(links).toContain('/admin/configuration');
    expect(links).toContain('/admin/event-logs');
    expect(links).toContain('/uploads');
  });
});
