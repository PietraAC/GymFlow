# Demo walkthrough

This walkthrough takes approximately five to eight minutes and uses only fictional local data.

## Preparation

1. Copy `.env.example` to the ignored `.env` and replace every `change-me` value.
2. Run `./start-gymflow.bat` and wait for all endpoints to become healthy.
3. Keep `AI_PROVIDER=demo` for a reproducible demonstration with no external API calls.

## Flow

1. Open `http://localhost:4200` and sign in as `admin.aurora` / `gymflow-admin-a`.
2. Show the assigned gym, its branches, and equipment quantities. Explain that another administrator cannot mutate this gym.
3. Sign out and sign in as `aluna.demo` / `gymflow-student-a`.
4. Open **Meu perfil**, choose a goal, experience level, weekly frequency, and session duration, then save.
5. Open **Meus treinos**, select a gym branch, name a plan, choose its goal and weekly frequency, and create the draft.
6. With the plan still empty, click **Completar com IA**, then **Gerar plano inicial**.
7. Show that the draft is saved first and that the assistant returns a separate proposal marked **Proposta em modo demo**. Review the explanation, observations, new-day count, and suggested-exercise count before making any change to the draft.
8. Click **Aplicar ao rascunho**. Show the requested week in the compact day tabs, open an exercise row to inspect its details, and point out that every exercise came from the eligible catalog without a free-text browser prompt or model-defined load.
9. Explain that the same provider contract can use Gemini by configuring `AI_PROVIDER=gemini`, `AI_MODEL`, and `GEMINI_API_KEY` only in the ignored local environment. The browser never sees the key; the assistant sends bounded context and `workout-service` revalidates the proposal only after explicit confirmation.
10. Return as the administrator and set a relevant equipment quantity to zero. Show the RabbitMQ outbox/consumer path and the revalidation warning on affected plans.
11. Restore the quantity and revalidate the plan to demonstrate that REST eligibility remains authoritative.

## Failure story

Stop only `assistant-service` and click the workout-completion button again. The interface reports the dependency failure while manual editing and saving remain available. Restart the environment afterward with `start-gymflow.bat`.

## Evidence to point out

- OAuth 2.0 Authorization Code + PKCE in the browser.
- Ownership derived from JWT `sub`, never from a client-supplied student identifier.
- Schema-constrained workout completion, explicit human review, and server-side validation before idempotent application.
- A model cannot activate the plan, invent catalog exercises, remove existing content, set load in kilograms, or write directly to service databases.
- Transactional outbox, publisher confirms, bounded retries, DLQ, and an idempotent consumer.
- Correlation IDs and local Prometheus metrics.
- GitHub Actions for backend, frontend, production build, and a real Chromium flow.
