# Stage 3 verification

Executed on October 7, 2026.

| Check | Result |
| --- | --- |
| Maven reactor with Java 21.0.12, Maven 3.9.16, Docker, and Testcontainers | All service modules completed after the timestamp assertion was aligned with PostgreSQL microsecond precision |
| `gym-service` tests | 12 passed, 0 failed, 0 skipped |
| `workout-service` tests | 13 passed, 0 failed, 0 skipped |
| `assistant-service` tests | 14 passed, 0 failed, 0 skipped |
| Testcontainers/Flyway | Gym V1/V2, workout V1/V2/V3, and assistant V1/V2 applied against real PostgreSQL |
| Gemini adapter tests | Structured response, invalid JSON, HTTP 429, HTTP 503, timeout, and no-retry behavior validated with a local mock HTTP server; no provider key or external request used |
| Output validation tests | Invented IDs, invalid operation shape, cross-day item references, prompt injection, and professional-guidance behavior covered |
| `npm test` | 3 files and 7 tests passed with Vitest 5.0.3/jsdom after the direct-action adaptation |
| `npm run build` | Production build completed; 350.22 kB raw initial bundle |
| Local start script | PostgreSQL, Keycloak, all three APIs, and Angular reported healthy |
| Authenticated Stage 3 smoke flow | Keycloak token, saved draft, DEMO suggestion, authoritative fetch, eligibility revalidation, confirmed application, idempotent replay, and archive all completed |
| Optimistic version smoke assertion | Version advanced from 0 on creation to 1 on draft save and 2 on suggestion application |
| `git diff --check` | No whitespace errors |

The assistant stores owned conversations and bounded history, builds context only through authenticated service APIs, persists expiring suggestion snapshots, and visibly identifies the deterministic DEMO source. The optional Gemini adapter uses structured output, explicit timeouts, bounded output, local rate/concurrency limits, and does not silently fall back when configuration is missing.

Suggestion application remains owned by `workout-service`: it fetches the suggestion from `assistant-service`, checks the plan and base version, validates expiration and single use, rebuilds the final aggregate, revalidates all exercise IDs with `gym-service`, and persists the plan plus idempotency record in one local transaction. The Angular editor now treats the “Adicionar sugestão da IA” click as explicit confirmation: it saves first, requests server-authored structured JSON, then applies the validated result without exposing a free-text prompt.

Flyway emitted an informational warning because the bundled version had been tested through PostgreSQL 17 while the pinned container is PostgreSQL 18.6. Every migration completed successfully. The final local environment was left running for manual review, and both smoke-test plans were archived.
