import { Component, Input, OnInit, OnChanges, SimpleChanges, forwardRef, ElementRef, Self, Optional } from '@angular/core';
import { FormsModule, FormControl, ReactiveFormsModule, NG_VALUE_ACCESSOR, ControlValueAccessor, NgControl } from '@angular/forms';
import { AsyncPipe, CommonModule } from '@angular/common';
import { map, Observable, startWith, Subject } from 'rxjs';
import { Musician } from '../../model/datamodels';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatFormField, MatInputModule } from '@angular/material/input';
import { MusiciansService } from '../../services/musicians.service';
import { FocusMonitor } from '@angular/cdk/a11y';
import { MatFormFieldControl } from '@angular/material/form-field';

@Component({
  selector: 'app-musician-autocomplete',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatInputModule,
    MatAutocompleteModule,
    AsyncPipe
  ],
  templateUrl: './musician-autocomplete.component.html',
  styleUrl: './musician-autocomplete.component.scss',
  providers: [
    {
      provide: MatFormFieldControl,
      useExisting: MusicianAutocompleteComponent,
    },
    MusiciansService
  ]
})
export class MusicianAutocompleteComponent implements OnInit, OnChanges, ControlValueAccessor, MatFormFieldControl<Musician> {

  static nextId = 0;

  @Input() initialValue?: string = '';
  @Input() placeholder: string = '';
  @Input() required = false;
  @Input() disabled = false;

  musicianControl = new FormControl<string | Musician>('');

  musicianOptions!: Musician[];
  filteredMusicians!: Observable<Musician[]>;

  stateChanges = new Subject<void>();
  focused = false;
  controlType = 'musician-autocomplete';
  id = `musician-autocomplete-${MusicianAutocompleteComponent.nextId++}`;
  describedBy = '';

  constructor(
    private musiciansService: MusiciansService,
    private fm: FocusMonitor,
    private elRef: ElementRef<HTMLElement>,
    @Optional() @Self() public ngControl: NgControl
  ) {
    if (this.ngControl != null) {
      this.ngControl.valueAccessor = this;
    }

    this.fm.monitor(this.elRef.nativeElement, true).subscribe((origin) => {
      this.focused = !!origin;
      this.stateChanges.next();
    });
  }

  async ngOnInit(): Promise<void> {
    await this.loadMusicianOptions();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['initialValue'] && !changes['initialValue'].isFirstChange()) {
      this.initializeControl();
    }
  }

  ngOnDestroy(): void {
    this.stateChanges.complete();
  }

  private async loadMusicianOptions(): Promise<void> {
    this.musiciansService.getMusicians().subscribe((musicians) => {
      this.musicianOptions = musicians || [];
      // Initialize the control with the initial value if provided
      this.initializeControl();
    });
  }

  private initializeControl(): void {
    if (this.initialValue) {
      const selectedMusician = this._filter(this.initialValue)?.pop();
      if (selectedMusician) {
        this.musicianControl.setValue(selectedMusician);
      }
    }
    this.filteredMusicians = this.musicianControl.valueChanges.pipe(
      startWith(''),
      map(value => {
        const name = typeof value === 'string' ? value : value?.name;
        return name ? this._filter(name as string) : this.musicianOptions?.slice();
      }),
    );

    // Notify parent of the initial value
    this.musicianControl.valueChanges.subscribe(value => {
      this.onChange(value);
    });
  }

  private _filter(name?: string): Musician[] {
    if (name) {
      const filterValue = name.toLowerCase();
      return this.musicianOptions?.filter(option => option.name.toLowerCase().includes(filterValue));
    } else {
      return this.musicianOptions || [];
    }
  }

  displayFn(musician: Musician): string {
    return musician && musician.name ? musician.name : '';
  }

  // MatFormFieldControl implementation
  get errorState(): boolean {
    return this.ngControl?.invalid && this.ngControl?.touched || false;
  }

  onChange = (value: any) => {};
  onTouched = () => {};
  
  get value(): Musician | null {
    return this.musicianControl.value as Musician;
  }

  set value(musician: Musician | null) {
    this.musicianControl.setValue(musician);
    this.stateChanges.next();
  }

  get empty(): boolean {
    return !this.musicianControl.value;
  }

  get shouldLabelFloat(): boolean {
    return this.focused || !this.empty;
  }

  setDescribedByIds(ids: string[]): void {
    this.describedBy = ids.join(' ');
  }

  onContainerClick(event: MouseEvent): void {
    if ((event.target as Element).tagName.toLowerCase() !== 'input') {
      this.elRef.nativeElement.querySelector('input')!.focus();
    }
  }

  // ControlValueAccessor methods
  writeValue(value: any): void {
    this.musicianControl.setValue(value);
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    isDisabled ? this.musicianControl.disable() : this.musicianControl.enable();
    this.stateChanges.next();
  }
}