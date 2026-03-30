import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { SplitButton } from 'primeng/splitbutton';
import { Toolbar } from 'primeng/toolbar';
import { TranslationService } from '../../core/translation.service';
import { SheetsApiService } from '../../core/api';
import { SheetMusic } from '../../model/datamodels';
import { SheetDetail } from './sheet-detail';

@Component({
  selector: 'app-sheet-detail-page',
  imports: [SheetDetail, Button, SplitButton, Toolbar],
  providers: [ConfirmationService],
  template: `
    <div class="sheet-detail-page">
      <p-toolbar styleClass="page-toolbar">
        <ng-template #start>
          <p-button
            icon="pi pi-arrow-left"
            [label]="t.t('sheets.detail.backToList')"
            [text]="true"
            (onClick)="goBack()"
          />
        </ng-template>
        <ng-template #end>
          <p-splitButton
            icon="pi pi-download"
            [label]="t.t('sheets.export.zip')"
            [model]="exportMenuItems"
            severity="secondary"
            [outlined]="true"
            size="small"
            (onClick)="exportSheet('ZIP')"
          />
        </ng-template>
      </p-toolbar>
      <app-sheet-detail
        [sheetId]="sheetId()"
        [autoEnrich]="autoEnrich()"
        mode="full"
        (edit)="onEdit($event)"
        (deleted)="onDeleted()"
      />
    </div>
  `,
  styles: `
    .sheet-detail-page {
      max-width: 100%;
    }

    .page-toolbar {
      margin-bottom: 0.5rem;
    }
  `,
})
export class SheetDetailPage implements OnInit {
  protected readonly t = inject(TranslationService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(SheetsApiService);
  private readonly messageService = inject(MessageService);

  protected readonly sheetId = signal('');
  protected readonly autoEnrich = signal(false);

  protected readonly exportMenuItems = [
    {
      label: 'JSON',
      icon: 'pi pi-file',
      command: () => this.exportSheet('JSON'),
    },
    {
      label: 'CSV',
      icon: 'pi pi-table',
      command: () => this.exportSheet('CSV'),
    },
  ];

  ngOnInit(): void {
    this.sheetId.set(this.route.snapshot.paramMap.get('id')!);
    this.autoEnrich.set(this.route.snapshot.queryParamMap.get('enrich') === 'true');
  }

  protected goBack(): void {
    this.router.navigate(['/sheets']);
  }

  protected onEdit(sheet: SheetMusic): void {
    this.router.navigate(['/sheets', sheet.id, 'edit']);
  }

  protected onDeleted(): void {
    this.router.navigate(['/sheets']);
  }

  protected exportSheet(format: 'ZIP' | 'JSON' | 'CSV'): void {
    this.api.export(this.sheetId(), format).subscribe({
      next: (response) => triggerDownload(response),
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: this.t.t('sheets.export.error'),
        });
      },
    });
  }
}

function triggerDownload(response: import('@angular/common/http').HttpResponse<Blob>): void {
  const disposition = response.headers.get('Content-Disposition');
  const filename = disposition?.match(/filename="?([^";]+)"?/i)?.[1] ?? 'export';
  const url = URL.createObjectURL(response.body!);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}
