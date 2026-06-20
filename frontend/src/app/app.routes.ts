import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./logs/logs').then((m) => m.Logs),
  },
];
