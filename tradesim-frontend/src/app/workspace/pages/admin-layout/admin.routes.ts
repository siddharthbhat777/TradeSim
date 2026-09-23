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
                loadComponent: () => import('./dashboard/dashboard').then((m) => m.Dashboard),
                title: 'Admin Dashboard'
            },
            {
                path: 'approvals',
                loadComponent: () => import('./approvals/approvals').then((m) => m.Approvals),
                title: 'Approvals Center'
            },
            {
                path: 'market',
                loadComponent: () => import('./market/market').then((m) => m.Market),
                title: 'Market Operations'
            },
            {
                path: 'companies',
                loadComponent: () => import('./companies/companies').then((m) => m.Companies),
                title: 'Company Directory'
            },
            {
                path: 'companies/:id',
                loadComponent: () => import('./companies/company-details/company-details').then((m) => m.CompanyDetails),
                title: 'Company Details'
            },
            {
                path: 'users',
                loadComponent: () => import('./users/users').then((m) => m.Users),
                title: 'User Management'
            }
        ]
    }
];