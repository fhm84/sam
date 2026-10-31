import { ChangeDetectionStrategy, Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Button } from '@openng/optimus-ui/button';
import { TableModule } from '@openng/optimus-ui/table';
import { Tag } from '@openng/optimus-ui/tag';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { AdminStorageApiService } from '../../core/api';
import { StorageIntegrityReport } from '../../model/datamodels';

/** Admin maintenance page; for now the storage integrity check. */
@Component({
  selector: 'app-configuration',
  imports: [Button, TableModule, Tag, TranslatePipe],
  templateUrl: './configuration.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Configuration {
  private readonly storageApi = inject(AdminStorageApiService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly running = signal(false);
  protected readonly report = signal<StorageIntegrityReport | null>(null);
  protected readonly failed = signal(false);

  /** Damaged and missing files together: both are documents that can't be served as stored. */
  protected readonly brokenDocuments = computed(() => {
    const r = this.report();
    return r ? [...(r.mismatches ?? []), ...(r.missingFiles ?? [])] : [];
  });

  protected readonly clean = computed(() => {
    const r = this.report();
    return (
      !!r &&
      !r.mismatches?.length &&
      !r.missingFiles?.length &&
      !r.orphanedFiles?.length &&
      !r.leftoverTempFiles?.length
    );
  });

  protected runCheck(): void {
    this.running.set(true);
    this.failed.set(false);
    this.storageApi
      .checkIntegrity()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (report) => {
          this.report.set(report);
          this.running.set(false);
        },
        error: () => {
          this.failed.set(true);
          this.running.set(false);
        },
      });
  }
}
