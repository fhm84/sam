import { ComponentFixture, TestBed } from '@angular/core/testing';

import { SheetMusicAddEditComponent } from './sheetMusic-add-edit.component';

describe('SheetAddEditComponent', () => {
  let component: SheetMusicAddEditComponent;
  let fixture: ComponentFixture<SheetMusicAddEditComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SheetMusicAddEditComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(SheetMusicAddEditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
