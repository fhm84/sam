import { Component, computed, inject, Input, OnChanges, signal, ChangeDetectionStrategy } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { FloatLabel } from '@openng/optimus-ui/floatlabel';
import { InputText } from '@openng/optimus-ui/inputtext';
import { Select } from '@openng/optimus-ui/select';
import { AutoComplete, AutoCompleteCompleteEvent } from '@openng/optimus-ui/autocomplete';
import { Textarea } from '@openng/optimus-ui/textarea';
import { Button } from '@openng/optimus-ui/button';
import { Tooltip } from '@openng/optimus-ui/tooltip';
import { MessageService } from '@openng/optimus-ui/api';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { AdminUsersApiService, InstrumentsApiService, MembersApiService, PersonsApiService } from '../../core/api';
import { convertEmptyStringsToNull } from '../../shared/utils/object.utils';
import { Instrument, Member, MemberContact, MemberInstrument, MembershipStatus, Person, UserInfo } from '../../model/datamodels';
import { debounceTime, distinctUntilChanged, filter, forkJoin, map, Observable, of, switchMap } from 'rxjs';
import { BaseForm } from '../../shared/base/base-form';
import { AuthService } from '../../core/auth/auth.service';

const STATUSES: MembershipStatus[] = ['ACTIVE', 'PAUSED', 'FORMER'];

@Component({
  selector: 'app-member-form',
  imports: [ReactiveFormsModule, FormsModule, FloatLabel, InputText, Select, AutoComplete, Textarea, Button, Tooltip, TranslatePipe],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './member-form.html',
})
export class MemberForm extends BaseForm<Member, Member> implements OnChanges {
  private readonly api = inject(MembersApiService);
  private readonly personsApi = inject(PersonsApiService);
  private readonly adminUsersApi = inject(AdminUsersApiService);
  private readonly instrumentsApi = inject(InstrumentsApiService);
  private readonly messageService = inject(MessageService);
  protected readonly auth = inject(AuthService);

  @Input() member: Member | null = null;

  protected readonly statusOptions = computed(() =>
    STATUSES.map((s) => ({ label: this.t.t(`members.status.${s}`), value: s })),
  );

  readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    email: new FormControl('', { nonNullable: true }),
    mobile: new FormControl('', { nonNullable: true }),
    notes: new FormControl('', { nonNullable: true }),
    status: new FormControl<MembershipStatus>('ACTIVE', { nonNullable: true }),
    instruments: new FormControl<MemberInstrument[]>([], { nonNullable: true }),
  });

  readonly linkedUser = signal<UserInfo | null>(null);
  readonly userSuggestions = signal<UserInfo[]>([]);
  userSearchModel: UserInfo | null = null;
  readonly linking = signal(false);
  // Without user search (Pocket ID without API key) the admin enters the user's ID instead
  readonly userSearchAvailable = signal(true);
  manualUserId = '';

  readonly instrumentSuggestions = signal<Instrument[]>([]);

  // On create: an existing person (e.g. a credited arranger) who gets the profile, instead of a new one
  readonly personSuggestions = signal<Person[]>([]);
  existingPerson: Person | null = null;

  /**
   * On create: a person in the catalogue with exactly the typed name and no profile yet, e.g. an
   * arranger who joins the band. Suggesting them avoids a second person of the same name.
   */
  readonly duplicateCandidate = signal<Person | null>(null);

  constructor() {
    super();
    this.form.controls.name.valueChanges
      .pipe(
        map((name) => name.trim()),
        debounceTime(400),
        distinctUntilChanged(),
        filter(() => !this.isEdit),
        switchMap((name) => (name.length < 2 || this.existingPerson ? of(null) : this.findDuplicate(name))),
        takeUntilDestroyed(),
      )
      .subscribe((candidate) => this.duplicateCandidate.set(candidate));
  }

  private findDuplicate(name: string): Observable<Person | null> {
    return forkJoin([this.personsApi.find({ name, size: 10 }), this.api.find({ name, size: 50 })]).pipe(
      map(([persons, members]) => {
        const memberIds = new Set((members.data ?? []).map((m) => m.id));
        return (persons.data ?? []).find(
          (p) => p.name.toLowerCase() === name.toLowerCase() && !memberIds.has(p.id),
        ) ?? null;
      }),
    );
  }

  useDuplicateCandidate(): void {
    const candidate = this.duplicateCandidate();
    if (candidate) {
      // Same as picking the person in the field: the profile keeps the person's own spelling
      this.onPersonSelect(candidate);
    }
  }

  getEntity = () => this.member;

  patchFormValues(m: Member): void {
    this.form.patchValue({
      name: m.name,
      email: m.contact?.email ?? '',
      mobile: m.contact?.mobile ?? '',
      notes: m.contact?.notes ?? '',
      status: m.status ?? 'ACTIVE',
      instruments: m.instruments ?? [],
    });
    this.linkedUser.set(null);
    this.userSearchModel = null;
    this.manualUserId = '';
    if (this.auth.isAdmin()) {
      this.adminUsersApi.directory()
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe((d) => this.userSearchAvailable.set(d.searchable ?? false));
    }
    if (m.userId && this.auth.isAdmin()) {
      this.adminUsersApi.getById(m.userId)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: (u) => {
            this.linkedUser.set(u);
            this.userSearchModel = u;
          },
          error: () => this.linkedUser.set(null),
        });
    }
  }

  buildSaveRequest(): Observable<Member> {
    const raw = convertEmptyStringsToNull(this.form.getRawValue()) as Record<string, unknown>;

    const contact: MemberContact = { email: raw['email'] as string, mobile: raw['mobile'] as string, notes: raw['notes'] as string };

    const payload: Member = {
      id: this.isEdit ? undefined : this.existingPerson?.id,
      name: raw['name'] as string,
      status: raw['status'] as MembershipStatus,
      instruments: raw['instruments'] as MemberInstrument[],
      contact: Object.values(contact).some((v) => v != null) ? contact : undefined,
    };

    return this.isEdit
      ? this.api.update(this.member!.id!, payload).pipe(map(() => ({ ...payload, id: this.member!.id, userId: this.member!.userId }) as Member))
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
    this.api.linkUser(this.member!.id!, user.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.linkedUser.set(user);
          this.linking.set(false);
          this.messageService.add({ severity: 'success', summary: this.t.t('members.form.linkSuccess') });
        },
        error: () => {
          this.userSearchModel = this.linkedUser();
          this.linking.set(false);
          this.messageService.add({ severity: 'error', summary: this.t.t('members.form.linkError') });
        },
      });
  }

  onLinkById(): void {
    const id = this.manualUserId.trim();
    if (id) {
      this.onUserSelect({ id, username: id });
    }
  }

  onUnlink(): void {
    this.linking.set(true);
    this.api.unlinkUser(this.member!.id!)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.linkedUser.set(null);
          this.userSearchModel = null;
          this.linking.set(false);
          this.messageService.add({ severity: 'success', summary: this.t.t('members.form.unlinkSuccess') });
        },
        error: () => {
          this.linking.set(false);
          this.messageService.add({ severity: 'error', summary: this.t.t('members.form.unlinkError') });
        },
      });
  }

  onPersonSearch(event: AutoCompleteCompleteEvent): void {
    const query = event.query.trim();
    if (!query) {
      this.personSuggestions.set([]);
      return;
    }
    forkJoin([this.personsApi.find({ name: query, size: 10 }), this.api.find({ name: query, size: 50 })])
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(([persons, members]) => {
        const memberIds = new Set((members.data ?? []).map((m) => m.id));
        this.personSuggestions.set((persons.data ?? []).filter((p) => !memberIds.has(p.id)));
      });
  }

  onPersonSelect(person: Person): void {
    this.duplicateCandidate.set(null);
    this.existingPerson = person;
    this.form.controls.name.setValue(person.name);
  }

  onPersonClear(): void {
    this.existingPerson = null;
  }

  onInstrumentSearch(event: AutoCompleteCompleteEvent): void {
    if (!event.query.trim()) {
      this.instrumentSuggestions.set([]);
      return;
    }
    this.instrumentsApi.find({ name: event.query, size: 20 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((result) => this.instrumentSuggestions.set(result.data ?? []));
  }

  onInstrumentSelect(instrument: Instrument): void {
    const current = this.form.controls.instruments.value;
    if (current.some((mi) => mi.instrumentId === instrument.id)) {
      return;
    }
    const mi: MemberInstrument = {
      instrumentId: instrument.id,
      instrumentName: instrument.name ?? instrument.displayName,
      primary: current.length === 0,
    };
    this.form.controls.instruments.setValue([...current, mi]);
  }

  onInstrumentRemove(instrumentId: string): void {
    const updated = this.form.controls.instruments.value.filter((mi) => mi.instrumentId !== instrumentId);
    this.form.controls.instruments.setValue(updated);
  }

  instrumentLabel(mi: MemberInstrument): string {
    return mi.instrumentName ?? mi.instrumentId ?? '';
  }
}
