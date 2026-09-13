import { Routes } from '@angular/router';
import { AdminLayout } from './admin-layout';

export const adminRoutes: Routes = [
    {
        path: '',
        component: AdminLayout,
        children: [
            {
                path: '',
                pathMatch: 'full',
                redirectTo: 'dashboard'
            },
            {
                path: 'dashboard',
                loadComponent: () => import('./dashboard/dashboard').then((module) => module.Dashboard),
                title: 'Dashboard'
            }
        ]
    }
];