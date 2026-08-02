# CI/CD Pipeline — SignallQ

- **Status:** ativo
- **Última validação:** 2026-07-23
- **Fonte de verdade:** `.github/workflows/*.yml`
- **Escopo:** CI (testes/lint/build) e CD (release/deploy) automatizados via GitHub Actions

Documentação do pipeline de integração contínua e deploy automatizado para SignallQ.

## Overview

O projeto tem **9 workflows** em `.github/workflows/` (1 desativado):

| Workflow | Função |
|---|---|
| `android-ci.yml` | CI — testes, lint, análise e build do app Android (detalhado abaixo) |
| `firebase-distribution.yml` | Deploy sob demanda (`workflow_dispatch`) para Firebase App Distribution |
| `release.yml` | Release oficial — dispara em tag `vX.Y.Z`, publica AAB na trilha `internal` da Play Console |
| `promote-release.yml` | Promove o mesmo AAB de `internal` → `alpha` (`workflow_dispatch`), guardrail bloqueia `beta`/`production` |
| `auto-move-board.yml` | Safety net — move card do Project quando PR/issue muda fora do controle dos agentes locais |
| `auto-update-branch.yml` | Mantém PRs abertas atualizadas com `main` automaticamente |
| `site-ci.yml` | CI do SignallQ Site (React/Vite/TS) |
| `site-deploy.yml` | Deploy do SignallQ Site para Cloudflare Pages (`signallq.pages.dev`) |
| `pages-deploy.yml.disabled` | Desativado — não roda (sufixo `.disabled`) |

Este documento detalha só o `android-ci.yml`. Para release/deploy Android ver
`docs_ai/operations/RELEASE.md` e `DEPLOY.md`.

## Android CI — `android-ci.yml`

### Triggers

Disparado automaticamente em `push`/`pull_request` contra `main`.

Workflow só roda se houve mudança em `android/` ou no próprio workflow.

### Jobs

#### 1. Unit Tests
- Timeout: 30 minutos
- Roda `./gradlew test` em todos os módulos Android
- Outputs: Relatório de testes em `android/**/build/reports/tests/`
- Artefato: `unit-test-reports`

#### 2. Ktlint Check
- Timeout: 15 minutos
- Verifica formatação e estilo Kotlin
- Falha em desvios do padrão de estilo

#### 3. Detekt Analysis
- Timeout: 20 minutos
- Análise de complexidade, bugs potenciais e anti-patterns
- Falha em violações críticas de qualidade

#### 4. Build Debug APK
- Timeout: 30 minutos
- Compila APK debug para validar compilação
- NÃO roda `assembleRelease` pois não há acesso às signing keys em CI
- Outputs: APK em `android/app/build/outputs/apk/debug/`
- Artefato: `debug-apk`

### Gradle Cache

Todos os jobs usam cache gradle para accelerar builds. Cache é automático entre runs na mesma branch.

### JDK

Versão fixa: **JDK 17** (Temurin).

## Histórico de Runs

Acessar em https://github.com/7ALabs/SignallQ/actions

Artefatos disponibilizados por 30 dias.

## Interpretando Falhas

### Unit Tests falham

Possíveis causas:

1. **Teste quebrado** — código novo não passou nos testes existentes
   - Solução: revisar o diff e corrigir lógica ou teste
   
2. **Dependência de teste ausente**
   - Solução: verificar `build.gradle.kts` do módulo
   
3. **Flakiness** — teste passa/falha aleatoriamente
   - Solução: investigar concorrência, timeouts ou estado compartilhado

### Ktlint falha

Solução automática:

```bash
cd android && ./gradlew ktlintFormat
```

Depois commit.

### Detekt falha

Revisar arquivo flagged, considerar refatoração ou suprimir se falso positivo:

```kotlin
@Suppress("ComplexMethod")
fun complexFunction() { ... }
```

### Build Debug falha

Causas comuns:

- **Erro de compilação Kotlin** — import faltando, tipo incorreto
- **Recurso não encontrado** — arquivo XML ou imagem deletado
- **Dependência duplicada** — conflito de versões

Solução: rodar localmente `./gradlew clean assembleDebug`.

## Adicionando Novos Checks

### Android

1. Adicione a dependência no módulo `build.gradle.kts`
2. Configure em `.gradle/` ou no próprio `build.gradle.kts`
3. Adicione novo job ao `android-ci.yml`
4. Commit e teste em branch

## Troubleshooting

### Cache Gradle corrompido

Solução: limpar cache em Settings → Actions → Clear all caches.

### Node ou JDK versão errada

Verificar versões em CI vs local. Se diferenças, atualizar `.github/workflows/*.yml`.

### Artefatos não aparecem

Se o job passou e nenhum arquivo foi gerado, o upload silenciosamente ignora com `if-no-files-found: ignore`.

Para debug, rodar localmente e verificar output.

## Performance

| Job | Tempo esperado |
|---|---|
| Unit Tests | 8-12 min |
| Ktlint | 2-3 min |
| Detekt | 3-5 min |
| Build Debug | 5-10 min |

Total por run: ~20-35 minutos.

## Próximos Passos

- E2E / UI tests em emulador
- Performance profiling automatizado
- Upload de resultados para dashboard externo
- Notificação automática em Slack/Discord via GitHub App (parcialmente coberto por
  `scripts/discord_notify.sh`/`slack_notify.sh`, chamados fora do CI hoje)

Release workflow (`release.yml`) e promoção de trilha (`promote-release.yml`) **já existem** —
removido da lista de pendências.

