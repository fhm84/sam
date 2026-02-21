import { LowerCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { TableModule } from 'primeng/table';
import { Dialog } from 'primeng/dialog';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { SelectButton } from 'primeng/selectbutton';
import { Paginator } from 'primeng/paginator';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { CollectionsApiService } from '../../core/api';
import { SheetCollection, SheetCollectionFilterRequest } from '../../model/datamodels';
import { BaseCrudList } from '../../shared/base/base-crud-list';
import { CrudApi } from '../../shared/base/crud-api.interface';
import { CollectionForm } from './collection-form';

@Component({
  selector: 'app-collections',
  imports: [
    TableModule,
    Dialog,
    ConfirmDialog,
Button,
    InputText,
    IconField,
    InputIcon,
    SelectButton,
    Paginator,
    LowerCasePipe,
    FormsModule,
    TranslatePipe,
    CollectionForm,
  ],
  providers: [ConfirmationService],
  templateUrl: './collections.html',
  styleUrl: './collections.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Collections extends BaseCrudList<SheetCollection, SheetCollectionFilterRequest> {
  api: CrudApi<SheetCollection, SheetCollectionFilterRequest> = inject(CollectionsApiService);
  translationPrefix = 'collections';
  getItemId = (c: SheetCollection) => c.id!;
  getItemName = (c: SheetCollection) => c.name;
  buildFilter = (): SheetCollectionFilterRequest => ({
    page: this.currentPage,
    size: this.rows,
    name: this.nameFilter || undefined,
  });

  protected readonly viewMode = signal<'list' | 'cards'>('list');

  protected readonly viewOptions = [
    { icon: 'pi pi-list', value: 'list' },
    { icon: 'pi pi-th-large', value: 'cards' },
  ];

  protected onPageChange(event: { first?: number; rows?: number }): void {
    this.currentPage = (event.first ?? 0) / (event.rows ?? this.rows);
    this.rows = event.rows ?? this.rows;
    this.loadData();
  }

  protected onViewModeChange(): void {
    this.currentPage = 0;
    this.loadData();
  }

  protected formatDate(date?: Date): string {
    if (!date) return '';
    const d = new Date(date);
    return d.toLocaleDateString();
  }
}
