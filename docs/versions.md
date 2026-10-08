# Pinned versions

Verified against official sources on October 7, 2026.

| Component | Version | Notes |
| --- | --- | --- |
| Java | 21 | Source and compiler target |
| Spring Boot | 3.5.16 | Stable 3.5 line compatible with Java 21 |
| springdoc-openapi | 2.9.1 | Spring Boot 3 compatible 2.x line |
| Maven | 3.9.16 | Maven Wrapper distribution |
| Maven Wrapper | 3.3.4 | Pinned wrapper release |
| PostgreSQL | 18.6 | Official `postgres:18.6-alpine3.24` image |
| Flyway | 11.7.2 | Managed by the Spring Boot BOM; PostgreSQL support uses its dedicated module |
| PostgreSQL JDBC | 42.7.11 | Managed by the Spring Boot BOM |
| Keycloak | 26.7.3 | Official Quay image |
| RabbitMQ | 4.3.6 | Official management Alpine image; durable exchanges/queues and publisher confirms |
| Testcontainers | 1.21.3 | Pinned BOM for PostgreSQL integration tests |
| Angular | 22.2.1 | Standalone components and reactive forms |
| Angular CLI / build | 22.2.2 | `@angular/build` esbuild/Vite builder |
| TypeScript | 6.0.3 | Angular 22.2 compatible range |
| RxJS | 7.8.2 | Angular-supported release |
| Keycloak JS | 26.2.4 | Browser OIDC adapter with PKCE S256 |
| Node.js | 24.15.0+ | Locally verified; Angular 22 accepts `^24.15.0` |
| Vitest | 5.0.3 | Frontend test runner |
| Playwright | 1.64.0 | Real Chromium end-to-end journey |
| Micrometer Prometheus registry | 1.15.12 | Managed by the Spring Boot BOM |
| Gemini model | `gemini-3.5-flash-lite` | Configurable default for the optional adapter; quota and availability remain project/account dependent |

Sources: [Spring Boot](https://docs.spring.io/spring-boot/3.5/reference/index.html), [springdoc](https://github.com/springdoc/springdoc-openapi), [Maven](https://maven.apache.org/download.cgi), [Maven Wrapper](https://maven.apache.org/tools/wrapper/download.cgi), [PostgreSQL](https://www.postgresql.org/docs/current/), [PostgreSQL image](https://hub.docker.com/_/postgres), [Keycloak](https://www.keycloak.org/downloads.html), [RabbitMQ image](https://hub.docker.com/_/rabbitmq), [RabbitMQ publisher confirms](https://www.rabbitmq.com/docs/confirms), [Keycloak JavaScript adapter](https://www.keycloak.org/securing-apps/javascript-adapter), [Angular compatibility](https://angular.dev/reference/versions), [Gemini models](https://ai.google.dev/gemini-api/docs/models), [Gemini structured output](https://ai.google.dev/gemini-api/docs/structured-output), and the official npm package metadata pinned by `package-lock.json`.
