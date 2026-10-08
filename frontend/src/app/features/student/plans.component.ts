import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { Gym, GymUnit, PlanSummary } from '../../core/api.models';
import { errorMessage } from '../../core/error-message';

@Component({
  imports:[FormsModule,RouterLink,DatePipe], changeDetection:ChangeDetectionStrategy.OnPush,
  template:`
    <header class="page-heading"><div><div class="eyebrow">Planejamento semanal</div><h1>Meus treinos</h1><p>Rascunhos podem ficar incompletos. A ativação sempre revalida a unidade.</p></div></header>
    @if(message()){<div class="feedback error" role="alert">{{message()}}</div>}
    <div class="grid" style="margin-top:1rem">
      <section class="card stack">
        <h2>Novo plano</h2>
        <label>Academia<select [ngModel]="gymId()" (ngModelChange)="selectGym($event)" name="gym"><option value="">Selecione</option>@for(gym of gyms();track gym.id){<option [value]="gym.id">{{gym.name}}</option>}</select></label>
        <label>Unidade<select [(ngModel)]="unitId" name="unit"><option value="">Selecione</option>@for(unit of units();track unit.id){<option [value]="unit.id">{{unit.name}} · {{unit.city}}</option>}</select></label>
        <label>Nome<input [(ngModel)]="name" name="name" maxlength="120" placeholder="Ex.: Base de força — 3 dias"></label>
        <div><button class="button" type="button" (click)="create()" [disabled]="saving()||!unitId||!name.trim()">Criar rascunho</button></div>
      </section>
      <section class="card">
        <h2>Planos salvos</h2>
        @if(loading()){<p class="muted">Carregando planos…</p>}
        @else if(!plans().length){<div class="empty">Você ainda não criou um plano.</div>}
        @else{<ul class="list">@for(plan of plans();track plan.id){<li class="list-item"><div><strong>{{plan.name}}</strong><div class="muted">{{plan.dayCount}} dia(s) · atualizado {{plan.updatedAt|date:'short'}}</div>@if(plan.inventoryRevalidationRequired){<div class="muted">Inventário alterado · revalidação pendente</div>}</div><div class="toolbar"><span class="badge" [class.active]="plan.status==='ACTIVE'" [class.archived]="plan.status==='ARCHIVED'">{{status(plan.status)}}</span><a class="button secondary compact" [routerLink]="['/student/plans',plan.id]">Abrir</a></div></li>}</ul>}
      </section>
    </div>
  `
})
export class PlansComponent {
  private readonly api=inject(ApiService); private readonly router=inject(Router);
  readonly gyms=signal<Gym[]>([]); readonly units=signal<GymUnit[]>([]); readonly plans=signal<PlanSummary[]>([]);
  readonly gymId=signal(''); readonly loading=signal(true); readonly saving=signal(false); readonly message=signal('');
  unitId=''; name='';
  constructor(){this.api.gyms().subscribe({next:p=>{this.gyms.set(p.content);if(p.content.length)this.selectGym(p.content[0].id);},error:e=>this.message.set(errorMessage(e))});this.loadPlans();}
  loadPlans():void{this.loading.set(true);this.api.plans().subscribe({next:p=>{this.plans.set(p);this.loading.set(false);},error:e=>{this.loading.set(false);this.message.set(errorMessage(e));}});}
  selectGym(id:string):void{this.gymId.set(id);this.unitId='';if(!id){this.units.set([]);return;}this.api.units(id).subscribe({next:p=>{this.units.set(p.content);if(p.content.length)this.unitId=p.content[0].id;},error:e=>this.message.set(errorMessage(e))});}
  create():void{this.saving.set(true);this.api.createPlan(this.unitId,this.name.trim()).subscribe({next:plan=>void this.router.navigate(['/student/plans',plan.id]),error:e=>{this.saving.set(false);this.message.set(errorMessage(e));}});}
  status(value:string):string{return {DRAFT:'Rascunho',ACTIVE:'Ativo',ARCHIVED:'Arquivado'}[value]??value;}
}
