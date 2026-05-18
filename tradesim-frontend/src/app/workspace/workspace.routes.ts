import { Routes } from '@angular/router';

export const workspaceRoutes: Routes = [
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
        path: 'stock',
        loadComponent: () => import('./pages/stock/stock').then((module) => module.Stock),
        title: 'Stock Details'
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
];