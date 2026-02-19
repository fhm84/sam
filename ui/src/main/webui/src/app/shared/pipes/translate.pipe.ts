import { Pipe, PipeTransform, inject } from '@angular/core';
import { TranslationService } from '../../core/translation.service';

@Pipe({
  name: 'translate',
  pure: false,
})
export class TranslatePipe implements PipeTransform {
  private readonly i18n = inject(TranslationService);

  transform(key: string): string {
    return this.i18n.t(key);
  }
}
