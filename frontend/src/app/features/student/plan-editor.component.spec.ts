import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiService } from '../../core/api.service';
import { WorkoutPlan } from '../../core/api.models';
import { PlanEditorComponent } from './plan-editor.component';

describe('PlanEditorComponent', () => {
  const initial: WorkoutPlan = { id:'plan-1', unitId:'unit-1', name:'Meu plano', status:'DRAFT', version:0,
    days:[], eligibilityCheckAvailable:true, issues:[] };
  let api: {
    plan: ReturnType<typeof vi.fn>; eligibleExercises: ReturnType<typeof vi.fn>;
    savePlan: ReturnType<typeof vi.fn>; activatePlan: ReturnType<typeof vi.fn>; archivePlan: ReturnType<typeof vi.fn>;
  };

  beforeEach(() => {
    api = {
      plan: vi.fn(() => of(initial)),
      eligibleExercises: vi.fn(() => of({content:[{id:'exercise-1',name:'Agachamento',kind:'STRENGTH',primaryMuscleGroups:['PERNAS'],difficulty:'BEGINNER',instructions:''}],page:0,size:100,totalElements:1,totalPages:1})),
      savePlan: vi.fn((plan: WorkoutPlan) => of({...plan, version:plan.version+1})),
      activatePlan: vi.fn((plan: WorkoutPlan) => of({...plan,status:'ACTIVE'})),
      archivePlan: vi.fn((plan: WorkoutPlan) => of({...plan,status:'ARCHIVED'}))
    };
    TestBed.configureTestingModule({
      imports:[PlanEditorComponent],
      providers:[provideRouter([]), {provide:ApiService,useValue:api},
        {provide:ActivatedRoute,useValue:{snapshot:{paramMap:convertToParamMap({id:'plan-1'})}}}]
    });
  });

  it('monta, ordena e envia o agregado manual', () => {
    const fixture=TestBed.createComponent(PlanEditorComponent);
    const component=fixture.componentInstance;
    component.addDay(); component.addItem(0); component.save();
    expect(api.savePlan).toHaveBeenCalledOnce();
    const sent=api.savePlan.mock.calls[0][0] as WorkoutPlan;
    expect(sent.days[0].position).toBe(1);
    expect(sent.days[0].items[0]).toMatchObject({exerciseId:'exercise-1',position:1,sets:3,repetitionMin:8,repetitionMax:12});
  });
});
