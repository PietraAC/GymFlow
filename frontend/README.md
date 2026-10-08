# GymFlow Web

Angular 22 standalone frontend covering GymFlow stages 2 through 4. It authenticates against the local `gymflow` realm with Authorization Code + PKCE S256 and reaches the gym, workout, and assistant services through the development proxy.

```powershell
npm ci
npm start
```

Prerequisites: Keycloak on `:8080`, `gym-service` on `:8081`, `workout-service` on `:8082`, and `assistant-service` on `:8083`. RabbitMQ is required for the stage 4 inventory event flow. Open `http://localhost:4200`.

Checks:

```powershell
npm run build
npm test
npm audit
```

The code is organized into `core/` for authentication, interceptors, and API clients, and `features/` for home, administration, profile, and workout plan screens. The workout editor can request a structured AI suggestion, apply it directly to the saved draft after backend validation, and surface inventory revalidation warnings. The frontend does not persist tokens manually or render API-provided HTML.
