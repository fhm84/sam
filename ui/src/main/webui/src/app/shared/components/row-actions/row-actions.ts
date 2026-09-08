import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { MenuItem } from 'primeng/api';
import { Menu } from 'primeng/menu';
import { Button } from 'primeng/button';

/**
 * Row-actions cell for a p-table: renders the given buttons (projected content) on
 * screens ≥640px, and collapses them into a single kebab menu (built from `items`)
 * below that. Pair with `pFrozenColumn alignFrozen="right"` on the host `<td>`/`<th>`
 * so the column stays pinned while the table scrolls horizontally.
 */
@Component({
  selector: 'app-row-actions',
  imports: [Menu, Button],
  templateUrl: './row-actions.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RowActions {
  items = input.required<MenuItem[]>();
  size = input<'small'>();
}
