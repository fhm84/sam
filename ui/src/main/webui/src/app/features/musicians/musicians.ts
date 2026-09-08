import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { TableModule } from '@openng/optimus-ui/table';
import { Dialog } from '@openng/optimus-ui/dialog';
import { ConfirmDialog } from '@openng/optimus-ui/confirmdialog';
import { ConfirmationService } from '@openng/optimus-ui/api';
import { Button } from '@openng/optimus-ui/button';
import { Tooltip } from '@openng/optimus-ui/tooltip';
import { InputText } from '@openng/optimus-ui/inputtext';
import { IconField } from '@openng/optimus-ui/iconfield';
import { InputIcon } from '@openng/optimus-ui/inputicon';
import { Toolbar } from '@openng/optimus-ui/toolbar';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { MusiciansApiService } from '../../core/api';
import { AuthService } from '../../core/auth/auth.service';
import { Musician, MusicianFilterRequest } from '../../model/datamodels';
import { BaseCrudList } from '../../shared/base/base-crud-list';
import { CrudApi } from '../../shared/base/crud-api.interface';
import { RowActions } from '../../shared/components/row-actions/row-actions';
import { MusicianForm } from './musician-form';

@Component({
  selector: 'app-musicians',
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
    TranslatePipe,
    MusicianForm,
    Toolbar,
  ],
  providers: [ConfirmationService],
  templateUrl: './musicians.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Musicians extends BaseCrudList<Musician, MusicianFilterRequest> {
  api: CrudApi<Musician, MusicianFilterRequest> = inject(MusiciansApiService);
  protected readonly auth = inject(AuthService);
  translationPrefix = 'musicians';
  getItemId = (m: Musician) => m.id!;
  getItemName = (m: Musician) => m.name;
  buildFilter = (): MusicianFilterRequest => ({
    page: this.currentPage,
    size: this.rows,
    name: this.nameFilter || undefined,
  });
}
