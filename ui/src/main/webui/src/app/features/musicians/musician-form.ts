import { Component, DestroyRef, inject, Input, OnChanges, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { FloatLabel } from 'primeng/floatlabel';
import { InputText } from 'primeng/inputtext';
import { InputNumber } from 'primeng/inputnumber';
import { Button } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { AutoComplete, AutoCompleteCompleteEvent } from 'primeng/autocomplete';
import { MessageService } from 'primeng/api';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { AdminUsersApiService, MusiciansApiService } from '../../core/api';
import { convertEmptyStringsToNull } from '../../shared/utils/object.utils';
import { Musician, UserInfo } from '../../model/datamodels';
import { map, Observable } from 'rxjs';
import { BaseForm } from '../../shared/base/base-form';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-musician-form',
  imports: [ReactiveFormsModule, FloatLabel, InputText, InputNumber, Button, Tooltip, AutoComplete, TranslatePipe],
  providers: [MessageService],
  templateUrl: './musician-form.html',
})
export class MusicianForm extends BaseForm<Musician, Musician> implements OnChanges {
  private readonly api = inject(MusiciansApiService);
  private readonly adminUsersApi = inject(AdminUsersApiService);
  private readonly messageService = inject(MessageService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly auth = inject(AuthService);

  @Input() musician: Musician | null = null;

  readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    ipi: new FormControl('', { nonNullable: true }),
    birthYear: new FormControl<number | null>(null),
    deathYear: new FormControl<number | null>(null),
  });

  readonly linkedUser = signal<UserInfo | null>(null);
  readonly userSuggestions = signal<UserInfo[]>([]);
  readonly userSearchValue = signal<UserInfo | null>(null);
  readonly linking = signal(false);

  getEntity = () => this.musician;

  patchFormValues(m: Musician): void {
    this.form.patchValue({
      name: m.name,
      ipi: m.ipi ?? '',
      birthYear: m.birthYear ?? null,
      deathYear: m.deathYear ?? null,
    });
    this.linkedUser.set(null);
    this.userSearchValue.set(null);
    if (m.userId && this.auth.isAdmin()) {
      this.adminUsersApi.getById(m.userId)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (u) => {
            this.linkedUser.set(u);
            this.userSearchValue.set(u);
          },
          error: () => this.linkedUser.set(null),
        });
    }
  }

  buildSaveRequest(): Observable<Musician> {
    const payload = convertEmptyStringsToNull(this.form.getRawValue()) as Musician;
    return this.isEdit
      ? this.api.update(this.musician!.id!, payload).pipe(map(() => ({ ...payload, id: this.musician!.id, userId: this.musician!.userId }) as Musician))
      : this.api.create(payload);
  }

  userDisplayLabel(u: UserInfo): string {
    const parts: string[] = [];
    if (u.firstName) parts.push(u.firstName);
    if (u.lastName) parts.push(u.lastName);
    const name = parts.join(' ').trim();
    if (name && u.email) return `${name} (${u.email})`;
    if (name) return name;
    return u.email ?? u.username;
  }

  onUserSearch(event: AutoCompleteCompleteEvent): void {
    if (!event.query.trim()) {
      this.userSuggestions.set([]);
      return;
    }
    this.adminUsersApi.search(event.query)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((results) => this.userSuggestions.set(results));
  }

  onUserSelect(user: UserInfo): void {
    this.linking.set(true);
    this.api.linkUser(this.musician!.id!, user.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.linkedUser.set(user);
          this.linking.set(false);
          this.messageService.add({ severity: 'success', summary: this.t.t('musicians.form.linkSuccess') });
        },
        error: () => {
          this.userSearchValue.set(this.linkedUser());
          this.linking.set(false);
          this.messageService.add({ severity: 'error', summary: this.t.t('musicians.form.linkError') });
        },
      });
  }

  onUnlink(): void {
    this.linking.set(true);
    this.api.unlinkUser(this.musician!.id!)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.linkedUser.set(null);
          this.userSearchValue.set(null);
          this.linking.set(false);
          this.messageService.add({ severity: 'success', summary: this.t.t('musicians.form.unlinkSuccess') });
        },
        error: () => {
          this.linking.set(false);
          this.messageService.add({ severity: 'error', summary: this.t.t('musicians.form.unlinkError') });
        },
      });
  }
}
