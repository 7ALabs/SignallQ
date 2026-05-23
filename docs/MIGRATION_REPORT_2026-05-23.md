# Relatorio De Migracao - 2026-05-23

## Origem e destino

- Origem analisada: `E:\Projetos\Linka`
- Origem Android considerada principal: `E:\Projetos\Linka\linkaAndroidKotlin\linka-android-kotlin`
- Destino criado: `C:\Projetos\Linka Android`

## Decisao principal

O destino foi estruturado como raiz do Android nativo Kotlin, nao como copia da pasta antiga inteira. A pasta `linka-android-kotlin` deixou de ser subpasta e seus arquivos passaram a viver diretamente na raiz nova.

## Migrado

- Projeto Gradle/Kotlin nativo e seus modulos `app`, `core*` e `feature*`.
- Gradle wrapper e configuracoes de build.
- `key.properties.template`.
- `key.properties` real, com `storeFile` ajustado para `segredos/linka.jks`.
- Keystore de assinatura `segredos/linka.jks`.
- Certificado e chave Tailnet em `certificados/tailscale/`.
- `.env` local com webhooks de notificacao do squad.
- `.env.example` sem credenciais.
- `docs_ai` atual, sem historicos `.old`.
- `docs` operacional.
- Comandos em `.claude/commands`.
- Scripts operacionais em `scripts`, sem `node_modules`, caches ou capturas.
- Worker `cloudflare/ai-diagnosis-worker` para `integrations/cloudflare/ai-diagnosis-worker`, sem `node_modules` e sem `.wrangler`.

## Nao migrado

- PWA: `linkaSpeedtestPwa`.
- Flutter legado: `source/app`.
- APKs e builds prontos: `apk`, `builds`, `**/build`.
- Caches: `.gradle`, `.kotlin`, `.idea`, `.wrangler`, `.dart_tool`.
- Temporarios e historicos: `.old`, `tmp`, logs, zips e backups.
- Dependencias baixadas: `node_modules`, `__pycache__`.

## Sobre o `.env`

O arquivo `.env` encontrado na raiz antiga contem variaveis de ambiente para webhooks de notificacao, como Discord e Slack. Ele foi migrado para a raiz nova porque foi explicitamente autorizado. Ele nao e necessario para compilar o Android e permanece fora do Git.

Em 2026-05-23, o Slack foi validado pelo conector Codex no canal `#projeto-linka` (`C0B4NSGSK1D`). Como o conector permite postar no canal, mas nao expõe um Incoming Webhook URL, o `.env` foi complementado com `SLACK_CHANNEL_LINKA_ID`, `SLACK_CHANNEL_LINKA_NAME` e `SLACK_CHANNEL_LINKA_URL`. `SLACK_WEBHOOK_LINKA` permanece vazio ate existir um webhook real.

## Arquivos sensiveis migrados

Os arquivos sensiveis foram migrados porque foram explicitamente autorizados. Eles permanecem ignorados pelo Git via `.gitignore`:

- `key.properties`
- `segredos/linka.jks`
- `certificados/tailscale/*.crt`
- `certificados/tailscale/*.key`
- `.env`

## Observacao Git

O `.git` interno copiado inicialmente rastreava apenas dois arquivos e deixava a nova raiz quase inteira como nao rastreada. Para evitar uma base Git enganosa, ele foi removido do destino. A recomendacao e iniciar um repositorio limpo em `C:\Projetos\Linka Android` quando a estrutura for aprovada.

## Validacao esperada

```powershell
cd "C:\Projetos\Linka Android"
.\gradlew.bat test
.\gradlew.bat assembleDebug
```

## Validacao executada

- `.\gradlew.bat test` sem ambiente: falhou porque `JAVA_HOME`/`java` nao estavam no `PATH`.
- `.\gradlew.bat test` com `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`: Gradle iniciou, mas falhou porque o Android SDK nao esta configurado para a nova raiz.

Proximo ajuste local:

```properties
sdk.dir=C\:\\Users\\luizg\\AppData\\Local\\Android\\Sdk
```

Use este valor em `local.properties` somente se o SDK existir nesse caminho. Caso contrario, instale/configure o SDK pelo Android Studio e gere um `local.properties` local.
