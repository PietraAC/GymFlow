import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { Goal, PlanSummary, WorkoutPlan } from '../../core/api.models';
import { errorMessage } from '../../core/error-message';

@Component({
  imports: [RouterLink, DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page-shell">
      <header class="page-heading compact-heading">
        <div><div class="eyebrow">Planejamento semanal</div><h1>Meus treinos</h1><p>Seus planos, dias e exercícios em um só lugar.</p></div>
        <a class="button" routerLink="/student/plans/new"><span aria-hidden="true">＋</span> Novo plano</a>
      </header>

      @if (message()) { <div class="feedback error" role="alert">{{ message() }}</div> }

      <section class="plans-panel panel">
        <div class="panel-toolbar"><div><h2>Planos salvos</h2><span>{{ plans().length }} {{ plans().length === 1 ? 'plano' : 'planos' }}</span></div></div>
        @if (loading()) {
          <div class="skeleton-list"><span></span><span></span><span></span></div>
        } @else if (!plans().length) {
          <div class="empty-state"><div class="empty-icon">＋</div><h2>Seu primeiro plano começa aqui</h2><p>Escolha uma unidade e organize sua semana usando o catálogo elegível.</p><a class="button" routerLink="/student/plans/new">Criar plano</a></div>
        } @else {
          <div class="plan-list">
            @for (plan of plans(); track plan.id) {
              <article class="plan-card" [class.expanded]="expandedPlanId() === plan.id">
                <button type="button" class="plan-card-header" (click)="togglePlan(plan)" [attr.aria-expanded]="expandedPlanId() === plan.id">
                  <span class="plan-monogram">{{ plan.name.charAt(0).toUpperCase() }}</span>
                  <span class="plan-main"><span class="plan-title-line"><strong>{{ plan.name }}</strong><span class="badge" [class.active]="plan.status === 'ACTIVE'" [class.archived]="plan.status === 'ARCHIVED'">{{ status(plan.status) }}</span></span><small>{{ goalLabel(plan.goal) }} · {{ plan.dayCount }} de {{ plan.targetDaysPerWeek }} dias · atualizado {{ plan.updatedAt | date:'shortDate' }}</small></span>
                  @if (plan.inventoryRevalidationRequired) { <span class="attention-label">Revalidar</span> }
                  <span class="chevron" [class.open]="expandedPlanId() === plan.id">⌄</span>
                </button>
                @if (expandedPlanId() === plan.id) {
                  <div class="plan-card-body">
                    @if (detailLoading()) {
                      <div class="inline-loading">Carregando dias…</div>
                    } @else if (expandedPlan(); as detail) {
                      <div class="plan-days">
                        @for (day of detail.days; track day.id || day.position) {
                          <div class="plan-day-row"><span class="day-index">{{ day.position }}</span><div><strong>{{ day.name }}</strong><small>{{ day.items.length }} {{ day.items.length === 1 ? 'exercício' : 'exercícios' }}</small></div><span class="row-arrow">→</span></div>
                        } @empty {
                          <div class="compact-empty"><span>Nenhum dia adicionado.</span><span>Abra o editor para montar sua semana.</span></div>
                        }
                      </div>
                      <div class="plan-footer"><span>Versão {{ detail.version }}</span><a class="button compact" [routerLink]="['/student/plans', detail.id]">Abrir editor <span aria-hidden="true">→</span></a></div>
                    }
                  </div>
                }
              </article>
            }
          </div>
        }
      </section>
    </div>
  `
})
export class PlansComponent {
  private readonly api = inject(ApiService);
  readonly plans = signal<PlanSummary[]>([]);
  readonly expandedPlanId = signal('');
  readonly expandedPlan = signal<WorkoutPlan | null>(null);
  readonly loading = signal(true);
  readonly detailLoading = signal(false);
  readonly message = signal('');

  constructor() { this.loadPlans(); }

  loadPlans(): void {
    this.loading.set(true);
    this.api.plans().subscribe({
      next: plans => {
        this.plans.set(plans);
        this.loading.set(false);
        const initial = plans.find(plan => plan.status === 'ACTIVE') ?? plans[0];
        if (initial) this.openPlan(initial);
      },
      error: error => { this.loading.set(false); this.message.set(errorMessage(error)); }
    });
  }

  togglePlan(plan: PlanSummary): void {
    if (this.expandedPlanId() === plan.id) {
      this.expandedPlanId.set('');
      this.expandedPlan.set(null);
      return;
    }
    this.openPlan(plan);
  }

  status(value: string): string { return { DRAFT:'Rascunho', ACTIVE:'Ativo', ARCHIVED:'Arquivado' }[value] ?? value; }
  goalLabel(value: Goal): string { return { HYPERTROPHY:'Hipertrofia', STRENGTH:'Força', GENERAL_FITNESS:'Condicionamento geral', ENDURANCE:'Resistência' }[value]; }

  private openPlan(plan: PlanSummary): void {
    this.expandedPlanId.set(plan.id);
    this.expandedPlan.set(null);
    this.detailLoading.set(true);
    this.api.plan(plan.id).subscribe({
      next: detail => { if (this.expandedPlanId() === plan.id) this.expandedPlan.set(detail); this.detailLoading.set(false); },
      error: error => { this.detailLoading.set(false); this.message.set(errorMessage(error)); }
    });
  }
}
