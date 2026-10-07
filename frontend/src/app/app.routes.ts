import { Routes } from '@angular/router';
import { authGuard, roleGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: '', loadComponent: () => import('./features/home/home.component').then(m => m.HomeComponent) },
  { path: 'admin', canActivate: [authGuard, roleGuard('GYM_ADMIN')], loadComponent: () => import('./features/admin/admin.component').then(m => m.AdminComponent) },
  { path: 'student/profile', canActivate: [authGuard, roleGuard('STUDENT')], loadComponent: () => import('./features/student/profile.component').then(m => m.ProfileComponent) },
  { path: 'student/plans', canActivate: [authGuard, roleGuard('STUDENT')], loadComponent: () => import('./features/student/plans.component').then(m => m.PlansComponent) },
  { path: 'student/plans/:id', canActivate: [authGuard, roleGuard('STUDENT')], loadComponent: () => import('./features/student/plan-editor.component').then(m => m.PlanEditorComponent) },
  { path: '**', redirectTo: '' }
];
