import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { TableModule } from 'primeng/table';
import { Dialog } from 'primeng/dialog';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { Select } from 'primeng/select';
import { Toolbar } from 'primeng/toolbar';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { InstrumentsApiService } from '../../core/api';
import { Instrument, InstrumentFilterRequest, InstrumentTransposing } from '../../model/datamodels';
import { BaseCrudList } from '../../shared/base/base-crud-list';
import { CrudApi } from '../../shared/base/crud-api.interface';
import { InstrumentForm } from './instrument-form';

@Component({
  selector: 'app-instruments',
  imports: [
    TableModule,
    Dialog,
    ConfirmDialog,
    Button,
    InputText,
    IconField,
    InputIcon,
    Select,
    FormsModule,
    TranslatePipe,
    InstrumentForm,
    Toolbar,
  ],
  providers: [ConfirmationService],
  templateUrl: './instruments.html',
  styleUrl: './instruments.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Instruments extends BaseCrudList<Instrument, InstrumentFilterRequest> {
  api: CrudApi<Instrument, InstrumentFilterRequest> = inject(InstrumentsApiService);
  translationPrefix = 'instruments';
  getItemId = (i: Instrument) => i.id!;
  getItemName = (i: Instrument) => i.name;
  protected readonly transpositionFilter = signal<InstrumentTransposing | null>(null);
  protected readonly transpositionOptions: InstrumentTransposing[] = ['C', 'D', 'Eb', 'F', 'G', 'A', 'Ab', 'Bb'];

  buildFilter = (): InstrumentFilterRequest => ({
    page: this.currentPage,
    size: this.rows,
    name: this.nameFilter || undefined,
    transposition: this.transpositionFilter() ?? undefined,
  });

  protected onTranspositionFilterChange(value: InstrumentTransposing | null): void {
    this.transpositionFilter.set(value ?? null);
    this.currentPage = 0;
    this.loadData();
  }

  protected clearFilters(): void {
    this.transpositionFilter.set(null);
    this.currentPage = 0;
    this.loadData();
  }
}
