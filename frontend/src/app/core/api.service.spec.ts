import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { describe, expect, it, beforeEach, afterEach } from 'vitest';
import { ApiService } from './api.service';
import { WorkoutPlan } from './api.models';

describe('ApiService', () => {
  let api: ApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [ApiService, provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(ApiService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('envia versão e agregado ao salvar um rascunho', () => {
    const plan: WorkoutPlan = { id:'plan-1', unitId:'unit-1', name:'Treino A', status:'DRAFT', version:4,
      days:[], inventoryRevalidationRequired:false, eligibilityCheckAvailable:true, issues:[] };
    api.savePlan(plan).subscribe();
    const request = http.expectOne('/workout-api/api/v1/me/plans/plan-1');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body.version).toBe(4);
    expect(request.request.body.days).toEqual([]);
    request.flush(plan);
  });

  it('consulta somente o catálogo elegível da unidade', () => {
    api.eligibleExercises('unit-1', 'STRENGTH').subscribe();
    const request = http.expectOne(value => value.url === '/gym-api/api/v1/units/unit-1/eligible-exercises');
    expect(request.request.params.get('kind')).toBe('STRENGTH');
    expect(request.request.params.get('size')).toBe('100');
    request.flush({content:[],page:0,size:100,totalElements:0,totalPages:0});
  });

  it('solicita uma sugestão estruturada sem texto livre do navegador', () => {
    api.generatePlanSuggestion('plan-1').subscribe();
    const request = http.expectOne('/assistant-api/api/v1/plans/plan-1/suggestions');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({});
    request.flush({id:'suggestion-1'});
  });
});
