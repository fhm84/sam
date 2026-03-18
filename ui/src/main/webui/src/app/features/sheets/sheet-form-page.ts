import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Panel } from 'primeng/panel';
import { Toolbar } from 'primeng/toolbar';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { SheetsApiService } from '../../core/api';
import { SheetMusic } from '../../model/datamodels';
import { DocumentHandler } from '../../shared/base/document-handler';
import { SheetForm } from './sheet-form';

@Component({
  selector: 'app-sheet-form-page',
  imports: [SheetForm, Button, Tooltip, Panel, ConfirmDialog, TranslatePipe, Toolbar],
  providers: [ConfirmationService],
  templateUrl: './sheet-form-page.html',
  styleUrl: './sheet-form-page.scss',
})
export class SheetFormPage extends DocumentHandler implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(SheetsApiService);

  protected readonly sheet = signal<SheetMusic | null>(null);
  protected readonly loading = signal(false);
  protected sheetId: string | null = null;

  protected get isEdit(): boolean {
    return this.sheetId !== null;
  }

  protected getDocumentBasePath(): string {
    return this.documentsApi.forSheets(this.sheetId!);
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.sheetId = id;
      this.loading.set(true);
      this.api.load(id).subscribe({
        next: (sheet) => {
          this.sheet.set(sheet);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
          this.router.navigate(['/sheets']);
        },
      });
      this.loadDocuments();
    }
  }

  protected onSaved(result: SheetMusic): void {
    const key = this.isEdit ? 'sheets.messages.updated' : 'sheets.messages.created';
    this.messageService.add({ severity: 'success', summary: this.t.t(key) });

    if (!this.isEdit && result.id) {
      this.router.navigate(['/sheets', result.id], { queryParams: { enrich: 'true' } });
    } else {
      this.router.navigate(['/sheets']);
    }
  }

  protected onCancelled(): void {
    this.router.navigate(['/sheets']);
  }
}
