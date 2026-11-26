import { ChangeDetectionStrategy, Component, computed, inject, input, model } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Button } from '@openng/optimus-ui/button';
import { InputNumber } from '@openng/optimus-ui/inputnumber';
import { Select } from '@openng/optimus-ui/select';
import { Tooltip } from '@openng/optimus-ui/tooltip';
import { TranslationService } from '../../core/translation.service';
import { Instrument } from '../../model/datamodels';
import { TranslatePipe } from '../../shared/pipes/translate.pipe';
import { instrumentLabel } from '../../shared/utils/format.utils';

/** Comparisons the server's `partCount` filter understands. */
export type PartCountComparison = 'EQ' | 'NEQ' | 'LT' | 'LTE' | 'GT' | 'GTE';

/** One "number of parts for an instrument" row; a row counts once instrument and parts are set. */
export interface PartCountRow {
  instrument: string | null;
  comparison: PartCountComparison;
  parts: number | null;
}

/** `partCount` query values (`<instrument>:<comparison>:<parts>`) of the filled-in rows. */
export function partCountParams(rows: readonly PartCountRow[]): string[] {
  return rows.flatMap(({ instrument, comparison, parts }) =>
    instrument && parts !== null ? [`${instrument}:${comparison}:${parts}`] : [],
  );
}

/**
 * Editable list of part count rows for the sheets filter panel ("exactly 4 horns", "no oboes").
 * Holds no state of its own: the rows live in the parent, so they survive closing the panel.
 */
@Component({
  selector: 'app-part-count-filter',
  imports: [FormsModule, Button, InputNumber, Select, Tooltip, TranslatePipe],
  templateUrl: './part-count-filter.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PartCountFilter {
  protected readonly t = inject(TranslationService);

  readonly instruments = input<Instrument[]>([]);
  readonly rows = model<PartCountRow[]>([]);

  protected readonly instrumentOptions = computed(() =>
    this.instruments().map((instrument) => ({ label: instrumentLabel(instrument), value: instrument.id })),
  );

  protected readonly comparisonOptions: { label: string; value: PartCountComparison }[] = (
    [
      ['EQ', '='],
      ['NEQ', '≠'],
      ['LT', '<'],
      ['LTE', '≤'],
      ['GT', '>'],
      ['GTE', '≥'],
    ] as const
  ).map(([value, label]) => ({ value, label }));

  protected addRow(): void {
    this.rows.set([...this.rows(), { instrument: null, comparison: 'EQ', parts: 1 }]);
  }

  protected removeRow(index: number): void {
    this.rows.set(this.rows().filter((_, i) => i !== index));
  }

  protected changeRow(index: number, change: Partial<PartCountRow>): void {
    this.rows.set(this.rows().map((row, i) => (i === index ? { ...row, ...change } : row)));
  }
}
