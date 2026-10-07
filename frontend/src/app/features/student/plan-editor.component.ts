import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { switchMap } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { Exercise, ExerciseKind, WorkoutDay, WorkoutPlan } from '../../core/api.models';
import { errorMessage } from '../../core/error-message';

@Component({
  imports:[ReactiveFormsModule,RouterLink], changeDetection:ChangeDetectionStrategy.OnPush,
  template:`
    <a routerLink="/student/plans" class="muted">← Voltar aos planos</a>
    @if(loading()){<div class="card" style="margin-top:1rem">Carregando treino…</div>}
    @else if(plan();as current){
      <form [formGroup]="form" (ngSubmit)="save()" class="stack" style="margin-top:1rem">
        <header class="page-heading"><div><div class="eyebrow">Editor semanal</div><h1>{{current.name}}</h1><p><span class="badge" [class.active]="current.status==='ACTIVE'" [class.archived]="current.status==='ARCHIVED'">{{status(current.status)}}</span> · versão {{current.version}}</p></div><div class="toolbar">@if(current.status==='DRAFT'){<button type="button" class="button secondary" (click)="addDay()" [disabled]="days.length>=7">Adicionar dia</button><button class="button" [disabled]="saving()||form.invalid">Salvar rascunho</button><button type="button" class="button" (click)="activate()" [disabled]="saving()||form.invalid">Ativar</button>}@if(current.status!=='ARCHIVED'){<button type="button" class="button danger" (click)="archive()" [disabled]="saving()">Arquivar</button>}</div></header>
        @if(message()){<div class="feedback" [class.error]="failed()" role="status">{{message()}}</div>}
        @if(!current.eligibilityCheckAvailable){<div class="feedback error">Não foi possível conferir a elegibilidade agora. Você pode consultar o rascunho, mas não salvá-lo ou ativá-lo até o gym-service voltar.</div>}
        @for(issue of current.issues;track issue.code){<div class="feedback error">{{issue.message}}</div>}
        <section class="card field-grid"><label>Nome do plano<input formControlName="name" maxlength="120"></label><label>Unidade<input formControlName="unitId" readonly></label><label>Filtrar catálogo elegível<select [value]="kindFilter()" (change)="kindFilter.set($any($event.target).value)"><option value="">Todas as modalidades</option><option value="STRENGTH">Força</option><option value="WARMUP">Aquecimento</option><option value="STRETCHING">Alongamento</option></select></label></section>
        <div formArrayName="days" class="stack">
          @for(day of days.controls;track day;let dayIndex=$index){
            <section class="card day" [formGroupName]="dayIndex">
              <div class="item-head"><div style="flex:1"><label>Dia {{dayIndex+1}}<input formControlName="name" maxlength="80"></label></div><div class="toolbar"><button type="button" class="icon-button" (click)="moveDay(dayIndex,-1)" [disabled]="dayIndex===0" aria-label="Mover dia para cima">↑</button><button type="button" class="icon-button" (click)="moveDay(dayIndex,1)" [disabled]="dayIndex===days.length-1" aria-label="Mover dia para baixo">↓</button><button type="button" class="button danger compact" (click)="removeDay(dayIndex)">Remover</button></div></div>
              <div formArrayName="items" class="stack">
                @for(item of items(dayIndex).controls;track item;let itemIndex=$index){
                  <div class="item" [formGroupName]="itemIndex">
                    <div class="item-head"><strong>Exercício {{itemIndex+1}}</strong><div class="toolbar"><button type="button" class="icon-button" (click)="moveItem(dayIndex,itemIndex,-1)" [disabled]="itemIndex===0" aria-label="Mover exercício para cima">↑</button><button type="button" class="icon-button" (click)="moveItem(dayIndex,itemIndex,1)" [disabled]="itemIndex===items(dayIndex).length-1" aria-label="Mover exercício para baixo">↓</button><button type="button" class="button danger compact" (click)="removeItem(dayIndex,itemIndex)">Remover</button></div></div>
                    <div class="field-grid">
                      <label>Exercício<select formControlName="exerciseId" (change)="exerciseChanged(item)"><option value="">Selecione</option>@for(exercise of filteredExercises();track exercise.id){<option [value]="exercise.id">{{exercise.name}} · {{kindLabel(exercise.kind)}}</option>}</select></label>
                      @if(kind(item)==='STRENGTH'){
                        <label>Séries<input type="number" min="1" max="10" formControlName="sets"></label><label>Repetições mínimas<input type="number" min="1" max="100" formControlName="repetitionMin"></label><label>Repetições máximas<input type="number" min="1" max="100" formControlName="repetitionMax"></label><label>Carga opcional (kg)<input type="number" min="0" step="0.25" formControlName="optionalLoadKg"></label>
                      }@else{
                        <label>Duração (segundos)<input type="number" min="5" max="1800" formControlName="durationSeconds"></label>
                      }
                      <label>Descanso (segundos)<input type="number" min="0" max="600" formControlName="restSeconds"></label><label>Observações<input maxlength="500" formControlName="notes"></label>
                    </div>
                  </div>
                }@empty{<div class="empty">Este dia ainda não tem exercícios.</div>}
              </div>
              <button type="button" class="button secondary compact" style="margin-top:1rem" (click)="addItem(dayIndex)" [disabled]="!exercises().length">Adicionar exercício</button>
            </section>
          }@empty{<div class="empty">Adicione um dia para começar a montar a semana.</div>}
        </div>
      </form>
    }
  `
})
export class PlanEditorComponent {
  private readonly api=inject(ApiService); private readonly route=inject(ActivatedRoute); private readonly router=inject(Router); private readonly fb=inject(FormBuilder);
  readonly loading=signal(true); readonly saving=signal(false); readonly plan=signal<WorkoutPlan|null>(null); readonly exercises=signal<Exercise[]>([]); readonly message=signal(''); readonly failed=signal(false);
  readonly kindFilter=signal<ExerciseKind|''>(''); readonly filteredExercises=computed(()=>this.kindFilter()?this.exercises().filter(exercise=>exercise.kind===this.kindFilter()):this.exercises());
  readonly form=this.fb.group({name:['',[Validators.required,Validators.maxLength(120)]],unitId:['',Validators.required],days:this.fb.array<FormGroup>([])});
  get days():FormArray<FormGroup>{return this.form.controls.days;}
  constructor(){const id=this.route.snapshot.paramMap.get('id');if(!id){void this.router.navigate(['/student/plans']);return;}this.api.plan(id).subscribe({next:plan=>this.load(plan),error:e=>{this.loading.set(false);this.show(errorMessage(e),true);}});}
  items(dayIndex:number):FormArray<FormGroup>{return this.days.at(dayIndex).get('items') as FormArray<FormGroup>;}
  addDay():void{this.days.push(this.dayGroup({position:this.days.length+1,name:`Dia ${this.days.length+1}`,items:[]}));}
  removeDay(index:number):void{this.days.removeAt(index);this.normalize();}
  moveDay(index:number,delta:number):void{const target=index+delta;if(target<0||target>=this.days.length)return;const control=this.days.at(index);this.days.removeAt(index);this.days.insert(target,control);this.normalize();}
  addItem(dayIndex:number):void{const exercise=this.filteredExercises()[0];if(!exercise)return;const array=this.items(dayIndex);array.push(this.itemGroup({exerciseId:exercise.id,position:array.length+1,sets:exercise.kind==='STRENGTH'?3:null,repetitionMin:exercise.kind==='STRENGTH'?8:null,repetitionMax:exercise.kind==='STRENGTH'?12:null,durationSeconds:exercise.kind==='STRENGTH'?null:60,restSeconds:60,optionalLoadKg:null,notes:null}));}
  removeItem(dayIndex:number,itemIndex:number):void{this.items(dayIndex).removeAt(itemIndex);this.normalize();}
  moveItem(dayIndex:number,index:number,delta:number):void{const array=this.items(dayIndex),target=index+delta;if(target<0||target>=array.length)return;const control=array.at(index);array.removeAt(index);array.insert(target,control);this.normalize();}
  kind(group:FormGroup):ExerciseKind|undefined{return this.exercises().find(ex=>ex.id===group.get('exerciseId')?.value)?.kind;}
  kindLabel(kind:ExerciseKind):string{return {STRENGTH:'Força',WARMUP:'Aquecimento',STRETCHING:'Alongamento'}[kind];}
  exerciseChanged(group:FormGroup):void{if(this.kind(group)==='STRENGTH'){group.patchValue({sets:3,repetitionMin:8,repetitionMax:12,durationSeconds:null});}else{group.patchValue({sets:null,repetitionMin:null,repetitionMax:null,durationSeconds:60,optionalLoadKg:null});}}
  save():void{const payload=this.payload();if(!payload)return;this.saving.set(true);this.api.savePlan(payload).subscribe({next:p=>{this.load(p);this.saving.set(false);this.show('Rascunho salvo e elegibilidade confirmada.',false);},error:e=>{this.saving.set(false);this.show(errorMessage(e),true);}});}
  activate():void{const payload=this.payload();if(!payload)return;this.saving.set(true);this.api.savePlan(payload).pipe(switchMap(saved=>this.api.activatePlan(saved))).subscribe({next:p=>{this.load(p);this.saving.set(false);this.show('Plano salvo, revalidado e ativado.',false);},error:e=>{this.saving.set(false);this.show(errorMessage(e),true);}});}
  archive():void{const current=this.plan();if(!current)return;this.saving.set(true);this.api.archivePlan(current).subscribe({next:p=>{this.load(p);this.saving.set(false);this.show('Plano arquivado.',false);},error:e=>{this.saving.set(false);this.show(errorMessage(e),true);}});}
  status(value:string):string{return {DRAFT:'Rascunho',ACTIVE:'Ativo',ARCHIVED:'Arquivado'}[value]??value;}
  private load(plan:WorkoutPlan):void{this.plan.set(plan);this.form.patchValue({name:plan.name,unitId:plan.unitId});this.days.clear();plan.days.forEach(day=>this.days.push(this.dayGroup(day)));this.loading.set(false);this.api.eligibleExercises(plan.unitId).subscribe({next:p=>this.exercises.set(p.content),error:e=>this.show(errorMessage(e),true)});}
  private dayGroup(day:Partial<WorkoutDay>):FormGroup{return this.fb.group({id:[day.id??null],position:[day.position??1,[Validators.required,Validators.min(1),Validators.max(7)]],name:[day.name??'',Validators.required],items:this.fb.array((day.items??[]).map(item=>this.itemGroup(item)))});}
  private itemGroup(item:Partial<WorkoutDay['items'][number]>):FormGroup{return this.fb.group({id:[item.id??null],exerciseId:[item.exerciseId??'',Validators.required],position:[item.position??1,[Validators.required,Validators.min(1)]],sets:[item.sets??null],repetitionMin:[item.repetitionMin??null],repetitionMax:[item.repetitionMax??null],durationSeconds:[item.durationSeconds??null],restSeconds:[item.restSeconds??null],optionalLoadKg:[item.optionalLoadKg??null],notes:[item.notes??null]});}
  private payload():WorkoutPlan|null{const current=this.plan();if(!current)return null;this.normalize();return {...current,name:this.form.controls.name.value??'',unitId:this.form.controls.unitId.value??'',days:this.form.controls.days.getRawValue() as WorkoutDay[]};}
  private normalize():void{this.days.controls.forEach((day,index)=>{day.get('position')?.setValue(index+1);this.items(index).controls.forEach((item,itemIndex)=>item.get('position')?.setValue(itemIndex+1));});}
  private show(message:string,failed:boolean):void{this.message.set(message);this.failed.set(failed);}
}
