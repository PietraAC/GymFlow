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

The Playwright journey expects the complete local environment to already be running. It authenticates through the real Keycloak realm, saves the student profile, creates an empty draft, reviews the deterministic AI proposal, and explicitly applies it before checking the validated days and exercises. CI always uses the deterministic provider. Documentation screenshots can be refreshed from fictional local data with `GYMFLOW_CAPTURE_DOCS=true`; an external provider is never required for that workflow.

GitHub Actions runs the frontend tests and production build independently, then starts the complete local stack for the authenticated Chromium journey after both backend and frontend checks pass.

The code is organized into `core/` for authentication, interceptors, and API clients, `shared/` for reusable presentation components, and `features/` for home, administration, profile, and workout plan screens. Plan creation uses a compact two-step flow. The workout editor shows one training day at a time, uses expandable exercise rows, requests a structured AI completion for empty or partial drafts, requires an explicit review-and-apply action, and surfaces inventory revalidation warnings. The frontend does not receive provider credentials, trust model-created identifiers, persist tokens manually, or render API-provided HTML.

## Exercise GIFs

Every exercise row, expanded detail, and catalog result already reserves media space. To add a GIF later, place an optimized 4:3 file in `public/exercises/` using the exercise UUID as the filename:

```text
public/exercises/<exercise-id>.gif
```

The shared media component loads that path automatically and retains the designed placeholder when no matching file exists. No Angular template or stylesheet change is required.
