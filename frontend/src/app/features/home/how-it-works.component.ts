import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';

@Component({
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="how-page">
      <section class="how-hero">
        <a routerLink="/" class="back-link">← Voltar ao início</a>
        <div class="eyebrow">Como funciona</div>
        <h1>Da unidade certa ao plano pronto para revisar.</h1>
        <p>O GymFlow conecta o inventário da academia, a organização semanal do aluno e sugestões estruturadas de IA em um fluxo simples e verificável.</p>
      </section>

      <section class="how-steps" aria-label="Etapas do GymFlow">
        <article>
          <span class="how-number">01</span>
          <div class="how-step-copy"><h2>Escolha a academia e a unidade</h2><p>A unidade define o contexto do plano. O GymFlow consulta o inventário cadastrado e prepara o catálogo compatível com aquele local.</p></div>
          <div class="how-visual context-visual" aria-hidden="true"><span>Academia</span><strong>GymFlow Centro</strong><i>›</i><span>Unidade</span><strong>Centro · Belo Horizonte</strong><b>Catálogo conectado</b></div>
        </article>
        <article>
          <span class="how-number">02</span>
          <div class="how-step-copy"><h2>Organize os dias e exercícios</h2><p>Crie os dias do plano, escolha exercícios elegíveis e ajuste séries, repetições, duração, descanso e observações.</p></div>
          <div class="how-visual days-visual" aria-hidden="true"><div><span>Dia 1</span><strong>Pernas e core</strong><small>6 exercícios</small></div><div><span>Dia 2</span><strong>Peito e ombros</strong><small>7 exercícios</small></div><div><span>Dia 3</span><strong>Costas e braços</strong><small>8 exercícios</small></div></div>
        </article>
        <article>
          <span class="how-number">03</span>
          <div class="how-step-copy"><h2>Revise a proposta da IA</h2><p>O assistente pode completar um rascunho usando seu objetivo, perfil e catálogo elegível. A proposta só entra no plano depois da sua confirmação.</p></div>
          <div class="how-visual review-visual" aria-hidden="true"><span>✦ Proposta pronta</span><strong>3 dias · 19 exercícios</strong><small>Revisar antes de aplicar</small><button tabindex="-1">Aplicar ao rascunho</button></div>
        </article>
      </section>

      <section class="how-principles">
        <div><span class="section-kicker">Princípios do produto</span><h2>O aluno continua no controle.</h2></div>
        <div class="principle-grid"><article><strong>Contexto real</strong><p>As opções dependem dos equipamentos disponíveis na unidade escolhida.</p></article><article><strong>Rascunho revisável</strong><p>Sugestões não ativam ou substituem um plano silenciosamente.</p></article><article><strong>Sem prescrição médica</strong><p>O catálogo organiza exercícios, mas não oferece diagnóstico ou tratamento.</p></article></div>
      </section>

      <section class="how-cta">
        <div><span class="section-kicker">Pronto para organizar?</span><h2>Comece pelo seu contexto de treino.</h2></div>
        @if (auth.authenticated() && auth.hasRole('STUDENT')) { <a class="button large" routerLink="/student/plans">Abrir meus treinos →</a> }
        @else if (auth.authenticated() && auth.hasRole('GYM_ADMIN')) { <a class="button large" routerLink="/admin">Abrir administração →</a> }
        @else { <button class="button large" type="button" (click)="auth.login()">Entrar no GymFlow →</button> }
      </section>
    </div>
  `
})
export class HowItWorksComponent {
  readonly auth = inject(AuthService);
}
