# Stage 2 verification

Executed on October 7, 2026.

| Check | Result |
| --- | --- |
| `mvnw -pl services/gym-service,services/workout-service test` with Java 21.0.12, Maven 3.9.16, and Docker | Build completed |
| `gym-service` tests | 12 passed, 0 failed, 0 skipped |
| `workout-service` tests | 11 passed, 0 failed, 0 skipped |
| Testcontainers/Flyway for both data services | Migrations applied and persistence validated against real PostgreSQL |
| Real Keycloak and APIs | Tokens issued for both students; profile and plan create/edit/activate/archive validated; cross-student access returned `404` |
| `npm ci` | Reproducible lockfile, 278 packages audited, 0 known vulnerabilities at verification time |
| `npm run build` | Production bundle completed; 348.93 kB raw initial bundle after the authentication resilience fix |
| `npm test` | 3 files and 4 tests passed with Vitest 5.0.3/jsdom |
| `mvnw -DskipTests package` | All three services packaged |
| `docker compose --env-file .env.example config --quiet` | Valid PostgreSQL and Keycloak configuration |
| Parse `infra/keycloak/gymflow-realm.json` | Valid JSON |
| Search for snapshots, floating versions, and `innerHTML` | No occurrences |

The suites cover exercise mode and ordering rules, resource ownership, optimistic locking, inventory revalidation, fail-closed behavior, HTTP authorization, persistence/migrations, and frontend error translation. Angular strict templates are enabled.

The complete environment was also started through `start-gymflow.bat`; Keycloak, the frontend, and all three services reported healthy. The stop cycle preserves the PostgreSQL volume. The plan created by the smoke test was archived afterward.

Flyway emitted an informational warning because its bundled version had been officially tested through PostgreSQL 17 while Testcontainers selected PostgreSQL 18.6. Migrations V1/V2 completed successfully, and the local Compose image remains pinned to the version recorded by this project.
