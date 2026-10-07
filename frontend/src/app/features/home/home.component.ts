import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="hero">
      <div class="eyebrow">Treino com contexto real</div>
      <h1>Seu plano acompanha a academia onde você treina.</h1>
      <p>Monte a semana usando apenas exercícios elegíveis para os equipamentos disponíveis na unidade.</p>
      <div class="actions">
        @if (!auth.authenticated()) { <button class="button" (click)="auth.login()">Entrar no GymFlow</button> }
        @if (auth.hasRole('STUDENT')) { <a class="button" routerLink="/student/plans">Abrir meus treinos</a> }
        @if (auth.hasRole('GYM_ADMIN')) { <a class="button" routerLink="/admin">Gerenciar unidades</a> }
      </div>
      <div class="stage-note"><span>Etapa 2</span> Treino manual e elegibilidade ao vivo. Sugestões por IA chegam na próxima etapa.</div>
    </section>
    <section class="feature-grid" aria-label="Recursos atuais">
      <article><strong>01</strong><h2>Inventário conectado</h2><p>A disponibilidade cadastrada pela academia define o catálogo de cada unidade.</p></article>
      <article><strong>02</strong><h2>Semana sob controle</h2><p>Organize dias, ordem, séries, repetições, duração, descanso e carga opcional.</p></article>
      <article><strong>03</strong><h2>Ativação segura</h2><p>Antes de ativar, o plano é revalidado. Mudanças simultâneas são detectadas.</p></article>
    </section>
  `
})
export class HomeComponent { readonly auth = inject(AuthService); }
