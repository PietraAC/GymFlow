import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (!auth.authenticated()) {
      <section class="landing-hero">
        <div class="landing-copy">
          <div class="eyebrow">Treino com contexto real</div>
          <h1>Seu treino, organizado para a academia onde você está.</h1>
          <p>Monte planos claros usando somente exercícios compatíveis com os equipamentos disponíveis na sua unidade.</p>
          <div class="actions"><button class="button large" (click)="auth.login()">Começar agora <span aria-hidden="true">→</span></button><a class="text-link" routerLink="/como-funciona">Como funciona</a></div>
          <div class="landing-proof"><span><strong>01</strong> Escolha sua unidade</span><span><strong>02</strong> Monte seu plano</span><span><strong>03</strong> Revise com IA</span></div>
        </div>
        <div class="landing-preview" aria-label="Prévia da interface GymFlow">
          <div class="preview-window">
            <div class="preview-top"><span></span><span></span><span></span><small>Meu treino</small></div>
            <div class="preview-body">
              <div class="preview-title"><div><small>PLANO ATIVO</small><strong>Força e mobilidade</strong></div><span>3 dias</span></div>
              @for (day of ['Pernas e core', 'Peito e ombros', 'Costas e braços']; track day; let index = $index) {
                <div class="preview-row"><div class="preview-media">{{ index + 1 }}</div><div><strong>{{ day }}</strong><small>{{ 6 + index }} exercícios</small></div><span>›</span></div>
              }
            </div>
          </div>
        </div>
      </section>
      <section class="landing-features" id="como-funciona" aria-label="Como o GymFlow funciona">
        <article><span class="feature-number">01</span><div><h2>Catálogo elegível</h2><p>Você vê apenas movimentos possíveis com o inventário real da unidade.</p></div></article>
        <article><span class="feature-number">02</span><div><h2>Planejamento sem ruído</h2><p>Dias e exercícios organizados em uma interface compacta e fácil de revisar.</p></div></article>
        <article><span class="feature-number">03</span><div><h2>IA sob seu controle</h2><p>Receba complementos contextualizados e revise tudo antes de aplicar.</p></div></article>
      </section>
    } @else {
      <div class="page-shell">
        <header class="page-heading compact-heading">
          <div><div class="eyebrow">Visão geral</div><h1>Olá, {{ auth.profile()?.firstName || auth.profile()?.username }}.</h1><p>Continue de onde parou ou ajuste seu contexto de treino.</p></div>
        </header>
        <section class="dashboard-grid">
          @if (auth.hasRole('STUDENT')) {
            <a class="dashboard-card primary-card" routerLink="/student/plans"><span class="card-icon">↗</span><div><small>Seus planos</small><h2>Organize sua semana de treino</h2><p>Abra um rascunho, revise exercícios e mantenha o plano compatível com sua unidade.</p></div><strong>Ver meus treinos →</strong></a>
            <a class="dashboard-card" routerLink="/student/profile"><span class="card-icon muted-icon">◎</span><div><small>Preferências</small><h2>Seu perfil de treino</h2><p>Objetivo, experiência, frequência e equipamentos preferidos.</p></div><strong>Revisar perfil →</strong></a>
          }
          @if (auth.hasRole('GYM_ADMIN')) {
            <a class="dashboard-card primary-card" routerLink="/admin"><span class="card-icon">↗</span><div><small>Operação</small><h2>Unidades e inventário</h2><p>Mantenha o catálogo elegível alinhado com os equipamentos disponíveis.</p></div><strong>Abrir administração →</strong></a>
          }
        </section>
      </div>
    }
  `
})
export class HomeComponent { readonly auth = inject(AuthService); }
