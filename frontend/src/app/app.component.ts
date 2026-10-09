import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (auth.authenticated()) {
      <div class="app-shell">
        <aside class="sidebar">
          <a class="brand sidebar-brand" routerLink="/" aria-label="GymFlow — início"><span class="brand-mark">GF</span><span>GymFlow</span></a>
          <nav class="side-nav" aria-label="Navegação principal">
            <a routerLink="/" routerLinkActive="active" [routerLinkActiveOptions]="{exact:true}">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M3 11.5 12 4l9 7.5v8a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z"/></svg><span>Início</span>
            </a>
            @if (auth.hasRole('STUDENT')) {
              <a routerLink="/student/plans" routerLinkActive="active">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 5v14m12-14v14M3 8h18M3 16h18M1.5 10v4M22.5 10v4"/></svg><span>Meus treinos</span>
              </a>
              <a routerLink="/student/profile" routerLinkActive="active">
                <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="8" r="4"/><path d="M4 21c.7-4.2 3.4-6.5 8-6.5s7.3 2.3 8 6.5"/></svg><span>Meu perfil</span>
              </a>
            }
            @if (auth.hasRole('GYM_ADMIN')) {
              <a routerLink="/admin" routerLinkActive="active">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 21V8l8-4 8 4v13M8 21v-5h8v5M8 10h.01M12 10h.01M16 10h.01"/></svg><span>Administração</span>
              </a>
            }
          </nav>
          <div class="sidebar-footer">
            <span class="avatar">{{ (auth.profile()?.firstName || auth.profile()?.username || 'U').charAt(0).toUpperCase() }}</span>
            <div><strong>{{ auth.profile()?.firstName || auth.profile()?.username }}</strong><small>{{ auth.hasRole('GYM_ADMIN') ? 'Administrador' : 'Aluno' }}</small></div>
            <button class="icon-button" type="button" (click)="auth.logout()" aria-label="Sair">↗</button>
          </div>
        </aside>
        <div class="app-content">
          <header class="workspace-bar">
            <div><span class="workspace-dot"></span><span>{{ auth.hasRole('GYM_ADMIN') ? 'Gestão da academia' : 'Espaço de treino' }}</span></div>
            <span class="environment-badge">Ambiente local</span>
          </header>
          <main class="authenticated-main">
            @if (auth.initializationError()) {
              <div class="auth-error" role="alert"><span>{{ auth.initializationError() }}</span><button class="button compact" type="button" (click)="auth.login()">Tentar entrar</button></div>
            }
            <router-outlet />
          </main>
        </div>
      </div>
    } @else {
      <header class="public-topbar">
        <a class="brand" routerLink="/" aria-label="GymFlow — início"><span class="brand-mark">GF</span><span>GymFlow</span></a>
        <button class="button compact" type="button" (click)="auth.login()">Entrar</button>
      </header>
      <main class="public-main">
        @if (auth.initializationError()) {
          <div class="auth-error" role="alert"><span>{{ auth.initializationError() }}</span><button class="button compact" type="button" (click)="auth.login()">Tentar entrar</button></div>
        }
        <router-outlet />
      </main>
    }
  `
})
export class AppComponent { readonly auth = inject(AuthService); }
