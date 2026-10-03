import { ApplicationConfig, ErrorHandler, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authTokenInterceptor } from './interceptors/auth-token-interceptor';
import { AuthService } from './services/auth/auth-service';
import { ForexService } from './services/forex/forex-service';
import { catchError, firstValueFrom, of } from 'rxjs';
import { GlobalErrorHandler } from './services/global-error-handler';
import { loadingInterceptor } from './interceptors/loading-interceptor';
import { errorInterceptor } from './interceptors/error-interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    { provide: ErrorHandler, useClass: GlobalErrorHandler },
    provideRouter(routes),
    provideHttpClient(withInterceptors([authTokenInterceptor, loadingInterceptor, errorInterceptor])),
    provideAppInitializer(() => {
      const authService = inject(AuthService);
      const forexService = inject(ForexService);

      return Promise.all([
        firstValueFrom(authService.refreshSession().pipe(catchError(() => of(null)))),
        firstValueFrom(forexService.fetchCountries().pipe(catchError(() => of([]))))
      ]);
    })
  ]
};