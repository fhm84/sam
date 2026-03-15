import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Toolbar } from 'primeng/toolbar';
import { TranslationService } from '../../core/translation.service';
import { SheetMusic } from '../../model/datamodels';
import { SheetDetail } from './sheet-detail';

@Component({
  selector: 'app-sheet-detail-page',
  imports: [SheetDetail, Button, Toolbar],
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

  protected readonly sheetId = signal('');
  protected readonly autoEnrich = signal(false);

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
}
