import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TableModule } from '@openng/optimus-ui/table';
import { Dialog } from '@openng/optimus-ui/dialog';
import { ConfirmDialog } from '@openng/optimus-ui/confirmdialog';
import { ConfirmationService } from '@openng/optimus-ui/api';
import { Button } from '@openng/optimus-ui/button';
import { Tooltip } from '@openng/optimus-ui/tooltip';
import { InputText } from '@openng/optimus-ui/inputtext';
import { IconField } from '@openng/optimus-ui/iconfield';
import { InputIcon } from '@openng/optimus-ui/inputicon';
import { SelectButton } from '@openng/optimus-ui/selectbutton';
import { Paginator } from '@openng/optimus-ui/paginator';
import { Toolbar } from '@openng/optimus-ui/toolbar';
import { Tag } from '@openng/optimus-ui/tag';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { LayoutPreferenceService } from '../../core/layout-preference.service';
import { EnsemblesApiService } from '../../core/api';
import { Ensemble, EnsembleFilterRequest } from '../../model/datamodels';
import { BaseCrudList } from '../../shared/base/base-crud-list';
import { CrudApi } from '../../shared/base/crud-api.interface';
import { RowActions } from '../../shared/components/row-actions/row-actions';
import { EnsembleForm } from './ensemble-form';

@Component({
  selector: 'app-ensembles',
  imports: [
    TableModule,
    Dialog,
    ConfirmDialog,
    RowActions,
    Button,
    Tooltip,
    InputText,
    IconField,
    InputIcon,
    SelectButton,
    Paginator,
    FormsModule,
    TranslatePipe,
    EnsembleForm,
    Toolbar,
    Tag,
  ],
  providers: [ConfirmationService],
  templateUrl: './ensembles.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Ensembles extends BaseCrudList<Ensemble, EnsembleFilterRequest> {
  api: CrudApi<Ensemble, EnsembleFilterRequest> = inject(EnsemblesApiService);
  private readonly layoutPref = inject(LayoutPreferenceService);
  translationPrefix = 'ensembles';
  getItemId = (e: Ensemble) => e.id!;
  getItemName = (e: Ensemble) => e.name;
  buildFilter = (): EnsembleFilterRequest => ({
    page: this.currentPage,
    size: this.rows,
    name: this.nameFilter || undefined,
  });

  private readonly router = inject(Router);

  protected readonly viewMode = signal<'list' | 'cards'>('cards');
  protected readonly viewOptions = [
    { icon: 'pi pi-th-large', value: 'cards' },
    { icon: 'pi pi-list', value: 'list' },
  ];

  protected override init(): void {
    this.viewMode.set(this.layoutPref.getViewMode('ensembles'));
    this.loadData();
  }

  protected onViewModeChange(): void {
    this.layoutPref.setViewMode('ensembles', this.viewMode());
  }

  protected openDetail(ensemble: Ensemble): void {
    this.router.navigate(['/admin/ensembles', ensemble.id]);
  }

  protected onPageChange(event: { first?: number; rows?: number }): void {
    this.currentPage = (event.first ?? 0) / (event.rows ?? this.rows);
    this.rows = event.rows ?? this.rows;
    this.loadData();
  }
}
