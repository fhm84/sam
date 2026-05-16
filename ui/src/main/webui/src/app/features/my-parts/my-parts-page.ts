import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { TableLazyLoadEvent, TableModule } from 'primeng/table';
import { Tag } from 'primeng/tag';
import { Skeleton } from 'primeng/skeleton';
import { MyPartsApiService } from '../../core/api/my-parts-api.service';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { TranslationService } from '../../core/translation.service';
import { SheetWithMyParts } from '../../model/datamodels';

@Component({
  selector: 'app-my-parts-page',
  imports: [TableModule, Tag, RouterLink, Skeleton, TranslatePipe],
  templateUrl: './my-parts-page.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MyPartsPage {
  protected readonly t = inject(TranslationService);
  private readonly api = inject(MyPartsApiService);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly sheets = signal<SheetWithMyParts[]>([]);
  protected readonly totalRecords = signal(0);
  protected readonly loading = signal(true);

  protected rows = 20;
  protected currentPage = 0;

  protected onLazyLoad(event: TableLazyLoadEvent): void {
    this.currentPage = Math.floor((event.first ?? 0) / this.rows);
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.api.find({ page: this.currentPage, size: this.rows })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.sheets.set(res.data ?? []);
          this.totalRecords.set(res.totalCount ?? 0);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  protected partLabel(instr: NonNullable<SheetWithMyParts['myInstrumentations']>[number]): string {
    const name = instr.instrument?.displayName ?? instr.instrument?.name ?? '';
    return instr.partLabel ? `${name} ${instr.partLabel}` : name;
  }
}
