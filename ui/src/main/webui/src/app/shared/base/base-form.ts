import { DestroyRef, Directive, EventEmitter, inject, OnChanges, Output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormGroup } from '@angular/forms';
import { Observable } from 'rxjs';
import { TranslationService } from '../../core/translation.service';

@Directive()
export abstract class BaseForm<TEntity, TSaved = void> implements OnChanges {
  protected readonly t = inject(TranslationService);
  protected readonly destroyRef = inject(DestroyRef);

  @Output() saved = new EventEmitter<TSaved>();
  @Output() cancelled = new EventEmitter<void>();

  saving = false;

  abstract form: FormGroup;
  abstract getEntity(): TEntity | null;
  abstract patchFormValues(entity: TEntity): void;
  abstract buildSaveRequest(): Observable<TSaved>;

  get isEdit(): boolean {
    return this.getEntity() !== null;
  }

  ngOnChanges(): void {
    const entity = this.getEntity();
    if (entity) {
      this.patchFormValues(entity);
    } else {
      this.resetForm();
    }
  }

  protected resetForm(): void {
    this.form.reset();
  }

  submit(): void {
    this.onSave();
  }

  protected onSave(): void {
    if (this.form.invalid) return;

    this.saving = true;
    this.buildSaveRequest().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (result) => {
        this.saving = false;
        this.saved.emit(result);
      },
      error: () => {
        this.saving = false;
      },
    });
  }
}
