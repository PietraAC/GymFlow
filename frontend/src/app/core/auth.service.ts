import { Injectable, signal } from '@angular/core';
import Keycloak, { KeycloakProfile } from 'keycloak-js';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly keycloak = new Keycloak({ url: `${window.location.origin}/keycloak`, realm: 'gymflow', clientId: 'gymflow-web' });
  readonly authenticated = signal(false);
  readonly profile = signal<KeycloakProfile | null>(null);
  readonly initializationError = signal<string | null>(null);
  readonly initialized = signal(false);

  async initialize(): Promise<void> {
    this.initializationError.set(null);
    try {
      const authenticated = await Promise.race([
        this.keycloak.init({
          onLoad: 'check-sso',
          pkceMethod: 'S256',
          checkLoginIframe: false,
          silentCheckSsoRedirectUri: `${window.location.origin}/silent-check-sso.html`,
          silentCheckSsoFallback: false
        }),
        new Promise<boolean>((_, reject) => window.setTimeout(() => reject(new Error('Tempo limite ao conectar com o Keycloak')), 6000))
      ]);
      this.authenticated.set(authenticated);
      this.profile.set(authenticated ? this.profileFromToken() : null);
    } catch (error) {
      console.error('Falha ao conectar com o Keycloak', error);
      this.authenticated.set(false);
      this.profile.set(null);
      this.initializationError.set('Não foi possível concluir a autenticação. Atualize a página ou entre novamente.');
    } finally {
      this.initialized.set(true);
    }
  }

  async login(): Promise<void> {
    this.initializationError.set(null);
    try {
      await this.keycloak.login({ redirectUri: window.location.origin });
    } catch (error) {
      console.error('Falha ao abrir o login do Keycloak', error);
      this.initializationError.set('Não foi possível abrir o login. Verifique se o ambiente local está ativo.');
    }
  }
  logout(): Promise<void> { return this.keycloak.logout({ redirectUri: window.location.origin }); }
  hasRole(role: string): boolean { return this.keycloak.hasRealmRole(role); }
  async token(): Promise<string | undefined> {
    if (!this.authenticated()) return undefined;
    await this.keycloak.updateToken(30);
    return this.keycloak.token;
  }

  private profileFromToken(): KeycloakProfile {
    const token = this.keycloak.tokenParsed;
    return {
      id: token?.sub,
      username: token?.['preferred_username'],
      email: token?.['email'],
      firstName: token?.['given_name'],
      lastName: token?.['family_name']
    };
  }
}
