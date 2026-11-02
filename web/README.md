# Painel Pryme & Move

Painel web conectado à API Laravel em `../api`. O administrador geral alterna entre as academias; administradores de unidade e personais ficam restritos à própria academia. Personais consultam alunos e montam/publicam treinos, sem cadastrar alunos ou alterar planos.

Consulte a [tabela de contas e o preparo do banco](../README.md). As senhas são definidas no `.env` da API, sem arquivos de credenciais no navegador.

## Executar localmente

Com Node 22.13+ e a API ativa:

```sh
pnpm install
LARAVEL_API_URL=http://127.0.0.1:8000/api pnpm preview:local
```

Abra <http://127.0.0.1:5173/>. O comando gera o cliente e a ponte de autenticação em `local-build/`, que não é versionado. Reinicie esse comando após alterar o código da ponte.

## Navegação e dados

- **Sair e voltar ao login** encerra a sessão e permite trocar de conta.
- O seletor de academia e os botões das unidades filtram os dados e limpam filtros anteriores.
- Cadastre um plano antes de salvar uma matrícula. Alunos existentes não podem ser transferidos de academia nesse formulário.
- Selecione um aluno para montar, visualizar, salvar como rascunho ou publicar um treino.
- A prévia permite voltar à edição; campos inválidos reaparecem para correção antes de salvar.
- Treinos publicados são disponibilizados pela API para o app do aluno.

O token da API é guardado em cookie HttpOnly. As restrições são verificadas no servidor, mesmo quando alguém chama a API diretamente.

## Verificação

```sh
pnpm test
pnpm exec tsc --noEmit
pnpm build:local
```

Os testes de autorização por academia ficam em `../api/tests/Feature/PanelAccessTest.php` (`php artisan test`).
