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
                redirectTo: 'overview'
            },
            {
                path: 'overview',
                loadComponent: () => import('./overview/overview').then((module) => module.Overview),
                title: 'Company Overview'
            },
            {
                path: 'listing',
                loadComponent: () => import('./listing/listing').then((module) => module.Listing),
                title: 'Listing'
            },
            {
                path: 'ipo',
                loadComponent: () => import('./ipo/ipo').then((module) => module.Ipo),
                title: 'IPO'
            },
            {
                path: 'team',
                loadComponent: () => import('./team/team').then((module) => module.Team),
                title: 'Team'
            }
        ]
    }
];