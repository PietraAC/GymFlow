ALTER TABLE workout_plans
    ADD COLUMN inventory_revalidation_required BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE processed_inventory_events (
    event_id UUID PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    unit_id UUID NOT NULL,
    equipment_type_id UUID NOT NULL,
    aggregate_version BIGINT NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE inventory_event_versions (
    unit_id UUID NOT NULL,
    equipment_type_id UUID NOT NULL,
    aggregate_version BIGINT NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (unit_id, equipment_type_id)
);

CREATE INDEX idx_workout_plans_inventory_revalidation
    ON workout_plans(unit_id, inventory_revalidation_required)
    WHERE status <> 'ARCHIVED';
