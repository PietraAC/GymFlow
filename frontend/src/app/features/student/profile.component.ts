import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { EquipmentType, Experience, Goal, StudentProfile } from '../../core/api.models';
import { errorMessage } from '../../core/error-message';

@Component({
  imports: [FormsModule], changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="page-heading"><div><div class="eyebrow">Preferências pessoais</div><h1>Meu perfil de treino</h1><p>Esses dados orientam o assistente ao analisar seu rascunho semanal.</p></div></header>
    @if (loading()) { <div class="card muted">Carregando perfil…</div> }
    @else {
      <form class="card stack" (ngSubmit)="save()">
        <div class="field-grid">
          <label>Objetivo
            <select name="goal" [(ngModel)]="profile.goal" required>
              <option value="HYPERTROPHY">Hipertrofia</option><option value="STRENGTH">Força</option><option value="GENERAL_FITNESS">Condicionamento geral</option><option value="ENDURANCE">Resistência</option>
            </select>
          </label>
          <label>Experiência
            <select name="experience" [(ngModel)]="profile.experienceLevel" required>
              <option value="BEGINNER">Iniciante</option><option value="INTERMEDIATE">Intermediário</option><option value="ADVANCED">Avançado</option>
            </select>
          </label>
          <label>Dias por semana<input type="number" name="days" min="1" max="7" [(ngModel)]="profile.daysPerWeek" required></label>
          <label>Duração por sessão (min)<input type="number" name="duration" min="10" max="180" [(ngModel)]="profile.sessionDurationMinutes" required></label>
        </div>
        <fieldset style="border:0;padding:0"><legend style="font-weight:700;margin-bottom:.7rem">Equipamentos preferidos</legend>
          <div class="field-grid">
            @for (type of equipment(); track type.id) {
              <label style="display:flex;grid-template-columns:auto 1fr;align-items:center"><input style="width:auto" type="checkbox" [checked]="selected(type.id)" (change)="toggle(type.id)">{{ type.name }}</label>
            }
          </div>
        </fieldset>
        @if (message()) { <div class="feedback" [class.error]="failed()" role="status">{{ message() }}</div> }
        <div><button class="button" [disabled]="saving()">{{ saving() ? 'Salvando…' : 'Salvar perfil' }}</button></div>
      </form>
    }
  `
})
export class ProfileComponent {
  private readonly api = inject(ApiService);
  readonly loading = signal(true); readonly saving = signal(false); readonly equipment = signal<EquipmentType[]>([]);
  readonly message = signal(''); readonly failed = signal(false);
  profile: StudentProfile = { goal:'GENERAL_FITNESS' as Goal, experienceLevel:'BEGINNER' as Experience, daysPerWeek:3, sessionDurationMinutes:60, preferredEquipmentTypeIds:[] };
  constructor() {
    this.api.equipmentTypes().subscribe({ next: page => this.equipment.set(page.content), error: error => this.show(errorMessage(error), true) });
    this.api.profile().subscribe({ next: profile => { this.profile = profile; this.loading.set(false); }, error: error => { if ((error as {status?:number}).status !== 404) this.show(errorMessage(error), true); this.loading.set(false); } });
  }
  selected(id: string): boolean { return this.profile.preferredEquipmentTypeIds.includes(id); }
  toggle(id: string): void { this.profile.preferredEquipmentTypeIds = this.selected(id) ? this.profile.preferredEquipmentTypeIds.filter(value => value !== id) : [...this.profile.preferredEquipmentTypeIds, id]; }
  save(): void { this.saving.set(true); this.api.saveProfile(this.profile).subscribe({ next: profile => { this.profile=profile; this.saving.set(false); this.show('Perfil salvo.', false); }, error: error => { this.saving.set(false); this.show(errorMessage(error), true); } }); }
  private show(message:string, failed:boolean):void { this.message.set(message); this.failed.set(failed); }
}
