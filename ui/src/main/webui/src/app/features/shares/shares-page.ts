import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { ConfirmationService, MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { ConfirmDialog } from 'primeng/confirmdialog';
import { TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Tooltip } from 'primeng/tooltip';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { TranslationService } from '../../core/translation.service';
import { SharesApiService } from '../../core/api/shares-api.service';
import { ShareResponse } from '../../model/datamodels';

@Component({
  selector: 'app-shares-page',
  imports: [TableModule, Tag, Button, Tooltip, ConfirmDialog, TranslatePipe, DatePipe],
  providers: [ConfirmationService],
  templateUrl: './shares-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SharesPage implements OnInit {
  protected readonly t = inject(TranslationService);
  private readonly sharesApi = inject(SharesApiService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);

  protected readonly shares = signal<ShareResponse[]>([]);
  protected readonly loading = signal(false);

  ngOnInit(): void {
    this.loadShares();
  }

  private loadShares(): void {
    this.loading.set(true);
    this.sharesApi.list().subscribe({
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

  protected confirmRevoke(share: ShareResponse): void {
    this.confirmationService.confirm({
      message: this.t.t('shares.page.revokeConfirm'),
      header: this.t.t('shares.page.revokeHeader'),
      icon: 'pi pi-exclamation-triangle',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => {
        this.sharesApi.revoke(share.id!).subscribe({
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
