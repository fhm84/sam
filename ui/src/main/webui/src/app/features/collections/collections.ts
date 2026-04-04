import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TableModule } from 'primeng/table';
import { Dialog } from 'primeng/dialog';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { InputText } from 'primeng/inputtext';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { Select } from 'primeng/select';
import { SelectButton } from 'primeng/selectbutton';
import { Paginator } from 'primeng/paginator';
import { Toolbar } from 'primeng/toolbar';
import { Tag } from 'primeng/tag';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { LayoutPreferenceService } from '../../core/layout-preference.service';
import { CollectionsApiService } from '../../core/api';
import { CollectionType, SheetCollection, SheetCollectionFilterRequest } from '../../model/datamodels';
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
    Tooltip,
    InputText,
    IconField,
    InputIcon,
    Select,
    SelectButton,
    Paginator,
    FormsModule,
    TranslatePipe,
    CollectionForm,
    Toolbar,
    Tag,
  ],
  providers: [ConfirmationService],
  templateUrl: './collections.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Collections extends BaseCrudList<SheetCollection, SheetCollectionFilterRequest> {
  api: CrudApi<SheetCollection, SheetCollectionFilterRequest> = inject(CollectionsApiService);
  private readonly layoutPref = inject(LayoutPreferenceService);
  private readonly router = inject(Router);
  translationPrefix = 'collections';
  getItemId = (c: SheetCollection) => c.id!;
  getItemName = (c: SheetCollection) => c.name;
  protected readonly typeFilter = signal<CollectionType | null>(null);

  protected readonly typeFilterOptions: { value: CollectionType }[] = [
    { value: 'FOLDER' },
    { value: 'SETLIST' },
  ];

  buildFilter = (): SheetCollectionFilterRequest => ({
    page: this.currentPage,
    size: this.rows,
    name: this.nameFilter || undefined,
    type: this.typeFilter() ?? undefined,
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

  protected onTypeFilterChange(value: CollectionType | null): void {
    this.typeFilter.set(value ?? null);
    this.currentPage = 0;
    this.loadData();
  }

  protected clearFilters(): void {
    this.typeFilter.set(null);
    this.currentPage = 0;
    this.loadData();
  }

  protected onViewModeChange(): void {
    this.layoutPref.setViewMode('collections', this.viewMode());
    this.currentPage = 0;
    this.loadData();
  }

  protected navigateToDetail(collection: SheetCollection): void {
    this.router.navigate(['/collections', collection.id]);
  }

  protected formatDate(date?: Date): string {
    if (!date) return '';
    const d = new Date(date);
    return d.toLocaleDateString();
  }
}
