import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { EquipmentType, Gym, GymUnit, UnitEquipment } from '../../core/api.models';
import { errorMessage } from '../../core/error-message';

@Component({
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="page-heading"><div><div class="eyebrow">Operação da academia</div><h1>Unidades e inventário</h1><p>Mantenha a disponibilidade que alimenta o catálogo dos alunos.</p></div></header>
    @if (message()) { <div class="feedback" [class.error]="failed()" role="status">{{ message() }}</div> }
    <div class="grid" style="margin-top:1rem">
      <section class="card stack">
        <h2>Academias</h2>
        <form class="toolbar" (ngSubmit)="createGym()">
          <label style="flex:1">Nome<input name="gymName" [(ngModel)]="newGymName" required maxlength="120"></label>
          <button class="button compact" [disabled]="!newGymName.trim() || saving()">Criar</button>
        </form>
        @if (loadingGyms()) { <p class="muted">Carregando academias…</p> }
        @else if (!gyms().length) { <div class="empty">Nenhuma academia vinculada.</div> }
        @else {
          <label>Academia ativa
            <select [ngModel]="selectedGymId()" (ngModelChange)="selectGym($event)" name="selectedGym">
              @for (gym of gyms(); track gym.id) { <option [value]="gym.id">{{ gym.name }}</option> }
            </select>
          </label>
        }
      </section>
      <section class="card stack">
        <h2>Unidades</h2>
        <form class="field-grid" (ngSubmit)="createUnit()">
          <label>Nome<input name="unitName" [(ngModel)]="newUnitName" required maxlength="120"></label>
          <label>Cidade<input name="unitCity" [(ngModel)]="newUnitCity" required maxlength="100"></label>
          <button class="button compact" [disabled]="!selectedGymId() || !newUnitName.trim() || !newUnitCity.trim() || saving()">Adicionar unidade</button>
        </form>
        @if (loadingUnits()) { <p class="muted">Carregando unidades…</p> }
        @else if (!units().length) { <div class="empty">Selecione uma academia com unidades.</div> }
        @else {
          <label>Unidade ativa
            <select [ngModel]="selectedUnitId()" (ngModelChange)="selectUnit($event)" name="selectedUnit">
              @for (unit of units(); track unit.id) { <option [value]="unit.id">{{ unit.name }} · {{ unit.city }}</option> }
            </select>
          </label>
        }
      </section>
    </div>
    <section class="card" style="margin-top:1rem">
      <div class="page-heading"><div><h2>Inventário da unidade</h2><p>O total registra o parque; disponível controla a elegibilidade.</p></div></div>
      @if (loadingInventory()) { <p class="muted">Carregando inventário…</p> }
      @else if (!selectedUnitId()) { <div class="empty">Selecione uma unidade.</div> }
      @else {
        <div class="stack">
          @for (type of equipmentTypes(); track type.id) {
            <div class="item">
              <div class="item-head"><strong>{{ type.name }}</strong><span class="muted">{{ type.description }}</span></div>
              <div class="field-grid">
                <label>Total<input type="number" min="0" max="10000" [ngModel]="draft(type.id).totalQuantity" (ngModelChange)="setDraft(type.id, 'totalQuantity', $event)"></label>
                <label>Disponível<input type="number" min="0" max="10000" [ngModel]="draft(type.id).availableQuantity" (ngModelChange)="setDraft(type.id, 'availableQuantity', $event)"></label>
                <label>Observação<input maxlength="500" [ngModel]="draft(type.id).notes" (ngModelChange)="setDraft(type.id, 'notes', $event)"></label>
                <div style="align-self:end"><button class="button secondary compact" type="button" (click)="saveInventory(type.id)" [disabled]="saving()">Salvar item</button></div>
              </div>
            </div>
          } @empty { <div class="empty">Nenhum tipo de equipamento cadastrado.</div> }
        </div>
      }
    </section>
  `
})
export class AdminComponent {
  private readonly api = inject(ApiService);
  readonly gyms = signal<Gym[]>([]); readonly units = signal<GymUnit[]>([]);
  readonly equipmentTypes = signal<EquipmentType[]>([]); readonly inventory = signal<UnitEquipment[]>([]);
  readonly selectedGymId = signal(''); readonly selectedUnitId = signal('');
  readonly loadingGyms = signal(true); readonly loadingUnits = signal(false); readonly loadingInventory = signal(false);
  readonly saving = signal(false); readonly message = signal(''); readonly failed = signal(false);
  private readonly drafts = new Map<string, { totalQuantity: number; availableQuantity: number; notes: string; version?: number }>();
  newGymName = ''; newUnitName = ''; newUnitCity = '';

  constructor() { this.loadGyms(); this.api.equipmentTypes().subscribe({ next: page => this.equipmentTypes.set(page.content), error: error => this.show(errorMessage(error), true) }); }

  loadGyms(): void { this.loadingGyms.set(true); this.api.gyms().subscribe({ next: page => { this.gyms.set(page.content); this.loadingGyms.set(false); if (page.content.length) this.selectGym(page.content[0].id); }, error: error => { this.loadingGyms.set(false); this.show(errorMessage(error), true); } }); }
  selectGym(id: string): void { this.selectedGymId.set(id); this.selectedUnitId.set(''); this.units.set([]); this.loadingUnits.set(true); this.api.units(id).subscribe({ next: page => { this.units.set(page.content); this.loadingUnits.set(false); if (page.content.length) this.selectUnit(page.content[0].id); }, error: error => { this.loadingUnits.set(false); this.show(errorMessage(error), true); } }); }
  selectUnit(id: string): void { this.selectedUnitId.set(id); this.loadInventory(); }
  loadInventory(): void { const id = this.selectedUnitId(); if (!id) return; this.loadingInventory.set(true); this.api.inventory(id).subscribe({ next: items => { this.inventory.set(items); this.drafts.clear(); items.forEach(item => this.drafts.set(item.equipmentTypeId, { totalQuantity:item.totalQuantity, availableQuantity:item.availableQuantity, notes:item.notes ?? '', version:item.version })); this.loadingInventory.set(false); }, error: error => { this.loadingInventory.set(false); this.show(errorMessage(error), true); } }); }
  draft(typeId: string) { return this.drafts.get(typeId) ?? { totalQuantity:0, availableQuantity:0, notes:'' }; }
  setDraft(typeId: string, field: 'totalQuantity'|'availableQuantity'|'notes', value: string|number): void { const current = this.draft(typeId); this.drafts.set(typeId, { ...current, [field]: field === 'notes' ? value : Number(value) }); }
  createGym(): void { this.saving.set(true); this.api.createGym(this.newGymName.trim()).subscribe({ next: gym => { this.newGymName=''; this.gyms.update(items => [...items, gym]); this.selectGym(gym.id); this.saving.set(false); this.show('Academia criada.', false); }, error: error => { this.saving.set(false); this.show(errorMessage(error), true); } }); }
  createUnit(): void { this.saving.set(true); this.api.createUnit(this.selectedGymId(), this.newUnitName.trim(), this.newUnitCity.trim()).subscribe({ next: unit => { this.newUnitName=''; this.newUnitCity=''; this.units.update(items => [...items, unit]); this.selectUnit(unit.id); this.saving.set(false); this.show('Unidade criada.', false); }, error: error => { this.saving.set(false); this.show(errorMessage(error), true); } }); }
  saveInventory(typeId: string): void { const value = this.draft(typeId); if (value.availableQuantity > value.totalQuantity) { this.show('A quantidade disponível não pode superar o total.', true); return; } this.saving.set(true); this.api.saveInventory(this.selectedUnitId(), typeId, value).subscribe({ next: item => { this.drafts.set(typeId, { totalQuantity:item.totalQuantity, availableQuantity:item.availableQuantity, notes:item.notes ?? '', version:item.version }); this.saving.set(false); this.show('Inventário atualizado.', false); }, error: error => { this.saving.set(false); this.show(errorMessage(error), true); } }); }
  private show(message: string, failed: boolean): void { this.message.set(message); this.failed.set(failed); }
}
