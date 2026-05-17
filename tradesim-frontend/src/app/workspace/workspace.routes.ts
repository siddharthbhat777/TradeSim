import { Routes } from '@angular/router';

export const workspaceRoutes: Routes = [
    {
        path: '',
        pathMatch: 'full',
        redirectTo: 'dashboard'
    },
    {
        path: 'dashboard',
        loadComponent: () => import('./dashboard/dashboard').then((module) => module.Dashboard),
        title: 'Dashboard'
    },
    {
        path: 'stock',
        loadComponent: () => import('./stock/stock').then((module) => module.Stock),
        title: 'Stock Details'
    },
    {
        path: 'portfolio',
        loadComponent: () => import('./portfolio/portfolio').then((module) => module.Portfolio),
        title: 'Portfolio'
    },
    {
        path: 'position',
        loadComponent: () => import('./position/position').then((module) => module.Position),
        title: 'Position'
    },
    {
        path: 'ipo',
        loadComponent: () => import('./ipo/ipo').then((module) => module.Ipo),
        title: 'IPO Center'
    },
    {
        path: 'account',
        loadComponent: () => import('./account/account').then((module) => module.Account),
        title: 'Account'
    },
    {
        path: 'order',
        loadComponent: () => import('./order/order').then((module) => module.Order),
        title: 'Order'
    },
    {
        path: '**',
        loadComponent: () => import('./shared/not-found/not-found').then((module) => module.NotFound),
        title: 'Not found'
    }
];