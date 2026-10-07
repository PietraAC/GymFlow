CREATE TABLE student_profiles (
    identity_subject VARCHAR(100) PRIMARY KEY,
    goal VARCHAR(30) NOT NULL CHECK (goal IN ('HYPERTROPHY','STRENGTH','GENERAL_FITNESS','ENDURANCE')),
    experience_level VARCHAR(30) NOT NULL CHECK (experience_level IN ('BEGINNER','INTERMEDIATE','ADVANCED')),
    days_per_week INTEGER NOT NULL CHECK (days_per_week BETWEEN 1 AND 7),
    session_duration_minutes INTEGER NOT NULL CHECK (session_duration_minutes BETWEEN 10 AND 180),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE student_profile_preferred_equipment (
    identity_subject VARCHAR(100) NOT NULL REFERENCES student_profiles(identity_subject) ON DELETE CASCADE,
    equipment_type_id UUID NOT NULL,
    PRIMARY KEY (identity_subject, equipment_type_id)
);

CREATE TABLE workout_plans (
    id UUID PRIMARY KEY,
    identity_subject VARCHAR(100) NOT NULL,
    unit_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT','ACTIVE','ARCHIVED')),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_workout_plans_owner ON workout_plans(identity_subject, updated_at DESC);

CREATE TABLE workout_days (
    id UUID PRIMARY KEY,
    plan_id UUID NOT NULL REFERENCES workout_plans(id) ON DELETE CASCADE,
    position INTEGER NOT NULL CHECK (position BETWEEN 1 AND 7),
    name VARCHAR(80) NOT NULL,
    CONSTRAINT uq_workout_day_position UNIQUE(plan_id, position) DEFERRABLE INITIALLY DEFERRED
);

CREATE TABLE workout_items (
    id UUID PRIMARY KEY,
    day_id UUID NOT NULL REFERENCES workout_days(id) ON DELETE CASCADE,
    exercise_id UUID NOT NULL,
    position INTEGER NOT NULL CHECK (position >= 1),
    sets INTEGER CHECK (sets BETWEEN 1 AND 10),
    repetition_min INTEGER CHECK (repetition_min BETWEEN 1 AND 100),
    repetition_max INTEGER CHECK (repetition_max BETWEEN 1 AND 100),
    duration_seconds INTEGER CHECK (duration_seconds BETWEEN 5 AND 1800),
    rest_seconds INTEGER CHECK (rest_seconds BETWEEN 0 AND 600),
    optional_load_kg NUMERIC(7,2) CHECK (optional_load_kg >= 0),
    notes VARCHAR(500),
    CONSTRAINT ck_repetition_range CHECK (repetition_min IS NULL OR repetition_max IS NULL OR repetition_min <= repetition_max),
    CONSTRAINT uq_workout_item_position UNIQUE(day_id, position) DEFERRABLE INITIALLY DEFERRED
);
