import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith('/gym-api') && !request.url.startsWith('/workout-api')) return next(request);
  return from(inject(AuthService).token()).pipe(switchMap(token => next(token
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : request)));
};
