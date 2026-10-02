# Pryme & Move — painel administrativo

Aplicação web para administrar as academias Pryme e Move, inspirada nas telas mobile fornecidas: fundo escuro, detalhes em laranja, tipografia Montserrat e cartões com contornos discretos.

## Visão geral

| Item | Descrição |
| --- | --- |
| Objetivo | Gerenciar alunos, planos e treinos das duas academias em um único painel. |
| Academias | Pryme Academia e Move Academia. |
| Interface | Painel responsivo com identidade visual compartilhada com o aplicativo mobile. |
| Dados iniciais | Registros fictícios para demonstração. Os indicadores usam 02/10/2026 como data-base. |
| Mensalidades | Representam o valor dos planos ativos, não pagamentos recebidos. |

## Funcionalidades

| Área | Recursos |
| --- | --- |
| Visão geral | Indicadores por unidade ou consolidados. |
| Alunos | Cadastro, edição, busca, filtros por situação e renovação. |
| Planos | Criação e edição de planos, preços e benefícios. |
| Treinos | Seleção de aluno em ordem alfabética, objetivo, frequência, exercícios, séries, repetições e descanso. |
| Publicação | Rascunho, publicação no painel e prévia com conteúdo inspirado no app mobile. |
| Persistência | Banco D1 e rotas protegidas por identidade na versão hospedada. |

## Limite da integração mobile

Publicar um treino salva o status no banco do painel. A versão atual ainda não envia notificações nem conecta automaticamente um aplicativo externo. Antes de usar dados reais, implemente a autenticação de alunos, as permissões por academia, a API do aplicativo mobile e a integração com o backend escolhido.

## Desenvolvimento

| Requisito | Versão / observação |
| --- | --- |
| Node.js | 22.13 ou superior (a prévia local foi validada no Node.js 24). |
| Gerenciador | npm. |
| Banco local | `node:sqlite`, gravado em `.local/gym.sqlite`. |
| Migrações | Arquivos em `drizzle/`; aplique cada migração pendente uma única vez. |

```sh
npm ci
npm run db:generate
npm run build
node --import ./scripts/sites-env.mjs ./node_modules/wrangler/bin/wrangler.js d1 execute DB --local --config dist/server/wrangler.json --persist-to .wrangler/state --file drizzle/0000_migration.sql
npm run dev
```

Substitua `0000_migration.sql` pelo nome real da primeira migração em `drizzle/`.

## Arquivos principais

| Arquivo | Responsabilidade |
| --- | --- |
| `app/dashboard.tsx` | Telas, navegação, formulários e montagem de treinos. |
| `app/globals.css` | Identidade visual, Montserrat e responsividade. |
| `app/api/gym/route.ts` | Validação, autorização e persistência. |
| `db/schema.ts` | Estrutura do banco. |
| `lib/gym.ts` | Tipos e dados fictícios iniciais. |
| `public/logos/` | Logos Pryme e Move usados no seletor de academia. |

As fontes são incluídas em `public/fonts` para evitar dependência de carregamento externo.

## Abrir a versão local pronta

| Passo | Ação |
| --- | --- |
| 1 | Abra `iniciar.cmd` no Windows ou execute `node scripts/local-server.mjs` nesta pasta. |
| 2 | Acesse [http://127.0.0.1:5173](http://127.0.0.1:5173). |
| 3 | Mantenha o terminal aberto enquanto usa o painel. |

A versão local compilada está em `local-build/` e não precisa de `npm install`. O servidor atende somente em `127.0.0.1`; nunca o exponha diretamente à rede. O banco local e o D1 hospedado são separados.

## Login administrativo local

| Informação | Detalhe |
| --- | --- |
| Entrada | Acesse `/login`. |
| Usuário inicial | `admin`. |
| Senha | Gerada aleatoriamente na primeira configuração e exibida no terminal; não existe senha fixa no código. |
| Armazenamento | Hash scrypt em `.local/admin.json`. |
| Sessão | Cookie HttpOnly/SameSite=Strict, validade de oito horas e revogação ao sair. |
| Proteção | Limite de tentativas; reiniciar o servidor encerra as sessões. |

As credenciais locais não integram o pacote de código. A autenticação hospedada é separada.

## Administradores e unidades

| Usuário | Escopo de acesso |
| --- | --- |
| `admin` | Acesso geral às duas academias. |
| `admin.prime` | Somente alunos e treinos da Pryme Academia. |
| `admin.move` | Somente alunos e treinos da Move Academia. |

As restrições são verificadas no servidor, inclusive em consultas e alterações por identificador. Administradores de unidade não podem transferir alunos nem associar treinos a alunos externos. Planos compartilhados da rede são somente leitura para essas contas; cada unidade pode criar seus próprios planos. Em uma instalação nova, o servidor gera as três contas e informa as senhas iniciais no terminal.
