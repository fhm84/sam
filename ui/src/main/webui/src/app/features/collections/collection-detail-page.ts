import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Dialog } from 'primeng/dialog';
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
  imports: [TranslatePipe, Button, Dialog, Tag, CollectionForm, CollectionSheets, Toolbar],
  templateUrl: './collection-detail-page.html',
  styleUrl: './collection-detail-page.scss',
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
    // TODO: generate and download a table-of-contents file for this collection
  }

  protected formatDate(date?: Date): string {
    if (!date) return '';
    return new Date(date).toLocaleDateString();
  }

  private loadCollection(id: string): void {
    this.api.load(id).subscribe({
      next: (c) => this.collection.set(c),
      error: () => {},
    });
  }
}
