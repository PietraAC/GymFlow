import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { Goal, Gym, GymUnit } from '../../core/api.models';
import { errorMessage } from '../../core/error-message';

@Component({
  imports: [FormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page-shell narrow-page">
      <a routerLink="/student/plans" class="back-link" aria-label="Voltar para meus treinos">← Meus treinos</a>

      <header class="page-heading compact-heading">
        <div>
          <div class="eyebrow">Novo plano</div>
          <h1>Monte a base do seu treino</h1>
          <p>Defina onde você treina e personalize os detalhes na próxima etapa.</p>
        </div>
      </header>

      <ol class="stepper" aria-label="Etapas de criação">
        <li [class.active]="step() === 1" [class.complete]="step() > 1"><span>1</span><div><strong>Academia e unidade</strong><small>Contexto do catálogo</small></div></li>
        <li [class.active]="step() === 2"><span>2</span><div><strong>Detalhes do plano</strong><small>Objetivo e frequência</small></div></li>
      </ol>

      @if (message()) { <div class="feedback error" role="alert">{{ message() }}</div> }

      <section class="panel creation-panel">
        @if (step() === 1) {
          <div class="section-heading">
            <div><span class="section-kicker">Etapa 1</span><h2>Onde você vai treinar?</h2></div>
          </div>
          <div class="field-grid">
            <label>Academia
              <select [ngModel]="gymId()" (ngModelChange)="selectGym($event)" name="gym">
                <option value="">Selecione uma academia</option>
                @for (gym of gyms(); track gym.id) { <option [value]="gym.id">{{ gym.name }}</option> }
              </select>
            </label>
            <label>Unidade
              <select [(ngModel)]="unitId" name="unit" [disabled]="!gymId()">
                <option value="">Selecione uma unidade</option>
                @for (unit of units(); track unit.id) { <option [value]="unit.id">{{ unit.name }} · {{ unit.city }}</option> }
              </select>
            </label>
          </div>
          <div class="context-note">
            <span class="context-icon" aria-hidden="true">✓</span>
            <div><strong>Catálogo contextual</strong><span>O plano mostrará apenas exercícios elegíveis para os equipamentos disponíveis nesta unidade.</span></div>
          </div>
          <div class="form-actions end"><button class="button" type="button" (click)="next()" [disabled]="!unitId">Continuar <span aria-hidden="true">→</span></button></div>
        } @else {
          <div class="section-heading">
            <div><span class="section-kicker">Etapa 2</span><h2>Como será este plano?</h2></div>
            <button type="button" class="text-button" (click)="step.set(1)">Alterar unidade</button>
          </div>
          <div class="selected-context"><span>Unidade selecionada</span><strong>{{ selectedUnitLabel() }}</strong></div>
          <div class="stack">
            <label>Nome do plano<input [(ngModel)]="name" name="name" maxlength="120" placeholder="Ex.: Base de força — 3 dias"></label>
            <div class="field-grid">
              <label>Objetivo
                <select [(ngModel)]="goal" name="goal"><option value="HYPERTROPHY">Hipertrofia</option><option value="STRENGTH">Força</option><option value="GENERAL_FITNESS">Condicionamento geral</option><option value="ENDURANCE">Resistência</option></select>
              </label>
              <label>Dias por semana<input type="number" [(ngModel)]="targetDaysPerWeek" name="targetDaysPerWeek" min="1" max="7"></label>
            </div>
          </div>
          <div class="form-actions split"><button class="button ghost" type="button" (click)="step.set(1)">Voltar</button><button class="button" type="button" (click)="create()" [disabled]="saving() || !name.trim()">{{ saving() ? 'Criando…' : 'Criar rascunho' }}</button></div>
        }
      </section>
    </div>
  `
})
export class NewPlanComponent {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  readonly gyms = signal<Gym[]>([]);
  readonly units = signal<GymUnit[]>([]);
  readonly gymId = signal('');
  readonly step = signal(1);
  readonly saving = signal(false);
  readonly message = signal('');
  unitId = '';
  name = '';
  goal: Goal = 'GENERAL_FITNESS';
  targetDaysPerWeek = 3;

  constructor() {
    this.api.gyms().subscribe({
      next: page => { this.gyms.set(page.content); if (page.content.length) this.selectGym(page.content[0].id); },
      error: error => this.message.set(errorMessage(error))
    });
    this.api.profile().subscribe({ next: profile => { this.goal = profile.goal; this.targetDaysPerWeek = profile.daysPerWeek; } });
  }

  selectGym(id: string): void {
    this.gymId.set(id);
    this.unitId = '';
    this.units.set([]);
    if (!id) return;
    this.api.units(id).subscribe({
      next: page => { this.units.set(page.content); if (page.content.length) this.unitId = page.content[0].id; },
      error: error => this.message.set(errorMessage(error))
    });
  }

  next(): void { if (this.unitId) this.step.set(2); }
  selectedUnitLabel(): string {
    const unit = this.units().find(value => value.id === this.unitId);
    const gym = this.gyms().find(value => value.id === this.gymId());
    return unit ? `${gym?.name ?? ''} · ${unit.name}, ${unit.city}` : 'Unidade selecionada';
  }
  create(): void {
    if (!this.unitId || !this.name.trim()) return;
    this.saving.set(true);
    this.api.createPlan(this.unitId, this.name.trim(), this.goal, this.targetDaysPerWeek).subscribe({
      next: plan => void this.router.navigate(['/student/plans', plan.id]),
      error: error => { this.saving.set(false); this.message.set(errorMessage(error)); }
    });
  }
}
