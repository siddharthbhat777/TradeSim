import { Routes } from '@angular/router';
import { Workspace } from './workspace';

export const workspaceRoutes: Routes = [
    {
        path: '',
        component: Workspace,
        children: [
            {
                path: '',
                pathMatch: 'full',
                redirectTo: 'dashboard'
            },
            {
                path: 'dashboard',
                loadComponent: () => import('./pages/dashboard/dashboard').then((module) => module.Dashboard),
                title: 'Dashboard'
            },
            {
                path: 'market',
                loadComponent: () => import('./pages/market/market').then((module) => module.Market),
                title: 'Market'
            },
            {
                path: 'portfolio',
                loadComponent: () => import('./pages/portfolio/portfolio').then((module) => module.Portfolio),
                title: 'Portfolio'
            },
            {
                path: 'position',
                loadComponent: () => import('./pages/position/position').then((module) => module.Position),
                title: 'Position'
            },
            {
                path: 'ipo',
                loadComponent: () => import('./pages/ipo/ipo').then((module) => module.Ipo),
                title: 'IPO Center'
            },
            {
                path: 'account',
                loadComponent: () => import('./pages/account/account').then((module) => module.Account),
                title: 'Account'
            },
            {
                path: 'order',
                loadComponent: () => import('./pages/order/order').then((module) => module.Order),
                title: 'Order'
            },
            {
                path: '**',
                loadComponent: () => import('./pages/not-found/not-found').then((module) => module.NotFound),
                title: 'Not found'
            }
        ]
    }
];