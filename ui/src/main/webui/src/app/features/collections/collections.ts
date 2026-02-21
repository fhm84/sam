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
import { LayoutPreferenceService } from '../../core/layout-preference.service';
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
  private readonly layoutPref = inject(LayoutPreferenceService);
  translationPrefix = 'collections';
  getItemId = (c: SheetCollection) => c.id!;
  getItemName = (c: SheetCollection) => c.name;
  buildFilter = (): SheetCollectionFilterRequest => ({
    page: this.currentPage,
    size: this.rows,
    name: this.nameFilter || undefined,
  });

  protected readonly viewMode = signal<'list' | 'cards'>('cards');

  protected readonly viewOptions = [
    { icon: 'pi pi-th-large', value: 'cards' },
    { icon: 'pi pi-list', value: 'list' },
  ];

  protected onPageChange(event: { first?: number; rows?: number }): void {
    this.currentPage = (event.first ?? 0) / (event.rows ?? this.rows);
    this.rows = event.rows ?? this.rows;
    this.loadData();
  }

  protected override init(): void {
    this.viewMode.set(this.layoutPref.getViewMode('collections'));
    this.loadData();
  }

  protected onViewModeChange(): void {
    this.layoutPref.setViewMode('collections', this.viewMode());
    this.currentPage = 0;
    this.loadData();
  }

  protected formatDate(date?: Date): string {
    if (!date) return '';
    const d = new Date(date);
    return d.toLocaleDateString();
  }
}
