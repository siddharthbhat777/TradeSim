import { Routes } from '@angular/router';
import { Workspace } from './workspace';
import { userGuard } from '../guards/user';
import { adminGuard } from '../guards/admin';
import { representativeGuard } from '../guards/representative';

export const workspaceRoutes: Routes = [
    {
        path: '',
        component: Workspace,
        children: [
            {
                path: '',
                pathMatch: 'full',
                redirectTo: 'portfolio'
            },
            {
                path: 'portfolio',
                canMatch: [userGuard],
                loadComponent: () => import('./pages/portfolio/portfolio').then((module) => module.Portfolio),
                title: 'Portfolio'
            },
            {
                path: 'market',
                canMatch: [userGuard],
                loadComponent: () => import('./pages/market/market').then((module) => module.Market),
                title: 'Market'
            },
            {
                path: 'ipo',
                canMatch: [userGuard],
                loadComponent: () => import('./pages/ipo/ipo').then((module) => module.Ipo),
                title: 'IPO Center'
            },
            {
                path: 'order',
                canMatch: [userGuard],
                loadComponent: () => import('./pages/order/order').then((module) => module.Order),
                title: 'Order'
            },
            {
                path: 'wallet',
                canMatch: [userGuard],
                loadComponent: () => import('./pages/wallet/wallet').then((module) => module.Wallet),
                title: 'Wallet'
            },
            {
                path: 'settings',
                loadComponent: () => import('./pages/settings/settings').then((module) => module.Settings),
                title: 'Settings'
            },
            {
                path: 'admin',
                canMatch: [adminGuard],
                loadChildren: () => import('./pages/admin-layout/admin.routes').then((module) => module.adminRoutes)
            },
            {
                path: 'representative',
                canMatch: [representativeGuard],
                loadChildren: () => import('./pages/representative-layout/representative.routes').then((module) => module.representativeRoutes)
            },
            {
                path: '**',
                loadComponent: () => import('./pages/not-found/not-found').then((module) => module.NotFound),
                title: 'Not found'
            }
        ]
    }
];