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
                redirectTo: 'portfolio'
            },
            {
                path: 'portfolio',
                loadComponent: () => import('./pages/portfolio/portfolio').then((module) => module.Portfolio),
                title: 'Portfolio'
            },
            {
                path: 'market',
                loadComponent: () => import('./pages/market/market').then((module) => module.Market),
                title: 'Market'
            },
            {
                path: 'ipo',
                loadComponent: () => import('./pages/ipo/ipo').then((module) => module.Ipo),
                title: 'IPO Center'
            },
            {
                path: 'order',
                loadComponent: () => import('./pages/order/order').then((module) => module.Order),
                title: 'Order'
            },
            {
                path: 'wallet',
                loadComponent: () => import('./pages/wallet/wallet').then((module) => module.Wallet),
                title: 'Wallet'
            },
            {
                path: 'account',
                loadComponent: () => import('./pages/account/account').then((module) => module.Account),
                title: 'Account'
            },
            {
                path: '**',
                loadComponent: () => import('./pages/not-found/not-found').then((module) => module.NotFound),
                title: 'Not found'
            }
        ]
    }
];