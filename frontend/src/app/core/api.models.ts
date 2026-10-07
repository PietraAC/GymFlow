export interface Page<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number; }
export interface Gym { id: string; name: string; active: boolean; }
export interface GymUnit { id: string; gymId: string; name: string; city: string; active: boolean; }
export interface EquipmentType { id: string; name: string; description: string; active: boolean; }
export interface UnitEquipment { id: string; unitId: string; equipmentTypeId: string; equipmentTypeName: string; totalQuantity: number; availableQuantity: number; notes?: string; version: number; }
export type ExerciseKind = 'STRENGTH' | 'WARMUP' | 'STRETCHING';
export interface Exercise { id: string; name: string; kind: ExerciseKind; primaryMuscleGroups: string[]; difficulty: string; instructions: string; }
export type Goal = 'HYPERTROPHY' | 'STRENGTH' | 'GENERAL_FITNESS' | 'ENDURANCE';
export type Experience = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
export interface StudentProfile { identitySubject?: string; goal: Goal; experienceLevel: Experience; daysPerWeek: number; sessionDurationMinutes: number; preferredEquipmentTypeIds: string[]; }
export type PlanStatus = 'DRAFT' | 'ACTIVE' | 'ARCHIVED';
export interface PlanSummary { id: string; unitId: string; name: string; status: PlanStatus; version: number; dayCount: number; updatedAt: string; }
export interface WorkoutItem { id?: string; exerciseId: string; position: number; sets: number | null; repetitionMin: number | null; repetitionMax: number | null; durationSeconds: number | null; restSeconds: number | null; optionalLoadKg: number | null; notes: string | null; }
export interface WorkoutDay { id?: string; position: number; name: string; items: WorkoutItem[]; }
export interface PlanIssue { code: string; message: string; exerciseIds: string[]; }
export interface WorkoutPlan { id: string; unitId: string; name: string; status: PlanStatus; version: number; days: WorkoutDay[]; eligibilityCheckAvailable: boolean; issues: PlanIssue[]; }
export interface ProblemDetail { title?: string; detail?: string; errors?: Record<string, string>; }
