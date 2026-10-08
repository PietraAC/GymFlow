ALTER TABLE workout_plans ADD COLUMN goal VARCHAR(30);
ALTER TABLE workout_plans ADD COLUMN target_days_per_week INTEGER;

UPDATE workout_plans p
SET goal = COALESCE(profile.goal, 'GENERAL_FITNESS'),
    target_days_per_week = GREATEST(1, LEAST(7, GREATEST(COALESCE(profile.days_per_week, 1),
        (SELECT COUNT(*) FROM workout_days d WHERE d.plan_id = p.id))))
FROM (SELECT identity_subject, goal, days_per_week FROM student_profiles) profile
WHERE profile.identity_subject = p.identity_subject;

UPDATE workout_plans
SET goal = COALESCE(goal, 'GENERAL_FITNESS'),
    target_days_per_week = COALESCE(target_days_per_week,
        GREATEST(3, LEAST(7, (SELECT COUNT(*) FROM workout_days d WHERE d.plan_id = workout_plans.id))));

ALTER TABLE workout_plans ALTER COLUMN goal SET NOT NULL;
ALTER TABLE workout_plans ALTER COLUMN target_days_per_week SET NOT NULL;
ALTER TABLE workout_plans ADD CONSTRAINT ck_workout_plan_goal
    CHECK (goal IN ('HYPERTROPHY','STRENGTH','GENERAL_FITNESS','ENDURANCE'));
ALTER TABLE workout_plans ADD CONSTRAINT ck_workout_plan_target_days
    CHECK (target_days_per_week BETWEEN 1 AND 7);
