# Stage 4 verification

Executed on October 7, 2026.

| Check | Result |
| --- | --- |
| Maven backend suites | 41 tests passed: 13 `gym-service`, 14 `workout-service`, and 14 `assistant-service` |
| RabbitMQ/Testcontainers | Real RabbitMQ 4.3.6 received a persistent outbox event after correlated publisher confirmation |
| PostgreSQL/Testcontainers | Gym V1–V3, workout V1–V4, and assistant V1–V2 migrations applied to PostgreSQL 18.6 |
| Consumer consistency | Duplicate `eventId` ignored; an older aggregate version did not replace the recorded version; affected plan marked for revalidation |
| Direct assistant action | Service unit test and frontend test cover saved-plan context, structured suggestion persistence, and save → generate → apply ordering |
| Frontend tests | 3 files and 7 tests passed, including active-plan inventory revalidation |
| Frontend production build | Completed; 349.32 kB raw initial bundle |
| Authenticated local smoke | DEMO JSON added one item; RabbitMQ marked draft and active plans; REST revalidation cleared both flags; inventory was restored; outbox `PUBLISHED` with zero retries; main queue and DLQ drained |
| No-op inventory update | Kept the same entity version and outbox row count (4 → 4), so only effective changes emit events |
| Whitespace validation | `git diff --check` reported no errors |

The event path has at-least-once delivery semantics. `gym-service` persists inventory and its outbox row in one transaction, publishes with a persistent message and broker confirmation, and retains failed rows for retry. `workout-service` records the event and its local plan signal in one transaction, retries a bounded number of times, and routes exhausted messages to a durable DLQ.

The asynchronous signal does not authorize a workout mutation. Saving, activating, and applying a suggestion continue to call the synchronous eligibility API and fail closed when the current state cannot be confirmed. Reprocessing instructions are in [rabbitmq-operations.md](rabbitmq-operations.md).
