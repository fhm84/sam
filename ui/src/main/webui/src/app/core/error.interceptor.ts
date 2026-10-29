import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { MessageService } from '@openng/optimus-ui/api';
import { TranslationService } from './translation.service';

/**
 * The server's own explanation of a refused request (400 validation, 409 conflict such as "still
 * credited on 2 sheets"), from its `ErrorResponse` body. Other errors have no message meant for users.
 */
export function serverMessage(error: HttpErrorResponse): string | undefined {
  if (error.status !== 400 && error.status !== 409) return undefined;
  const message = (error.error as { message?: unknown } | null)?.message;
  return typeof message === 'string' && message.trim() ? message : undefined;
}

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const messageService = inject(MessageService);
  const t = inject(TranslationService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      messageService.add({
        severity: 'error',
        summary: t.t('errors.requestFailed'),
        detail: serverMessage(error),
        life: 5000,
      });
      return throwError(() => error);
    }),
  );
};
