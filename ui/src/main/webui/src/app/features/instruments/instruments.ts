import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { TableModule } from '@openng/optimus-ui/table';
import { Dialog } from '@openng/optimus-ui/dialog';
import { ConfirmDialog } from '@openng/optimus-ui/confirmdialog';
import { ConfirmationService } from '@openng/optimus-ui/api';
import { Button } from '@openng/optimus-ui/button';
import { Tooltip } from '@openng/optimus-ui/tooltip';
import { InputText } from '@openng/optimus-ui/inputtext';
import { IconField } from '@openng/optimus-ui/iconfield';
import { InputIcon } from '@openng/optimus-ui/inputicon';
import { Select } from '@openng/optimus-ui/select';
import { Toolbar } from '@openng/optimus-ui/toolbar';
import { FormsModule } from '@angular/forms';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { InstrumentsApiService } from '../../core/api';
import { Instrument, InstrumentFilterRequest, InstrumentTransposing } from '../../model/datamodels';
import { BaseCrudList } from '../../shared/base/base-crud-list';
import { CrudApi } from '../../shared/base/crud-api.interface';
import { RowActions } from '../../shared/components/row-actions/row-actions';
import { InstrumentForm } from './instrument-form';

@Component({
  selector: 'app-instruments',
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
    Select,
    FormsModule,
    TranslatePipe,
    InstrumentForm,
    Toolbar,
  ],
  providers: [ConfirmationService],
  templateUrl: './instruments.html',
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
