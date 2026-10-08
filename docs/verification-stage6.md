# Stage 6 verification record

Verified locally on October 8, 2026.

## Automated checks

| Check | Result |
| --- | --- |
| Backend Maven suite | Passed: 46 tests, including PostgreSQL/Flyway and RabbitMQ integration tests |
| Frontend unit tests | Passed: 9 tests across 4 files |
| Frontend production build | Passed: 351.08 kB initial bundle |
| Gemini HTTP adapter contract | Passed against the mock server, including the workout-completion response schema |
| Authenticated browser journey | Passed in Chromium with the deterministic local provider: empty draft to three-day workout |
| Opt-in Gemini browser journey | Passed locally: empty draft to a five-day, schema-valid workout; excluded from CI |
| Git diff validation | Passed with no whitespace errors |

New automated coverage verifies that:

- the demo provider creates all missing days with eligible exercise IDs;
- malformed completion output with invented IDs is rejected;
- direct suggestions are persisted as `WORKOUT_COMPLETION` proposals;
- `workout-service` preserves existing items while applying additions and missing days;
- an empty draft can invoke the assistant without first creating a day in the browser.

## Runtime verification

Docker Desktop, PostgreSQL, Keycloak, RabbitMQ, all three APIs, and the Angular frontend were started successfully. Testcontainers applied every migration to clean PostgreSQL 18 databases, including the new plan briefing and workout-completion payload migrations. The Playwright journey authenticated through Keycloak, created an empty three-day plan, invoked **Montar meu treino com IA**, and verified three rendered day cards with exercise details.

The external Gemini journey is intentionally excluded from CI because it requires a secret, quota, network access, and sends the bounded fictional profile/plan context to a third party. Its HTTP header, failure mapping, JSON schema, and completion parsing remain covered against a local mock server. An explicit local run with fictional demo data also completed and rendered a five-day workout. No API key or provider payload was written to tracked files, screenshots, or logs.
