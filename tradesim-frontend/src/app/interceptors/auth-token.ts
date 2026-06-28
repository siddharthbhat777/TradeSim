import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { environment } from '../../environment/environment';
import { AuthService } from '../services/auth-service/auth-service';

export const authTokenInterceptor: HttpInterceptorFn = (req, next) => {
    const authService = inject(AuthService);

    const isApiRequest = req.url.startsWith(environment.apiBaseURL);
    const isAuthRequest = req.url.startsWith(`${environment.apiBaseURL}/auth`);
    const token = authService.getAccessToken();

    const authReq = isApiRequest && !isAuthRequest && token ? req.clone({
        setHeaders: {
            Authorization: `Bearer ${token}`
        }
    }) : req;

    return next(authReq).pipe(
        catchError((error) => {
            const shouldRefresh = error instanceof HttpErrorResponse && error.status === 401 && isApiRequest && !isAuthRequest;

            if (!shouldRefresh) {
                return throwError(() => error);
            }

            return authService.refreshSession().pipe(
                switchMap(() => {
                    const newToken = authService.getAccessToken();

                    if (!newToken) {
                        return throwError(() => error);
                    }

                    return next(req.clone({
                        setHeaders: {
                            Authorization: `Bearer ${newToken}`
                        }
                    }));
                })
            );
        })
    );
};