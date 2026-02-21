import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { TableModule } from 'primeng/table';
import { Dialog } from 'primeng/dialog';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { InstrumentsApiService } from '../../core/api';
import { Instrument, InstrumentFilterRequest } from '../../model/datamodels';
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
    TranslatePipe,
    InstrumentForm,
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
  buildFilter = (): InstrumentFilterRequest => ({
    page: this.currentPage,
    size: this.rows,
    name: this.nameFilter || undefined,
  });
}
