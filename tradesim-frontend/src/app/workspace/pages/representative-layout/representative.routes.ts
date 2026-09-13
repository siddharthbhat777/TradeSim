import { Routes } from '@angular/router';
import { RepresentativeLayout } from './representative-layout';

export const representativeRoutes: Routes = [
    {
        path: '',
        component: RepresentativeLayout,
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