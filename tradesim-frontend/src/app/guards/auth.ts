import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';

import { AuthStatus } from '../constants/auth';
import { AuthService } from '../services/auth/auth-service';

export const authGuard: CanMatchFn = () => {
    const authService = inject(AuthService);
    const router = inject(Router);

    if (authService.isLoggedIn()) {
        return true;
    }

    authService.showAuthDialog.set({
        show: true,
        status: AuthStatus.Login
    });

    return router.createUrlTree(['/']);
};