import { Routes } from '@angular/router';
import { LandingPage } from './marketing/pages/landing-page/landing-page';
import { authGuard } from './guards/auth';

export const routes: Routes = [
    {
        path: '',
        component: LandingPage,
        title: 'TradeSim'
    },
    {
        path: 'app',
        canMatch: [authGuard],
        loadChildren: () => import('./workspace/workspace.routes').then((module) => module.workspaceRoutes)
    },
    {
        path: 'docs',
        loadComponent: () => import('./design-system/design-system').then((module) => module.DesignSystem)
    },
    {
        path: '**',
        redirectTo: ''
    }
];