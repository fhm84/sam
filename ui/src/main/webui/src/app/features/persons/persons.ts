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
import { PersonsApiService } from '../../core/api';
import { AuthService } from '../../core/auth/auth.service';
import { Person, PersonFilterRequest } from '../../model/datamodels';
import { BaseCrudList } from '../../shared/base/base-crud-list';
import { CrudApi } from '../../shared/base/crud-api.interface';
import { RowActions } from '../../shared/components/row-actions/row-actions';
import { PersonForm } from './person-form';

@Component({
  selector: 'app-persons',
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
    PersonForm,
    Toolbar,
  ],
  providers: [ConfirmationService],
  templateUrl: './persons.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Persons extends BaseCrudList<Person, PersonFilterRequest> {
  api: CrudApi<Person, PersonFilterRequest> = inject(PersonsApiService);
  protected readonly auth = inject(AuthService);
  translationPrefix = 'persons';
  getItemId = (p: Person) => p.id!;
  getItemName = (p: Person) => p.name;
  buildFilter = (): PersonFilterRequest => ({
    page: this.currentPage,
    size: this.rows,
    name: this.nameFilter || undefined,
  });
}
