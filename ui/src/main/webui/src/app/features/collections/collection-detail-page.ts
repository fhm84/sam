import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpResponse } from '@angular/common/http';
import { MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
import { SplitButton } from 'primeng/splitbutton';
import { Tag } from 'primeng/tag';
import { Toolbar } from 'primeng/toolbar';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { TranslationService } from '../../core/translation.service';
import { CollectionsApiService } from '../../core/api';
import { SheetCollection } from '../../model/datamodels';
import { CollectionForm } from './collection-form';
import { CollectionSheets } from './collection-sheets';

@Component({
  selector: 'app-collection-detail-page',
  imports: [TranslatePipe, Button, SplitButton, Dialog, Tag, CollectionForm, CollectionSheets, Toolbar],
  templateUrl: './collection-detail-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CollectionDetailPage implements OnInit {
  protected readonly t = inject(TranslationService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly api = inject(CollectionsApiService);
  private readonly messageService = inject(MessageService);

  protected readonly collectionId = signal('');
  protected readonly collection = signal<SheetCollection | null>(null);
  protected editDialogVisible = false;

  protected readonly exportMenuItems = [
    {
      label: 'JSON',
      icon: 'pi pi-file',
      command: () => this.exportCollection('JSON'),
    },
    {
      label: 'CSV',
      icon: 'pi pi-table',
      command: () => this.exportCollection('CSV'),
    },
  ];

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.collectionId.set(id);
    this.loadCollection(id);
  }

  protected goBack(): void {
    this.router.navigate(['/collections']);
  }

  protected openEdit(): void {
    this.editDialogVisible = true;
  }

  protected onEditSaved(): void {
    this.editDialogVisible = false;
    this.messageService.add({
      severity: 'success',
      summary: this.t.t('collections.messages.updated'),
    });
    this.loadCollection(this.collectionId());
  }

  protected generateToc(): void {
    this.api.downloadToc(this.collectionId()).subscribe({
      next: (response) => {
        const disposition = response.headers.get('Content-Disposition');
        const serverFilename = disposition?.match(/filename="?([^";]+)"?/i)?.[1];
        const filename = serverFilename ?? `toc-${this.collectionId()}.pdf`;
        const url = URL.createObjectURL(response.body!);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: this.t.t('collections.toc.error'),
        });
      },
    });
  }

  protected formatDate(date?: Date): string {
    if (!date) return '';
    return new Date(date).toLocaleDateString();
  }

  protected exportCollection(format: 'ZIP' | 'JSON' | 'CSV'): void {
    this.api.export(this.collectionId(), format).subscribe({
      next: (response) => triggerDownload(response),
      error: () => {
        this.messageService.add({
          severity: 'error',
          summary: this.t.t('collections.export.error'),
        });
      },
    });
  }

  private loadCollection(id: string): void {
    this.api.load(id).subscribe({
      next: (c) => this.collection.set(c),
      error: () => {},
    });
  }
}

function triggerDownload(response: HttpResponse<Blob>): void {
  const disposition = response.headers.get('Content-Disposition');
  const filename = disposition?.match(/filename="?([^";]+)"?/i)?.[1] ?? 'export';
  const url = URL.createObjectURL(response.body!);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}
