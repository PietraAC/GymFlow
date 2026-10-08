ALTER TABLE suggestions ADD COLUMN suggestion_kind VARCHAR(30) NOT NULL DEFAULT 'ITEM_CHANGES';
ALTER TABLE suggestions ADD COLUMN completion_payload JSONB;

ALTER TABLE suggestions ADD CONSTRAINT ck_suggestion_kind
    CHECK (suggestion_kind IN ('ITEM_CHANGES', 'WORKOUT_COMPLETION'));

ALTER TABLE suggestions ADD CONSTRAINT ck_suggestion_payload
    CHECK (
        (suggestion_kind = 'ITEM_CHANGES' AND completion_payload IS NULL)
        OR
        (suggestion_kind = 'WORKOUT_COMPLETION' AND completion_payload IS NOT NULL)
    );
