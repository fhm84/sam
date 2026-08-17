import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { DatePipe, JsonPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule, TablePageEvent } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { MultiSelect } from 'primeng/multiselect';
import { InputText } from 'primeng/inputtext';
import { Button } from 'primeng/button';
import { Toolbar } from 'primeng/toolbar';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { EventLogsApiService } from '../../core/api';
import { EventLogEntry, EventLogFilterRequest, EventType } from '../../model/datamodels';

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
  imports: [TableModule, Tag, MultiSelect, InputText, Button, Toolbar, FormsModule, TranslatePipe, DatePipe, JsonPipe],
  templateUrl: './event-logs.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EventLogs implements OnInit {
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

  protected onPage(event: TablePageEvent): void {
    this.currentPage = Math.floor(event.first / event.rows);
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
    this.api.find(filter).subscribe({
      next: (res) => {
        this.entries.set(res.data ?? []);
        this.totalCount.set(res.totalCount ?? 0);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  protected tagSeverity(eventType: EventType | undefined): 'info' | 'warn' | 'success' | 'secondary' {
    switch (eventType) {
      case 'DOCUMENT_DOWNLOAD':
        return 'info';
      case 'DOCUMENT_BATCH_DOWNLOAD':
        return 'warn';
      case 'SHEET_EXPORT':
      case 'COLLECTION_EXPORT':
        return 'secondary';
      case 'COLLECTION_TOC_GENERATED':
      case 'GEMA_SETLIST_GENERATED':
        return 'warn';
      case 'DOCUMENT_CLASSIFIED':
      case 'DOCUMENT_CLASSIFICATION_APPLIED':
      case 'SETLIST_AI_SUGGESTION_GENERATED':
      case 'SETLIST_AI_TEXT_DRAFTED':
        return 'success';
      case 'SHARE_CREATED':
      case 'SHARE_ACCESSED':
        return 'info';
      case 'SHARE_REVOKED':
        return 'warn';
      default:
        return 'info';
    }
  }

  protected eventTypeLabel(eventType: EventType | undefined): string {
    if (!eventType) return '';
    return `eventLogs.eventTypes.${eventType}`;
  }
}
