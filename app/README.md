# App Android em Java

Abra esta pasta (`app/` do repositório) no Android Studio. O código do aplicativo está em `app/src/main/java/com/example/app_academiafr/MainActivity.java` e os arquivos Gradle usam Groovy (`.gradle`).

Para compilar pelo terminal, execute `sh gradlew :app:assembleDebug` nesta pasta. O APK de depuração será criado em `app/build/outputs/apk/debug/`.

O arquivo `App_AcademiaFR.zip` contém a mesma versão Java do projeto, sem arquivos temporários de compilação ou configurações locais do computador.

## Conexão com a API

O app usa a API Laravel da pasta `api/` (ver `api/README.md`). O código de comunicação fica em `app/src/main/java/com/example/app_academiafr/api/` (Retrofit + Gson).

- **Login:** e-mail e senha do aluno cadastrado na academia. O token fica no aparelho só se "Manter conectado" estiver marcado.
- **Dados reais:** academias do aluno, ficha de treino publicada, matrícula (perfil) e planos da academia.
- **Ainda demonstração:** aulas, evolução, sequência e promoções (dados ilustrativos, sem persistência no servidor). O aplicativo dos alunos não inclui gestão, retenção ou criação de anúncios.

No **emulador**, o padrão é `http://10.0.2.2:8000/api/`, correspondente a `php artisan serve --host=127.0.0.1 --port=8000` no Mac. No Android 17+, permita o acesso à rede local quando solicitado. Para usar o Herd, configure `apiBaseUrl=http://10.0.2.2/api/` e `apiHost=academia-api.test`.

Para outro endereço (celular físico ou servidor), defina no `gradle.properties`:

```
apiBaseUrl=https://seu-servidor/api/
apiHost=
```

Em produção a API deve usar HTTPS; HTTP sem criptografia só está liberado para `10.0.2.2` e `localhost` (`res/xml/network_security_config.xml`).

`ApiContractTest` testa o cliente contra a API real. Ative com `ACADEMIA_TEST_API_URL=http://127.0.0.1:8000/api/ sh gradlew testDebugUnitTest`. Com essa variável definida, API indisponível faz o teste falhar; sem ela, a integração é explicitamente pulada.

`sh gradlew connectedDebugAndroidTest` executa testes no emulador (API e contas fictícias devem existir), incluindo validação de login, botões, filtros, reservas demonstrativas, retorno, recriação da Activity e logout. `sh gradlew assembleRelease lintDebug` verifica a compilação de release e análise estática. O release é não assinado e não constitui publicação na Play Store.

## Atualização dos treinos

O app consulta novamente as fichas publicadas ao voltar do segundo plano e ao entrar em Treinos ou Início. A tela de Treinos também possui o botão ATUALIZAR TREINOS. Respostas pendentes de uma sessão encerrada são ignoradas para evitar carregar dados ao trocar de conta.

Para que o treino apareça, publique a ficha vinculada ao aluno correto no painel e configure a web e o app para usar a mesma API e banco de dados.


## Conta cadastrada no web e persistência

O cadastro de novos alunos no painel define a senha inicial usada no app; a edição da matrícula não a troca. Para executar o cenário integrado, publique uma ficha para uma conta de teste no painel, defina `ACADEMIA_E2E_EMAIL`, `ACADEMIA_E2E_PASSWORD`, `ACADEMIA_E2E_TITLE`, `ACADEMIA_E2E_COUNT` (quantidade de exercícios) e `ACADEMIA_E2E_SETS` (séries do primeiro exercício) no ambiente e execute `python3 scripts/test-published-workout.py`. Ele confere o login real, a ficha, os valores salvos, a atualização e a reabertura com a sessão persistida. Falha se o executor rodar zero testes ou omitir o cenário. Não versionar credenciais.
