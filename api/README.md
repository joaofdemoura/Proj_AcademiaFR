# API Academia (Laravel)

Backend único do projeto: o **app Android** e o **painel web** (`../web`) usam esta API, que acessa o banco MySQL `academiafinal`.

## Rodar localmente (Herd)

1. Instale as dependências: `composer install`
2. Copie `.env.example` para `.env` e rode `php artisan key:generate`.
3. No `.env`, preencha a conexão MySQL (`DB_*`) e as senhas dos administradores (`ADMIN_*_PASSWORD`).
4. Crie o banco com `../banco/academiafinal.sql`.
5. Registre o site no Herd: `herd link academia-api` → `http://academia-api.test`
6. Crie os administradores do painel: `php artisan db:seed --class=AdminSeeder`

> **Não rode `php artisan migrate`.** A estrutura do banco é definida em `../banco/academiafinal.sql`; alterações de tabela vão nos arquivos `.sql` dessa pasta.

## Autenticação

`POST /api/login` com `{login, password, device_name}` (login = e-mail do aluno ou usuário do admin) devolve um token.
Envie-o nas demais chamadas: `Authorization: Bearer <token>`.

## Rotas

| Método | Rota | Quem usa |
|---|---|---|
| POST | `/api/login` | app e painel (5 tentativas/min) |
| GET | `/api/me` | app e painel |
| POST | `/api/logout` | app e painel |
| GET | `/api/me/workouts` | app: fichas publicadas do aluno |
| GET | `/api/me/memberships` | app: matrículas do aluno |
| GET | `/api/gyms/{id}/plans` | app: planos da academia |
| GET | `/api/admin/data` | painel: alunos, planos e fichas |
| POST | `/api/admin/records` | painel: salva aluno, plano ou ficha (`{kind, record}`) |

Admin geral (`users.is_global_admin`) vê todas as academias; admin de unidade (`gym_user_roles.role = 'admin'`) só a própria.
