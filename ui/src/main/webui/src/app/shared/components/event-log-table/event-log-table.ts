import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { DatePipe, JsonPipe } from '@angular/common';
import { TableModule, TablePageEvent } from '@openng/optimus-ui/table';
import { Tag } from '@openng/optimus-ui/tag';
import { TranslatePipe } from '../../pipes/translate.pipe';
import { EventLogEntry, EventType } from '../../../model/datamodels';

/**
 * Lazy, paged table of event-log entries, shared by the admin event log and the user's own
 * activity page. The parent loads the data and reacts to {@link pageChange}.
 */
@Component({
  selector: 'app-event-log-table',
  imports: [TableModule, Tag, TranslatePipe, DatePipe, JsonPipe],
  templateUrl: './event-log-table.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EventLogTable {
  readonly entries = input.required<EventLogEntry[]>();
  readonly totalCount = input.required<number>();
  readonly loading = input(false);
  readonly rows = input(25);
  readonly page = input(0);
  /** Show the user column; off on the user's own activity, where it is always the caller. */
  readonly showUser = input(true);
  /** Translation key of the text shown when there are no entries. */
  readonly emptyKey = input('eventLogs.empty');

  /** The zero-based page the user switched to. */
  readonly pageChange = output<number>();

  protected onPage(event: TablePageEvent): void {
    this.pageChange.emit(Math.floor(event.first / event.rows));
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
