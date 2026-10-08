# GymFlow Web

Angular 22 standalone frontend covering GymFlow stages 2 through 6. It authenticates against the local `gymflow` realm with Authorization Code + PKCE S256 and reaches the gym, workout, and assistant services through the development proxy.

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
npx playwright install chromium
npm run e2e
```

The Playwright journey expects the complete local environment to already be running. It authenticates through the real Keycloak realm, saves the student profile, creates an empty draft, and verifies that the demo AI provider completes the requested week with validated days and exercises. CI always uses the deterministic provider. Documentation screenshots can be refreshed from fictional local data with `GYMFLOW_CAPTURE_DOCS=true`; an external provider is never required for that workflow.

The code is organized into `core/` for authentication, interceptors, and API clients, and `features/` for home, administration, profile, and workout plan screens. Plan creation captures goal and weekly frequency. The workout editor shows expandable days, can request a structured AI completion for empty or partial drafts, applies it only after backend validation, and surfaces inventory revalidation warnings. The frontend does not receive provider credentials, trust model-created identifiers, persist tokens manually, or render API-provided HTML.
