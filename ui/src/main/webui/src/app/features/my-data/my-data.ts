import { ChangeDetectionStrategy, Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { HttpErrorResponse } from '@angular/common/http';
import { TableModule } from '@openng/optimus-ui/table';
import { Tag } from '@openng/optimus-ui/tag';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { MembersApiService } from '../../core/api';
import { OwnMemberData } from '../../model/datamodels';

/** Everything SAM stores about the logged-in member (GDPR right of access). */
@Component({
  selector: 'app-my-data',
  imports: [TableModule, Tag, TranslatePipe],
  templateUrl: './my-data.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyData implements OnInit {
  private readonly api = inject(MembersApiService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly data = signal<OwnMemberData | null>(null);
  protected readonly loading = signal(true);
  /** No member profile is linked to this login (404). */
  protected readonly notLinked = signal(false);

  ngOnInit(): void {
    this.api.own()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (data) => {
          this.data.set(data);
          this.loading.set(false);
        },
        error: (error: HttpErrorResponse) => {
          this.notLinked.set(error.status === 404);
          this.loading.set(false);
        },
      });
  }
}
