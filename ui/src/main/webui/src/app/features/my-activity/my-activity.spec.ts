import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';

import { MyActivity } from './my-activity';
import { EventLogsApiService } from '../../core/api';
import { EventLogEntry } from '../../model/datamodels';

function page(data: EventLogEntry[], totalCount = data.length) {
  return { data, totalCount, page: 0, size: data.length };
}

describe('MyActivity', () => {
  let component: MyActivity;
  let api: { own: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    api = { own: vi.fn().mockReturnValue(of(page([{ id: 'e1' as any, eventType: 'DOCUMENT_DOWNLOAD' }], 30))) };

    await TestBed.overrideComponent(MyActivity, { set: { imports: [], template: '<div></div>' } })
      .configureTestingModule({
        imports: [MyActivity],
        providers: [{ provide: EventLogsApiService, useValue: api }],
      })
      .compileComponents();

    component = TestBed.createComponent(MyActivity).componentInstance;
  });

  it('loads the first page of the own entries on init', () => {
    component.ngOnInit();

    expect(api.own).toHaveBeenCalledWith(0, 25);
    expect((component as any).entries()).toHaveLength(1);
    expect((component as any).totalCount()).toBe(30);
    expect((component as any).loading()).toBe(false);
  });

  it('loads the page the table switched to', () => {
    (component as any).onPage(1);

    expect(api.own).toHaveBeenLastCalledWith(1, 25);
    expect((component as any).page()).toBe(1);
  });

  it('stops loading when the request fails', () => {
    api.own.mockReturnValue(throwError(() => new Error('offline')));

    component.ngOnInit();

    expect((component as any).loading()).toBe(false);
    expect((component as any).entries()).toEqual([]);
  });
});
