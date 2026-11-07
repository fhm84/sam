import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  signal,
  DestroyRef,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ConfirmationService, MenuItem, MessageService } from '@openng/optimus-ui/api';
import { Button } from '@openng/optimus-ui/button';
import { ConfirmDialog } from '@openng/optimus-ui/confirmdialog';
import { SelectButton } from '@openng/optimus-ui/selectbutton';
import { TableModule } from '@openng/optimus-ui/table';
import { Tag } from '@openng/optimus-ui/tag';
import { Tooltip } from '@openng/optimus-ui/tooltip';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { TranslationService } from '../../core/translation.service';
import { SharesApiService } from '../../core/api/shares-api.service';
import { ShareResponse } from '../../model/datamodels';
import { RowActions } from '../../shared/components/row-actions/row-actions';
import { AuthService } from '../../core/auth/auth.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

/** Whose links the page shows: the caller's own, or (admins) everyone's. */
type ShareScope = 'mine' | 'all';

@Component({
  selector: 'app-shares-page',
  imports: [
    TableModule,
    Tag,
    Button,
    Tooltip,
    ConfirmDialog,
    SelectButton,
    FormsModule,
    RowActions,
    TranslatePipe,
    DatePipe,
  ],
  providers: [ConfirmationService],
  templateUrl: './shares-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SharesPage implements OnInit {
  private readonly destroyRef = inject(DestroyRef);

  protected readonly t = inject(TranslationService);
  protected readonly auth = inject(AuthService);
  private readonly sharesApi = inject(SharesApiService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);

  protected readonly shares = signal<ShareResponse[]>([]);
  protected readonly loading = signal(false);

  protected readonly scope = signal<ShareScope>('mine');
  protected readonly showingAll = computed(() => this.scope() === 'all');
  protected readonly scopeOptions = computed(() => [
    { label: this.t.t('shares.page.scope.mine'), value: 'mine' },
    { label: this.t.t('shares.page.scope.all'), value: 'all' },
  ]);

  ngOnInit(): void {
    this.loadShares();
  }

  protected onScopeChange(scope: ShareScope): void {
    this.scope.set(scope);
    this.loadShares();
  }

  private loadShares(): void {
    this.loading.set(true);
    const request = this.showingAll() ? this.sharesApi.listAll() : this.sharesApi.list();
    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res) => {
        this.shares.set(res.data ?? []);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  protected shareStatus(share: ShareResponse): 'revoked' | 'expired' | 'active' {
    if (share.revoked) return 'revoked';
    if (share.expiresAt && new Date(share.expiresAt) < new Date()) return 'expired';
    return 'active';
  }

  protected statusSeverity(share: ShareResponse): 'danger' | 'warn' | 'success' {
    const s = this.shareStatus(share);
    if (s === 'revoked') return 'danger';
    if (s === 'expired') return 'warn';
    return 'success';
  }

  protected statusLabel(share: ShareResponse): string {
    return `shares.page.status.${this.shareStatus(share)}`;
  }

  protected copyShareUrl(share: ShareResponse): void {
    if (!share.id) return;
    const url = `${window.location.origin}/share/${share.id}`;
    navigator.clipboard.writeText(url).then(() => {
      this.messageService.add({
        severity: 'success',
        summary: this.t.t('shares.page.copied'),
        life: 2000,
      });
    });
  }

  protected rowMenuItems(share: ShareResponse): MenuItem[] {
    return [
      { label: this.t.t('shares.page.copyLink'), icon: 'pi pi-copy', command: () => this.copyShareUrl(share) },
      { label: this.t.t('shares.page.revoke'), icon: 'pi pi-ban', styleClass: 'row-menu-danger', command: () => this.confirmRevoke(share) },
    ];
  }

  protected confirmRevoke(share: ShareResponse): void {
    this.confirmationService.confirm({
      message: this.t.t('shares.page.revokeConfirm'),
      header: this.t.t('shares.page.revokeHeader'),
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.sharesApi.revoke(share.id!).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
          next: () => {
            this.messageService.add({
              severity: 'success',
              summary: this.t.t('shares.page.revokeSuccess'),
            });
            this.loadShares();
          },
          error: () => {},
        });
      },
    });
  }
}
