# Estrutura Do Projeto SignallQ Android

Esta pasta e a nova raiz independente do Android.

## Arvore principal

```text
C:\Projetos\SignallQ Android
|-- app/                         Aplicativo Android principal
|-- coreDatabase/                Persistencia local
|-- coreDatastore/               Preferencias e configuracoes locais
|-- coreNetwork/                 Rede e clientes compartilhados
|-- corePermissions/             Permissoes Android
|-- coreTelephony/               Telefonia e contexto celular
|-- featureDevices/              Dispositivos da rede local
|-- featureDiagnostico/          Diagnostico inteligente
|-- featureDns/                  DNS e benchmark
|-- featureFibra/                Fluxos de fibra/modem
|-- featureHistory/              Historico e exportacao
|-- featureHome/                 Home
|-- featureSettings/             Ajustes
|-- featureSpeedtest/            Medicao de velocidade
|-- featureWifi/                 Wi-Fi local
|-- docs/                        Documentos operacionais complementares
|-- docs_ai/                     Documentacao viva para agentes e contexto
|-- integrations/                Servicos externos relacionados ao Android
|-- scripts/                     Automacoes de build, release e analise
|-- scripts/legacy/              Scripts historicos fora do fluxo ativo
|-- builds/apk/                  APKs gerados, separados por tipo e versao
|-- segredos/                    Keystore local de assinatura Android
|-- certificados/tailscale/      Certificado e chave Tailnet locais
|-- .env                         Webhooks locais e canal Slack do conector
|-- .env.example                 Template sem credenciais
|-- gradle/                      Gradle wrapper
|-- build.gradle.kts             Plugins do build
|-- settings.gradle.kts          Mapa de modulos Gradle
|-- gradle.properties            Configuracao Gradle
|-- key.properties.template      Template de assinatura local
|-- AGENTS.md                    Guia de trabalho para agentes
```

## O que ficou fora

- `source/app/`: app Flutter legado, mantido fora da nova raiz.
- `.old/`, `tmp/`, caches, logs e builds locais.
- `apk/`: pasta antiga substituida por `builds/apk/`.
- APKs ja gerados: artefatos devem ser recriados pelos scripts oficiais.
- Nenhum arquivo `.env`; o arquivo real foi migrado localmente e continua fora do Git.
- `node_modules/`, `.wrangler/`, `.gradle/`, `.kotlin/`, `.idea/`.

## Criterio de migracao

Um arquivo foi migrado quando atende pelo menos um criterio:

- Necessario para compilar/testar o Android Kotlin.
- Fonte de verdade da documentacao Android atual.
- Script operacional ainda referenciado por docs Android.
- Integracao diretamente consumida pelo Android.

Um arquivo foi excluido quando e artefato gerado, cache ou legado nao ativo.

`segredos/`, `certificados/`, `key.properties` e `.env` existem localmente no destino, mas continuam protegidos por `.gitignore`.
