-- V2__add_skills_and_related_tables.sql

-- skillsテーブルを作成（存在しなければ）
CREATE TABLE IF NOT EXISTS skills (
                                      id BIGSERIAL PRIMARY KEY,
                                      name VARCHAR(255) NOT NULL UNIQUE,
    category VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

-- user_skillsテーブルを作成（存在しなければ）
CREATE TABLE IF NOT EXISTS user_skills (
                                           id BIGSERIAL PRIMARY KEY,
                                           user_id BIGINT NOT NULL REFERENCES users(id),
    skill_id BIGINT NOT NULL REFERENCES skills(id),
    proficiency INT NOT NULL CHECK (proficiency >= 0 AND proficiency <= 100),
    level VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, skill_id)
    );

-- インデックスの作成（存在しなければ）
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE tablename = 'user_skills' AND indexname = 'idx_user_skills_user_id'
    ) THEN
CREATE INDEX idx_user_skills_user_id ON user_skills(user_id);
END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE tablename = 'user_skills' AND indexname = 'idx_user_skills_skill_id'
    ) THEN
CREATE INDEX idx_user_skills_skill_id ON user_skills(skill_id);
END IF;
END
$$;

-- analysis_requestsテーブルを作成（存在しなければ）
CREATE TABLE IF NOT EXISTS analysis_requests (
                                                 id BIGSERIAL PRIMARY KEY,
                                                 user_id BIGINT NOT NULL REFERENCES users(id),
    repo_owner VARCHAR(255) NOT NULL,
    repo_name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE INDEX IF NOT EXISTS idx_analysis_requests_user_id ON analysis_requests(user_id);

-- analysis_cachesテーブルを作成（存在しなければ）
CREATE TABLE IF NOT EXISTS analysis_caches (
                                               id BIGSERIAL PRIMARY KEY,
                                               repo_owner VARCHAR(255) NOT NULL,
    repo_name VARCHAR(255) NOT NULL,
    result_json JSONB NOT NULL,
    analyzed_at TIMESTAMPTZ NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_analysis_caches_analyzed_at ON analysis_caches(analyzed_at);

-- user_contributionsテーブルを作成（存在しなければ）
CREATE TABLE IF NOT EXISTS user_contributions (
                                                  event_id VARCHAR(255) PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    event_type VARCHAR(255) NOT NULL,
    repo_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
    );

CREATE INDEX IF NOT EXISTS idx_user_contributions_user_id ON user_contributions(user_id);
CREATE INDEX IF NOT EXISTS idx_user_contributions_created_at ON user_contributions(created_at);
