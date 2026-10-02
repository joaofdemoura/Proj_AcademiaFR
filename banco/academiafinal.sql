
CREATE DATABASE academiafinal CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE academiafinal;

CREATE TABLE users (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(150) NOT NULL,
 email VARCHAR(255) NOT NULL UNIQUE,
 -- [WEB] login do painel por usuário (ex.: admin, admin.prime); alunos entram por e-mail.
 username VARCHAR(60) NULL UNIQUE,
 -- [WEB] NULL quando o aluno foi cadastrado pelo painel e ainda não ativou o acesso no app.
 password VARCHAR(255) NULL COMMENT 'Hash gerado pelo Laravel; nunca senha em texto',
 -- [WEB] administrador geral: gerencia todas as academias. Admin de uma unidade usa gym_user_roles.
 is_global_admin BOOLEAN NOT NULL DEFAULT FALSE,
 phone VARCHAR(30), avatar_path VARCHAR(500), email_verified_at TIMESTAMP NULL,
 remember_token VARCHAR(100), created_at TIMESTAMP NULL, updated_at TIMESTAMP NULL
) ENGINE=InnoDB;

CREATE TABLE gyms (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 name VARCHAR(150) NOT NULL, logo_path VARCHAR(500), address VARCHAR(500),
 active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMP NULL, updated_at TIMESTAMP NULL
) ENGINE=InnoDB;

CREATE TABLE gym_users (
 gym_id BIGINT UNSIGNED NOT NULL, user_id BIGINT UNSIGNED NOT NULL,
 status ENUM('active','inactive') NOT NULL DEFAULT 'active', joined_at DATE,
 PRIMARY KEY(gym_id,user_id),
 FOREIGN KEY(gym_id) REFERENCES gyms(id), FOREIGN KEY(user_id) REFERENCES users(id)
) ENGINE=InnoDB;

CREATE TABLE gym_user_roles (
 gym_id BIGINT UNSIGNED NOT NULL, user_id BIGINT UNSIGNED NOT NULL,
 role ENUM('student','instructor','admin') NOT NULL,
 PRIMARY KEY(gym_id,user_id,role),
 FOREIGN KEY(gym_id,user_id) REFERENCES gym_users(gym_id,user_id)
) ENGINE=InnoDB;

CREATE TABLE plans (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 -- [WEB] NULL = plano da rede (opção "Todas" no painel), criado pelo administrador geral.
 gym_id BIGINT UNSIGNED NULL,
 name VARCHAR(150) NOT NULL, description TEXT,
 billing_period ENUM('monthly','annual') NOT NULL,
 price DECIMAL(10,2) NOT NULL, currency CHAR(3) NOT NULL DEFAULT 'BRL',
 active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP NULL, updated_at TIMESTAMP NULL,
 UNIQUE(gym_id,id), FOREIGN KEY(gym_id) REFERENCES gyms(id), CHECK(price>=0)
) ENGINE=InnoDB;

-- [WEB] Em quais academias cada plano pode ser contratado.
-- Plano de uma academia: uma linha com a própria academia. Plano da rede: uma linha por academia.
CREATE TABLE plan_gyms (
 gym_id BIGINT UNSIGNED NOT NULL, plan_id BIGINT UNSIGNED NOT NULL,
 PRIMARY KEY(gym_id,plan_id),
 FOREIGN KEY(gym_id) REFERENCES gyms(id), FOREIGN KEY(plan_id) REFERENCES plans(id)
) ENGINE=InnoDB;

CREATE TABLE plan_features (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, plan_id BIGINT UNSIGNED NOT NULL,
 description VARCHAR(255) NOT NULL, sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
 FOREIGN KEY(plan_id) REFERENCES plans(id)
) ENGINE=InnoDB;

CREATE TABLE memberships (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, gym_id BIGINT UNSIGNED NOT NULL,
 user_id BIGINT UNSIGNED NOT NULL, plan_id BIGINT UNSIGNED NOT NULL,
 status ENUM('pending','active','expired','cancelled') NOT NULL DEFAULT 'pending',
 starts_on DATE NOT NULL, ends_on DATE NOT NULL, contracted_price DECIMAL(10,2) NOT NULL,
 cancelled_at DATETIME NULL, created_at TIMESTAMP NULL, updated_at TIMESTAMP NULL,
 FOREIGN KEY(gym_id,user_id) REFERENCES gym_users(gym_id,user_id),
 -- [WEB] o plano precisa estar disponível na academia (inclui planos da rede).
 FOREIGN KEY(gym_id,plan_id) REFERENCES plan_gyms(gym_id,plan_id),
 INDEX(gym_id,status,ends_on), CHECK(ends_on>=starts_on), CHECK(contracted_price>=0)
) ENGINE=InnoDB;

CREATE TABLE promotions (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, gym_id BIGINT UNSIGNED NOT NULL,
 plan_id BIGINT UNSIGNED NULL, title VARCHAR(200) NOT NULL, description TEXT,
 banner_path VARCHAR(500), starts_at DATETIME NOT NULL, ends_at DATETIME NOT NULL,
 promotional_price DECIMAL(10,2) NULL, active BOOLEAN NOT NULL DEFAULT TRUE,
 created_at TIMESTAMP NULL, updated_at TIMESTAMP NULL,
 -- [WEB] referência via plan_gyms para aceitar também planos da rede.
 FOREIGN KEY(gym_id) REFERENCES gyms(id), FOREIGN KEY(gym_id,plan_id) REFERENCES plan_gyms(gym_id,plan_id),
 CHECK(ends_at>starts_at), CHECK(promotional_price IS NULL OR promotional_price>=0)
) ENGINE=InnoDB;

CREATE TABLE exercises (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, name VARCHAR(150) NOT NULL,
 muscle_group VARCHAR(100), instructions TEXT, video_url VARCHAR(1000), image_path VARCHAR(500),
 created_at TIMESTAMP NULL, updated_at TIMESTAMP NULL
) ENGINE=InnoDB;

CREATE TABLE workout_plans (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, gym_id BIGINT UNSIGNED NOT NULL,
 student_id BIGINT UNSIGNED NOT NULL, instructor_id BIGINT UNSIGNED NULL,
 name VARCHAR(150) NOT NULL, goal VARCHAR(150), version SMALLINT UNSIGNED NOT NULL DEFAULT 1,
 -- [WEB] rascunho fica só no painel; o app mostra apenas fichas publicadas.
 status ENUM('draft','published') NOT NULL DEFAULT 'draft',
 published_at DATETIME NULL,
 -- [WEB] dias de treino como o painel exibe (ex.: "Segunda, quarta e sexta").
 schedule_description VARCHAR(200) NULL,
 starts_on DATE, ends_on DATE, archived_at DATETIME NULL,
 created_at TIMESTAMP NULL, updated_at TIMESTAMP NULL,
 FOREIGN KEY(gym_id,student_id) REFERENCES gym_users(gym_id,user_id),
 FOREIGN KEY(gym_id,instructor_id) REFERENCES gym_users(gym_id,user_id),
 INDEX(gym_id,student_id), INDEX(gym_id,status),
 CHECK(ends_on IS NULL OR starts_on IS NULL OR ends_on>=starts_on),
 CHECK(status='draft' OR published_at IS NOT NULL)
) ENGINE=InnoDB;

CREATE TABLE workout_days (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, workout_plan_id BIGINT UNSIGNED NOT NULL,
 label VARCHAR(30) NOT NULL, title VARCHAR(150) NOT NULL,
 estimated_minutes SMALLINT UNSIGNED NULL, sort_order SMALLINT UNSIGNED NOT NULL DEFAULT 0,
 FOREIGN KEY(workout_plan_id) REFERENCES workout_plans(id), UNIQUE(workout_plan_id,label)
) ENGINE=InnoDB;

CREATE TABLE workout_exercises (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, workout_day_id BIGINT UNSIGNED NOT NULL,
 exercise_id BIGINT UNSIGNED NOT NULL, sort_order SMALLINT UNSIGNED NOT NULL,
 sets SMALLINT UNSIGNED NOT NULL, repetitions_min SMALLINT UNSIGNED,
 repetitions_max SMALLINT UNSIGNED, target_weight_kg DECIMAL(6,2),
 rest_seconds SMALLINT UNSIGNED, notes TEXT,
 FOREIGN KEY(workout_day_id) REFERENCES workout_days(id), FOREIGN KEY(exercise_id) REFERENCES exercises(id),
 UNIQUE(workout_day_id,sort_order), CHECK(sets>0),
 CHECK(repetitions_max IS NULL OR repetitions_min IS NULL OR repetitions_max>=repetitions_min),
 CHECK(target_weight_kg IS NULL OR target_weight_kg>=0)
) ENGINE=InnoDB;

CREATE TABLE workout_sessions (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, gym_id BIGINT UNSIGNED NOT NULL,
 user_id BIGINT UNSIGNED NOT NULL, workout_day_id BIGINT UNSIGNED NOT NULL,
 started_at DATETIME NOT NULL, finished_at DATETIME NULL,
 status ENUM('in_progress','completed','abandoned') NOT NULL DEFAULT 'in_progress',
 calories_kcal DECIMAL(7,2) NULL, notes TEXT,
 FOREIGN KEY(gym_id,user_id) REFERENCES gym_users(gym_id,user_id),
 FOREIGN KEY(workout_day_id) REFERENCES workout_days(id),
 INDEX(gym_id,user_id,started_at), CHECK(finished_at IS NULL OR finished_at>=started_at),
 CHECK(calories_kcal IS NULL OR calories_kcal>=0)
) ENGINE=InnoDB;

CREATE TABLE exercise_logs (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, workout_session_id BIGINT UNSIGNED NOT NULL,
 workout_exercise_id BIGINT UNSIGNED NOT NULL, set_number SMALLINT UNSIGNED NOT NULL,
 repetitions SMALLINT UNSIGNED NOT NULL, weight_kg DECIMAL(6,2), performed_at DATETIME NOT NULL,
 FOREIGN KEY(workout_session_id) REFERENCES workout_sessions(id),
 FOREIGN KEY(workout_exercise_id) REFERENCES workout_exercises(id),
 UNIQUE(workout_session_id,workout_exercise_id,set_number), CHECK(set_number>0),
 CHECK(weight_kg IS NULL OR weight_kg>=0)
) ENGINE=InnoDB;

CREATE TABLE body_measurements (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, gym_id BIGINT UNSIGNED NOT NULL,
 user_id BIGINT UNSIGNED NOT NULL, measured_at DATETIME NOT NULL,
 weight_kg DECIMAL(6,2), height_cm DECIMAL(5,2), waist_cm DECIMAL(5,2),
 muscle_mass_kg DECIMAL(6,2), body_fat_percent DECIMAL(5,2), notes TEXT,
 FOREIGN KEY(gym_id,user_id) REFERENCES gym_users(gym_id,user_id),
 INDEX(gym_id,user_id,measured_at), CHECK(weight_kg IS NULL OR weight_kg>0),
 CHECK(height_cm IS NULL OR height_cm>0), CHECK(waist_cm IS NULL OR waist_cm>0),
 CHECK(muscle_mass_kg IS NULL OR muscle_mass_kg>=0),
 CHECK(body_fat_percent IS NULL OR body_fat_percent BETWEEN 0 AND 100)
) ENGINE=InnoDB;

CREATE TABLE class_types (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, gym_id BIGINT UNSIGNED NOT NULL,
 name VARCHAR(150) NOT NULL, description TEXT, UNIQUE(gym_id,id),
 FOREIGN KEY(gym_id) REFERENCES gyms(id)
) ENGINE=InnoDB;

CREATE TABLE class_sessions (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, gym_id BIGINT UNSIGNED NOT NULL,
 class_type_id BIGINT UNSIGNED NOT NULL, instructor_id BIGINT UNSIGNED NULL,
 starts_at DATETIME NOT NULL, ends_at DATETIME NOT NULL, capacity SMALLINT UNSIGNED NOT NULL,
 status ENUM('scheduled','cancelled','completed') NOT NULL DEFAULT 'scheduled',
 location VARCHAR(150), UNIQUE(gym_id,id), INDEX(gym_id,starts_at),
 FOREIGN KEY(gym_id,class_type_id) REFERENCES class_types(gym_id,id),
 FOREIGN KEY(gym_id,instructor_id) REFERENCES gym_users(gym_id,user_id),
 CHECK(ends_at>starts_at), CHECK(capacity>0)
) ENGINE=InnoDB;

CREATE TABLE class_bookings (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, gym_id BIGINT UNSIGNED NOT NULL,
 class_session_id BIGINT UNSIGNED NOT NULL, user_id BIGINT UNSIGNED NOT NULL,
 status ENUM('booked','cancelled','attended','no_show') NOT NULL DEFAULT 'booked',
 booked_at DATETIME NOT NULL, cancelled_at DATETIME NULL,
 UNIQUE(class_session_id,user_id), INDEX(gym_id,user_id,status),
 FOREIGN KEY(gym_id,class_session_id) REFERENCES class_sessions(gym_id,id),
 FOREIGN KEY(gym_id,user_id) REFERENCES gym_users(gym_id,user_id)
) ENGINE=InnoDB;

CREATE TABLE gym_checkins (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, gym_id BIGINT UNSIGNED NOT NULL,
 user_id BIGINT UNSIGNED NOT NULL, checked_in_at DATETIME NOT NULL, checked_out_at DATETIME NULL,
 FOREIGN KEY(gym_id,user_id) REFERENCES gym_users(gym_id,user_id),
 INDEX(gym_id,user_id,checked_in_at), CHECK(checked_out_at IS NULL OR checked_out_at>=checked_in_at)
) ENGINE=InnoDB;

CREATE TABLE retention_campaigns (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, gym_id BIGINT UNSIGNED NOT NULL,
 created_by BIGINT UNSIGNED NOT NULL, title VARCHAR(200) NOT NULL, message TEXT NOT NULL,
 inactivity_days SMALLINT UNSIGNED NOT NULL DEFAULT 30,
 channel ENUM('in_app','email','sms','whatsapp') NOT NULL DEFAULT 'in_app',
 status ENUM('draft','scheduled','running','completed','cancelled') NOT NULL DEFAULT 'draft',
 scheduled_at DATETIME NULL, created_at TIMESTAMP NULL, updated_at TIMESTAMP NULL,
 UNIQUE(gym_id,id), FOREIGN KEY(gym_id,created_by) REFERENCES gym_users(gym_id,user_id)
) ENGINE=InnoDB;

CREATE TABLE campaign_recipients (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY, gym_id BIGINT UNSIGNED NOT NULL,
 campaign_id BIGINT UNSIGNED NOT NULL, user_id BIGINT UNSIGNED NOT NULL,
 message_snapshot TEXT NOT NULL,
 status ENUM('pending','sent','delivered','read','failed') NOT NULL DEFAULT 'pending',
 sent_at DATETIME NULL, delivered_at DATETIME NULL, read_at DATETIME NULL, error_message TEXT,
 UNIQUE(campaign_id,user_id), FOREIGN KEY(gym_id,campaign_id) REFERENCES retention_campaigns(gym_id,id),
 FOREIGN KEY(gym_id,user_id) REFERENCES gym_users(gym_id,user_id)
) ENGINE=InnoDB;

CREATE TABLE notifications (
 id CHAR(36) PRIMARY KEY, type VARCHAR(255) NOT NULL,
 notifiable_type VARCHAR(255) NOT NULL, notifiable_id BIGINT UNSIGNED NOT NULL,
 data TEXT NOT NULL, read_at TIMESTAMP NULL, created_at TIMESTAMP NULL, updated_at TIMESTAMP NULL,
 INDEX(notifiable_type,notifiable_id)
) ENGINE=InnoDB;

CREATE TABLE personal_access_tokens (
 id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
 tokenable_type VARCHAR(255) NOT NULL, tokenable_id BIGINT UNSIGNED NOT NULL,
 name VARCHAR(255) NOT NULL, token CHAR(64) NOT NULL UNIQUE, abilities TEXT,
 last_used_at TIMESTAMP NULL, expires_at TIMESTAMP NULL,
 created_at TIMESTAMP NULL, updated_at TIMESTAMP NULL,
 INDEX(tokenable_type,tokenable_id), INDEX(expires_at)
) ENGINE=InnoDB;

CREATE TABLE password_reset_tokens (
 email VARCHAR(255) PRIMARY KEY, token VARCHAR(255) NOT NULL, created_at TIMESTAMP NULL
) ENGINE=InnoDB;

-- [WEB] Sessão de login do painel (driver "database" do Laravel).
CREATE TABLE sessions (
 id VARCHAR(255) PRIMARY KEY, user_id BIGINT UNSIGNED NULL,
 ip_address VARCHAR(45) NULL, user_agent TEXT NULL,
 payload LONGTEXT NOT NULL, last_activity INT NOT NULL,
 INDEX(user_id), INDEX(last_activity)
) ENGINE=InnoDB;

-- [WEB] Cache do Laravel; usado também para limitar tentativas de login no painel.
CREATE TABLE cache (
 `key` VARCHAR(255) PRIMARY KEY, value MEDIUMTEXT NOT NULL, expiration INT NOT NULL
) ENGINE=InnoDB;

CREATE TABLE cache_locks (
 `key` VARCHAR(255) PRIMARY KEY, owner VARCHAR(255) NOT NULL, expiration INT NOT NULL
) ENGINE=InnoDB;

-- [WEB] As duas unidades exibidas no painel. Não são dados fictícios.
INSERT INTO gyms (name, active, created_at, updated_at) VALUES
 ('Pryme Academia', TRUE, NOW(), NOW()),
 ('Move Academia', TRUE, NOW(), NOW());

-- [TESTE] Alunos FICTÍCIOS (2 por academia). Remover este bloco antes de usar em produção.
-- Senhas em bcrypt no formato do Laravel; a senha em texto está no comentário de cada linha.
SET @pryme = (SELECT id FROM gyms WHERE name='Pryme Academia' LIMIT 1);
SET @move  = (SELECT id FROM gyms WHERE name='Move Academia' LIMIT 1);

INSERT INTO users (name, email, password, email_verified_at, created_at, updated_at) VALUES
 ('Gabriel Souza',  'gabriel.souza@example.com',  '$2y$12$e5py1LITIVDyanTp3hfGeuSGeLD9tPvRei3NzIdyU..LqWNZDmc0K', NOW(), NOW(), NOW()); -- Gabriel@2026!
SET @gabriel = LAST_INSERT_ID();
INSERT INTO users (name, email, password, email_verified_at, created_at, updated_at) VALUES
 ('Larissa Mendes', 'larissa.mendes@example.com', '$2y$12$yQgzy8NlIGbmgN9OJXnXCeU0EvpV6iIw.bWFLvj.9MAJjPO/Mhxqi', NOW(), NOW(), NOW()); -- Larissa@2026!
SET @larissa = LAST_INSERT_ID();
INSERT INTO users (name, email, password, email_verified_at, created_at, updated_at) VALUES
 ('Thiago Rocha',   'thiago.rocha@example.com',   '$2y$12$p7gRHd9JEo/6Khh4jrRfWOEnRl0nvQkGKN1Wak0sk.EuLqKBZ4mqu', NOW(), NOW(), NOW()); -- Thiago@2026!
SET @thiago = LAST_INSERT_ID();
INSERT INTO users (name, email, password, email_verified_at, created_at, updated_at) VALUES
 ('Beatriz Nunes',  'beatriz.nunes@example.com',  '$2y$12$qJvXf2Jbvc1A6gVXsx5QkO2Xmrde4x0Vey6QtH0scrlfszREZUcMC', NOW(), NOW(), NOW()); -- Beatriz@2026!
SET @beatriz = LAST_INSERT_ID();

INSERT INTO gym_users (gym_id, user_id, status, joined_at) VALUES
 (@pryme, @gabriel, 'active', CURDATE()),
 (@pryme, @larissa, 'active', CURDATE()),
 (@move,  @thiago,  'active', CURDATE()),
 (@move,  @beatriz, 'active', CURDATE());

INSERT INTO gym_user_roles (gym_id, user_id, role) VALUES
 (@pryme, @gabriel, 'student'),
 (@pryme, @larissa, 'student'),
 (@move,  @thiago,  'student'),
 (@move,  @beatriz, 'student');

