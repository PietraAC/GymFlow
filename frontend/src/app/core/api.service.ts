import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { AssistantSuggestion, EquipmentType, Exercise, Goal, Gym, GymUnit, Page, PlanSummary, StudentProfile, UnitEquipment, WorkoutPlan } from './api.models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  constructor(private readonly http: HttpClient) {}
  gyms() { return this.http.get<Page<Gym>>('/gym-api/api/v1/gyms', { params: { size: 100 } }); }
  createGym(name: string) { return this.http.post<Gym>('/gym-api/api/v1/gyms', { name, active: true }); }
  units(gymId: string) { return this.http.get<Page<GymUnit>>(`/gym-api/api/v1/gyms/${gymId}/units`, { params: { size: 100 } }); }
  createUnit(gymId: string, name: string, city: string) { return this.http.post<GymUnit>(`/gym-api/api/v1/gyms/${gymId}/units`, { name, city, active: true }); }
  equipmentTypes() { return this.http.get<Page<EquipmentType>>('/gym-api/api/v1/equipment-types', { params: { size: 100 } }); }
  inventory(unitId: string) { return this.http.get<UnitEquipment[]>(`/gym-api/api/v1/units/${unitId}/equipment`); }
  saveInventory(unitId: string, equipmentTypeId: string, body: object) { return this.http.put<UnitEquipment>(`/gym-api/api/v1/units/${unitId}/equipment/${equipmentTypeId}`, body); }
  eligibleExercises(unitId: string, kind?: string) {
    let params = new HttpParams().set('size', 100); if (kind) params = params.set('kind', kind);
    return this.http.get<Page<Exercise>>(`/gym-api/api/v1/units/${unitId}/eligible-exercises`, { params });
  }
  profile() { return this.http.get<StudentProfile>('/workout-api/api/v1/me/profile'); }
  saveProfile(profile: StudentProfile) { return this.http.put<StudentProfile>('/workout-api/api/v1/me/profile', profile); }
  plans() { return this.http.get<PlanSummary[]>('/workout-api/api/v1/me/plans'); }
  createPlan(unitId: string, name: string, goal: Goal, targetDaysPerWeek: number) { return this.http.post<WorkoutPlan>('/workout-api/api/v1/me/plans', { unitId, name, goal, targetDaysPerWeek }); }
  plan(id: string) { return this.http.get<WorkoutPlan>(`/workout-api/api/v1/me/plans/${id}`); }
  savePlan(plan: WorkoutPlan) { return this.http.put<WorkoutPlan>(`/workout-api/api/v1/me/plans/${plan.id}`, { version: plan.version, unitId: plan.unitId, name: plan.name, goal: plan.goal, targetDaysPerWeek: plan.targetDaysPerWeek, days: plan.days }); }
  activatePlan(plan: WorkoutPlan) { return this.http.post<WorkoutPlan>(`/workout-api/api/v1/me/plans/${plan.id}/activate`, { version: plan.version }); }
  archivePlan(plan: WorkoutPlan) { return this.http.post<WorkoutPlan>(`/workout-api/api/v1/me/plans/${plan.id}/archive`, { version: plan.version }); }
  generatePlanSuggestion(planId: string) { return this.http.post<AssistantSuggestion>(`/assistant-api/api/v1/plans/${planId}/suggestions`, {}); }
  applySuggestion(plan: WorkoutPlan, suggestionId: string, idempotencyKey: string) {
    return this.http.post<WorkoutPlan>(`/workout-api/api/v1/me/plans/${plan.id}/apply-suggestion`,
      { suggestionId, expectedPlanVersion: plan.version }, { headers: new HttpHeaders({'Idempotency-Key': idempotencyKey}) });
  }
}
