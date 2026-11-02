# Pryme & Move — AcademiaFR

Aplicativo Android dos alunos (`app/`), painel web (`web/`), API Laravel (`api/`) e banco MySQL (`banco/`).

## Contas de demonstração

Contas destinadas a desenvolvimento local. Defina as senhas dos administradores e personais no `.env`, que não é versionado. Os alunos fictícios são criados pelo SQL de demonstração; administradores e personais entram no painel web. Não use os dados de demonstração em produção.

| Perfil / nome | Usuário ou e-mail | Academia | Acesso |
|---|---|---|---|
| Administrador geral | `admin` | Pryme e Move | Trocar de academia; cadastrar alunos e planos; criar e publicar treinos |
| Administrador Pryme | `admin.prime` | Pryme | Gestão apenas da Pryme; planos da rede em leitura |
| Administrador Move | `admin.move` | Move | Gestão apenas da Move; planos da rede em leitura |
| Personal Pryme | `personal.prime` | Pryme | Consultar alunos e criar/publicar treinos da Pryme; sem alterar alunos ou planos |
| Personal Move | `personal.move` | Move | Consultar alunos e criar/publicar treinos da Move; sem alterar alunos ou planos |
| Aluno — Gabriel Souza | `gabriel.souza@example.com` | Pryme | Próprios dados e treinos no app; sem acesso ao painel |
| Aluna — Larissa Mendes | `larissa.mendes@example.com` | Pryme | Próprios dados e treinos no app; sem acesso ao painel |
| Aluno — Thiago Rocha | `thiago.rocha@example.com` | Move | Próprios dados e treinos no app; sem acesso ao painel |
| Aluna — Beatriz Nunes | `beatriz.nunes@example.com` | Move | Próprios dados e treinos no app; sem acesso ao painel |

## Preparar as contas e executar

1. Em um MySQL vazio, importe `banco/academiafinal.sql`. Ele cria as duas academias e os quatro alunos fictícios. Não execute a migração web novamente sobre esse esquema completo.
2. Na pasta `api`, execute `composer install`, copie `.env.example` para `.env`, configure seu MySQL e execute `php artisan key:generate`.
3. No `.env` local, defina as senhas das contas a criar:

```dotenv
ADMIN_PASSWORD="defina-uma-senha-local"
ADMIN_PRIME_PASSWORD="defina-uma-senha-local"
ADMIN_MOVE_PASSWORD="defina-uma-senha-local"
PERSONAL_PRIME_PASSWORD="defina-uma-senha-local"
PERSONAL_MOVE_PASSWORD="defina-uma-senha-local"
```

4. Execute `php artisan db:seed --class=AdminSeeder` e `php artisan db:seed --class=PersonalSeeder`. O AdminSeeder atualiza os administradores; o PersonalSeeder preserva contas existentes. Não envie o `.env` ao GitHub.
5. Inicie a API: `php artisan serve --host=127.0.0.1 --port=8000`.
6. Em outro terminal, na pasta `web`, execute `pnpm install` e `LARAVEL_API_URL=http://127.0.0.1:8000/api pnpm preview:local` (Node 22.13+).
7. Abra <http://127.0.0.1:5173/>. Use **Sair e voltar ao login** para trocar de conta. O administrador geral troca de unidade no seletor ou nos botões das academias; o personal permanece na sua unidade.

O SQL não cadastra planos ou treinos. Cadastre um plano antes de matricular um aluno; a ausência de registros aparece como estado vazio. As restrições de unidade e perfil são verificadas também na API, inclusive em chamadas diretas.

## Verificação

- API: em `api/`, execute `php artisan test`. Os testes de permissões usam SQLite em memória, sem alterar o MySQL local.
- Web: em `web/`, execute `pnpm test`, `pnpm exec tsc --noEmit` e `pnpm build:local`.
- Confira no navegador: troca de unidade com `admin`, sair e entrar com cada personal, criação/edição de treino e bloqueio do login de aluno no painel.
