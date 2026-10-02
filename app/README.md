# App Android em Java

Abra esta pasta (`app/` do repositório) no Android Studio. O código do aplicativo está em `app/src/main/java/com/example/app_academiafr/MainActivity.java` e os arquivos Gradle usam Groovy (`.gradle`).

Para compilar pelo terminal, execute `sh gradlew :app:assembleDebug` nesta pasta. O APK de depuração será criado em `app/build/outputs/apk/debug/`.

O arquivo `App_AcademiaFR.zip` contém a mesma versão Java do projeto, sem arquivos temporários de compilação ou configurações locais do computador.

## Conexão com a API

O app usa a API Laravel da pasta `api/` (ver `api/README.md`). O código de comunicação fica em `app/src/main/java/com/example/app_academiafr/api/` (Retrofit + Gson).

- **Login:** e-mail e senha do aluno cadastrado na academia. O token fica no aparelho só se "Manter conectado" estiver marcado.
- **Dados reais:** academias do aluno, ficha de treino publicada, matrícula (perfil) e planos da academia.
- **Ainda demonstração:** aulas, evolução, retenção e anúncios (a API ainda não tem essas rotas).

No **emulador**, o app chama `http://10.0.2.2/api/` (o próprio computador) com o cabeçalho `Host: academia-api.test`, que é como o Herd encontra o site. Basta a API estar rodando no Herd.

Para outro endereço (celular físico ou servidor), defina no `gradle.properties`:

```
apiBaseUrl=https://seu-servidor/api/
apiHost=
```

Em produção a API deve usar HTTPS; HTTP sem criptografia só está liberado para `10.0.2.2` e `localhost` (`res/xml/network_security_config.xml`).

`ApiContractTest` (em `app/src/test`) testa o cliente contra a API do computador com o aluno de teste; se a API não estiver no ar, os testes são pulados.
