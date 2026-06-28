# SignallQ

[![Android CI](https://github.com/gmmattey/linka-android/actions/workflows/android-ci.yml/badge.svg)](https://github.com/gmmattey/linka-android/actions/workflows/android-ci.yml)
[![PWA CI](https://github.com/gmmattey/linka-android/actions/workflows/pwa-ci.yml/badge.svg)](https://github.com/gmmattey/linka-android/actions/workflows/pwa-ci.yml)

App Android de **diagnóstico de conectividade** com IA. O SignallQ analisa Wi-Fi, fibra, rede móvel e dispositivos da rede local para gerar diagnósticos claros, acionáveis e escritos em linguagem natural.

> **Nota de marca:** a marca atual é **SignallQ**. Alguns identificadores técnicos ainda preservam nomes anteriores por compatibilidade de infraestrutura, como package `io.veloo.app`, repositório `gmmattey/linka-android`, banco `linkaKotlin.db` e workers legados.

## Status do projeto

- **Produto:** SignallQ
- **Plataforma principal:** Android
- **Estado:** desenvolvimento / preparação de lançamento
- **Distribuição atual:** builds internos e validação via Firebase / Play Store interna
- **Roadmap público:** lançamento Android na Play Store
- **Gestão:** Linear + GitHub Issues + fluxo de revisão por agentes

## Visão geral

O SignallQ é um app utilitário para usuários que querem entender a qualidade real da própria conexão sem precisar interpretar métricas técnicas cruas.

O app cobre:

- teste de velocidade;
- leitura de sinal Wi-Fi e rede móvel;
- diagnóstico de estabilidade;
- análise de DNS;
- histórico de medições;
- detecção de dispositivos na rede local;
- diagnóstico assistido por IA;
- telemetria, analytics e crash reporting para melhoria contínua.

Fora do escopo atual:

- app iOS em produção;
- automação completa via Open Finance, CEI ou integrações não relacionadas à rede;
- recomendações comerciais invasivas;
- dependência obrigatória de chat livre para diagnóstico.

## Estrutura do repositório

```text
.
├── android/                                  # App Android Kotlin + Jetpack Compose
├── SignallQ Admin/                           # Painel administrativo React + Vite + TypeScript
├── integrations/cloudflare/                  # Workers Cloudflare e integrações server-side
├── docs_ai/                                  # Documentação ativa e fonte principal de contexto
├── docs/                                     # Documentação complementar e arquivo histórico
├── assets/                                   # Assets de marca, ícones e materiais de loja
├── .github/workflows/                        # Workflows de CI
└── CHANGELOG.md                              # Histórico de mudanças do projeto
```

A documentação canônica começa em [`docs_ai/README.md`](docs_ai/README.md). Material em pastas `_archive/` é histórico e não deve ser usado como fonte de verdade atual.

## Stack principal

### Android

- Kotlin
- Jetpack Compose
- Material 3
- MVVM + `StateFlow`
- Hilt
- Room (`SignallQDatabase`)
- DataStore
- WorkManager (`MonitoramentoWorker`)
- Firebase Analytics
- Firebase Crashlytics
- minSdk 24
- targetSdk / compileSdk 36
- JDK 17

### IA e integrações

- Cloudflare Worker para diagnóstico com IA
- Cloudflare Workers AI
- URL de IA via `BuildConfig.AI_WORKER_URL`
- Fallbacks e contratos documentados em `docs_ai/technical/AI_FLOW.md` e `docs_ai/technical/CLOUDFLARE.md`

### Admin / Web

- React
- TypeScript
- Vite
- Tailwind CSS
- Cloudflare Workers / D1 quando aplicável

## Arquitetura Android

O app Android está organizado em módulos Gradle independentes.

```text
:app
:coreNetwork
:coreDatabase
:coreDatastore
:coreTelephony
:corePermissions
:featureHome
:featureSpeedtest
:featureWifi
:featureDevices
:featureDns
:featureFibra
:featureDiagnostico
:featureHistory
:featureSettings
```

Regras principais:

- `:app` faz composição, navegação e injeção inicial.
- `:core*` concentra infraestrutura, dados compartilhados e integrações nativas.
- `:feature*` concentra telas e fluxos de produto.
- Features não devem depender diretamente de outras features.
- Regra de negócio não deve ficar dentro de Composable.
- Componentes visuais devem seguir o design system do SignallQ.

Referências úteis:

- [`docs_ai/technical/ARCHITECTURE.md`](docs_ai/technical/ARCHITECTURE.md)
- [`docs_ai/technical/MODULES.md`](docs_ai/technical/MODULES.md)
- [`docs_ai/technical/PROJECT_STRUCTURE.md`](docs_ai/technical/PROJECT_STRUCTURE.md)
- [`docs_ai/design-system/COMPONENTS_ANDROID.md`](docs_ai/design-system/COMPONENTS_ANDROID.md)

## Requisitos locais

Antes de rodar o projeto, garanta:

- Android Studio em versão estável recente;
- JDK 17 configurado;
- Android SDK compatível com compileSdk 36;
- Gradle Wrapper do próprio projeto;
- Firebase configurado para o app Android;
- arquivo `android/app/google-services.json` disponível no ambiente de build;
- variáveis/URLs de workers configuradas conforme ambiente, quando necessário.

## Como rodar localmente

Clone o repositório e abra a pasta `android/` no Android Studio.

Comandos principais:

```bash
cd android

# Build debug
./gradlew assembleDebug

# Testes unitários
./gradlew test

# Ktlint
./gradlew ktlintCheck

# Detekt
./gradlew detekt
```

Para corrigir formatação Kotlin automaticamente:

```bash
cd android
./gradlew ktlintFormat
```

## Qualidade e CI

O repositório possui workflows separados por plataforma.

### Android CI

Arquivo: [`.github/workflows/android-ci.yml`](.github/workflows/android-ci.yml)

Executa em `push` para `main` e em `pull_request` contra `main` quando há mudança em `android/**` ou no próprio workflow.

Jobs:

- testes unitários;
- `ktlintCheck`;
- `detekt`;
- build de APK debug.

### PWA CI

Arquivo: [`.github/workflows/pwa-ci.yml`](.github/workflows/pwa-ci.yml)

Executa em `push` para `main` e em `pull_request` contra `main` quando há mudança em `pwa/**` ou no próprio workflow.

Quando o PWA estiver inicializado, executa:

- `npm ci`;
- build;
- testes, se houver script disponível.

Referência completa: [`docs_ai/operations/ci-cd.md`](docs_ai/operations/ci-cd.md).

## Release

O README não é a fonte completa de release. Ele só aponta o caminho certo.

Documentos canônicos:

- [`docs_ai/operations/RELEASE.md`](docs_ai/operations/RELEASE.md)
- [`docs_ai/operations/DEPLOY.md`](docs_ai/operations/DEPLOY.md)
- [`docs_ai/operations/VERSIONING.md`](docs_ai/operations/VERSIONING.md)
- [`docs_ai/operations/GuiaReleaseBuild.md`](docs_ai/operations/GuiaReleaseBuild.md)
- [`docs_ai/operations/APK_OUTPUT_POLICY.md`](docs_ai/operations/APK_OUTPUT_POLICY.md)

Regra importante: build de release deve ser feito com `clean` e `--no-build-cache` para evitar artefato desatualizado.

Resumo operacional:

```bash
git push origin main
cd android
./gradlew clean assembleRelease --no-build-cache
./gradlew appDistributionUploadRelease
```

Se houver mudança em worker Cloudflare dentro de `integrations/cloudflare/*/src/`, o deploy do worker deve ser feito antes do fechamento da entrega.

## Fluxo de contribuição

Fluxo mínimo esperado:

1. Criar ou vincular uma issue/task.
2. Criar branch a partir de `main`.
3. Implementar escopo pequeno e verificável.
4. Rodar checks locais relevantes.
5. Atualizar documentação quando o comportamento mudar.
6. Atualizar `CHANGELOG.md` quando houver mudança visível ao usuário ou relevante para release.
7. Abrir PR.
8. Aguardar CI verde e revisão.
9. Fazer merge somente após aprovação.

Bugs nascem no GitHub Issues. Features, tasks e planejamento ficam no Linear quando aplicável.

## Documentação

Entrada principal:

- [`docs_ai/README.md`](docs_ai/README.md)

Documentos recomendados para onboarding:

- [`docs_ai/ANDROID_FUNCIONAL.md`](docs_ai/ANDROID_FUNCIONAL.md)
- [`docs_ai/ANDROID_TECNICO.md`](docs_ai/ANDROID_TECNICO.md)
- [`docs_ai/technical/ARCHITECTURE.md`](docs_ai/technical/ARCHITECTURE.md)
- [`docs_ai/technical/SCREEN_MAP.md`](docs_ai/technical/SCREEN_MAP.md)
- [`docs_ai/technical/FEATURE_FILE_MAPS.md`](docs_ai/technical/FEATURE_FILE_MAPS.md)
- [`docs_ai/operations/RELEASE.md`](docs_ai/operations/RELEASE.md)
- [`docs_ai/operations/ci-cd.md`](docs_ai/operations/ci-cd.md)

## Roadmap resumido

- estabilização do Android;
- Firebase Beta Testing;
- Play Store — teste interno;
- preparação de listing e assets;
- lançamento Android;
- evolução do painel administrativo;
- preparação futura para iOS, sem compromisso de release imediato.

## Licença

Este repositório não publica uma licença open source neste momento. Até que um arquivo `LICENSE` seja adicionado, trate o código como proprietário e com todos os direitos reservados.
