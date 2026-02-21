import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { MessageService } from 'primeng/api';
import { TranslationService } from './translation.service';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const messageService = inject(MessageService);
  const t = inject(TranslationService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      messageService.add({
        severity: 'error',
        summary: t.t('errors.requestFailed'),
        life: 5000,
      });
      return throwError(() => error);
    }),
  );
};
