import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';

describe('authInterceptor', () => {
  let client: HttpClient;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { token: vi.fn().mockResolvedValue('student-token') } },
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting()
      ]
    });
    client = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('envia o JWT para o assistant-service', async () => {
    client.post('/assistant-api/api/v1/plans/plan-1/suggestions', {}).subscribe();
    await Promise.resolve();

    const request = http.expectOne('/assistant-api/api/v1/plans/plan-1/suggestions');
    expect(request.request.headers.get('Authorization')).toBe('Bearer student-token');
    request.flush({ id: 'suggestion-1' });
  });

  it('não envia o JWT para URLs fora das APIs do domínio', () => {
    client.get('/assets/config.json').subscribe();
    const request = http.expectOne('/assets/config.json');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({});
  });
});
