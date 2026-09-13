import { inject } from '@angular/core';
import { CanMatchFn, Router } from '@angular/router';
import { Role } from '../constants/auth';
import { AuthService } from '../services/auth/auth-service';

export const representativeGuard: CanMatchFn = () => {
    const authService = inject(AuthService);
    const router = inject(Router);
    const user = authService.currentUser();

    if (user?.role === Role.companyRepresentative) {
        return true;
    }

    return router.createUrlTree(['/']);
};