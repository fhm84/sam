import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SheetMusicDetailsComponent } from './sheetMusic-details.component';

describe('SheetMusicDetailsComponent', () => {
  let component: SheetMusicDetailsComponent;
  let fixture: ComponentFixture<SheetMusicDetailsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SheetMusicDetailsComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SheetMusicDetailsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
