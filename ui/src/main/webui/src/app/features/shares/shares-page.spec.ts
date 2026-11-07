import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { MessageService } from '@openng/optimus-ui/api';

import { SharesPage } from './shares-page';
import { SharesApiService } from '../../core/api/shares-api.service';
import { provideAuthStub } from '../../core/auth/auth.testing';
import { Roles } from '../../core/auth/roles';

const mine = { id: 'own-link', resourceType: 'SHEET', resourceLabel: 'Own sheet', revoked: false };
const someoneElses = {
  id: 'other-link',
  resourceType: 'SHEET',
  resourceLabel: 'Their sheet',
  creatorName: 'Erika Musterfrau',
  revoked: false,
};

function paginatedOf<T>(data: T[]) {
  return of({ data, totalCount: data.length, page: 0, size: data.length });
}

describe('SharesPage', () => {
  let sharesApi: {
    list: ReturnType<typeof vi.fn>;
    listAll: ReturnType<typeof vi.fn>;
    revoke: ReturnType<typeof vi.fn>;
  };

  async function render(roles: readonly string[]) {
    // The table's frozen action column observes its size; the test DOM has no ResizeObserver
    globalThis.ResizeObserver ??= class {
      observe() {}
      unobserve() {}
      disconnect() {}
    } as unknown as typeof ResizeObserver;
    sharesApi = {
      list: vi.fn().mockReturnValue(paginatedOf([mine])),
      listAll: vi.fn().mockReturnValue(paginatedOf([mine, someoneElses])),
      revoke: vi.fn().mockReturnValue(of(undefined)),
    };
    await TestBed.configureTestingModule({
      imports: [SharesPage],
      providers: [
        provideNoopAnimations(),
        provideAuthStub(roles),
        MessageService,
        { provide: SharesApiService, useValue: sharesApi },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(SharesPage);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  it('shows a member their own links, without the switch to everyone', async () => {
    const fixture = await render([]);
    const page: HTMLElement = fixture.nativeElement;

    expect(sharesApi.list).toHaveBeenCalled();
    expect(sharesApi.listAll).not.toHaveBeenCalled();
    expect(page.querySelector('[data-testid="share-scope"]')).toBeNull();
    expect(page.textContent).toContain('Own sheet');
  });

  it('lets an admin switch to everyone\'s links, with who created them', async () => {
    const fixture = await render([Roles.ADMIN]);
    const page: HTMLElement = fixture.nativeElement;
    expect(page.querySelector('[data-testid="share-scope"]')).not.toBeNull();

    (fixture.componentInstance as unknown as { onScopeChange(s: string): void }).onScopeChange('all');
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(sharesApi.listAll).toHaveBeenCalled();
    expect(page.textContent).toContain('Their sheet');
    expect(page.textContent).toContain('Erika Musterfrau');
  });
});
