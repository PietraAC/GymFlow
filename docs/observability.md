# Local observability

GymFlow exposes diagnostic metrics only in the default `local` profile. Health checks do not contact the AI provider and therefore never consume quota.

## Endpoints

| Service | Metrics | Prometheus |
| --- | --- | --- |
| gym-service | `http://localhost:8081/actuator/metrics` | `http://localhost:8081/actuator/prometheus` |
| workout-service | `http://localhost:8082/actuator/metrics` | `http://localhost:8082/actuator/prometheus` |
| assistant-service | `http://localhost:8083/actuator/metrics` | `http://localhost:8083/actuator/prometheus` |

Standard JVM, HTTP server, datasource, and RabbitMQ meters come from Spring Boot Actuator. Domain meters include:

- `gymflow.ai.suggestions`, tagged by provider source and outcome.
- `gymflow.ai.suggestion.duration`, tagged by provider source and outcome.
- `gymflow.ai.suggestion.changes`, recording the number of accepted changes.
- `gymflow.inventory.outbox.publications`, tagged as success or failure.
- `gymflow.inventory.events`, tagged as processed, rejected, or failed.

Example:

```powershell
Invoke-RestMethod http://localhost:8083/actuator/metrics/gymflow.ai.suggestions
curl.exe http://localhost:8083/actuator/prometheus
```

## Correlation IDs

HTTP services accept a bounded `X-Correlation-Id`, create one when absent, return it in the response, and propagate it in internal REST calls. Inventory messages use the event UUID as the message correlation ID and restore it into the consumer log context.

Local console logs include the correlation ID in brackets. Request bodies, bearer tokens, AI keys, and complete provider prompts are not logged.

## Operational limits

These endpoints support local diagnostics and a portfolio demonstration; this repository does not deploy a Prometheus server. In a real deployment, management endpoints must be isolated or authenticated and scraped over a private network.
