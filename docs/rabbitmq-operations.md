# RabbitMQ operations

`gym-service` writes an `EquipmentAvailabilityChanged.v1` record to `outbox_events` in the same database transaction as the inventory update. The scheduled publisher marks it `PUBLISHED` only after a positive broker confirmation and successful routing. Delivery is at least once, not exactly once.

`workout-service` consumes queue `gymflow.workout.inventory.v1`, records each `eventId`, ignores an older aggregate version, and marks non-archived plans in that unit for revalidation. Saving, activating, or applying an AI suggestion still performs the authoritative synchronous eligibility check.

## Failure and reprocessing

The listener retries three times with bounded backoff. A message that is still rejected is routed to `gymflow.workout.inventory.v1.dlq` through `gymflow.inventory.dlx`.

1. Inspect the workout-service logs and the DLQ message without editing its identifiers or version.
2. Correct the consumer/configuration cause and deploy or restart the fixed service.
3. In the RabbitMQ management UI (`http://localhost:15672` locally), move/publish the original payload from the DLQ to exchange `gymflow.inventory` using routing key `equipment.availability.changed.v1`.
4. Confirm the main queue drains and the DLQ no longer grows. Replaying the same `eventId` is safe because the consumer is idempotent.

The versioned event contract is [equipment-availability-changed-v1.schema.json](events/equipment-availability-changed-v1.schema.json).
