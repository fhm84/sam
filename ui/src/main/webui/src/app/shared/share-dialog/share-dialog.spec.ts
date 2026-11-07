import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { ShareDialogComponent } from './share-dialog';
import { SharesApiService } from '../../core/api/shares-api.service';

describe('ShareDialogComponent', () => {
  it('creates a link that expires after 30 days unless someone picks otherwise', async () => {
    const create = vi.fn().mockReturnValue(of({ id: 'token' }));
    await TestBed.configureTestingModule({
      imports: [ShareDialogComponent],
      providers: [provideNoopAnimations(), { provide: SharesApiService, useValue: { create } }],
    }).compileComponents();
    const fixture = TestBed.createComponent(ShareDialogComponent);
    fixture.componentRef.setInput('resourceType', 'SHEET');
    fixture.componentRef.setInput('resourceId', 'sheet-1');
    fixture.componentRef.setInput('resourceLabel', 'A sheet');
    fixture.detectChanges();

    (fixture.componentInstance as unknown as { createLink(): void }).createLink();

    const expiresAt: Date = create.mock.calls[0][0].expiresAt;
    const days = (expiresAt.getTime() - Date.now()) / (24 * 60 * 60 * 1000);
    expect(days).toBeGreaterThan(29.9);
    expect(days).toBeLessThan(30.1);
  });
});
