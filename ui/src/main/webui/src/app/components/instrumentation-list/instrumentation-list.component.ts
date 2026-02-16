import { Component, Input, OnInit, SimpleChanges, ViewChild } from '@angular/core';
import { FormBuilder, FormGroup, FormArray, Validators, AbstractControl, ValidationErrors, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatFormField, MatInputModule } from '@angular/material/input';
import { CommonModule } from '@angular/common';
import { MatSelectModule } from '@angular/material/select';
import { MatRadioModule } from '@angular/material/radio';
import { MatCardModule } from '@angular/material/card';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTable, MatTableModule } from '@angular/material/table';
import { ConfirmDialogComponent } from '../confirm-dialog/confirm-dialog.component';
import { MatTooltipModule } from '@angular/material/tooltip';
import { A } from '@angular/cdk/keycodes';

@Component({
    selector: 'app-instrumentation-list',
    imports: [
        CommonModule,
        MatTableModule,
        MatInputModule,
        MatSelectModule,
        MatTooltipModule,
        MatFormFieldModule,
        MatButtonModule,
        MatIconModule,
        FormsModule,
        ReactiveFormsModule
    ],
    templateUrl: './instrumentation-list.component.html',
    styleUrls: ['./instrumentation-list.component.scss']
})
export class InstrumentationListComponent implements OnInit {

    @Input() instrumentationsData: any[] = []; // Accepts array of instrumentations
    @Input() editMode = false; // global edit mode

    instrumentationForm: FormGroup;
    @ViewChild(MatTable) table!: MatTable<any>;

    displayedColumns: string[] = [];
    editingIndex: number | null = null; // for inline editing

    constructor(private fb: FormBuilder, private dialog: MatDialog) {
        this.instrumentationForm = this.fb.group({
            instrumentations: this.fb.array([])
        });
        this.setDisplayedColumns();
    }

    ngOnInit(): void {
        this.setInstrumentations(this.instrumentationsData);
    }

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['editMode']) {
            this.setDisplayedColumns();
            this.editingIndex = null; // reset inline editing if switching modes
        }
        if (changes['instrumentationsData'] && !changes['instrumentationsData'].firstChange) {
            this.setInstrumentations(this.instrumentationsData);
        }
    }

    setDisplayedColumns() {
        this.displayedColumns = this.editMode
            ? ['instrumentName', 'partLabel', 'transposition', 'clef', 'notationType', 'attachments', 'actions']
            : ['instrumentName', 'partLabel', 'transposition', 'clef', 'notationType', 'attachments', 'actions'];
    }

    /** Extract instrument fields from the nested instrument object. */
    private extractInstrumentFields(instr: any) {
        return {
            instrumentName: instr.instrument?.name ?? instr.instrumentName,
            transposition: instr.instrument?.transposition ?? instr.transposition,
            clef: instr.instrument?.clef ?? instr.clef,
        };
    }

    setInstrumentations(data: any[]) {
        const array = this.instrumentationForm.get('instrumentations') as FormArray;
        array.clear();
        data.forEach(instr => {
            const fields = this.extractInstrumentFields(instr);
            array.push(this.fb.group({
                instrumentName: [fields.instrumentName, [Validators.required, Validators.maxLength(100)]],
                partLabel: [instr.partLabel],
                transposition: [fields.transposition],
                clef: [fields.clef],
                notationType: [instr.notationType],
                attachments: [instr.attachments || []],
                dirty: [false]
            }));
        });
        if (this.table) this.table.renderRows();
    }

    get instrumentations(): FormArray {
        return this.instrumentationForm.get('instrumentations') as FormArray;
    }

    addInstrumentation(instr?: any) {
        const fields = instr ? this.extractInstrumentFields(instr) : { instrumentName: '', transposition: 'C', clef: 'TREBLE' };
        const group = this.fb.group({
            instrumentName: [fields.instrumentName || '', [Validators.required, Validators.maxLength(100)]],
            partLabel: [instr?.partLabel || ''],
            transposition: [fields.transposition || 'C'],
            clef: [fields.clef || 'TREBLE'],
            notationType: [instr?.notationType || 'STANDARD'],
            attachments: [instr?.attachments || []],
            dirty: [false] // track unsaved state
        })
        group.valueChanges.subscribe(() => group.get('dirty')?.setValue(true, { emitEvent: false }));
        this.instrumentations.push(group);
        if (this.table) this.table.renderRows();
    }

    transpositionValidator(control: AbstractControl): ValidationErrors | null {
        const value = control.value;
        if (!value) return null;
        const pattern = /^([+-]?\d+)?$/;
        return pattern.test(value) ? null : { invalidTransposition: true };
    }

    removeInstrumentation(index: number) {
        const dialogRef = this.dialog.open(ConfirmDialogComponent, {
            data: {
                title: 'Eintrag löschen?',
                message: 'Möchtest du diesen Instrumentierungseintrag wirklich löschen?'
            }
        });

        dialogRef.afterClosed().subscribe(result => {
            if (result === true) {
                this.instrumentations.removeAt(index);
                if (this.table) this.table.renderRows();
                if (this.editingIndex === index) this.editingIndex = null;
            }
        });
    }

    startEdit(index: number) {
        if (!this.editMode) {
            this.editingIndex = index;
        }
    }

    cancelEdit() {
        this.editingIndex = null;
    }

    saveRow(index: number) {
        this.instrumentations.at(index).markAsPristine();
        this.editingIndex = null;
    }

    saveChanges() {
        if (this.instrumentationForm.invalid) return;
        // TODO: send instrumentationForm.value to backend
        // Save all changes (global edit mode)
        this.instrumentations.controls.forEach(c => c.get('dirty')?.setValue(false));
        this.instrumentationForm.markAsPristine();
    }
}
