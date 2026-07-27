import { ErrorHandler, Injectable, inject, Injector } from '@angular/core';
import { Router } from '@angular/router';
import { DialogService } from '../shared/components/dialog/dialog.service';

@Injectable()
export class GlobalErrorHandler implements ErrorHandler {
  private readonly dialogService = inject(DialogService);
  private readonly injector = inject(Injector);

  handleError(error: unknown): void {
    console.error(error);

    const errorMessage = error instanceof Error ? error.message : 'A fatal error occurred. Please refresh the page to continue.';

    this.dialogService.open({
      title: 'Application Error',
      message: errorMessage,
      primaryLabel: 'Reload Page',
      secondaryLabel: 'Go Back',
      isBlocking: true,
      showClose: true,
      onPrimary: () => window.location.reload(),
      onSecondary: () => {
        const router = this.injector.get(Router);
        router.navigate(['/']);
      }
    });
  }
}