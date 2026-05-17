import { Routes } from '@angular/router';
import { LandingPage } from './marketing/landing-page/landing-page';

export const routes: Routes = [
    {
        path: '',
        component: LandingPage,
        title: 'TradeSim'
    },
    {
        path: 'app',
        loadChildren: () => import('./workspace/workspace.routes').then((module) => module.workspaceRoutes)
    },
    {
        path: '**',
        redirectTo: ''
    }
];