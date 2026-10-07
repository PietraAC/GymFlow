import { HttpErrorResponse } from '@angular/common/http';
import { describe, expect, it } from 'vitest';
import { errorMessage } from './error-message';

describe('errorMessage', () => {
  it('prioriza o detalhe de ProblemDetail', () => {
    const error = new HttpErrorResponse({ status: 409, error: { title: 'Conflito', detail: 'Plano desatualizado' } });
    expect(errorMessage(error)).toBe('Plano desatualizado');
  });
});
