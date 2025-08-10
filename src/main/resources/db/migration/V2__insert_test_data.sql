-- Flyway Test Data Insertion Script
-- Version: 2
-- Description: Inserts sample data for all tables.

-- 1. users テーブルにテストデータを挿入
-- 実際のGitHub IDとは異なります。トークンはダミーです。
INSERT INTO users (id, github_id, username, avatar_url, github_access_token, department, job_title, self_introduction) VALUES
                                                                                                                           (1, 10001, 'taro-yamada', 'https://avatars.githubusercontent.com/u/10001', 'dummy_token_for_taro', '開発部', 'バックエンドエンジニア', 'Spring Bootが得意です。趣味は登山です。'),
                                                                                                                           (2, 10002, 'hanako-tanaka', 'https://avatars.githubusercontent.com/u/10002', 'dummy_token_for_hanako', '開発部', 'フロントエンドエンジニア', 'ReactとTypeScriptが好きです。週末はカフェ巡りをしています。'),
                                                                                                                           (3, 10003, 'jiro-suzuki', 'https://avatars.githubusercontent.com/u/10003', 'dummy_token_for_jiro', 'インフラ部', 'SRE', 'KubernetesとAWSの運用を担当しています。');

-- 2. skills テーブルにマスタデータを挿入
INSERT INTO skills (id, name, category) VALUES
                                            (1, 'Java', 'プログラミング言語'),
                                            (2, 'Spring Boot', 'フレームワーク'),
                                            (3, 'PostgreSQL', 'データベース'),
                                            (4, 'JavaScript', 'プログラミング言語'),
                                            (5, 'TypeScript', 'プログラミング言語'),
                                            (6, 'React', 'フレームワーク'),
                                            (7, 'Next.js', 'フレームワーク'),
                                            (8, 'AWS', 'クラウド'),
                                            (9, 'Docker', 'コンテナ技術'),
                                            (10, 'Kubernetes', 'コンテナオーケストレーション');

-- 3. user_skills テーブルにユーザーとスキルの関連を挿入
-- Taro Yamada (ID: 1)
INSERT INTO user_skills (user_id, skill_id, proficiency, level) VALUES
                                                                    (1, 1, 90, '上級'),
                                                                    (1, 2, 85, '上級'),
                                                                    (1, 3, 70, '中級'),
                                                                    (1, 9, 60, '中級');
-- Hanako Tanaka (ID: 2)
INSERT INTO user_skills (user_id, skill_id, proficiency, level) VALUES
                                                                    (2, 4, 95, '上級'),
                                                                    (2, 5, 90, '上級'),
                                                                    (2, 6, 88, '上級'),
                                                                    (2, 7, 75, '中級');
-- Jiro Suzuki (ID: 3)
INSERT INTO user_skills (user_id, skill_id, proficiency, level) VALUES
                                                                    (3, 8, 90, '上級'),
                                                                    (3, 9, 80, '上級'),
                                                                    (3, 10, 85, '上級');

-- 4. teams テーブルにテストデータを挿入
INSERT INTO teams (id, name, owner_id) VALUES
                                           (1, 'Alpha Team', 1), -- Taro Yamadaがオーナー
                                           (2, 'Bravo Team', 3);  -- Jiro Suzukiがオーナー

-- 5. team_members テーブルにチームメンバーを挿入
INSERT INTO team_members (user_id, team_id, role) VALUES
                                                      (1, 1, 'admin'), -- TaroはAlpha Teamの管理者
                                                      (2, 1, 'member'),-- HanakoはAlpha Teamのメンバー
                                                      (3, 2, 'admin'); -- JiroはBravo Teamの管理者

-- 6. projects テーブルにテストデータを挿入 (IDを1から始まるように修正)
INSERT INTO projects (id, team_id, name, description) VALUES
                                                          (1, 1, 'KPTG共有アプリ', 'Keep-Problem-Try-Goalをチームで共有するためのWebアプリケーション'),
                                                          (2, 1, '社内Wikiシステム', 'Markdownで記述できる社内向け情報共有ツール'),
                                                          (3, 2, 'インフラ監視基盤', 'PrometheusとGrafanaを利用したシステム監視基盤の構築');

-- 7. dev_logs テーブルに開発ログを挿入 (project_idを修正後の値に変更)
-- Taro YamadaがKPTG共有アプリについて記録
INSERT INTO dev_logs (project_id, user_id, log_date, keep_content, problem_content, try_content, goal_content) VALUES
    (1, 1, '2025-08-04', '・認証機能の実装がスムーズに進んだ', '・DB設計で考慮漏れがあり手戻りが発生した', '・次回の設計ではペアレビューを取り入れる', '・今週中にプロトタイプを完成させる');
-- Hanako TanakaがKPTG共有アプリについて記録
INSERT INTO dev_logs (project_id, user_id, log_date, keep_content, problem_content, try_content, goal_content) VALUES
    (1, 2, '2025-08-05', '・UIコンポーネントの共通化ができた', '・特定のブラウザでCSSが崩れる問題があった', '・CSSのベンダープレフィックスを見直す', '・ログイン画面のUIを完成させる');

-- 8. analysis_requests テーブルに分析リクエストを挿入
INSERT INTO analysis_requests (user_id, repo_owner, repo_name, status) VALUES
                                                                           (1, 'spring-projects', 'spring-boot', 'success'),
                                                                           (2, 'facebook', 'react', 'error');

-- 9. analysis_caches テーブルに分析結果のキャッシュを挿入
INSERT INTO analysis_caches (repo_owner, repo_name, result_json, analyzed_at) VALUES
    ('spring-projects', 'spring-boot', '{
  "language_composition": {
    "Java": 0.95,
    "Groovy": 0.03,
    "Other": 0.02
  },
  "activity_level": "very_high",
  "recommended_skills": ["Java", "Spring Framework", "Maven", "Gradle"]
}', '2025-08-01 10:00:00');

-- 主キーのシーケンスが手動挿入したIDの次に進むように更新
-- PostgreSQLの場合
SELECT setval('users_id_seq', (SELECT MAX(id) FROM users));
SELECT setval('skills_id_seq', (SELECT MAX(id) FROM skills));
SELECT setval('teams_id_seq', (SELECT MAX(id) FROM teams));
SELECT setval('projects_id_seq', (SELECT MAX(id) FROM projects));
-- 他のテーブルも同様に設定...
