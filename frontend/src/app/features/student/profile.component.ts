import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { EquipmentType, Experience, Goal, StudentProfile } from '../../core/api.models';
import { errorMessage } from '../../core/error-message';

@Component({
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page-shell narrow-page">
      <header class="page-heading compact-heading"><div><div class="eyebrow">Preferências pessoais</div><h1>Meu perfil de treino</h1><p>Esses dados ajudam o assistente a respeitar seu ritmo e seus objetivos.</p></div></header>
      @if (loading()) {
        <div class="panel loading-panel">Carregando perfil…</div>
      } @else {
        <form class="profile-form" (ngSubmit)="save()">
          <section class="panel form-section">
            <div class="section-heading"><div><span class="section-kicker">Planejamento</span><h2>Objetivo e rotina</h2><p>Use valores que representem sua rotina atual.</p></div></div>
            <div class="field-grid"><label>Objetivo<select name="goal" [(ngModel)]="profile.goal" required><option value="HYPERTROPHY">Hipertrofia</option><option value="STRENGTH">Força</option><option value="GENERAL_FITNESS">Condicionamento geral</option><option value="ENDURANCE">Resistência</option></select></label><label>Experiência<select name="experience" [(ngModel)]="profile.experienceLevel" required><option value="BEGINNER">Iniciante</option><option value="INTERMEDIATE">Intermediário</option><option value="ADVANCED">Avançado</option></select></label><label>Dias por semana<input type="number" name="days" min="1" max="7" [(ngModel)]="profile.daysPerWeek" required></label><label>Duração por sessão (min)<input type="number" name="duration" min="10" max="180" [(ngModel)]="profile.sessionDurationMinutes" required></label></div>
          </section>
          <section class="panel form-section">
            <div class="section-heading"><div><span class="section-kicker">Preferências</span><h2>Equipamentos favoritos</h2><p>Marque o que você prefere usar quando estiver disponível.</p></div><span class="selection-count">{{ profile.preferredEquipmentTypeIds.length }} selecionado(s)</span></div>
            <div class="choice-grid">
              @for (type of equipment(); track type.id) {
                <label class="choice-card" [class.selected]="selected(type.id)"><input type="checkbox" [checked]="selected(type.id)" (change)="toggle(type.id)"><span class="choice-check">✓</span><span><strong>{{ type.name }}</strong><small>{{ type.description }}</small></span></label>
              }
            </div>
          </section>
          @if (message()) { <div class="feedback" [class.error]="failed()" role="status">{{ message() }}</div> }
          <div class="sticky-form-actions"><span>As alterações serão usadas nas próximas sugestões.</span><button class="button" [disabled]="saving()">{{ saving() ? 'Salvando…' : 'Salvar alterações' }}</button></div>
        </form>
      }
    </div>
  `
})
export class ProfileComponent {
  private readonly api = inject(ApiService);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly equipment = signal<EquipmentType[]>([]);
  readonly message = signal('');
  readonly failed = signal(false);
  profile: StudentProfile = { goal:'GENERAL_FITNESS' as Goal, experienceLevel:'BEGINNER' as Experience, daysPerWeek:3, sessionDurationMinutes:60, preferredEquipmentTypeIds:[] };

  constructor() {
    this.api.equipmentTypes().subscribe({ next: page => this.equipment.set(page.content), error: error => this.show(errorMessage(error), true) });
    this.api.profile().subscribe({ next: profile => { this.profile = profile; this.loading.set(false); }, error: error => { if ((error as {status?:number}).status !== 404) this.show(errorMessage(error), true); this.loading.set(false); } });
  }
  selected(id: string): boolean { return this.profile.preferredEquipmentTypeIds.includes(id); }
  toggle(id: string): void { this.profile.preferredEquipmentTypeIds = this.selected(id) ? this.profile.preferredEquipmentTypeIds.filter(value => value !== id) : [...this.profile.preferredEquipmentTypeIds, id]; }
  save(): void { this.saving.set(true); this.api.saveProfile(this.profile).subscribe({ next: profile => { this.profile = profile; this.saving.set(false); this.show('Perfil salvo.', false); }, error: error => { this.saving.set(false); this.show(errorMessage(error), true); } }); }
  private show(message: string, failed: boolean): void { this.message.set(message); this.failed.set(failed); }
}
