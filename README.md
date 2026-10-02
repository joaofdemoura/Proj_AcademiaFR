# Pryme & Move — painel administrativo

Aplicação web para administrar duas academias, inspirada nas referências mobile fornecidas: fundo escuro, detalhes em laranja, tipografia condensada e cartões com contornos discretos.

## Funcionalidades

- Visão geral por unidade ou consolidada, calculada a partir dos cadastros.
- Cadastro e edição de alunos, plano, unidade, situação e vencimento.
- Busca de alunos e filtros por situação/renovação.
- Criação e edição de planos, preços e benefícios.
- Editor de treinos com aluno, objetivo, frequência, exercícios, séries, repetições e descanso.
- Rascunhos e publicação no painel, com prévia de conteúdo mobile.
- Banco persistente D1 e rotas protegidas por identidade da plataforma na versão hospedada.

Os registros iniciais são fictícios e estão identificados como demonstração. Os indicadores de vencimento usam 02/10/2026 como data-base da demonstração. Mensalidades ativas representam o valor dos planos, e não pagamentos recebidos.

## Limite da integração mobile

Publicar um treino salva o status no banco do painel. Não envia notificações nem conecta automaticamente um aplicativo externo. A autenticação de alunos, permissões por academia, API para o aplicativo mobile e integração com o backend escolhido devem ser implementadas antes de usar dados reais em produção.

## Desenvolvimento

Requer Node.js 22.13 ou superior e npm.

```sh
npm ci
npm run db:generate
npm run build
node --import ./scripts/sites-env.mjs ./node_modules/wrangler/bin/wrangler.js d1 execute DB --local --config dist/server/wrangler.json --persist-to .wrangler/state --file drizzle/0000_migration.sql
npm run dev
```

Substitua `0000_migration.sql` pelo nome real da primeira migração em `drizzle/`. Aplique cada migração pendente uma única vez. A prévia usa autenticação local de desenvolvimento; o ambiente hospedado usa o acesso privado da plataforma.

## Arquivos principais

- `app/dashboard.tsx`: telas, navegação e formulários.
- `app/globals.css`: identidade visual e responsividade.
- `app/api/gym/route.ts`: validação e persistência.
- `db/schema.ts`: estrutura do banco.
- `lib/gym.ts`: tipos e dados fictícios iniciais.

As fontes são incluídas em `public/fonts` para evitar dependência de carregamento externo.

## Abrir a versão local pronta

1. Abra `iniciar.cmd` no Windows (ou execute `node scripts/local-server.mjs` nesta pasta).
2. Acesse http://127.0.0.1:5173 no navegador.
3. Mantenha o terminal aberto enquanto usa o painel.

A versão local já compilada está em `local-build/` e não precisa de `npm install` para abrir. Requer Node.js 22.13+ com `node:sqlite` (validado em Node.js 24). O iniciador também encontra o Node incluído no Codex, quando disponível.

Esta prévia atende apenas em 127.0.0.1, com login de administrador local, e grava no arquivo `.local/gym.sqlite`. Nunca exponha este servidor à rede: use a versão hospedada protegida para acesso externo. O banco local e o D1 hospedado são separados.

A publicação hospedada ficou pendente porque o ambiente bloqueou a entrada do terminal usada pelo publicador e subprocessos das ferramentas de build. A versão local foi compilada e validada. A migração inicial gerada pelo Drizzle é `drizzle/0000_gym_records.sql`.

## Login administrativo local

Abra `/login` e use a conta `admin`. A senha inicial é gerada aleatoriamente na primeira configuração e exibida no terminal; não existe senha fixa no código. A conta atual já foi configurada e a senha foi entregue na conversa.

A senha é armazenada como hash scrypt em `.local/admin.json`. A sessão usa cookie HttpOnly/SameSite=Strict, expira em oito horas e é revogada ao sair. Reiniciar o servidor encerra as sessões, preservando a conta. Há limite de tentativas de login. O servidor continua restrito a 127.0.0.1; a autenticação hospedada é separada.

## Administradores e unidades

- `admin`: acesso geral às duas academias; a senha anterior foi preservada.
- `admin.prime`: acesso somente aos alunos e treinos da Pryme Academia.
- `admin.move`: acesso somente aos alunos e treinos da Move Academia.

As restrições são verificadas no servidor, incluindo consultas e alterações por identificador. Administradores de unidade não podem transferir alunos nem associar treinos a alunos externos. Os planos compartilhados da rede são somente leitura para essas contas; cada unidade pode criar seus próprios planos. As credenciais são persistidas como hashes em `.local/accounts.json` e não integram o pacote de código. Em uma instalação nova, o servidor gera as três contas e informa as senhas iniciais no terminal.
