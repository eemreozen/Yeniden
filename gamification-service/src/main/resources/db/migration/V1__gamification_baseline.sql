CREATE SCHEMA IF NOT EXISTS gamification;

CREATE TABLE IF NOT EXISTS gamification.badges (
    id uuid PRIMARY KEY,
    code varchar(50) NOT NULL UNIQUE,
    name varchar(100) NOT NULL,
    description varchar(255),
    icon_key varchar(100),
    active boolean NOT NULL DEFAULT true,
    rule jsonb
);
ALTER TABLE gamification.badges ADD COLUMN IF NOT EXISTS active boolean NOT NULL DEFAULT true;
ALTER TABLE gamification.badges ADD COLUMN IF NOT EXISTS rule jsonb;

CREATE TABLE IF NOT EXISTS gamification.user_badges (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    badge_id uuid,
    badge_code varchar(50) NOT NULL,
    earned_at timestamp NOT NULL
);
ALTER TABLE gamification.user_badges ADD COLUMN IF NOT EXISTS badge_id uuid;
INSERT INTO gamification.badges (id, code, name, active)
SELECT md5('legacy-badge:' || badge_code)::uuid, badge_code, badge_code, true
FROM gamification.user_badges
WHERE badge_code IS NOT NULL
ON CONFLICT (code) DO NOTHING;
UPDATE gamification.user_badges ub SET badge_id = b.id
FROM gamification.badges b WHERE ub.badge_id IS NULL AND ub.badge_code = b.code;
ALTER TABLE gamification.user_badges ALTER COLUMN badge_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_gamification_user_badge
    ON gamification.user_badges (user_id, badge_id);

CREATE TABLE IF NOT EXISTS gamification.user_levels (
    user_id uuid PRIMARY KEY,
    level integer NOT NULL DEFAULT 1,
    total_points_earned integer NOT NULL DEFAULT 0,
    created_at timestamp NOT NULL,
    updated_at timestamp
);

CREATE TABLE IF NOT EXISTS gamification.user_gamification_profiles (
    user_id uuid PRIMARY KEY,
    total_coins_earned bigint NOT NULL DEFAULT 0,
    level integer NOT NULL DEFAULT 1,
    listings_published integer NOT NULL DEFAULT 0,
    confirmed_give_count integer NOT NULL DEFAULT 0,
    confirmed_take_count integer NOT NULL DEFAULT 0,
    distinct_category_count integer NOT NULL DEFAULT 0
);
INSERT INTO gamification.user_gamification_profiles (user_id, total_coins_earned, level)
SELECT user_id, total_points_earned, level FROM gamification.user_levels
ON CONFLICT (user_id) DO NOTHING;

CREATE TABLE IF NOT EXISTS gamification.processed_events (
    listener varchar(255) NOT NULL,
    event_id uuid NOT NULL,
    processed_at timestamp NOT NULL,
    PRIMARY KEY (listener, event_id)
);
CREATE TABLE IF NOT EXISTS gamification.user_categories (
    user_id uuid NOT NULL,
    category_id uuid NOT NULL,
    PRIMARY KEY (user_id, category_id)
);
CREATE TABLE IF NOT EXISTS gamification.quests (
    id uuid PRIMARY KEY,
    period varchar(7) NOT NULL,
    code varchar(255) NOT NULL,
    title varchar(255) NOT NULL,
    target_metric varchar(255) NOT NULL,
    target_value bigint NOT NULL,
    reward_coins bigint NOT NULL,
    UNIQUE (period, code)
);
CREATE TABLE IF NOT EXISTS gamification.quest_progress (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL,
    quest_id uuid NOT NULL,
    current_value bigint NOT NULL DEFAULT 0,
    completed_at timestamp,
    UNIQUE (user_id, quest_id)
);
CREATE TABLE IF NOT EXISTS gamification.quest_progress_categories (
    user_id uuid NOT NULL,
    quest_id uuid NOT NULL,
    category_id uuid NOT NULL,
    PRIMARY KEY (user_id, quest_id, category_id)
);
CREATE TABLE IF NOT EXISTS gamification.leaderboard_snapshots (
    id uuid PRIMARY KEY,
    scope varchar(255) NOT NULL,
    scope_id uuid,
    period varchar(7) NOT NULL,
    snapshot_batch_id uuid NOT NULL,
    user_id uuid NOT NULL,
    rank integer NOT NULL,
    score bigint NOT NULL,
    generated_at timestamp NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_leaderboard_scope_period
    ON gamification.leaderboard_snapshots (scope, scope_id, period, generated_at DESC);
CREATE INDEX IF NOT EXISTS idx_leaderboard_batch_rank
    ON gamification.leaderboard_snapshots (snapshot_batch_id, rank);

CREATE TABLE IF NOT EXISTS gamification.quest_reward_outbox_events (
    id uuid PRIMARY KEY,
    event_id uuid NOT NULL UNIQUE,
    event_type varchar(255) NOT NULL,
    aggregate_id uuid NOT NULL,
    payload text NOT NULL,
    created_at timestamp NOT NULL,
    published_at timestamp
);
CREATE INDEX IF NOT EXISTS idx_quest_reward_outbox_pending
    ON gamification.quest_reward_outbox_events (published_at, created_at);
