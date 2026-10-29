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
import { Tag } from '@openng/optimus-ui/tag';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { MembersApiService } from '../../core/api';
import { AuthService } from '../../core/auth/auth.service';
import { Member, MemberFilterRequest } from '../../model/datamodels';
import { BaseCrudList } from '../../shared/base/base-crud-list';
import { CrudApi } from '../../shared/base/crud-api.interface';
import { RowActions } from '../../shared/components/row-actions/row-actions';
import { MemberForm } from './member-form';

@Component({
  selector: 'app-members',
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
    MemberForm,
    Toolbar,
    Tag,
  ],
  providers: [ConfirmationService],
  templateUrl: './members.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Members extends BaseCrudList<Member, MemberFilterRequest> {
  api: CrudApi<Member, MemberFilterRequest> = inject(MembersApiService);
  protected readonly auth = inject(AuthService);
  translationPrefix = 'members';
  getItemId = (m: Member) => m.id!;
  getItemName = (m: Member) => m.name;
  buildFilter = (): MemberFilterRequest => ({
    page: this.currentPage,
    size: this.rows,
    name: this.nameFilter || undefined,
  });
}
