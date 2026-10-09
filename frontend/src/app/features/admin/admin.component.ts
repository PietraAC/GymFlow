import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../../core/api.service';
import { EquipmentType, Gym, GymUnit, UnitEquipment } from '../../core/api.models';
import { errorMessage } from '../../core/error-message';

@Component({
  imports: [FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="page-shell">
      <header class="page-heading compact-heading"><div><div class="eyebrow">Operação da academia</div><h1>Unidades e inventário</h1><p>Mantenha o catálogo dos alunos alinhado com a disponibilidade real.</p></div><div class="toolbar"><button class="button ghost compact" type="button" (click)="gymFormOpen.set(true)">＋ Academia</button><button class="button compact" type="button" (click)="unitFormOpen.set(true)" [disabled]="!selectedGymId()">＋ Unidade</button></div></header>
      @if (message()) { <div class="feedback" [class.error]="failed()" role="status">{{ message() }}</div> }

      <section class="context-bar panel">
        <div class="context-copy"><span class="context-icon">⌂</span><div><strong>Contexto de gestão</strong><small>Selecione onde deseja atualizar o inventário.</small></div></div>
        <label>Academia<select [ngModel]="selectedGymId()" (ngModelChange)="selectGym($event)" name="selectedGym">@for (gym of gyms(); track gym.id) { <option [value]="gym.id">{{ gym.name }}</option> }</select></label>
        <span class="context-separator">›</span>
        <label>Unidade<select [ngModel]="selectedUnitId()" (ngModelChange)="selectUnit($event)" name="selectedUnit">@for (unit of units(); track unit.id) { <option [value]="unit.id">{{ unit.name }} · {{ unit.city }}</option> }</select></label>
      </section>

      <section class="panel inventory-panel">
        <div class="panel-toolbar"><div><h2>Inventário da unidade</h2><span>Disponibilidade positiva torna exercícios elegíveis.</span></div><span class="inventory-count">{{ equipmentTypes().length }} tipos</span></div>
        @if (loadingGyms() || loadingUnits() || loadingInventory()) {
          <div class="skeleton-list"><span></span><span></span><span></span></div>
        } @else if (!selectedUnitId()) {
          <div class="empty-state compact-empty-state"><div class="empty-icon">⌂</div><h3>Selecione uma unidade</h3><p>O inventário aparecerá aqui.</p></div>
        } @else {
          <div class="inventory-table" role="table" aria-label="Inventário da unidade">
            <div class="inventory-header" role="row"><span>Equipamento</span><span>Total</span><span>Disponível</span><span>Observação</span><span></span></div>
            @for (type of equipmentTypes(); track type.id) {
              <div class="inventory-row" role="row">
                <div class="equipment-name"><span class="availability-dot" [class.available]="draft(type.id).availableQuantity > 0"></span><span><strong>{{ type.name }}</strong><small>{{ type.description }}</small></span></div>
                <label><span class="mobile-label">Total</span><input type="number" min="0" max="10000" [ngModel]="draft(type.id).totalQuantity" (ngModelChange)="setDraft(type.id, 'totalQuantity', $event)"></label>
                <label><span class="mobile-label">Disponível</span><input type="number" min="0" max="10000" [ngModel]="draft(type.id).availableQuantity" (ngModelChange)="setDraft(type.id, 'availableQuantity', $event)"></label>
                <label><span class="mobile-label">Observação</span><input maxlength="500" placeholder="Opcional" [ngModel]="draft(type.id).notes" (ngModelChange)="setDraft(type.id, 'notes', $event)"></label>
                <button class="button ghost compact" type="button" (click)="saveInventory(type.id)" [disabled]="saving()">Salvar</button>
              </div>
            } @empty { <div class="compact-empty"><span>Nenhum tipo de equipamento cadastrado.</span></div> }
          </div>
        }
      </section>
    </div>

    @if (gymFormOpen()) {
      <div class="drawer-backdrop" (click)="gymFormOpen.set(false)"></div><aside class="drawer small-drawer"><div class="drawer-header"><div><span class="section-kicker">Novo cadastro</span><h2>Adicionar academia</h2></div><button class="icon-button" type="button" (click)="gymFormOpen.set(false)">×</button></div><form class="stack" (ngSubmit)="createGym()"><label>Nome da academia<input name="gymName" [(ngModel)]="newGymName" required maxlength="120" placeholder="Ex.: GymFlow Centro"></label><button class="button full-width" [disabled]="!newGymName.trim() || saving()">Criar academia</button></form></aside>
    }
    @if (unitFormOpen()) {
      <div class="drawer-backdrop" (click)="unitFormOpen.set(false)"></div><aside class="drawer small-drawer"><div class="drawer-header"><div><span class="section-kicker">Nova unidade</span><h2>Adicionar unidade</h2></div><button class="icon-button" type="button" (click)="unitFormOpen.set(false)">×</button></div><form class="stack" (ngSubmit)="createUnit()"><label>Nome<input name="unitName" [(ngModel)]="newUnitName" required maxlength="120"></label><label>Cidade<input name="unitCity" [(ngModel)]="newUnitCity" required maxlength="100"></label><button class="button full-width" [disabled]="!selectedGymId() || !newUnitName.trim() || !newUnitCity.trim() || saving()">Adicionar unidade</button></form></aside>
    }
  `
})
export class AdminComponent {
  private readonly api = inject(ApiService);
  readonly gyms = signal<Gym[]>([]);
  readonly units = signal<GymUnit[]>([]);
  readonly equipmentTypes = signal<EquipmentType[]>([]);
  readonly inventory = signal<UnitEquipment[]>([]);
  readonly selectedGymId = signal('');
  readonly selectedUnitId = signal('');
  readonly loadingGyms = signal(true);
  readonly loadingUnits = signal(false);
  readonly loadingInventory = signal(false);
  readonly saving = signal(false);
  readonly message = signal('');
  readonly failed = signal(false);
  readonly gymFormOpen = signal(false);
  readonly unitFormOpen = signal(false);
  private readonly drafts = new Map<string, { totalQuantity:number; availableQuantity:number; notes:string; version?:number }>();
  newGymName = '';
  newUnitName = '';
  newUnitCity = '';

  constructor() { this.loadGyms(); this.api.equipmentTypes().subscribe({ next: page => this.equipmentTypes.set(page.content), error: error => this.show(errorMessage(error), true) }); }
  loadGyms(): void { this.loadingGyms.set(true); this.api.gyms().subscribe({ next: page => { this.gyms.set(page.content); this.loadingGyms.set(false); if (page.content.length) this.selectGym(page.content[0].id); }, error: error => { this.loadingGyms.set(false); this.show(errorMessage(error), true); } }); }
  selectGym(id: string): void { this.selectedGymId.set(id); this.selectedUnitId.set(''); this.units.set([]); this.loadingUnits.set(true); this.api.units(id).subscribe({ next: page => { this.units.set(page.content); this.loadingUnits.set(false); if (page.content.length) this.selectUnit(page.content[0].id); }, error: error => { this.loadingUnits.set(false); this.show(errorMessage(error), true); } }); }
  selectUnit(id: string): void { this.selectedUnitId.set(id); this.loadInventory(); }
  loadInventory(): void { const id = this.selectedUnitId(); if (!id) return; this.loadingInventory.set(true); this.api.inventory(id).subscribe({ next: items => { this.inventory.set(items); this.drafts.clear(); items.forEach(item => this.drafts.set(item.equipmentTypeId, { totalQuantity:item.totalQuantity,availableQuantity:item.availableQuantity,notes:item.notes ?? '',version:item.version })); this.loadingInventory.set(false); }, error: error => { this.loadingInventory.set(false); this.show(errorMessage(error), true); } }); }
  draft(typeId: string) { return this.drafts.get(typeId) ?? { totalQuantity:0,availableQuantity:0,notes:'' }; }
  setDraft(typeId: string, field:'totalQuantity'|'availableQuantity'|'notes', value:string|number): void { const current = this.draft(typeId); this.drafts.set(typeId, { ...current,[field]:field === 'notes' ? value : Number(value) }); }
  createGym(): void { this.saving.set(true); this.api.createGym(this.newGymName.trim()).subscribe({ next: gym => { this.newGymName = ''; this.gyms.update(items => [...items,gym]); this.selectGym(gym.id); this.saving.set(false); this.gymFormOpen.set(false); this.show('Academia criada.', false); }, error: error => { this.saving.set(false); this.show(errorMessage(error), true); } }); }
  createUnit(): void { this.saving.set(true); this.api.createUnit(this.selectedGymId(),this.newUnitName.trim(),this.newUnitCity.trim()).subscribe({ next: unit => { this.newUnitName = ''; this.newUnitCity = ''; this.units.update(items => [...items,unit]); this.selectUnit(unit.id); this.saving.set(false); this.unitFormOpen.set(false); this.show('Unidade criada.', false); }, error: error => { this.saving.set(false); this.show(errorMessage(error), true); } }); }
  saveInventory(typeId: string): void { const value = this.draft(typeId); if (value.availableQuantity > value.totalQuantity) { this.show('A quantidade disponível não pode superar o total.', true); return; } this.saving.set(true); this.api.saveInventory(this.selectedUnitId(),typeId,value).subscribe({ next: item => { this.drafts.set(typeId, { totalQuantity:item.totalQuantity,availableQuantity:item.availableQuantity,notes:item.notes ?? '',version:item.version }); this.saving.set(false); this.show('Inventário atualizado.', false); }, error: error => { this.saving.set(false); this.show(errorMessage(error), true); } }); }
  private show(message: string, failed: boolean): void { this.message.set(message); this.failed.set(failed); }
}
