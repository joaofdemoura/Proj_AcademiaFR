-- Atualiza um banco academiafinal JÁ CRIADO com o script antigo (só app)
-- para a versão com o painel web. Não apaga dados.
-- Se o banco ainda não existe, use academiafinal.sql inteiro em vez deste arquivo.
USE academiafinal;

-- users: login do painel, admin geral e aluno sem senha até ativar o app
ALTER TABLE users
 ADD COLUMN username VARCHAR(60) NULL UNIQUE AFTER email,
 MODIFY password VARCHAR(255) NULL COMMENT 'Hash gerado pelo Laravel; nunca senha em texto',
 ADD COLUMN is_global_admin BOOLEAN NOT NULL DEFAULT FALSE AFTER password;

-- Planos da rede ("Todas"): solta as referências antigas para poder mudar plans.gym_id
ALTER TABLE memberships DROP FOREIGN KEY memberships_ibfk_2;
ALTER TABLE promotions DROP FOREIGN KEY promotions_ibfk_2;
ALTER TABLE plans DROP FOREIGN KEY plans_ibfk_1;
ALTER TABLE plans MODIFY gym_id BIGINT UNSIGNED NULL;
ALTER TABLE plans ADD FOREIGN KEY(gym_id) REFERENCES gyms(id);

CREATE TABLE plan_gyms (
 gym_id BIGINT UNSIGNED NOT NULL, plan_id BIGINT UNSIGNED NOT NULL,
 PRIMARY KEY(gym_id,plan_id),
 FOREIGN KEY(gym_id) REFERENCES gyms(id), FOREIGN KEY(plan_id) REFERENCES plans(id)
) ENGINE=InnoDB;

-- Planos já existentes continuam disponíveis na própria academia
INSERT INTO plan_gyms (gym_id, plan_id) SELECT gym_id, id FROM plans WHERE gym_id IS NOT NULL;

ALTER TABLE memberships ADD FOREIGN KEY(gym_id,plan_id) REFERENCES plan_gyms(gym_id,plan_id);
ALTER TABLE promotions ADD FOREIGN KEY(gym_id,plan_id) REFERENCES plan_gyms(gym_id,plan_id);

-- workout_plans: rascunho/publicado e dias de treino
-- Fichas que já existem ficam como publicadas, para continuarem aparecendo no app.
ALTER TABLE workout_plans
 ADD COLUMN status ENUM('draft','published') NOT NULL DEFAULT 'draft' AFTER version,
 ADD COLUMN published_at DATETIME NULL AFTER status,
 ADD COLUMN schedule_description VARCHAR(200) NULL AFTER published_at;
UPDATE workout_plans SET status='published', published_at=COALESCE(created_at,NOW());
ALTER TABLE workout_plans
 ADD INDEX(gym_id,status),
 ADD CHECK(status='draft' OR published_at IS NOT NULL);

-- Tabelas do Laravel para o login do painel
CREATE TABLE sessions (
 id VARCHAR(255) PRIMARY KEY, user_id BIGINT UNSIGNED NULL,
 ip_address VARCHAR(45) NULL, user_agent TEXT NULL,
 payload LONGTEXT NOT NULL, last_activity INT NOT NULL,
 INDEX(user_id), INDEX(last_activity)
) ENGINE=InnoDB;

CREATE TABLE cache (
 `key` VARCHAR(255) PRIMARY KEY, value MEDIUMTEXT NOT NULL, expiration INT NOT NULL
) ENGINE=InnoDB;

CREATE TABLE cache_locks (
 `key` VARCHAR(255) PRIMARY KEY, owner VARCHAR(255) NOT NULL, expiration INT NOT NULL
) ENGINE=InnoDB;

-- As duas unidades do painel (só cadastra se ainda não existirem)
INSERT INTO gyms (name, active, created_at, updated_at)
SELECT 'Pryme Academia', TRUE, NOW(), NOW() WHERE NOT EXISTS (SELECT 1 FROM gyms WHERE name='Pryme Academia');
INSERT INTO gyms (name, active, created_at, updated_at)
SELECT 'Move Academia', TRUE, NOW(), NOW() WHERE NOT EXISTS (SELECT 1 FROM gyms WHERE name='Move Academia');

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
