import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal,
  DestroyRef,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MultiSelect } from '@openng/optimus-ui/multiselect';
import { InputText } from '@openng/optimus-ui/inputtext';
import { Button } from '@openng/optimus-ui/button';
import { Toolbar } from '@openng/optimus-ui/toolbar';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { EventLogTable } from '../../shared/components/event-log-table/event-log-table';
import { EventLogsApiService } from '../../core/api';
import { EventLogEntry, EventLogFilterRequest, EventType } from '../../model/datamodels';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

const ALL_EVENT_TYPES: EventType[] = [
  'DOCUMENT_DOWNLOAD',
  'DOCUMENT_BATCH_DOWNLOAD',
  'SHEET_EXPORT',
  'COLLECTION_EXPORT',
  'COLLECTION_TOC_GENERATED',
  'GEMA_SETLIST_GENERATED',
  'DOCUMENT_CLASSIFIED',
  'DOCUMENT_CLASSIFICATION_APPLIED',
  'SHARE_CREATED',
  'SHARE_ACCESSED',
  'SHARE_REVOKED',
  'SETLIST_AI_SUGGESTION_GENERATED',
  'SETLIST_AI_TEXT_DRAFTED',
];

@Component({
  selector: 'app-event-logs',
  imports: [EventLogTable, MultiSelect, InputText, Button, Toolbar, FormsModule, TranslatePipe],
  templateUrl: './event-logs.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EventLogs implements OnInit {
  private readonly destroyRef = inject(DestroyRef);

  private readonly api = inject(EventLogsApiService);

  protected readonly entries = signal<EventLogEntry[]>([]);
  protected readonly totalCount = signal(0);
  protected readonly loading = signal(false);
  protected readonly rows = 25;
  protected currentPage = 0;

  protected readonly eventTypeOptions = ALL_EVENT_TYPES;
  protected filterEventTypes: EventType[] = [];
  protected filterUserId = '';
  protected filterEntityType = '';

  ngOnInit(): void {
    this.loadData();
  }

  protected onPage(page: number): void {
    this.currentPage = page;
    this.loadData();
  }

  protected applyFilters(): void {
    this.currentPage = 0;
    this.loadData();
  }

  protected clearFilters(): void {
    this.filterEventTypes = [];
    this.filterUserId = '';
    this.filterEntityType = '';
    this.currentPage = 0;
    this.loadData();
  }

  private loadData(): void {
    this.loading.set(true);
    const filter: EventLogFilterRequest = {
      page: this.currentPage,
      size: this.rows,
      ...(this.filterEventTypes.length ? { eventTypes: this.filterEventTypes } : {}),
      ...(this.filterUserId.trim() ? { userId: this.filterUserId.trim() } : {}),
      ...(this.filterEntityType.trim() ? { entityType: this.filterEntityType.trim() } : {}),
    };
    this.api.find(filter).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res) => {
        this.entries.set(res.data ?? []);
        this.totalCount.set(res.totalCount ?? 0);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  protected eventTypeLabel(eventType: EventType | undefined): string {
    return eventType ? `eventLogs.eventTypes.${eventType}` : '';
  }
}
