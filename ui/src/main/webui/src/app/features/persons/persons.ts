import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { AutoComplete, AutoCompleteCompleteEvent } from '@openng/optimus-ui/autocomplete';
import { MenuItem } from '@openng/optimus-ui/api';
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
    FormsModule,
    AutoComplete,
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

  private readonly personsApi = inject(PersonsApiService);

  // ── Merge a duplicate into another person ─────────────
  protected readonly mergeSource = signal<Person | null>(null);
  protected mergeTarget: Person | null = null;
  protected readonly mergeSuggestions = signal<Person[]>([]);
  protected merging = false;

  protected override rowMenuItems(item: Person): MenuItem[] {
    return [
      ...super.rowMenuItems(item),
      { label: this.t.t('persons.merge.action'), icon: 'pi pi-arrow-right-arrow-left', command: () => this.openMerge(item) },
    ];
  }

  protected openMerge(person: Person): void {
    this.mergeTarget = null;
    this.mergeSuggestions.set([]);
    this.mergeSource.set(person);
  }

  protected closeMerge(): void {
    this.mergeSource.set(null);
  }

  protected onMergeSearch(event: AutoCompleteCompleteEvent): void {
    const source = this.mergeSource();
    this.personsApi.find({ name: event.query.trim() || undefined, size: 20 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((res) => this.mergeSuggestions.set((res.data ?? []).filter((p) => p.id !== source?.id)));
  }

  protected merge(): void {
    const source = this.mergeSource();
    const target = this.mergeTarget;
    if (!source || !target) return;
    this.merging = true;
    this.personsApi.mergeInto(source.id!, target.id!)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.merging = false;
          this.mergeSource.set(null);
          this.messageService.add({
            severity: 'success',
            summary: this.t.t('persons.merge.success').replace('{source}', source.name).replace('{target}', target.name),
          });
          this.loadData();
        },
        error: () => {
          this.merging = false;
        },
      });
  }
}
