---
title: "Arquitetura — SignallQ consumer"
description: "Visão de sistema, módulos Gradle e dependências, do código real"
type: "técnico"
status: "ativo"
owner: "Camilo"
last_updated: "2026-08-06"
---

# Arquitetura — SignallQ consumer

- **Fonte de verdade:** o código. Este documento é derivado dele, não o contrário. Os números do
  bloco de inventário abaixo são **gerados** por `scripts/gerar-inventario-docs.sh`.
- **Escopo:** app consumer Android (`io.signallq.app`) e sua relação com o backend Cloudflare.
  Não cobre SignallQ Pro (on hold — `../pro-onhold/`), Admin (`buildea-admin`) nem web
  (`signallq-web`).
- **Detalhe por módulo:** `MODULOS/` — um documento por módulo Gradle consumer.

<!-- INVENTARIO:INICIO — gerado por scripts/gerar-inventario-docs.sh, nao editar a mao -->

> **Inventário gerado do código.** Não editar manualmente — rode
> `scripts/gerar-inventario-docs.sh`. Cada número abaixo sai da fonte citada.

| Fato | Valor | Fonte |
|---|---|---|
| versionName / versionCode (consumer) | **0.31.0** / **72** | `android/gradle/libs.versions.toml` |
| proVersionName / proVersionCode | 0.3.0 / 8 | `android/gradle/libs.versions.toml` |
| compileSdk / minSdk / targetSdk | 37 / 24 / 36 | `android/gradle/libs.versions.toml` |
| Compose BOM · Room · Hilt | 2026.06.01 · 2.8.4 · 2.60.1 | `android/gradle/libs.versions.toml` |
| Módulos Gradle | **28** — 19 consumer + 9 Pro | `android/settings.gradle.kts` |
| Workers Cloudflare | 5 | `integrations/cloudflare/*/wrangler.toml` |
| Tabelas D1 | 38 — 20 admin + 18 diagnostic | `*/migrations/*.sql`, `*/schema.sql` |
| Contratos OpenAPI | 7 contratos · **122** endpoints | `docs_ai/CONTRATOS/openapi/` |
| Arquivos `.kt` em caminho legado `io/veloo` | 525 (sendo 361 em `src/main`) | dívida conhecida — higiene §4.1 |

**Módulos consumer (19):** :app :core:diagnostico :core:featureflags :core:relatorio :coreDatabase :coreDatastore :coreNetwork :corePermissions :coreRecommendation :coreTelephony :featureDevices :featureDiagnostico :featureDns :featureFibra :featureHistory :featureHome :featureSettings :featureSpeedtest :featureWifi

**Módulos Pro (9, on hold):** :pro:app :pro:core:database :pro:core:designsystem :pro:feature:ambiente :pro:feature:auth :pro:feature:cliente :pro:feature:laudo :pro:feature:medicao-diagnostico :pro:feature:visita

**Workers:**

| Diretório | `name` no wrangler |
|---|---|
| `ai-diagnosis-worker` | `linka-ai-diagnosis-worker` |
| `game-latency-probe-worker` | `signallq-game-latency-probe` |
| `signallq-admin-worker` | `signallq-admin` |
| `signallq-diagnostic-worker` | `signallq-diagnostic` |
| `signallq-privacy-worker` | `signallq-privacy` |

**Contratos:**

| Arquivo | Versão | Endpoints |
|---|---|---:|
| `ai-diagnosis-worker.yaml` | 2 | 2 |
| `game-latency-probe-worker.yaml` | 1 | 2 |
| `signallq-admin-api.yaml` | 2.1.0 | 59 |
| `signallq-analytics-events.yaml` | 1.0.0 | 5 |
| `signallq-diagnostic-worker.yaml` | 1 | 43 |
| `signallq-integrations-api.yaml` | 1.0.0 | 9 |
| `signallq-privacy-worker.yaml` | 1 | 2 |

<!-- INVENTARIO:FIM -->

---

## 1. Visão geral

App Android nativo de diagnóstico de conectividade. Mede velocidade, analisa Wi-Fi e rede móvel,
lê modem/ONT de fibra, testa DNS e interpreta tudo isso em veredito humano — por um motor
determinístico local e, opcionalmente, por um Worker de IA.

Quatro camadas:

```
┌─────────────────────────────────────────────────────────────┐
│  :app          UI (Compose), navegação, DI, composição      │
│                ↑ TODA a UI vive aqui — ver §4               │
├─────────────────────────────────────────────────────────────┤
│  :feature*     motores e vocabulário por domínio            │
│                (9 módulos — sem Composable)                 │
├─────────────────────────────────────────────────────────────┤
│  :core*        infraestrutura compartilhada                 │
│                (9 módulos — rede, banco, prefs, permissões, │
│                 telefonia, recomendação, diagnóstico,       │
│                 relatório, feature flags)                   │
└─────────────────────────────────────────────────────────────┘
                              ↕ HTTPS
┌─────────────────────────────────────────────────────────────┐
│  Cloudflare    5 Workers · 2 bancos D1                      │
└─────────────────────────────────────────────────────────────┘
```

## 2. Regras de dependência

1. `:feature*` **nunca** depende de outra `:feature*` — composição acontece em `:app` ou por
   contrato normalizado em um `core`.
2. `:core*` não depende de `:feature*`.
3. `:app` pode depender de tudo.
4. `:core:featureflags` é exclusivo do consumer — proibido para `:pro:*`.

**Duas violações da regra 1 existem hoje:**

| Violação | Onde | Uso real |
|---|---|---|
| `:featureDiagnostico` → `:featureSpeedtest` | `android/feature/diagnostico/build.gradle.kts:62` | `SignallQOrchestrator.kt` importa `ExecutorSpeedtest`, `ResultadoSpeedtest`, `ModoSpeedtest`, `SpeedtestQualityClassifier` |
| `:pro:feature:medicao-diagnostico` → `:featureSpeedtest` | `android/pro/feature/medicao-diagnostico/build.gradle.kts:69` | mesma dependência, do lado Pro (on hold) |

Não são acidentes de import: o acoplamento é profundo. O destino correto é extrair o contrato de
speedtest para um `core` — tarefa dedicada, não correção oportunista.

O contraexemplo de como fazer certo está em `:featureHome`, que precisa de dados de medição e
**não** depende de `:featureSpeedtest`: define uma struct genérica (`ResolvedorMedicaoHome`) e
empurra a adaptação para `HomeMedicaoAdapter.kt`, em `:app`.

## 3. Módulos

### `:core*` — infraestrutura (9)

| Módulo | Papel | Observação |
|---|---|---|
| `:coreNetwork` | Sondagens de rede, contratos de analytics | **Sem lib HTTP** — `HttpURLConnection`/`Socket`/`InetAddress` amarrados à `Network` sob análise. Maior e mais consumido: 9 consumidores |
| `:coreDatabase` | Room — histórico, outbox de analytics | Schema **v18**, 8 entidades, 7 DAOs, 17 migrations encadeadas |
| `:coreDatastore` | Preferências do usuário, credenciais de modem | DataStore `linkaPreferencias` |
| `:corePermissions` | Fluxo de permissões de rede | Sem testes |
| `:coreTelephony` | Rede móvel (RSRP/RSRQ/SINR) | Exige só `READ_PHONE_STATE`; não usa IMEI/IMSI |
| `:coreRecommendation` | Motor de recomendação por tags | **Único módulo fisicamente em `io/signallq/`** |
| `:core:diagnostico` | Motor canônico de diagnóstico | Compartilhado com o Pro |
| `:core:relatorio` | Paginação HTML→PDF | Compartilhado com o Pro; 194 linhas, **zero testes** |
| `:core:featureflags` | Flags remotas do consumer | 11 flags no catálogo; proibido para `:pro:*` |

Os seis primeiros são **aliases flat legados** (`:coreNetwork`) com `projectDir` remapeado para
pasta hierárquica (`core/network`). Os três últimos nasceram já hierárquicos (`:core:diagnostico`).
Renomear os legados para `:core:network` é migração dedicada — afeta CI, scripts e documentação.

### `:feature*` — domínios (9)

| Módulo | Produção | Papel |
|---|---|---|
| `:featureSpeedtest` | motor de medição | `ExecutorSpeedtestCloudflare.kt` tem **1495 linhas**, sem teste direto |
| `:featureDiagnostico` | orquestração + IA | Cliente do Worker de IA e do ingest de analytics |
| `:featureDevices` | scanner da rede local | O mais maduro: 2033 linhas de produção, 1487 de teste |
| `:featureFibra` | leitura de ONT GPON | Um único driver real: Nokia G-1425G-B |
| `:featureDns` | comparação de resolvedores | Sem ViewModel próprio — estado vai direto ao `MainViewModel` |
| `:featureHistory` | histórico e exportação | **Dois motores de PDF ativos em paralelo** |
| `:featureWifi` | vocabulário de Wi-Fi | 93 linhas; a classificação real está em `SinalScreen.kt` |
| `:featureHome` | resolução de medição da Home | 67 linhas; exemplo canônico da regra de dependência |
| `:featureSettings` | regras de ajustes | 203 linhas, zero dependências, zero UI — **não é feature**, é biblioteca de regras puras; destino natural é um `core` |

## 4. A inconsistência principal: UI fora das features

**Nenhum dos 9 módulos `:feature*` contém um único `@Composable`.** Toda a interface vive em
`android/app/src/main/kotlin/io/veloo/app/kotlin/ui/screen/`.

Consequência direta: as features viraram bibliotecas de motor e vocabulário, e `:app` concentra
40.017 linhas em 150 arquivos, com dez acima de 800 linhas:

| Arquivo | Linhas |
|---|---:|
| `SinalScreen.kt` | 3383 |
| `HomeScreen.kt` | 2967 |
| `MainViewModel.kt` | 2438 |
| `DispositivosScreen.kt` | 1380 |
| `DnsScreen.kt` | 815 |

Isso puxa efeitos concretos: `:featureWifi` tem 93 linhas porque a classificação de redes é montada
dentro de `SinalScreen.kt` (que chega a construir `RedeClassificada(...)` inline na linha 1078); e
`:featureDns` não tem ViewModel porque o `MainViewModel` monolítico assume o estado.

O destino arquitetural é mover cada tela para o módulo da sua feature. É migração dedicada, por
tela, com teste de caracterização antes — ver `.claude/rules/higiene-e-padronizacao-repositorio.md`
§4 para o registro de cada arquivo crítico.

## 5. Fluxo de dados

**Medição:** `:app` dispara → `:featureSpeedtest` executa → resultado classificado por
`:core:diagnostico` → persistido por `:coreDatabase` → recomendação por `:coreRecommendation` →
renderizado por `:app`.

**Diagnóstico com IA:** `:featureDiagnostico` monta payload → `POST` ao `ai-diagnosis-worker` →
resposta v2 → fallback local determinístico em qualquer falha (sem auth, timeout, não-2xx, JSON
inválido).

**Analytics:** cada evento vai **simultaneamente** ao Firebase e a uma outbox Room local; um
processador com backoff drena a outbox para `POST /ingest/analytics` no `signallq-admin-worker`,
que grava em D1. Não há Cloudflare Queue — a ingestão é HTTP síncrono direto para D1.

## 6. Backend Cloudflare

5 Workers. Dois têm banco D1 próprio: `signallq-admin` (20 tabelas) e `signallq-diagnostic`
(18 tabelas). O nome declarado no `wrangler.toml` difere do nome do diretório em **todos os cinco**
— conferir a tabela do inventário acima antes de fazer deploy.

Contratos em `../CONTRATOS/openapi/`.

## 7. Riscos arquiteturais

| Risco | Evidência | Efeito |
|---|---|---|
| UI monolítica em `:app` | 5 arquivos acima de 800 linhas, `SinalScreen.kt` com 3383 | Features anêmicas; mudança visual exige tocar arquivo gigante |
| Feature→feature | 2 violações confirmadas (§2) | Grafo de dependências deixa de ser acíclico por camada |
| Caminho físico legado `io/veloo` | Todos os módulos exceto `:coreRecommendation` | Duas árvores concorrentes; `:featureDiagnostico` já tem as duas no mesmo source set |
| Três mecanismos de feature flag | `:core:featureflags` + `FeatureFlagProvider` legado em `:coreNetwork` + Firebase Remote Config | Colisão de nome e ambiguidade sobre qual vence |
| Dois motores de PDF | `:featureHistory` usa `PdfDocument` e HTML→WebView via `:core:relatorio` | Manutenção dupla |
| Versão de dependência fora do catálogo | `:featureDevices` fixa `okhttp:5.4.0` no build | Pode divergir do `libs.okhttp` dos demais |
| Schema Room sem `15.json` | `core/database/schemas/` tem 10–14, 16, 17, 18 | Migration 14→15 não verificável por diff de schema |
| `:core:diagnostico` não é Kotlin puro | `build.gradle.kts` declara "zero `android.*`", mas `topology/` faz HTTP e `Runtime.exec("/system/bin/ping")` | Contrato do módulo não corresponde ao conteúdo |
| Credencial de modem em claro | `CredenciaisModemStore` cai para `SharedPreferences` sem cifra em `catch (_: Exception)` genérico quando o AndroidKeyStore falha | Exposição de senha de roteador — ver `../TECNICO.md` §6 |

## 8. Decisões arquiteturais relacionadas

`ADR-003` DispatcherProvider por injeção · `ADR-004` estrutura multi-módulo · `ADR-008` features
novas D1-only · `ADR-011` motor canônico de diagnóstico, fase 0 · `ADR-012` `executionId`/
`rulesVersion` · `ADR-013` unificação de latência/perda/upload. Todos em `../decisions/`.
