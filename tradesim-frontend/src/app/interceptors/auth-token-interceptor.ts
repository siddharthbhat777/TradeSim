import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { BehaviorSubject, catchError, filter, switchMap, take, throwError } from 'rxjs';
import { environment } from '../../environment/environment';
import { AuthService } from '../services/auth/auth-service';

let isRefreshing = false;
const refreshTokenSubject = new BehaviorSubject<string | null>(null);

export const authTokenInterceptor: HttpInterceptorFn = (req, next) => {
    const authService = inject(AuthService);

    const isApiRequest = req.url.startsWith(environment.apiBaseURL);
    const isAuthRequest = req.url.startsWith(`${environment.apiBaseURL}/auth`);
    const token = authService.getAccessToken();

    const addTokenHeader = (request: any, tokenString: string | null) => {
        return request.clone({
            setHeaders: { Authorization: `Bearer ${tokenString}` }
        });
    };

    const authReq = isApiRequest && !isAuthRequest && token ? addTokenHeader(req, token) : req;

    return next(authReq).pipe(
        catchError((error) => {
            if (error instanceof HttpErrorResponse && error.status === 401 && isApiRequest && !isAuthRequest) {
                if (!isRefreshing) {
                    isRefreshing = true;
                    refreshTokenSubject.next(null);

                    return authService.refreshSession().pipe(
                        switchMap(() => {
                            isRefreshing = false;
                            const newToken = authService.getAccessToken();
                            refreshTokenSubject.next(newToken);
                            return next(addTokenHeader(req, newToken));
                        }),
                        catchError((refreshError) => {
                            isRefreshing = false;
                            authService.logout();
                            return throwError(() => refreshError);
                        })
                    );
                } else {
                    return refreshTokenSubject.pipe(
                        filter(result => result !== null),
                        take(1),
                        switchMap((newToken) => {
                            return next(addTokenHeader(req, newToken));
                        })
                    );
                }
            }

            return throwError(() => error);
        })
    );
};