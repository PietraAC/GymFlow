import { HttpErrorResponse } from '@angular/common/http';
import { ProblemDetail } from './api.models';
export function errorMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    const problem = error.error as ProblemDetail | undefined;
    return problem?.detail ?? problem?.title ?? `Falha na comunicação (${error.status || 'sem resposta'})`;
  }
  return 'Não foi possível concluir a operação.';
}
