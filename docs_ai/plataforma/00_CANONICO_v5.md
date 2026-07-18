# SignallQ Platform — Dicionário Canônico e Reconciliações (v5)

- **Status:** ativo · fonte única de nomes e decisões transversais
- **Versão:** 5.0 · **Data:** 17/07/2026
- **Escopo:** vale para TODOS os documentos v5. Em caso de divergência entre qualquer documento e este, **este prevalece**.
- **Porquê existe:** o pacote v1→v4 divergiu internamente (eventos em dois estilos, dois dicionários D1, três árvores de monorepo, cinco grafias de Nethal, paleta com token morto). O v5 fixa uma escolha única aqui e todos os documentos passam a apontar para ela.

> **Leia junto:** `00_CHANGELOG_e_Validacao_Cruzada_v5.md` explica cada reconciliação e a validação contra o código real.

---

## 1. Estado atual vs. Alvo (a regra que faltava no v4)

Todo o pacote v1→v4 era "baseline proposta" sem separar o que existe do que é desejo. No v5, **cada documento marca item a item** usando estes dois rótulos, com os fatos abaixo (validados contra o código do repo `gmmattey/linka-android` em 17/07/2026):

| Componente | Estado | Fato verificado |
|---|---|---|
| **SignallQ (android-consumer)** | ✅ **ATUAL** | Repo `gmmattey/linka-android`. versionName **0.26.0**, versionCode **62**. 16 módulos Gradle (app + 6 core + 9 features). Kotlin/Compose/M3/Hilt/Room/DataStore/WorkManager/Firebase. Package `io.signallq.app`. |
| **SignallQ Admin (admin-web)** | ✅ **ATUAL** | Diretório `SignallQ Admin/` dentro do monorepo. React **19.0.1** + Vite 6 + TS 5.8 + Tailwind 4 + Lucide + Motion + Recharts + Vitest. Pacote `signallq-admin` v0.1.0. |
| **Workers Cloudflare** | ✅ **ATUAL (5 workers)** | `ai-diagnosis-worker`, `signallq-admin-worker`, `signallq-diagnostic-worker`, `signallq-privacy-worker`, `game-latency-probe-worker`. **A doc v4 só citava 2.** |
| **D1 do Admin** | ✅ **ATUAL (13 tabelas)** | `diagnostic_sessions`, `ai_usage`, `admin_settings`, `system_errors`, `admin_users`, `admin_sessions`, `auth_rate_limit`, `alerts`, `feature_flags`, `feature_flag_audit`, `analytics_events`, `play_console_tracks`, `system_health_snapshots`. |
| **Telemetria** | ✅ **ATUAL: Firebase** | Firebase Analytics + Crashlytics. Eventos reais: `feature_used`, `screen_view`, `app_session_start`, `feature_crash`, `battery_snapshot`. **Sem** Realtime DB. **Sem** pipeline Cloudflare Queue→D1. |
| **Tokens de marca** | ✅ **ATUAL** | Consumer primary `#5B21D6`, secondary `#2851B8` (confirmado em `SignallQTheme.kt` e na skill `SignallQ-design`). |
| **SignallQ Pro (android-pro)** | 🎯 **ALVO** | Não existe. App novo, `io.signallq.pro`, Firebase/Play separados. |
| **Portal (portal-web)** | 🎯 **ALVO** | Não existe. Funde `linka-webapp` + `linka-speedtest`. |
| **Monorepo `signallq-platform`** | 🎯 **ALVO** | Não existe. Hoje o código vive em `linka-android` (+ repos separados). |
| **Modelo de dados D1 "Pro"** | 🎯 **ALVO (greenfield)** | Nenhuma das tabelas Pro existe hoje. 100% novo sobre o D1 do Admin. |
| **Pipeline de telemetria Cloudflare** | 🎯 **ALVO** | Telemetry Worker → Queue → D1/Analytics Engine é proposta. |
| **SignallQ Nethal** | 🎯 **ALVO** | Hoje é repo separado `gmmattey/nethal`; ainda sem driver de produção estável. |

**Identificadores técnicos a nunca renomear** (parecem marca, são técnicos): `io.signallq.app`, repo `gmmattey/linka-android`, DB `linkaKotlin.db`, DataStore `linkaPreferencias`, canais `linka_*`. O nome-alvo `signallq-platform` é do monorepo futuro, não substitui esses identificadores.

---

## 2. Glossário canônico de nomes (fim das divergências)

Cada linha fixa **um** nome. Onde o v1→v4 usava outro, está listado em "Não usar".

| Conceito | Termo de domínio / UI | Classe (Kotlin/TS) | Tabela D1 | Prefixo de evento | Não usar |
|---|---|---|---|---|---|
| Cliente do técnico | Cliente | `Customer` | `customer` | `customer.*` | `client`, `client.created` |
| Local do cliente | Local | `Location` | `service_location` | `location.*` | `locations` (plural) |
| Cômodo/zona medida | Ambiente | `Environment` | `environment` | `environment.*` | `room`, `room.measured`, `environments` (plural) |
| Conta de acesso | Conta | `Account` | `account` | `auth.*`/`account.*` | `users`, `User` como tabela |
| Identidade vinculada | Identidade | `IdentityProvider` | `identity_provider` | — | `user_identities` |
| Sessão/refresh | Sessão | `Session` | `session` | — | `refresh_sessions` |
| Medição (agrupador) | Medição | `MeasurementSession` | `measurement_session` | `measurement.*` | `measurements` como única tabela |
| Amostra de medição | Ponto de medição | `MeasurementPoint` | `measurement_point` | — | — |
| Cobrança Pix | Cobrança | `PixCharge` | `pix_charge` | `pix_charge.*` | (omissão — ver C5 no changelog) |
| Perfil Pix | Perfil Pix | `PixProfile` | `pix_profile` | — | — |
| Ferramenta de hardware | SignallQ Nethal | — | (domínio Nethal) | `device.*`/`driver.*`/`command.*` | `NetHAL`, `NetHAL Lab`, `nethal-lab`, `nethal-lab-internal` |
| App web público | Portal | — | — | `web_speedtest.*` | `web-consumer` |
| App Nethal no monorepo | `apps/signallq-nethal` | — | — | source `signallq_nethal` | `apps/nethal-lab*` |
| Armazenamento de arquivos | StorageProvider | `StorageProvider` | `storage_object` (metadado) | `storage.*`/`upload.*` | R2 como storage padrão (é add-on pago) |

---

## 3. Catálogo canônico de eventos de telemetria

**Convenção única: `dot.case`** (confirmada pelo envelope da Telemetria v4: `"event_name": "visit.completed"`). O Funcional v4 usava `snake_case` flat — no v5 todos os documentos usam `dot.case`. Nomes de propriedades continuam `snake_case`.

### 3.1 Catálogo-alvo (plataforma completa, pipeline Cloudflare)

| Domínio | Eventos canônicos |
|---|---|
| Identidade | `auth.started`, `auth.succeeded`, `auth.failed`, `account.deleted`, `account.identity_linked` |
| Ativação Pro | `profile.completed`, `pix.configured` |
| Speedtest | `speedtest.started`, `speedtest.phase_completed`, `speedtest.completed`, `speedtest.failed` |
| Diagnóstico | `diagnosis.requested`, `diagnosis.completed`, `recommendation.viewed`, `recommendation.opened`, `feedback.sent` |
| CRM/Visita (Pro) | `customer.created`, `location.created`, `appointment.created`, `visit.started`, `environment.measured`, `evidence.added`, `visit.completed` |
| Entrega (Pro) | `comparison.viewed`, `report.generated`, `report.shared` |
| Assinatura/Receita | `paywall.viewed`, `trial.started`, `purchase.started`, `purchase.completed`, `subscription.activated`, `entitlement.changed`, `pix_charge.created`, `payment.confirmed` |
| Storage | `storage.connected`, `upload.started`, `upload.completed`, `upload.failed`, `sync.conflict` |
| Retenção | `visit.completed_7d`, `visit.completed_30d`, `customer.returned` |
| Qualidade | `sync.failed`, `report.failed`, `measurement.failed`, `feature.crash` |
| Admin | `admin.login`, `admin.action`, `config.changed`, `flag.updated`, `sensitive_record.viewed`, `export.requested`, `export.generated` |
| Nethal | `device.detected`, `fingerprint.completed`, `driver.selected`, `driver.executed`, `command.executed`, `command.blocked`, `capability.failed` |
| Portal | `web_speedtest.completed`, `app_download.clicked`, `ad_slot.viewable` |
| Backend | `request.failed`, `queue.retry`, `sync.conflict` |

**IDs de fonte (`source`):** `android_consumer`, `android_pro`, `portal_web`, `admin_web`, `signallq_nethal`, `worker`.

### 3.2 Mapa de migração snake→dot (o que o Funcional v4 usava → canônico)

`signup_started`→`auth.started` · `signup_completed`→`auth.succeeded` · `profile_completed`→`profile.completed` · `pix_configured`→`pix.configured` · `customer_created`→`customer.created` · `appointment_created`→`appointment.created` · `visit_started`→`visit.started` · `environment_measured`→`environment.measured` · `comparison_viewed`→`comparison.viewed` · `report_generated`→`report.generated` · `report_shared`→`report.shared` · `paywall_viewed`→`paywall.viewed` · `trial_started`→`trial.started` · `subscription_activated`→`subscription.activated` · `pix_charge_created`→`pix_charge.created` · `payment_confirmed`→`payment.confirmed` · `sync_failed`→`sync.failed` · `report_failed`→`report.failed` · `measurement_failed`→`measurement.failed` · `feature_crash`→`feature.crash` · `visit_completed_7d`→`visit.completed_7d` · `visit_completed_30d`→`visit.completed_30d` · `customer_returned`→`customer.returned`.

### 3.3 Telemetria ATUAL (consumer, Firebase) — não confundir com o alvo

O app consumidor **hoje** dispara via Firebase Analytics: `feature_used`, `screen_view`, `app_session_start`, `feature_crash`, `battery_snapshot`. Estes são `snake_case` porque seguem a convenção do Firebase. A migração para o catálogo-alvo `dot.case` sobre pipeline Cloudflare é trabalho de plataforma (marcado 🎯 ALVO), com janela de convivência dos dois esquemas.

---

## 4. Árvore canônica do monorepo `signallq-platform`

Uma única árvore. Substitui as três divergentes (Arquitetura Android v1, Especificação Técnica v1, Platform Arquitetura v4).

```
signallq-platform/
├── apps/
│   ├── android-consumer/     # SignallQ  (ATUAL: gmmattey/linka-android)
│   ├── android-pro/          # SignallQ Pro  (ALVO)
│   ├── portal-web/           # Portal público  (ALVO: funde linka-webapp + linka-speedtest)
│   ├── admin-web/            # SignallQ Admin  (ATUAL: "SignallQ Admin/")
│   └── signallq-nethal/      # Lab interno  (ALVO: hoje repo gmmattey/nethal)
├── mobile/
│   ├── core/{designsystem, network, database, preferences, permissions, files, sync, analytics, telephony}
│   └── features/{auth, customers, appointments, visits, environments, measurements, evidence,
│                 reports, payments, history, settings, speedtest, wifi, devices, dns, fibra, diagnostico}
├── domain/
│   └── {customer, appointment, visit, diagnostics, reporting, billing, entitlement}
├── network-hardware/
│   └── {core, engine, security, catalog, drivers}
├── backend/
│   ├── workers/{auth, sync, billing, report, admin, telemetry, diagnosis, privacy, game-latency}
│   ├── d1/{migrations, seeds}
│   ├── queues/
│   └── contracts/
├── web/
│   └── packages/{ui, diagnostics, speedtest, reporting}
├── packages/
│   └── {telemetry-schema, api-contracts, shared-types}
├── docs/
│   └── {product, technical, architecture, modules, api, qa, decisions, archive}
└── tooling/
    └── {gradle, scripts, github-actions}
```

**Regras de nome de módulo:** hierárquico com `/` (`mobile/core/network`), nunca concatenado (`core-network`). Alias Gradle-alvo `:mobile:core:network`, `:mobile:features:speedtest` etc.

**Mapa dos 5 workers reais → `backend/workers/`:** `signallq-admin-worker`→`admin`; `ai-diagnosis-worker` + `signallq-diagnostic-worker`→`diagnosis` (consolidar); `signallq-privacy-worker`→`privacy`; `game-latency-probe-worker`→`game-latency`. Workers-alvo adicionais: `auth`, `sync`, `billing`, `report`, `telemetry`.

**Regras de dependência (inalteradas do v4, reafirmadas):** `android-consumer` e `android-pro` nunca dependem um do outro; `domain` não depende de Android/Firebase/Cloudflare/UI; features nunca dependem de outra feature; drivers não importam telas/ViewModels; design system sem regra de negócio.

---

## 5. Paleta canônica (duas identidades, mesma fundação)

Consumer e Pro compartilham fundações, tipografia e componentes de medição; a **matiz de marca diverge de propósito**.

### 5.1 SignallQ (consumer) — ATUAL, do código
- **Primary `#5B21D6`** (violeta) · **Secondary `#2851B8`** (azul, FIXO, não deriva do primary)
- Dark: primary `#D0BCFF`, secondary `#AAC7FF`
- Material 3 estrito, Google Sans Flex, grid 8dp. Fonte: `SignallQTheme.kt` + skill `SignallQ-design`.
- **`#6C2BFF` está MORTO** (era o primary do MD3 estrito de 11/07, substituído em 13/07). Nenhum documento v5 pode citá-lo como token vivo.

### 5.2 SignallQ Pro — identidade azul (fonte: projeto Claude Design `77a19317`, snapshot 2026-07-18)

> **A fonte da verdade visual do Pro é o projeto online `77a19317` — ele evolui.** A identidade mudou de teal-dominante (Design System v3) para **azul-dominante** em 2026-07-18. Os valores abaixo são snapshot dessa data; ao desenhar, reler `foundations/tokens.html`, `status-and-charts.html` e `dark-mode.html` no projeto.

| Papel M3 (tema claro) | Valor | Uso |
|---|---|---|
| Primary — marca | `#0B6CFF` (azul) | CTAs, ações primárias, identidade |
| Primary Container | `#D8E7FF` | Seleção, destaque suave |
| Secondary — ciano técnico | `#006B76` | Apoio: chips, destaques secundários |
| Secondary Container | `#A9EDF3` | Superfícies informativas |
| Tertiary — roxo de apoio | `#6558E8` | Apoio: gráficos, realces (nunca compete com o azul) |
| Tertiary Container | `#E5DEFF` | — |
| Success/Good | `#1AA25A` | Medição aprovada / melhora |
| Warning/Attention | `#E9AD27` | Risco ou recomendação |
| Error/Critical | `#D9363E` | Falha ou bloqueio |
| Background / Surface | `#F7F9FC` / `#FFFFFF` | Fundos e cards |
| Outline / Divider | `#C4CBD5` / `#E3E7EC` | Contornos e divisores |

**Escala de sinal — 6 níveis (`--sqp-status-*`):** Excelente `#16A85A` · Bom `#1AA25A` · Atenção `#E9AD27` · Fraco `#ED7D2D` · Crítico `#D9363E` · Informação `#0B6CFF` (variantes de tema escuro no projeto). **Gráficos (`--sqp-chart-*`):** download/upload/latência/jitter/perda + grid/reference.

**Dois temas oficiais** (claro e escuro), mesma estrutura, token-driven via `data-theme`; laudo técnico sempre claro. Cor nunca é o único sinal de estado (sempre ícone + rótulo + valor).

**Mortos:** `#6C2BFF` (primary antigo do consumer) e a paleta **teal-dominante** anterior do Pro (`#006B73` como primary — agora rebaixado a Secondary `#006B76`). O antigo "elo violeta `#5B21D6`" foi substituído pelo roxo próprio do Pro `#6558E8`.

---

## 6. Storage canônico

**Local-first + Android SAF (pasta do próprio técnico) no MVP; StorageProvider abstrai tudo.** R2 é apenas **add-on hospedado pago**, nunca o storage padrão. Qualquer documento que ainda diga "R2 com URLs assinadas" como storage principal (Roadmap, Arquitetura Android v3) está desatualizado e é corrigido no v5.

Providers: `DeviceLocalProvider`, `AndroidSafProvider` (1ª opção nuvem própria), `GoogleDriveProvider`/`OneDriveProvider`/`DropboxProvider` (OAuth, futuro), `S3CompatibleProvider` (avançado), `SignallQHostedProvider` (R2 hospedado, pago).

---

## 7. Identidade de produto e release

| Produto | applicationId / destino | Firebase/Play | Tag de release |
|---|---|---|---|
| SignallQ | `io.signallq.app` | projeto `signallq-app` | `consumer/android/vX.Y.Z` |
| SignallQ Pro | `io.signallq.pro` | projeto/Play separados | `pro/android/vX.Y.Z` |
| Portal | Cloudflare Pages, domínio-alvo `signallq.app` (a adquirir) | — | `portal-web/vX.Y.Z` |
| Admin | Cloudflare Pages / ambiente protegido | — | `admin-web/vX.Y.Z` |
| Nethal | APK interno / Firebase App Distribution | — | `signallq-nethal/vX.Y.Z` |
| Workers | Cloudflare Workers | — | `worker-<nome>/vX.Y.Z` |

`versionName` fica em `0.x.y` enquanto em trilha de teste; `1.0.0` reservado ao primeiro publish em `production`.

---

## 8. Decisões pendentes (herdadas do v4, ainda abertas — o v5 as torna visíveis, não as inventa)

1. **Preço do Pro** — mensal/anual sem valor definido em nenhum documento. Bloqueia o gate de monetização do Roadmap.
2. **Domínio do portal** — `signallq.app` "sujeito a aquisição".
3. **Provedor de identidade e recuperação de conta.**
4. **Conectores de nuvem** além do Android SAF.
5. **Política de retenção por tipo de dado e plano.**
6. **ADR-001..008** citados mas inexistentes — o v5 abre os stubs em `docs/decisions/`.
7. **Contratos OpenAPI** referenciados (`packages/api-contracts`) mas inexistentes — v5 define a estrutura, não os contratos completos.
