import { Routes } from '@angular/router';
import { RepresentativeLayout } from './representative-layout';

export const representativeRoutes: Routes = [
    {
        path: '',
        component: RepresentativeLayout,
        children: [
            {
                path: ':companyId/overview',
                loadComponent: () => import('./overview/overview').then((module) => module.Overview),
                title: 'Company Overview'
            },
            {
                path: ':companyId/listing',
                loadComponent: () => import('./listing/listing').then((module) => module.Listing),
                title: 'Listing'
            },
            {
                path: ':companyId/ipo',
                loadComponent: () => import('./ipo/ipo').then((module) => module.Ipo),
                title: 'IPO'
            },
            {
                path: ':companyId/team',
                loadComponent: () => import('./team/team').then((module) => module.Team),
                title: 'Team'
            }
        ]
    }
];