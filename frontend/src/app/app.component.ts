import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="topbar">
      <a class="brand" routerLink="/" aria-label="GymFlow — início"><span class="brand-mark">GF</span><span>GymFlow</span></a>
      <nav aria-label="Navegação principal">
        @if (auth.hasRole('GYM_ADMIN')) { <a routerLink="/admin" routerLinkActive="active">Administração</a> }
        @if (auth.hasRole('STUDENT')) {
          <a routerLink="/student/profile" routerLinkActive="active">Meu perfil</a>
          <a routerLink="/student/plans" routerLinkActive="active">Meus treinos</a>
        }
      </nav>
      <div class="account">
        @if (auth.authenticated()) {
          <span class="account-name">{{ auth.profile()?.firstName || auth.profile()?.username }}</span>
          <button class="button ghost compact" type="button" (click)="auth.logout()">Sair</button>
        } @else {
          <button class="button compact" type="button" (click)="auth.login()">Entrar</button>
        }
      </div>
    </header>
    <main>
      @if (auth.initializationError()) {
        <div class="auth-error" role="alert">
          <span>{{ auth.initializationError() }}</span>
          <button class="button compact" type="button" (click)="auth.login()">Tentar entrar</button>
        </div>
      }
      <router-outlet />
    </main>
  `
})
export class AppComponent { readonly auth = inject(AuthService); }
