import { TestBed } from '@angular/core/testing';

import { MusiciansService } from './musicians.service';

describe('MusiciansService', () => {
  let service: MusiciansService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(MusiciansService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
