import { TestBed } from '@angular/core/testing';

import { PartCountFilter, PartCountRow, partCountParams } from './part-count-filter';
import { TranslationService } from '../../core/translation.service';

describe('partCountParams', () => {
  it('encodes filled-in rows as instrument:comparison:parts', () => {
    expect(
      partCountParams([
        { instrument: 'HORN_F', comparison: 'EQ', parts: 4 },
        { instrument: 'OBOE', comparison: 'EQ', parts: 0 },
        { instrument: 'TUBA', comparison: 'GTE', parts: 1 },
      ]),
    ).toEqual(['HORN_F:EQ:4', 'OBOE:EQ:0', 'TUBA:GTE:1']);
  });

  it('leaves out rows without an instrument or a number', () => {
    expect(
      partCountParams([
        { instrument: null, comparison: 'EQ', parts: 2 },
        { instrument: 'HORN_F', comparison: 'LT', parts: null },
      ]),
    ).toEqual([]);
  });
});

describe('PartCountFilter', () => {
  let component: PartCountFilter;
  let emitted: PartCountRow[][];

  const rows = () => component.rows();
  const call = (method: string, ...args: unknown[]) => (component as any)[method](...args);

  beforeEach(async () => {
    await TestBed.overrideComponent(PartCountFilter, { set: { imports: [], template: '' } })
      .configureTestingModule({
        imports: [PartCountFilter],
        providers: [{ provide: TranslationService, useValue: { t: (k: string) => k } }],
      })
      .compileComponents();

    const fixture = TestBed.createComponent(PartCountFilter);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('instruments', [
      { id: 'HORN_F', name: 'Horn', transposition: 'F' },
      { id: 'OBOE', name: 'Oboe', displayName: 'Oboe 1' },
    ]);
    emitted = [];
    component.rows.subscribe((value) => emitted.push(value));
    fixture.detectChanges();
  });

  it('offers the instruments under their display labels', () => {
    expect((component as any).instrumentOptions()).toEqual([
      { label: 'Horn (F)', value: 'HORN_F' },
      { label: 'Oboe 1', value: 'OBOE' },
    ]);
  });

  it('adds an empty "exactly 1" row', () => {
    call('addRow');

    expect(rows()).toEqual([{ instrument: null, comparison: 'EQ', parts: 1 }]);
    expect(emitted).toEqual([rows()]);
  });

  it('changes only the edited row', () => {
    call('addRow');
    call('addRow');

    call('changeRow', 1, { instrument: 'OBOE', comparison: 'LTE' });

    expect(rows()).toEqual([
      { instrument: null, comparison: 'EQ', parts: 1 },
      { instrument: 'OBOE', comparison: 'LTE', parts: 1 },
    ]);
  });

  it('removes the row at the index', () => {
    component.rows.set([
      { instrument: 'HORN_F', comparison: 'EQ', parts: 4 },
      { instrument: 'OBOE', comparison: 'EQ', parts: 0 },
    ]);

    call('removeRow', 0);

    expect(rows()).toEqual([{ instrument: 'OBOE', comparison: 'EQ', parts: 0 }]);
    expect(emitted.at(-1)).toEqual(rows());
  });
});
