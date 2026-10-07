# GymFlow Web

Angular 22 standalone frontend for Stage 2. It authenticates against the local `gymflow` realm with Authorization Code + PKCE S256 and reaches both backend APIs through the development proxy.

```powershell
npm ci
npm start
```

Prerequisites: Keycloak on `:8080`, `gym-service` on `:8081`, and `workout-service` on `:8082`. Open `http://localhost:4200`.

Checks:

```powershell
npm run build
npm test
npm audit
```

The code is organized into `core/` for authentication, interceptors, and API clients, and `features/` for home, administration, profile, and workout plan screens. The frontend does not persist tokens manually, does not render API-provided HTML, and does not contain AI functionality yet.
