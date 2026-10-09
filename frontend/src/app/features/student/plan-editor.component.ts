import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { switchMap, tap } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { AssistantSuggestion, Exercise, ExerciseKind, Goal, WorkoutDay, WorkoutPlan } from '../../core/api.models';
import { errorMessage } from '../../core/error-message';
import { ExerciseMediaComponent } from '../../shared/exercise-media.component';

@Component({
  imports: [ReactiveFormsModule, RouterLink, ExerciseMediaComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page-shell editor-page">
      <a routerLink="/student/plans" class="back-link">← Meus treinos</a>
      @if (loading()) {
        <div class="panel loading-panel">Carregando treino…</div>
      } @else if (plan(); as current) {
        <form [formGroup]="form" (ngSubmit)="save()" class="editor-form">
          <header class="editor-header">
            <div class="editor-title">
              <div class="editor-meta"><span class="badge" [class.active]="current.status === 'ACTIVE'" [class.archived]="current.status === 'ARCHIVED'">{{ status(current.status) }}</span><span>Versão {{ current.version }}</span><span>{{ days.length }} de {{ targetDays() }} dias</span></div>
              <h1>{{ current.name }}</h1>
            </div>
            <div class="editor-actions">
              @if (current.status === 'DRAFT') {
                <button type="button" class="button ai-button" (click)="openAssistant()"><span class="sparkle" aria-hidden="true">✦</span> Completar com IA</button>
                <button class="button ghost" [disabled]="saving() || form.invalid">Salvar</button>
                <button type="button" class="button" (click)="activate()" [disabled]="saving() || form.invalid">Ativar plano</button>
              }
              @if (current.status === 'ACTIVE' && current.inventoryRevalidationRequired) { <button type="button" class="button" (click)="revalidate()" [disabled]="saving()">Revalidar inventário</button> }
              @if (current.status !== 'ARCHIVED') { <button type="button" class="icon-button more-button" (click)="settingsOpen.set(!settingsOpen())" aria-label="Configurações do plano">•••</button> }
            </div>
          </header>

          @if (message()) { <div class="feedback" [class.error]="failed()" role="status">{{ message() }}</div> }
          @if (!current.eligibilityCheckAvailable) { <div class="feedback error">Não foi possível conferir a elegibilidade agora. O rascunho fica disponível para consulta, mas não pode ser salvo ou ativado.</div> }
          @for (issue of current.issues; track issue.code) { <div class="feedback" [class.error]="issue.code !== 'INVENTORY_CHANGED'">{{ issue.message }}</div> }

          @if (settingsOpen()) {
            <section class="panel plan-settings">
              <div class="section-heading"><div><span class="section-kicker">Configurações</span><h2>Detalhes do plano</h2></div><button type="button" class="icon-button" (click)="settingsOpen.set(false)" aria-label="Fechar configurações">×</button></div>
              <div class="field-grid"><label>Nome do plano<input formControlName="name" maxlength="120"></label><label>Unidade<input formControlName="unitId" readonly></label><label>Objetivo<select formControlName="goal"><option value="HYPERTROPHY">Hipertrofia</option><option value="STRENGTH">Força</option><option value="GENERAL_FITNESS">Condicionamento geral</option><option value="ENDURANCE">Resistência</option></select></label><label>Dias por semana<input type="number" formControlName="targetDaysPerWeek" min="1" max="7"></label></div>
              <div class="settings-footer">@if (current.status !== 'ARCHIVED') { <button type="button" class="text-button danger-text" (click)="archive()" [disabled]="saving()">Arquivar plano</button> }</div>
            </section>
          }

          <section class="week-section">
            <div class="section-heading"><div><span class="section-kicker">Sua semana</span><h2>Dias de treino</h2></div>@if (current.status === 'DRAFT') { <button type="button" class="button ghost compact" (click)="addDay()" [disabled]="days.length >= targetDays()">＋ Adicionar dia</button> }</div>
            <div class="day-tabs" role="tablist" aria-label="Dias do plano">
              @for (day of days.controls; track day; let dayIndex = $index) {
                <button type="button" role="tab" [class.active]="isExpanded(dayIndex)" [attr.aria-selected]="isExpanded(dayIndex)" (click)="toggleDay(dayIndex)"><span>Dia {{ dayIndex + 1 }}</span><strong>{{ day.get('name')?.value || 'Sem nome' }}</strong><small>{{ items(dayIndex).length }} exercício(s)</small></button>
              } @empty {
                <div class="empty-day-tab">Adicione um dia para começar.</div>
              }
            </div>
          </section>

          <div formArrayName="days">
            @for (day of days.controls; track day; let dayIndex = $index) {
              @if (isExpanded(dayIndex)) {
                <section class="day-workspace" [formGroupName]="dayIndex">
                  <div class="day-workspace-header">
                    <label>Nome do dia<input formControlName="name" maxlength="80"></label>
                    <div class="toolbar"><button type="button" class="icon-button" (click)="moveDay(dayIndex, -1)" [disabled]="dayIndex === 0" aria-label="Mover dia para a esquerda">←</button><button type="button" class="icon-button" (click)="moveDay(dayIndex, 1)" [disabled]="dayIndex === days.length - 1" aria-label="Mover dia para a direita">→</button>@if (current.status === 'DRAFT') { <button type="button" class="text-button danger-text" (click)="removeDay(dayIndex)">Remover dia</button> }</div>
                  </div>
                  <div formArrayName="items" class="exercise-list">
                    @for (item of items(dayIndex).controls; track item; let itemIndex = $index) {
                      <article class="exercise-card" [formGroupName]="itemIndex" [class.expanded]="isItemExpanded(dayIndex, itemIndex)">
                        <button type="button" class="exercise-row" (click)="toggleItem(dayIndex, itemIndex)" [attr.aria-expanded]="isItemExpanded(dayIndex, itemIndex)">
                          <app-exercise-media [exerciseId]="item.get('exerciseId')?.value" [exerciseName]="exerciseName(item)" [compact]="true" />
                          <span class="exercise-copy"><strong>{{ exerciseName(item) }}</strong><small>{{ exerciseSummary(item) }}</small></span>
                          <span class="exercise-kind">{{ kindLabel(kind(item) || 'STRENGTH') }}</span>
                          <span class="chevron" [class.open]="isItemExpanded(dayIndex, itemIndex)">⌄</span>
                        </button>
                        @if (isItemExpanded(dayIndex, itemIndex)) {
                          <div class="exercise-details">
                            <app-exercise-media [exerciseId]="item.get('exerciseId')?.value" [exerciseName]="exerciseName(item)" />
                            <div class="exercise-fields">
                              <label>Exercício<select formControlName="exerciseId" (change)="exerciseChanged(item)"><option value="">Selecione</option>@for (exercise of filteredExercises(); track exercise.id) { <option [value]="exercise.id">{{ exercise.name }} · {{ kindLabel(exercise.kind) }}</option> }</select></label>
                              <div class="field-grid dense-fields">
                                @if (kind(item) === 'STRENGTH') { <label>Séries<input type="number" min="1" max="10" formControlName="sets"></label><label>Repetições mínimas<input type="number" min="1" max="100" formControlName="repetitionMin"></label><label>Repetições máximas<input type="number" min="1" max="100" formControlName="repetitionMax"></label><label>Carga opcional (kg)<input type="number" min="0" step="0.25" formControlName="optionalLoadKg"></label> } @else { <label>Duração (segundos)<input type="number" min="5" max="1800" formControlName="durationSeconds"></label> }
                                <label>Descanso (segundos)<input type="number" min="0" max="600" formControlName="restSeconds"></label><label>Observações<input maxlength="500" formControlName="notes"></label>
                              </div>
                              <div class="item-actions"><div><button type="button" class="icon-button" (click)="moveItem(dayIndex, itemIndex, -1)" [disabled]="itemIndex === 0" aria-label="Mover exercício para cima">↑</button><button type="button" class="icon-button" (click)="moveItem(dayIndex, itemIndex, 1)" [disabled]="itemIndex === items(dayIndex).length - 1" aria-label="Mover exercício para baixo">↓</button></div><button type="button" class="text-button danger-text" (click)="removeItem(dayIndex, itemIndex)">Remover exercício</button></div>
                            </div>
                          </div>
                        }
                      </article>
                    } @empty {
                      <div class="empty-state compact-empty-state"><div class="empty-icon">＋</div><h3>Nenhum exercício neste dia</h3><p>Abra o catálogo elegível para começar.</p></div>
                    }
                  </div>
                  @if (current.status === 'DRAFT') { <button type="button" class="add-exercise-button" (click)="openCatalog(dayIndex)" [disabled]="!exercises().length"><span>＋</span><div><strong>Adicionar exercício</strong><small>Escolher no catálogo da unidade</small></div></button> }
                </section>
              }
            }
          </div>
        </form>
      }
    </div>

    @if (catalogOpen()) {
      <div class="drawer-backdrop" (click)="closeCatalog()"></div>
      <aside class="drawer catalog-drawer" aria-labelledby="catalog-title">
        <div class="drawer-header"><div><span class="section-kicker">Catálogo elegível</span><h2 id="catalog-title">Adicionar exercício</h2></div><button type="button" class="icon-button" (click)="closeCatalog()" aria-label="Fechar catálogo">×</button></div>
        <div class="drawer-filters"><label class="search-field"><span>⌕</span><input placeholder="Buscar exercício" [value]="catalogSearch()" (input)="catalogSearch.set($any($event.target).value)"></label><label>Modalidade<select [value]="kindFilter()" (change)="kindFilter.set($any($event.target).value)"><option value="">Todas</option><option value="STRENGTH">Força</option><option value="WARMUP">Aquecimento</option><option value="STRETCHING">Alongamento</option></select></label></div>
        <div class="catalog-list">
          @for (exercise of catalogExercises(); track exercise.id) {
            <button type="button" class="catalog-item" (click)="addExercise(catalogDayIndex(), exercise)"><app-exercise-media [exerciseId]="exercise.id" [exerciseName]="exercise.name" [compact]="true" /><span><strong>{{ exercise.name }}</strong><small>{{ kindLabel(exercise.kind) }} · {{ exercise.primaryMuscleGroups.join(', ') || 'Corpo todo' }}</small></span><b>＋</b></button>
          } @empty { <div class="compact-empty"><span>Nenhum exercício encontrado.</span><span>Ajuste a busca ou o filtro.</span></div> }
        </div>
      </aside>
    }

    @if (assistantOpen()) {
      <div class="drawer-backdrop" (click)="assistantOpen.set(false)"></div>
      <aside class="drawer assistant-drawer" aria-labelledby="assistant-title">
        <div class="drawer-header"><div><span class="section-kicker">Sugestão estruturada</span><h2 id="assistant-title">Assistente de treino</h2></div><button type="button" class="icon-button" (click)="assistantOpen.set(false)" aria-label="Fechar assistente">×</button></div>
        <p class="drawer-intro">A IA considera seu objetivo, perfil e somente os exercícios disponíveis nesta unidade.</p>
        <div class="completion-status"><div class="progress-ring">{{ days.length }}/{{ targetDays() }}</div><div><strong>Progresso da semana</strong><span>{{ days.length ? 'Seu rascunho atual será preservado.' : 'A semana pode ser criada desde o primeiro dia.' }}</span></div></div>
        @if (pendingSuggestion(); as suggestion) {
          <div class="proposal-card"><div class="proposal-label"><span>✦</span><strong>{{ suggestion.source === 'DEMO' ? 'Proposta em modo demo' : 'Proposta do Gemini' }}</strong></div><p>{{ suggestion.explanation }}</p>@for (observation of suggestion.observations; track observation) { <div class="proposal-observation">{{ observation }}</div> }<div class="proposal-summary"><span>{{ suggestion.completion?.newDays?.length || 0 }} novo(s) dia(s)</span><span>{{ proposedExerciseCount(suggestion) }} exercício(s) sugeridos</span></div></div>
          <div class="drawer-actions"><button type="button" class="button ghost" (click)="pendingSuggestion.set(null)">Descartar</button><button type="button" class="button" (click)="applyAiSuggestion()" [disabled]="assistantLoading()">{{ assistantLoading() ? 'Aplicando…' : 'Aplicar ao rascunho' }}</button></div>
        } @else {
          <div class="assistant-principles"><div><span>✓</span><p><strong>Elegível para a unidade</strong><small>Nenhum exercício fora do inventário.</small></p></div><div><span>✓</span><p><strong>Você revisa antes</strong><small>A proposta não altera o plano automaticamente.</small></p></div><div><span>✓</span><p><strong>Sem carga prescrita</strong><small>A IA não define peso em quilogramas.</small></p></div></div>
          <button type="button" class="button full-width" (click)="generateAiSuggestion()" [disabled]="assistantLoading() || form.invalid">{{ assistantLoading() ? 'Preparando proposta…' : days.length ? 'Gerar complemento' : 'Gerar plano inicial' }}</button>
        }
      </aside>
    }
  `
})
export class PlanEditorComponent {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly plan = signal<WorkoutPlan | null>(null);
  readonly exercises = signal<Exercise[]>([]);
  readonly message = signal('');
  readonly failed = signal(false);
  readonly settingsOpen = signal(false);
  readonly assistantOpen = signal(false);
  readonly assistantLoading = signal(false);
  readonly pendingSuggestion = signal<AssistantSuggestion | null>(null);
  readonly aiResult = signal<{source:'DEMO'|'GEMINI';days:number;exercises:number} | null>(null);
  readonly expandedDays = signal<Set<number>>(new Set());
  readonly expandedItems = signal<Set<string>>(new Set());
  readonly catalogOpen = signal(false);
  readonly catalogDayIndex = signal(0);
  readonly catalogSearch = signal('');
  readonly kindFilter = signal<ExerciseKind | ''>('');
  readonly filteredExercises = computed(() => this.kindFilter() ? this.exercises().filter(exercise => exercise.kind === this.kindFilter()) : this.exercises());
  readonly catalogExercises = computed(() => {
    const query = this.catalogSearch().trim().toLocaleLowerCase('pt-BR');
    return this.filteredExercises().filter(exercise => !query || `${exercise.name} ${exercise.primaryMuscleGroups.join(' ')}`.toLocaleLowerCase('pt-BR').includes(query));
  });
  readonly form = this.fb.group({ name:['',[Validators.required,Validators.maxLength(120)]], unitId:['',Validators.required], goal:['GENERAL_FITNESS' as Goal,Validators.required], targetDaysPerWeek:[3,[Validators.required,Validators.min(1),Validators.max(7)]], days:this.fb.array<FormGroup>([]) });

  get days(): FormArray<FormGroup> { return this.form.controls.days; }

  constructor() {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) { void this.router.navigate(['/student/plans']); return; }
    this.api.plan(id).subscribe({ next: plan => this.load(plan), error: error => { this.loading.set(false); this.show(errorMessage(error), true); } });
  }

  items(dayIndex: number): FormArray<FormGroup> { return this.days.at(dayIndex).get('items') as FormArray<FormGroup>; }
  targetDays(): number { return Number(this.form.controls.targetDaysPerWeek.value ?? 3); }
  addDay(): void { if (this.days.length >= this.targetDays()) return; this.days.push(this.dayGroup({ position:this.days.length + 1, name:`Dia ${this.days.length + 1}`, items:[] })); this.expandedDays.set(new Set([this.days.length - 1])); }
  removeDay(index: number): void { this.days.removeAt(index); this.normalize(); this.expandedDays.set(new Set(this.days.length ? [Math.max(0, index - 1)] : [])); }
  moveDay(index: number, delta: number): void { const target = index + delta; if (target < 0 || target >= this.days.length) return; const control = this.days.at(index); this.days.removeAt(index); this.days.insert(target, control); this.normalize(); this.expandedDays.set(new Set([target])); }
  addItem(dayIndex: number): void { const exercise = this.filteredExercises()[0]; if (exercise) this.addExercise(dayIndex, exercise, false); }
  addExercise(dayIndex: number, exercise: Exercise, close = true): void { const array = this.items(dayIndex); array.push(this.itemGroup({ exerciseId:exercise.id, position:array.length + 1, sets:exercise.kind === 'STRENGTH' ? 3 : null, repetitionMin:exercise.kind === 'STRENGTH' ? 8 : null, repetitionMax:exercise.kind === 'STRENGTH' ? 12 : null, durationSeconds:exercise.kind === 'STRENGTH' ? null : 60, restSeconds:60, optionalLoadKg:null, notes:null })); this.expandedItems.set(new Set([`${dayIndex}-${array.length - 1}`])); if (close) this.closeCatalog(); }
  removeItem(dayIndex: number, itemIndex: number): void { this.items(dayIndex).removeAt(itemIndex); this.normalize(); this.expandedItems.set(new Set()); }
  moveItem(dayIndex: number, index: number, delta: number): void { const array = this.items(dayIndex), target = index + delta; if (target < 0 || target >= array.length) return; const control = array.at(index); array.removeAt(index); array.insert(target, control); this.normalize(); this.expandedItems.set(new Set([`${dayIndex}-${target}`])); }
  kind(group: FormGroup): ExerciseKind | undefined { return this.exercises().find(exercise => exercise.id === group.get('exerciseId')?.value)?.kind; }
  exerciseName(group: FormGroup): string { return this.exercises().find(exercise => exercise.id === group.get('exerciseId')?.value)?.name ?? 'Exercício'; }
  kindLabel(kind: ExerciseKind): string { return { STRENGTH:'Força', WARMUP:'Aquecimento', STRETCHING:'Alongamento' }[kind]; }
  exerciseSummary(group: FormGroup): string { if (this.kind(group) === 'STRENGTH') return `${group.get('sets')?.value ?? '—'} séries · ${group.get('repetitionMin')?.value ?? '—'}–${group.get('repetitionMax')?.value ?? '—'} repetições · ${group.get('restSeconds')?.value ?? 0}s descanso`; return `${group.get('durationSeconds')?.value ?? '—'}s de duração · ${group.get('restSeconds')?.value ?? 0}s descanso`; }
  exerciseChanged(group: FormGroup): void { if (this.kind(group) === 'STRENGTH') group.patchValue({ sets:3,repetitionMin:8,repetitionMax:12,durationSeconds:null }); else group.patchValue({ sets:null,repetitionMin:null,repetitionMax:null,durationSeconds:60,optionalLoadKg:null }); }
  openCatalog(dayIndex: number): void { this.catalogDayIndex.set(dayIndex); this.catalogSearch.set(''); this.kindFilter.set(''); this.catalogOpen.set(true); }
  closeCatalog(): void { this.catalogOpen.set(false); }
  openAssistant(): void { this.assistantOpen.set(true); }
  isItemExpanded(dayIndex: number, itemIndex: number): boolean { return this.expandedItems().has(`${dayIndex}-${itemIndex}`); }
  toggleItem(dayIndex: number, itemIndex: number): void { const key = `${dayIndex}-${itemIndex}`; this.expandedItems.update(value => value.has(key) ? new Set() : new Set([key])); }
  save(): void { const payload = this.payload(); if (!payload) return; this.saving.set(true); this.api.savePlan(payload).subscribe({ next: plan => { this.load(plan); this.saving.set(false); this.show('Rascunho salvo e elegibilidade confirmada.', false); }, error: error => { this.saving.set(false); this.show(errorMessage(error), true); } }); }
  activate(): void { const payload = this.payload(); if (!payload) return; this.saving.set(true); this.api.savePlan(payload).pipe(switchMap(saved => this.api.activatePlan(saved))).subscribe({ next: plan => { this.load(plan); this.saving.set(false); this.show('Plano salvo, revalidado e ativado.', false); }, error: error => { this.saving.set(false); this.show(errorMessage(error), true); } }); }
  revalidate(): void { const current = this.plan(); if (!current || current.status !== 'ACTIVE') return; this.saving.set(true); this.api.activatePlan(current).subscribe({ next: plan => { this.load(plan); this.saving.set(false); this.show('Inventário revalidado para o plano ativo.', false); }, error: error => { this.saving.set(false); this.show(errorMessage(error), true); } }); }
  archive(): void { const current = this.plan(); if (!current) return; this.saving.set(true); this.api.archivePlan(current).subscribe({ next: plan => { this.load(plan); this.saving.set(false); this.settingsOpen.set(false); this.show('Plano arquivado.', false); }, error: error => { this.saving.set(false); this.show(errorMessage(error), true); } }); }
  addAiSuggestion(): void { this.generateAiSuggestion(); }
  generateAiSuggestion(): void {
    const payload = this.payload();
    if (!payload || this.form.invalid || this.assistantLoading()) return;
    this.assistantLoading.set(true); this.pendingSuggestion.set(null);
    this.api.savePlan(payload).pipe(tap(saved => this.load(saved)), switchMap(saved => this.api.generatePlanSuggestion(saved.id))).subscribe({
      next: suggestion => { this.pendingSuggestion.set(suggestion); this.assistantLoading.set(false); },
      error: error => { this.assistantLoading.set(false); this.show(errorMessage(error), true); }
    });
  }
  applyAiSuggestion(): void {
    const current = this.plan(), suggestion = this.pendingSuggestion();
    if (!current || !suggestion || this.assistantLoading()) return;
    const previousDays = current.days.length;
    const previousExercises = current.days.reduce((sum, day) => sum + day.items.length, 0);
    this.assistantLoading.set(true);
    this.api.applySuggestion(current, suggestion.id, crypto.randomUUID()).subscribe({
      next: plan => { this.load(plan); this.aiResult.set({ source:suggestion.source, days:Math.max(0, plan.days.length - previousDays), exercises:Math.max(0, plan.days.reduce((sum, day) => sum + day.items.length, 0) - previousExercises) }); this.pendingSuggestion.set(null); this.assistantLoading.set(false); this.assistantOpen.set(false); this.show('Sugestão aplicada ao rascunho para sua revisão.', false); },
      error: error => { this.assistantLoading.set(false); this.show(errorMessage(error), true); }
    });
  }
  proposedExerciseCount(suggestion: AssistantSuggestion): number { return (suggestion.completion?.newDays ?? []).reduce((sum, day) => sum + day.items.length, 0) + (suggestion.completion?.existingDayAdditions ?? []).reduce((sum, day) => sum + day.items.length, 0); }
  isExpanded(index: number): boolean { return this.expandedDays().has(index); }
  toggleDay(index: number): void { this.expandedDays.set(new Set([index])); this.expandedItems.set(new Set()); }
  expandAll(): void { this.expandedDays.set(new Set(this.days.length ? [0] : [])); }
  collapseAll(): void { this.expandedDays.set(new Set()); }
  status(value: string): string { return { DRAFT:'Rascunho', ACTIVE:'Ativo', ARCHIVED:'Arquivado' }[value] ?? value; }

  private load(plan: WorkoutPlan): void { this.plan.set(plan); this.form.patchValue({ name:plan.name,unitId:plan.unitId,goal:plan.goal,targetDaysPerWeek:plan.targetDaysPerWeek }); this.days.clear(); plan.days.forEach(day => this.days.push(this.dayGroup(day))); this.expandedDays.set(new Set(plan.days.length ? [0] : [])); this.loading.set(false); this.api.eligibleExercises(plan.unitId).subscribe({ next: page => this.exercises.set(page.content), error: error => this.show(errorMessage(error), true) }); }
  private dayGroup(day: Partial<WorkoutDay>): FormGroup { return this.fb.group({ id:[day.id ?? null], position:[day.position ?? 1,[Validators.required,Validators.min(1),Validators.max(7)]], name:[day.name ?? '',Validators.required], items:this.fb.array((day.items ?? []).map(item => this.itemGroup(item))) }); }
  private itemGroup(item: Partial<WorkoutDay['items'][number]>): FormGroup { return this.fb.group({ id:[item.id ?? null], exerciseId:[item.exerciseId ?? '',Validators.required], position:[item.position ?? 1,[Validators.required,Validators.min(1)]], sets:[item.sets ?? null], repetitionMin:[item.repetitionMin ?? null], repetitionMax:[item.repetitionMax ?? null], durationSeconds:[item.durationSeconds ?? null], restSeconds:[item.restSeconds ?? null], optionalLoadKg:[item.optionalLoadKg ?? null], notes:[item.notes ?? null] }); }
  private payload(): WorkoutPlan | null { const current = this.plan(); if (!current) return null; this.normalize(); return { ...current,name:this.form.controls.name.value ?? '',unitId:this.form.controls.unitId.value ?? '',goal:(this.form.controls.goal.value ?? 'GENERAL_FITNESS') as Goal,targetDaysPerWeek:this.targetDays(),days:this.form.controls.days.getRawValue() as WorkoutDay[] }; }
  private normalize(): void { this.days.controls.forEach((day,index) => { day.get('position')?.setValue(index + 1); this.items(index).controls.forEach((item,itemIndex) => item.get('position')?.setValue(itemIndex + 1)); }); }
  private show(message: string, failed: boolean): void { this.message.set(message); this.failed.set(failed); }
}
