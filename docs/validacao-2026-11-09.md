# Validação da integração da academia

Foram exercitados o cadastro de aluno no painel, a rejeição de e-mail duplicado, o login no aplicativo e a leitura de fichas publicadas. O cenário de persistência altera uma ficha pelo painel e confere seus exercícios e séries no aplicativo após reabrir a sessão.

As correções incluem senha inicial para novos alunos, preservação da senha na edição, coerência de status da matrícula, validação de repetições e atualização dos treinos durante a navegação. O ZIP Android corresponde ao código-fonte e foi compilado após uma extração limpa.

## Reproduzir

- API: `php artisan test` na pasta `api`.
- Integração MySQL de desenvolvimento: `php scripts/audit-mysql.php --run-local`, com o ambiente e as contas de demonstração preparados conforme o README. Os registros temporários são revertidos.
- Web: `pnpm test`, `pnpm exec tsc --noEmit`, `pnpm lint`, `pnpm build` e `pnpm build:local`.
- Android/API: configurar `ACADEMIA_TEST_API_URL` e executar `sh gradlew testDebugUnitTest`.
- Android/build: `sh gradlew assembleDebug assembleRelease lintDebug`.
- Android/navegação: `sh gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.app_academiafr.NavigationTest`.
- Persistência entre web e app: preparar uma conta e ficha de teste, definir as variáveis `ACADEMIA_E2E_*` descritas em `app/README.md` e executar `python3 scripts/test-published-workout.py`. O executor recusa resultados sem testes ou com omissões.

Respeite o limite de login da API ao executar as suítes reais. Não versione credenciais ou relatórios do seu ambiente.

## Limites

Aulas e reservas, medidas/evolução, sequência e promoções ainda são demonstrações no Android. As marcações de execução de exercícios não constituem histórico persistido no servidor. Contratação de plano orienta procurar a recepção; não há pagamento integrado.

Os PDFs são referências estáticas. A estrutura Drizzle/D1 é auxiliar ao template, enquanto as telas da academia usam a API/MySQL. Compilação e testes locais não substituem validação em aparelhos físicos, outros sistemas operacionais ou publicação em serviços de hospedagem.
