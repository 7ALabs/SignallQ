# Publicação na Play Console (gradle-play-publisher)

Publicação automatizada do AAB assinado via linha de comando. Configurado em
`app/build.gradle.kts` (bloco `play {}`) com o plugin `com.github.triplet.play`.

## Pré-requisitos (uma vez)

1. **App criado na Play Console** com o package `io.signallq.app`.
2. **Primeira release enviada manualmente** pela web em cada trilha — o Google
   exige o primeiro upload manual; a API não cria o app do zero.
3. **Service account** no Google Cloud (projeto `signallq-app`) com a Play
   Developer API habilitada, convidada em Play Console → Usuários e permissões
   com permissão de release na trilha alvo.
4. Baixar o JSON da service account e referenciá-lo (nunca commitar):
   - `key.properties`: `playServiceAccountFile=play-service-account.json`, ou
   - env `PLAY_SERVICE_ACCOUNT_JSON_FILE=/caminho/para/o.json`

O `.gitignore` já bloqueia `play-service-account*.json` e `key.properties`.

## Publicar

Trilha default: `alpha` (teste fechado). Override com `-PplayTrack=`.

```
# clean build do AAB assinado
./android/gradlew.bat clean bundleRelease --no-build-cache

# enviar para teste fechado (alpha)
./android/gradlew.bat :app:publishReleaseBundle

# outra trilha
./android/gradlew.bat :app:publishReleaseBundle -PplayTrack=internal
```

Versão publicada vem de `libs.versions.toml` (`versionCode` / `versionName`).
Subir o `versionCode` antes de cada envio — a Play rejeita code repetido.

## Release notes públicas

Ficam em `app/src/main/play/release-notes/pt-BR/default.txt` (lidas
automaticamente pelo gradle-play-publisher no `publishReleaseBundle` e também
reaproveitadas como corpo do GitHub Release pelo workflow `release.yml`).

Regras para escrever a nota de cada versão:

- Limite de 500 caracteres (limite da Play Console).
- Português, tom direto, foco no que o usuário ganha — não é changelog técnico.
- **Proibido**: nomes de marca antiga (Veloo/Linka), menção a Firebase App
  Distribution, "beta", "teste fechado", nomes internos de squad/agente,
  números de issue/PR.
- O histórico técnico completo (com marca antiga, PRs, issues) continua só no
  `CHANGELOG.md` — nunca copiar trechos dele direto para cá.
- Atualizar este arquivo a cada bump de versão, antes de criar a tag.
