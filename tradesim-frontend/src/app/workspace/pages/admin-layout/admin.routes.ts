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
                title: 'Admin Dashboard'
            },
            {
                path: 'approvals',
                loadComponent: () => import('./approvals/approvals').then((module) => module.Approvals),
                title: 'Approvals Center'
            },
            {
                path: 'market',
                loadComponent: () => import('./market/market').then((module) => module.Market),
                title: 'Market Operations'
            },
            {
                path: 'companies',
                loadComponent: () => import('./companies/companies').then((module) => module.Companies),
                title: 'Company Directory'
            },
            {
                path: 'companies/:id',
                loadComponent: () => import('./companies/company-details/company-details').then((module) => module.CompanyDetails),
                title: 'Company Details'
            },
            {
                path: 'users',
                loadComponent: () => import('./users/users').then((module) => module.Users),
                title: 'User Management'
            }
        ]
    }
];