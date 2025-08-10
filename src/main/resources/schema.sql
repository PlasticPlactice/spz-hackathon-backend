-- 1. users テーブル
-- プロフィール情報用のカラムを追加
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY, -- ユーザーを一位に識別するID (主キー)
                       github_id BIGINT NOT NULL UNIQUE, -- GitHubから取得するユニークなID
                       username VARCHAR(255) NOT NULL, -- GitHubのユーザー名
                       avatar_url TEXT, -- GitHubのプロフィール画像のURL
                       github_access_token TEXT NOT NULL, -- GitHub API操作用のトークン (暗号化必須)
                       department VARCHAR(255), -- プロフィール用の部署名
                       job_title VARCHAR(255), -- プロフィール用の役職名
                       self_introduction TEXT, -- プロフィール用の自己紹介文
                       email VARCHAR(255), -- メールアドレス
                       location VARCHAR(255), -- 所在地
                       hire_date DATE, -- 入社日
                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), -- レコード作成日時
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), -- レコード更新日時
                       deleted_at TIMESTAMPTZ -- 論理削除日時 (NULLは未削除)
);
CREATE INDEX idx_users_github_id ON users(github_id);

-- 2. teams テーブル
CREATE TABLE teams (
                       id BIGSERIAL PRIMARY KEY, -- チームを一位に識別するID (主キー)
                       name VARCHAR(255) NOT NULL, -- チーム名
                       owner_id BIGINT NOT NULL REFERENCES users(id), -- チームの所有者 (usersテーブルのID)
                       created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), -- レコード作成日時
                       updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), -- レコード更新日時
                       deleted_at TIMESTAMPTZ -- 論理削除日時 (NULLは未削除)
);
CREATE INDEX idx_teams_owner_id ON teams(owner_id);

-- 3. team_members テーブル
CREATE TABLE team_members (
                              id BIGSERIAL PRIMARY KEY, -- メンバーシップを一位に識別するID (主キー)
                              user_id BIGINT NOT NULL REFERENCES users(id), -- メンバーのID (usersテーブルのID)
                              team_id BIGINT NOT NULL REFERENCES teams(id), -- 所属するチームのID (teamsテーブルのID)
                              role VARCHAR(50) NOT NULL, -- チーム内での役割 ('admin', 'member' など)
                              joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), -- チームへの参加日時
                              deleted_at TIMESTAMPTZ, -- 論理削除日時 (NULLは未削除)
                              UNIQUE (user_id, team_id)
);
CREATE INDEX idx_team_members_user_id ON team_members(user_id);
CREATE INDEX idx_team_members_team_id ON team_members(team_id);

-- 4. projects テーブル
CREATE TABLE projects (
                          id BIGSERIAL PRIMARY KEY, -- プロジェクトを一位に識別するID (主キー)
                          team_id BIGINT NOT NULL REFERENCES teams(id), -- 所属するチームのID (teamsテーブルのID)
                          name VARCHAR(255) NOT NULL, -- プロジェクト名
                          description TEXT, -- プロジェクトの説明文
                          created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), -- レコード作成日時
                          updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), -- レコード更新日時
                          deleted_at TIMESTAMPTZ -- 論理削除日時 (NULLは未削除)
);
CREATE INDEX idx_projects_team_id ON projects(team_id);

-- 5. dev_logs テーブル
CREATE TABLE dev_logs (
                          id BIGSERIAL PRIMARY KEY, -- 開発ログを一位に識別するID (主キー)
                          project_id BIGINT NOT NULL REFERENCES projects(id), -- 関連するプロジェクトのID (projectsテーブルのID)
                          user_id BIGINT NOT NULL REFERENCES users(id), -- 記録したユーザーのID (usersテーブルのID)
                          log_date DATE NOT NULL, -- ログの対象となる日付
                          keep_content TEXT, -- Keep (良かったこと) の内容
                          problem_content TEXT, -- Problem (問題点) の内容
                          try_content TEXT, -- Try (次に試すこと) の内容
                          goal_content TEXT, -- Goal (目標) の内容
                          created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), -- レコード作成日時
                          updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), -- レコード更新日時
                          deleted_at TIMESTAMPTZ -- 論理削除日時 (NULLは未削除)
);
CREATE INDEX idx_dev_logs_project_id ON dev_logs(project_id);
CREATE INDEX idx_dev_logs_user_id ON dev_logs(user_id);

-- 6. skills テーブル (新規)
-- スキルのマスターデータを管理
CREATE TABLE skills (
                        id BIGSERIAL PRIMARY KEY, -- スキルを一位に識別するID (主キー)
                        name VARCHAR(255) NOT NULL UNIQUE, -- スキル名 ('JavaScript', 'React' など)
                        category VARCHAR(255) NOT NULL, -- スキルのカテゴリ ('プログラミング', 'フロントエンド' など)
                        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW() -- レコード作成日時
);

-- 7. user_skills テーブル (新規)
-- ユーザーとスキルの関連を管理
CREATE TABLE user_skills (
                             id BIGSERIAL PRIMARY KEY, -- ユーザースキルを一位に識別するID (主キー)
                             user_id BIGINT NOT NULL REFERENCES users(id), -- ユーザーのID (usersテーブルのID)
                             skill_id BIGINT NOT NULL REFERENCES skills(id), -- スキルのID (skillsテーブルのID)
                             proficiency INT NOT NULL CHECK (proficiency >= 0 AND proficiency <= 100), -- 習熟度 (0-100の数値)
                             level VARCHAR(50), -- 習熟度レベル ('上級', '中級' など)
                             created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), -- レコード作成日時
                             updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(), -- レコード更新日時
                             UNIQUE (user_id, skill_id)
);
CREATE INDEX idx_user_skills_user_id ON user_skills(user_id);
CREATE INDEX idx_user_skills_skill_id ON user_skills(skill_id);

-- 8. analysis_requests テーブル
CREATE TABLE analysis_requests (
                                   id BIGSERIAL PRIMARY KEY, -- 分析リクエストを一位に識別するID (主キー)
                                   user_id BIGINT NOT NULL REFERENCES users(id), -- リクエストしたユーザーのID (usersテーブルのID)
                                   repo_owner VARCHAR(255) NOT NULL, -- 分析対象リポジトリのオーナー名
                                   repo_name VARCHAR(255) NOT NULL, -- 分析対象リポジトリ名
                                   status VARCHAR(50) NOT NULL, -- リクエストの状態 ('success', 'error' など)
                                   requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW() -- リクエストされた日時
);
CREATE INDEX idx_analysis_requests_user_id ON analysis_requests(user_id);

-- 9. analysis_caches テーブル
CREATE TABLE analysis_caches (
                                 id BIGSERIAL PRIMARY KEY, -- キャッシュを一位に識別するID (主キー)
                                 repo_owner VARCHAR(255) NOT NULL, -- 分析対象リポジトリのオーナー名
                                 repo_name VARCHAR(255) NOT NULL, -- 分析対象リポジトリ名
                                 result_json JSONB NOT NULL, -- AIによる分析結果 (JSON形式)
                                 analyzed_at TIMESTAMPTZ NOT NULL -- 分析が実行された日時
);
CREATE INDEX idx_analysis_caches_analyzed_at ON analysis_caches(analyzed_at);

-- 10. user_contributions テーブル
CREATE TABLE user_contributions (
                                    event_id VARCHAR(255) PRIMARY KEY, -- GitHubイベントのID (主キー)
                                    user_id BIGINT NOT NULL REFERENCES users(id), -- ユーザーのID (usersテーブルのID)
                                    event_type VARCHAR(255) NOT NULL, -- イベントの種類 ('PushEvent', 'IssuesEvent' など)
                                    repo_name VARCHAR(255) NOT NULL, -- リポジトリ名
                                    created_at TIMESTAMPTZ NOT NULL -- イベントの発生日時
);

CREATE INDEX idx_user_contributions_user_id ON user_contributions(user_id);
CREATE INDEX idx_user_contributions_created_at ON user_contributions(created_at);