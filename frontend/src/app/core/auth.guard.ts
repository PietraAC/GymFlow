import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  if (auth.authenticated()) return true;
  void auth.login();
  return false;
};

export const roleGuard = (role: string): CanActivateFn => () =>
  inject(AuthService).hasRole(role) ? true : inject(Router).createUrlTree(['/']);
