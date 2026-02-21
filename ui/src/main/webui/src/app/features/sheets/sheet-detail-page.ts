import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { TranslationService } from '../../core/translation.service';
import { SheetMusic } from '../../model/datamodels';
import { SheetDetail } from './sheet-detail';

@Component({
  selector: 'app-sheet-detail-page',
  imports: [SheetDetail, Button],
  providers: [ConfirmationService],
  template: `
    <div class="sheet-detail-page">
      <div class="page-toolbar">
        <p-button
          icon="pi pi-arrow-left"
          [label]="t.t('sheets.detail.backToList')"
          [text]="true"
          (onClick)="goBack()"
        />
      </div>
      <app-sheet-detail
        [sheetId]="sheetId()"
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

  ngOnInit(): void {
    this.sheetId.set(this.route.snapshot.paramMap.get('id')!);
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
