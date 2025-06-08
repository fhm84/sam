import { ComponentFixture, TestBed } from '@angular/core/testing';

import { MusicianAutocompleteComponent } from './musician-autocomplete.component';

describe('MusicianAutocompleteComponent', () => {
  let component: MusicianAutocompleteComponent;
  let fixture: ComponentFixture<MusicianAutocompleteComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MusicianAutocompleteComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(MusicianAutocompleteComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
