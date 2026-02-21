import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
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
import { EnsemblesApiService } from '../../core/api';
import { Ensemble, EnsembleFilterRequest } from '../../model/datamodels';
import { BaseCrudList } from '../../shared/base/base-crud-list';
import { CrudApi } from '../../shared/base/crud-api.interface';
import { EnsembleForm } from './ensemble-form';

@Component({
  selector: 'app-ensembles',
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
    FormsModule,
    TranslatePipe,
    EnsembleForm,
  ],
  providers: [ConfirmationService],
  templateUrl: './ensembles.html',
  styleUrl: './ensembles.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Ensembles extends BaseCrudList<Ensemble, EnsembleFilterRequest> {
  api: CrudApi<Ensemble, EnsembleFilterRequest> = inject(EnsemblesApiService);
  translationPrefix = 'ensembles';
  getItemId = (e: Ensemble) => e.id!;
  getItemName = (e: Ensemble) => e.name;
  buildFilter = (): EnsembleFilterRequest => ({
    page: this.currentPage,
    size: this.rows,
    name: this.nameFilter || undefined,
  });

  private readonly router = inject(Router);

  protected readonly viewMode = signal<'list' | 'cards'>('list');
  protected readonly viewOptions = [
    { icon: 'pi pi-list', value: 'list' },
    { icon: 'pi pi-th-large', value: 'cards' },
  ];

  protected openDetail(ensemble: Ensemble): void {
    this.router.navigate(['/admin/ensembles', ensemble.id]);
  }

  protected onPageChange(event: { first?: number; rows?: number }): void {
    this.currentPage = (event.first ?? 0) / (event.rows ?? this.rows);
    this.rows = event.rows ?? this.rows;
    this.loadData();
  }
}
