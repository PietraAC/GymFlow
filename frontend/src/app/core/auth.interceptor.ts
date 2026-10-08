import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { from, switchMap } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const protectedApi = ['/gym-api', '/workout-api', '/assistant-api']
    .some(prefix => request.url.startsWith(prefix));
  if (!protectedApi) return next(request);
  return from(inject(AuthService).token()).pipe(switchMap(token => next(token
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : request)));
};
