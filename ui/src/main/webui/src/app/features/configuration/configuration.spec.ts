import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { Configuration } from './configuration';
import { AdminStorageApiService } from '../../core/api';
import { StorageIntegrityReport } from '../../model/datamodels';

function report(overrides: Partial<StorageIntegrityReport> = {}): StorageIntegrityReport {
  return {
    checkedDocuments: 2,
    storedFiles: 2,
    mismatches: [],
    missingFiles: [],
    orphanedFiles: [],
    leftoverTempFiles: [],
    ...overrides,
  };
}

describe('Configuration', () => {
  let component: Configuration;
  let api: { checkIntegrity: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    api = { checkIntegrity: vi.fn().mockReturnValue(of(report())) };

    await TestBed.overrideComponent(Configuration, { set: { imports: [], template: '<div></div>' } })
      .configureTestingModule({
        imports: [Configuration],
        providers: [{ provide: AdminStorageApiService, useValue: api }],
      })
      .compileComponents();

    component = TestBed.createComponent(Configuration).componentInstance;
  });

  it('shows a clean report', () => {
    (component as any).runCheck();

    expect((component as any).clean()).toBe(true);
    expect((component as any).running()).toBe(false);
  });

  it('lists damaged and missing documents together', () => {
    api.checkIntegrity.mockReturnValue(
      of(
        report({
          mismatches: [{ filename: 'a.pdf' }],
          missingFiles: [{ filename: 'b.pdf' }],
        }),
      ),
    );

    (component as any).runCheck();

    expect((component as any).clean()).toBe(false);
    expect((component as any).brokenDocuments().map((i: any) => i.filename)).toEqual(['a.pdf', 'b.pdf']);
  });

  it('is not clean with orphans only', () => {
    api.checkIntegrity.mockReturnValue(of(report({ orphanedFiles: ['zz/zz/x'] })));

    (component as any).runCheck();

    expect((component as any).clean()).toBe(false);
  });

  it('reports a failed check', () => {
    api.checkIntegrity.mockReturnValue(throwError(() => new Error('500')));

    (component as any).runCheck();

    expect((component as any).failed()).toBe(true);
    expect((component as any).running()).toBe(false);
    expect((component as any).report()).toBeNull();
  });
});
