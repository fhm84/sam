import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { EventLogTable } from '../../shared/components/event-log-table/event-log-table';
import { EventLogsApiService } from '../../core/api';
import { EventLogEntry } from '../../model/datamodels';

/** What the event log recorded about the logged-in user (GDPR right of access). */
@Component({
  selector: 'app-my-activity',
  imports: [EventLogTable, TranslatePipe],
  templateUrl: './my-activity.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyActivity implements OnInit {
  private readonly api = inject(EventLogsApiService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly entries = signal<EventLogEntry[]>([]);
  protected readonly totalCount = signal(0);
  protected readonly loading = signal(false);
  protected readonly page = signal(0);
  protected readonly rows = 25;

  ngOnInit(): void {
    this.load();
  }

  protected onPage(page: number): void {
    this.page.set(page);
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.api
      .own(this.page(), this.rows)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.entries.set(res.data ?? []);
          this.totalCount.set(res.totalCount ?? 0);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }
}
