# API Academia (Laravel)

Backend único do projeto: o **app Android** e o **painel web** (`../web`) usam esta API, que acessa o banco MySQL `academiafinal`.

## Rodar localmente (Herd)

1. Instale as dependências: `composer install`
2. Copie `.env.example` para `.env` e rode `php artisan key:generate`.
3. No `.env`, preencha a conexão MySQL (`DB_*`) e as senhas dos administradores e personais (`ADMIN_*_PASSWORD` e `PERSONAL_*_PASSWORD`).
4. Crie o banco com `../banco/academiafinal.sql`.
5. Registre o site no Herd: `herd link academia-api` → `http://academia-api.test`
6. Crie os administradores do painel: `php artisan db:seed --class=AdminSeeder`. Crie os personais: `php artisan db:seed --class=PersonalSeeder` (preserva contas existentes).

> **Não rode `php artisan migrate`.** A estrutura do banco é definida em `../banco/academiafinal.sql`; alterações de tabela vão nos arquivos `.sql` dessa pasta.

## Autenticação

`POST /api/login` com `{login, password, device_name}` (login = e-mail do aluno ou usuário do administrador/personal) devolve um token.
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

Admin geral (`users.is_global_admin`) vê todas as academias; admin de unidade (`gym_user_roles.role = 'admin'`) só a própria. Personal (`instructor`) consulta alunos e cria/edita treinos somente da unidade em que possui vínculo ativo. Personal não altera alunos ou planos e não recebe a lista de planos. Os dados de um treino incluem `unit`, validado junto ao aluno e ao registro existente. Consulte a tabela completa de contas no [README principal](../README.md).

## Auditoria local com rollback

`php artisan test` usa SQLite em memória. Para validar cadastros, matrícula, publicação e edição com as restrições reais do MySQL, execute `php scripts/audit-mysql.php --run-local` no ambiente de desenvolvimento já preparado. Os registros criados pelo script ficam numa transação revertida ao final; as contas admin e personal.prime devem existir.

A API também pode ser iniciada sem Herd: `php artisan serve --host=127.0.0.1 --port=8000`. Essa é a configuração padrão do emulador e do guia principal.
