import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { TableModule } from 'primeng/table';
import { Dialog } from 'primeng/dialog';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { ConfirmationService } from 'primeng/api';
import { Button } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { InputText } from 'primeng/inputtext';
import { IconField } from 'primeng/iconfield';
import { InputIcon } from 'primeng/inputicon';
import { Toolbar } from 'primeng/toolbar';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { MusiciansApiService } from '../../core/api';
import { AuthService } from '../../core/auth/auth.service';
import { Musician, MusicianFilterRequest } from '../../model/datamodels';
import { BaseCrudList } from '../../shared/base/base-crud-list';
import { CrudApi } from '../../shared/base/crud-api.interface';
import { MusicianForm } from './musician-form';

@Component({
  selector: 'app-musicians',
  imports: [
    TableModule,
    Dialog,
    ConfirmDialog,
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
