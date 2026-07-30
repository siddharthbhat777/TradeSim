import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../shared/components/toast/toast.service';
import { SKIP_ERROR_TOAST } from '../constants/http-context';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toastService = inject(ToastService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      let errorMessage = 'An unexpected error occurred.';

      if (error.error instanceof ErrorEvent) {
        errorMessage = `Network Error: ${error.error.message}`;
      } else {
        const backendMessage = error.error?.message;

        switch (error.status) {
          case 400:
            errorMessage = backendMessage || 'Invalid request. Please check your data.';
            break;
          case 401:
            return throwError(() => error);
          case 403:
            errorMessage = backendMessage || 'You do not have permission to perform this action.';
            break;
          case 404:
            errorMessage = backendMessage || 'The requested resource was not found.';
            break;
          case 409:
            errorMessage = backendMessage || 'This record already exists.';
            break;
          case 500:
            errorMessage = backendMessage || 'Server error. Our team has been notified.';
            break;
        }
      }

      console.error('[Global Error]', errorMessage);

      if (!req.context.get(SKIP_ERROR_TOAST)) {
        toastService.danger(errorMessage);
      }

      return throwError(() => error);
    })
  );
};