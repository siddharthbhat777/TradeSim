import { ApplicationConfig, ErrorHandler, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authTokenInterceptor } from './interceptors/auth-token';
import { AuthService } from './services/auth-service/auth-service';
import { catchError, firstValueFrom, of } from 'rxjs';
import { GlobalErrorHandler } from './services/global-error-handler';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    { provide: ErrorHandler, useClass: GlobalErrorHandler },
    provideRouter(routes),
    provideHttpClient(withInterceptors([authTokenInterceptor])),
    provideAppInitializer(() => {
      const authService = inject(AuthService);

      return firstValueFrom(
        authService.refreshSession().pipe(
          catchError(() => of(null))
        )
      );
    })
  ]
};