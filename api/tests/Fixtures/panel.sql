-- Esquema mínimo em SQLite, isolado em memória, para testes das rotas do painel.
CREATE TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, email TEXT UNIQUE, username TEXT UNIQUE, password TEXT, is_global_admin INTEGER DEFAULT 0, created_at TEXT, updated_at TEXT);
CREATE TABLE gyms (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, active INTEGER DEFAULT 1, created_at TEXT, updated_at TEXT);
CREATE TABLE gym_users (gym_id INTEGER, user_id INTEGER, status TEXT, joined_at TEXT, PRIMARY KEY(gym_id,user_id));
CREATE TABLE gym_user_roles (gym_id INTEGER, user_id INTEGER, role TEXT, PRIMARY KEY(gym_id,user_id,role));
CREATE TABLE memberships (id INTEGER PRIMARY KEY, gym_id INTEGER, user_id INTEGER, ends_on TEXT);
CREATE TABLE plans (id INTEGER PRIMARY KEY, gym_id INTEGER, name TEXT, price NUMERIC);
CREATE TABLE plan_features (id INTEGER PRIMARY KEY, plan_id INTEGER, description TEXT, sort_order INTEGER);
CREATE TABLE workout_plans (id INTEGER PRIMARY KEY AUTOINCREMENT, gym_id INTEGER, student_id INTEGER, instructor_id INTEGER, name TEXT, goal TEXT, version INTEGER DEFAULT 1, status TEXT, published_at TEXT, archived_at TEXT, schedule_description TEXT, created_at TEXT, updated_at TEXT);
CREATE TABLE workout_days (id INTEGER PRIMARY KEY AUTOINCREMENT, workout_plan_id INTEGER, label TEXT, title TEXT, sort_order INTEGER);
CREATE TABLE exercises (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, created_at TEXT, updated_at TEXT);
CREATE TABLE workout_exercises (id INTEGER PRIMARY KEY AUTOINCREMENT, workout_day_id INTEGER, exercise_id INTEGER, sort_order INTEGER, sets INTEGER, repetitions_min INTEGER, repetitions_max INTEGER, rest_seconds INTEGER);
CREATE TABLE workout_sessions (id INTEGER PRIMARY KEY, workout_day_id INTEGER);
