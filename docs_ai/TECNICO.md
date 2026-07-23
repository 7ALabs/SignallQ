# Documentação Técnica — SignallQ Android

- **Status:** ativo
- **Última validação:** 2026-07-23
- **Fonte de verdade:** este arquivo — consolida `ANDROID_TECNICO.md`, `technical/BUILD_SYSTEM.md`,
  `technical/CLOUDFLARE.md`, `technical/STORAGE.md`, `technical/DATA_FLOW.md`, `technical/API_MAP.md`,
  `technical/MODULES.md`, `technical/SERVICES.md`, `technical/DEPENDENCY_AUDIT.md`,
  `technical/analytics-events.md`, `technical/analytics-events-schema.md`,
  `technical/THIRD_PARTY_LICENSES.md` e a parte técnica de `functional/FEATURE_FLAGS.md`. Os
  arquivos de origem devem ser movidos para `docs_ai/_archive/` com data e referência a este
  documento — não duplicar conteúdo técnico fora daqui.
- **Escopo:** stack, build, persistência, integrações Cloudflare, analytics, dependências,
  segurança e processo de release do app Android SignallQ **consumer** (`io.signallq.app`). Não
  cobre SignallQ Pro (`android/pro/`, `io.signallq.pro`), Admin ou Site. Detalhe por módulo Gradle
  vive em `docs_ai/ARQUITETURA/MODULOS/*.md` — este documento cobre visão geral de stack, não
  repete responsabilidade módulo a módulo. Contratos de API/endpoints detalhados vivem em
  `docs_ai/CONTRATOS/openapi/`, referenciados, não repetidos.
- **Responsável:** Camilo (implementação Android/Admin/Cloudflare), Rhodolfo (manutenção da doc)
- **Para funcionalidades da perspectiva do usuário:** ver `docs_ai/FUNCIONAL.md`.

> Segue o template de **Especificação Técnica**
> (`.claude/rules/higiene-e-padronizacao-repositorio.md`, seção 10): Objetivo técnico → Visão geral
> da solução → Modelo de dados → APIs/Endpoints → Integrações e dependências → Segurança e
> privacidade → Performance e escalabilidade → Rollout e observabilidade → Riscos técnicos. O
> conteúdo pré-existente (identidade, stack, módulos, dependências) é mantido e reorganizado dentro
> dessas seções — não foi descartado.

---

## 1. Objetivo técnico

Documentar, com código real como fonte, a stack, arquitetura, persistência, integrações Cloudflare,
segurança, performance e processo de release do app Android SignallQ consumer — para que qualquer
agente ou desenvolvedor entenda como o app é construído e integrado sem precisar reler o código
inteiro, e para servir de checagem factual contra desatualização de doc.

---

## 2. Visão geral da solução

### 2.1 Identidade e Versão

| Campo | Valor |
|---|---|
| App | SignallQ — diagnóstico de conectividade Android |
| Estrutura | Monorepo — `android/` (Kotlin), `integrations/` (Cloudflare), `packages/`, `scripts/`, `docs_ai/` |
| Package / applicationId / namespace | `io.signallq.app` — **identificador técnico, nunca renomear** (quebra Firebase/assinatura). Renomeado de `io.veloo.app` em 2026-06-28, antes de qualquer publicação |
| Marca | Linka → Veloo → **SignallQ** (rebrand em 0.16.0) |
| versionName | **0.30.1** |
| versionCode | **67** |
| Fonte da versão | `android/gradle/libs.versions.toml` |

> Correção (2026-07-23): a revisão anterior deste documento (2026-07-16) já havia corrigido o
> valor de `0.23.0`/56 (histórico, citado em docs arquivados) para `0.25.0`/60. Reconfirmado nesta
> revisão contra `libs.versions.toml`: o valor real atual é `0.30.1`/`67` — a versão sobe a cada
> release, sempre confirmar diretamente no arquivo, nunca fixar de memória.

**Caminho físico legado:** ~460 arquivos `.kt` ainda residem em
`android/<módulo>/src/.../kotlin/io/veloo/app/kotlin/...` apesar de declararem
`package io.signallq.app...`. Divergência confirmada e documentada como dívida estrutural em
`.claude/rules/higiene-e-padronizacao-repositorio.md` (seção 4.1) — migração é tarefa dedicada,
fora do escopo deste documento.

---

### 2.2 Stack

| Tecnologia | Versão | Função |
|---|---|---|
| Kotlin | 2.2.20 | Linguagem principal |
| Jetpack Compose BOM | 2025.05.01 | UI declarativa |
| Material Design 3 | — | Sistema de design |
| Room | 2.8.4 | Persistência local (SQLite) |
| DataStore Preferences | 1.1.1 | Preferências do usuário |
| Kotlin Coroutines | 1.9.0 | Operações assíncronas |
| OkHttp | 4.12.0 | HTTP (speedtest, IA, fibra) |
| Hilt / Dagger | 2.58 | Injeção de dependência |
| Timber | 5.0.1 | Logging |
| Coil | 3.1.0 | Carregamento de imagens |
| WorkManager | 2.11.2 | Background tasks |
| Navigation Compose | 2.8.0 | Navegação |
| Android Gradle Plugin | 8.11.1 | Build system |
| Firebase BOM | 34.15.0 | Analytics, Crashlytics |

**Injeção de dependência:** Hilt via `di/AppModule.kt` (`@Module @InstallIn(SingletonComponent::class)`).
`@HiltAndroidApp` em `SignallQApplication`. Módulos `*Modulo.kt` seguem existindo como fábricas
estáticas usadas internamente pelos Hilt modules.

**Arquitetura:** MVVM + StateFlow. Fluxo unidirecional: evento UI → função no ViewModel → atualiza
`StateFlow` → recomposição. `stateIn(viewModelScope, WhileSubscribed(5000), initial)` em todos os
flows expostos; `collectAsStateWithLifecycle()` nas telas (nunca `collectAsState()`).

---

### 2.3 Config Android (SDKs, build types)

| Parâmetro | Valor |
|---|---|
| compileSdk | **37** |
| minSdk | 24 (Android 7.0 Nougat) |
| targetSdk | **36** |
| JVM target | 17 |
| rootProject.name | `linkaAndroidKotlin` |
| Chaves de assinatura | `key.properties` (gitignored), template em `key.properties.template` |
| Desugaring | habilitado — suporte a APIs Java 8+ em minSdk 24 |

**Plugins do módulo `:app`:** `com.android.application 8.11.1`, `kotlin.android 2.2.20`,
`kotlin.plugin.compose 2.2.20`, `hilt 2.58`, `kapt`, `detekt 1.23.7`, `ktlint 12.1.1`,
`firebase-crashlytics`.

**Build types:**
- `debug` — todas as feature flags de compilação ativas.
- `release` — apenas flags MVP ativas (ver seção 5.2).

> Correção: `BUILD_SYSTEM.md` estava travado em `0.16.0`/versionCode `46`/compileSdk `36` — os
> valores reais atuais são os da tabela acima.

### 2.4 Módulos Gradle

**16 módulos consumer** declarados em `android/settings.gradle.kts` (confirmado por leitura direta
do arquivo): `:app` + 6 `core*` (`coreNetwork`, `corePermissions`, `coreDatabase`, `coreDatastore`,
`coreTelephony`, `coreRecommendation`) + 9 `feature*` (`featureHome`, `featureWifi`, `featureDevices`,
`featureDns`, `featureSpeedtest`, `featureDiagnostico`, `featureFibra`, `featureHistory`,
`featureSettings`).

> Correção histórica: `MODULES.md` e `ANDROID_TECNICO.md` (docs arquivados) afirmavam **15**
> módulos, alegando que o `.claude/CLAUDE.md` estava errado ao citar 16. É o inverso — o
> `.claude/CLAUDE.md` estava certo. O módulo omitido nesses dois documentos era
> `:coreRecommendation` (issue #790, engine de recomendação desacoplada do motor de diagnóstico —
> **já integrada à UI** via `RecommendationCard`/`RecommendationEngineCard` em
> `ResultadoVelocidadeScreen.kt` (GH#813), ainda não integrada a monetização real (AdMob/afiliados).
> Não confundir com o `RecommendationEngine` de `:featureDiagnostico`, que gera as 14 regras
> (REC-01..REC-14) de dicas práticas do diagnóstico local, sem monetização/catálogo.

> **Nota de escopo (2026-07-23):** `settings.gradle.kts` também declara módulos do **SignallQ Pro**
> (`:pro:app`, `:core:relatorio`, `:core:diagnostico`, `:pro:core:designsystem`,
> `:pro:core:database`, `:pro:feature:auth`, `:pro:feature:cliente`, `:pro:feature:visita`,
> `:pro:feature:ambiente`, `:pro:feature:medicao-diagnostico`, `:pro:feature:laudo`) — produto
> distinto (`io.signallq.pro`), fora do escopo deste documento. Os 16 módulos acima são
> especificamente do app consumer.

Detalhe de responsabilidade por módulo, namespace e dependências não é repetido aqui — ver
`docs_ai/ARQUITETURA/MODULOS/*.md`.

---

## 3. Modelo de dados

### 3.1 Room — `SignallQDatabase`

**Módulo:** `:coreDatabase`. **Versão atual do schema: v14** (confirmado em
`android/core/database/schemas/` — migration 13→14 em GH#1027 adiciona coluna `bandaWifi` para
capturar banda Wi-Fi durante medição; reconfirmado nesta revisão, sem migration nova desde então).

**Histórico dos 3 nomes de banco** (as três pastas de schema coexistem em
`android/core/database/schemas/`, refletindo o histórico de rebrand Linka → Veloo → SignallQ):

| Pasta de schema | Package declarado | Versão mais alta | Status |
|---|---|---|---|
| `io.linka.app.kotlin.core.database.LinkaDatabase` | pacote antigo `io.linka.app.kotlin` | v10 | **Legado** — schema da era Linka, congelado |
| `io.signallq.app.core.database.VelooDatabase` | já `io.signallq.app`, classe ainda `VelooDatabase` | v10 (única versão) | **Residual** — não recebeu migration própria; classe renomeada de novo antes de v11 nascer |
| `io.signallq.app.core.database.SignallQDatabase` | `io.signallq.app` | **v14** | **Atual** — única classe `RoomDatabase` presente no `.kt` atual |

`LinkaDatabase`/`VelooDatabase` não têm arquivo `.kt` correspondente — existem só como histórico de
schema JSON gerado pelo Room em builds anteriores ao rebrand (comportamento normal de
`exportSchema`, preservado para validar migrations históricas). Não consolidar/apagar essas pastas.

**Entidades (banco atual):** `MedicaoEntity` (tabela `medicao`), `ApelidoDispositivoEntity`
(`apelido_dispositivo`), `ChatSessionEntity` (`chat_sessions`), `ChatMessageEntity`
(`chat_messages`). **DAOs:** `MedicaoDao`, `ApelidoDispositivoDao`, `ChatSessionDao`.

`medicao`: registra tanto speedtests completos (`downloadMbps`/`uploadMbps` preenchidos,
`connectionType` real, `fonte = "android"`) quanto monitoramento passivo (`connectionType =
"monitor"`, `downloadMbps`/`uploadMbps = null`, só latência e RSSI).

### 3.2 DataStore — `PreferenciasAppRepository`

**Módulo:** `:coreDatastore`. **Arquivo DataStore:** `linkaPreferencias` (nome técnico legado,
preservado — não renomear). Chaves organizadas em Boolean, String, Int, Long — cobrindo
monitoramento, modem GPON, onboarding, histerese de alertas, tema, perfil, operadora/ISP,
plano/UF/cidade e versão de changelog vista.

### 3.3 D1 (Cloudflare)

Usado pelos workers `signallq-admin` e `signallq-diagnostic`. Migrations versionadas em
`integrations/cloudflare/signallq-diagnostic-worker/migrations/` e
`integrations/cloudflare/signallq-admin-worker/migrations/`. Inventário completo e histórico de
cada migration: `docs_ai/CONTRATOS/schemas/README.md` — não repetido aqui.

### 3.4 Uso conjunto

| Dado | Onde | Escreve | Lê |
|---|---|---|---|
| Speedtest completo | Room / `medicao` | `ExecutorSpeedtest` via ViewModel | `HistoricoScreen`, `ResultadoVelocidadeScreen` |
| Medição passiva | Room / `medicao` | `MonitoramentoWorker` | `HistoricoScreen` (gráfico uptime) |
| Apelidos de dispositivos | Room / `apelido_dispositivo` | `DispositivosScreen` via ViewModel | `DispositivosScreen` |
| Preferências do usuário | DataStore | `AjustesScreen` via ViewModel | Múltiplas telas + `MonitoramentoWorker` |
| Flags remotas de produto | DataStore (cache) ← D1 via Admin Worker | `FeatureFlagRepository` | `FeatureFlagProvider` |

---

## 4. APIs/Endpoints

### 4.1 Cloudflare — Visão Geral dos Workers

**5 Workers** em `integrations/cloudflare/`, confirmados por diretório + `name` real em cada
`wrangler.toml`:

| Diretório | Name (wrangler.toml) | Propósito |
|---|---|---|
| `ai-diagnosis-worker` | `linka-ai-diagnosis-worker` (nome legado, técnico — não renomear) | Motor de IA de diagnóstico (LLM). Endpoint consumido pelo app: `POST /api/ai/diagnostico-conexao` |
| `signallq-admin-worker` | `signallq-admin` | Backend do painel admin + ingest de dados do app |
| `signallq-diagnostic-worker` | `signallq-diagnostic` | Motor de diagnóstico server-side (ruleset versionado, catálogo de jogos, diretório de provedores/ISP, auth admin) — persistência D1 própria |
| `signallq-privacy-worker` | `signallq-privacy` | Página pública de política de privacidade (HTML estático, sem D1/IA/auth) — exigida por Play Store/LGPD |
| `game-latency-probe-worker` | `signallq-game-latency-probe` | Sonda de latência para a tela Jogos (estratégia `REGIONAL_ESTIMATE`), responde `/probe` com 204 sem corpo |

Contrato completo de cada um: `docs_ai/CONTRATOS/openapi/`.

### 4.2 IA de Diagnóstico (`linka-ai-diagnosis-worker`)

**Provider primário:** Gemini 2.0 Flash quando a secret `GEMINI_API_KEY` está configurada
(produção) — tentado primeiro. **Fallback cloud:** Qwen3 30B MoE FP8 (Cloudflare Workers AI). Sem a
secret, Qwen3/CF é o único provider cloud; em falha de ambos, o cliente Kotlin usa fallback local
(sem IA externa). Ordem confirmada em `src/providers.ts` (comentário do arquivo: "Primary: Gemini
2.0 Flash… Fallback: Qwen3 30B MoE FP8").

**Endpoint consumido pelo app:** `POST /api/ai/diagnostico-conexao` (worker `ai-diagnosis-worker`).
Motor de laudo automático e análise por problema específico usam o `signallq-diagnostic-worker`
(timeout 42s, GH#962/#969) — ver `docs_ai/FUNCIONAL.md` RF-02 para o fluxo do usuário.

**Integração Android:** módulo `:featureDiagnostico`, classe `AiDiagnosisRepository`. Transporte:
OkHttp 4.12.0, POST JSON. **Deploy:** `npx wrangler deploy` no diretório do worker — **obrigatório
antes do commit Android** quando houver mudança em
`integrations/cloudflare/ai-diagnosis-worker/src/` (ver seção 8.3).

---

## 5. Integrações e dependências

### 5.1 Dependências Principais e Auditoria

Fonte: `android/gradle/libs.versions.toml`, auditado em 2026-06-28/2026-07-05.

**Sem CVE crítico afetando as versões em uso.**

| Dependência | Status | Nota |
|---|---|---|
| Firebase BOM 34.15.0, Hilt 2.58, Room 2.8.4, Material 1.12.0, WorkManager 2.11.2, Timber 5.0.1, ProfileInstaller 1.4.1, Desugar 2.1.5, Detekt 1.23.7, JUnit 4.13.2 | ok | Atualizados |
| **Compose BOM 2025.05.01** | **atualizar — prioridade alta** | Defasado (recente: 2026.06.00) — reconfirmar defasagem exata na próxima auditoria de dependências, não repetida nesta revisão de docs. Todas as libs Compose sobem juntas via BOM |
| Lifecycle 2.8.7, Coroutines 1.9.0, Navigation 2.8.0, Coil 3.1.0 | atualizar — prioridade média | Bug fixes e melhorias acumuladas |
| OkHttp 4.12.0 | sem urgência | 5.x traz HTTP/3; 4.12.0 sem CVE conhecido |
| DataStore, Robolectric, AndroidX Test JUnit, Espresso, Google Services Plugin, Firebase plugins, KtLint Gradle Plugin | atualizar — prioridade baixa | Incrementais |

**Plano sugerido (2 PRs):** PR1 = Compose BOM + Lifecycle + Coroutines + Navigation (roda suite
completa depois); PR2 = demais libs de baixo risco em batch. `[a confirmar]` se este plano já foi
executado — não reconfirmado contra histórico de PRs nesta revisão.

### 5.2 Feature Flags Técnicas (compile-time)

Controle granular via **flags booleanas em compiletime**, definidas em `app/build.gradle.kts` como
`buildConfigField`, com blocos distintos para `debug` (todas `true`) e `release` (apenas MVP `true`).
Objeto `FeatureFlags` (`app/src/main/kotlin/io/veloo/app/kotlin/FeatureFlags.kt`) expõe cada flag via
`BuildConfig.FEATURE_*` — **acesso sempre via `FeatureFlags.*`, nunca `BuildConfig.DEBUG` ou
`BuildConfig.FEATURE_*` diretamente nas telas.**

**Ativação em release:** alterar valor no bloco `release` de `app/build.gradle.kts`, incrementar
versão, rebuild e testar. Lista atual de flags ativas/inativas em release: ver
`docs_ai/FUNCIONAL.md` seção 8.3 (não duplicada aqui — evita duas listas divergentes).

**Arquitetura de proteção de overlays controlados por flag** (dupla camada):
1. `AnimatedVisibility` no `AppShell` — `visible` verifica `FeatureFlags.*`; flag inativa impede o
   Composable de entrar em composição.
2. Gate na lambda de entrada (ex.: `onConectarFibra`, `onAbrirDnsBenchmark`) — verifica a flag antes
   de executar side-effects, mesmo em cenário de recomposição inesperada.

**Sistema separado — flags remotas (produto):** existe também um `FeatureFlagManager` /
`FeatureFlagRepository` (`app/src/main/kotlin/io/veloo/app/kotlin/featureflags/`) que consome
`GET /flags` (schema atual) e `GET /feature-flags` (legado) do Admin Worker, com cache em
DataStore. É um mecanismo **distinto** das flags de compilação acima — controla toggles de produto
via painel administrativo, não requer novo build. Efeito do lado do usuário: ver `FUNCIONAL.md`.
Endpoints: ver `docs_ai/CONTRATOS/openapi/signallq-admin-api.yaml`.

> Correção de escopo: a documentação anterior de feature flags (`functional/FEATURE_FLAGS.md`)
> descrevia majoritariamente esse segundo sistema (remoto/D1), inclusive citando `Felipe`/`Gema`
> como responsáveis — ambos fora do squad desde 2026-07-09/07-10. O código real confirma que esse
> sistema existe (`FeatureFlagManager.kt`, `FeatureFlagRepository.kt`), mas seu detalhe de produto
> pertence a `FUNCIONAL.md`, não a este documento.

### 5.3 Firebase

**Projeto ativo:** `signallq-app` (conta 7Agents) — app Android `io.signallq.app`.

**Projeto legado (abandonado):** `device-streaming-ef179de4` (conta pessoal), app
`io.linka.app.kotlin` — requer limpeza manual no console (fora do escopo deste doc, ver
`.claude/CLAUDE.md` seção "Infraestrutura e Contas Legadas").

**Analytics:** Firebase Analytics (eventos) habilitado com LGPD consent gate. **Crashlytics:** logs
de erro. **Não usa:** Realtime Database.

Ver seção 8.1 (Rollout e observabilidade → Analytics) para o contrato de eventos.

---

## 6. Segurança e privacidade

- **Assinatura:** chaves em `key.properties` (gitignored, nunca versionado); template público em
  `key.properties.template`.
- **Secrets de worker** (ex.: `GEMINI_API_KEY`) configuradas via Cloudflare, nunca hardcoded em
  `wrangler.toml` nem no app.
- **Endpoint de leitura de flags** (`GET /flags`, `GET /feature-flags`): público, rate-limited por
  device.
- **Endpoint de escrita** (toggle de flag no Admin): requer sessão + `role=admin`.
- **Analytics:** sem PII nos parâmetros de evento (sem SSID completo, sem IP público, sem BSSID).
- **LGPD:** Firebase Analytics habilitado com consent gate; página pública de política de
  privacidade servida pelo worker `signallq-privacy`.

### 6.1 Licenças de terceiros (assets)

Distinto de `operations/THIRD_PARTY_NOTICES.md` (licenças de bibliotecas OSS — não tocado por este
documento, permanece onde está). Este item cobre **assets** embutidos no app:

**Google Sans Flex** — fonte base do tema (`SignallQTheme.kt`, `signallQFontFamily`). Licença SIL
Open Font License v1.1, texto integral embutido no APK em
`android/app/src/main/assets/licenses/google_sans_flex_OFL.txt`. Procedência verificada em canais
oficiais do Google. 4 pesos estáticos versionados (400/500/600/700).

---

## 7. Performance e escalabilidade

- **Compose BOM defasado** (ver 5.1) é o principal item de dívida com potencial impacto em
  performance/manutenibilidade — bug fixes e otimizações de recomposição do Compose ficam de fora
  até a atualização.
- **Rate limiting de scan Wi-Fi (Android, API 28+):** `startScan()` limitado a 4 chamadas/2min em
  foreground pelo próprio SO; scanner usa cache de `scanResults` como fallback em throttling (ver
  `docs_ai/FUNCIONAL.md` RF-03).
- **`stateIn(viewModelScope, WhileSubscribed(5000), initial)`** em todos os flows expostos — evita
  recomputação enquanto não há coletor ativo (ex.: tela em background).
- Escalabilidade de backend (D1, Workers) não tem meta formal de carga documentada —
  `[a confirmar]`, não encontrado em código nem doc ativa nesta revisão.

---

## 8. Rollout e observabilidade

### 8.1 Analytics

Dois esquemas de instrumentação coexistem, sem redundância entre si — cobrem escopos diferentes,
embora compartilhem a mesma instância de `FirebaseAnalytics`:

#### 8.1.1 `AnalyticsHelper` — funil principal

Interface em `core/network/AnalyticsHelper.kt`, implementação `FirebaseAnalyticsHelper` em `:app`,
injetada via Hilt. **7 eventos implementados e testados**
(`FirebaseAnalyticsHelperTest.kt`, MockK + Robolectric):

```
app_aberto → speedtest_iniciado → speedtest_concluido → diag_iniciado
  → diag_concluido → ia_laudo_solicitado → ia_laudo_recebido
```

Contrato ampliado (eventos ainda não implementados): `docs_ai/technical/analytics-events.md` —
mantido como documento de detalhe, não absorvido integralmente aqui.

#### 8.1.2 `AnalyticsTracker` — schema GA4

Interface em `core/network` (`AnalyticsTracker`), implementação `FirebaseAnalyticsTracker`
(`@Singleton`) em `:app`. Alimenta o `ProductAnalyticsPage` do Admin. **5 eventos:**
`feature_used`, `screen_view`, `app_session_start`, `feature_crash`, `battery_snapshot` —
`session_id` é um UUID por instância de processo, sem PII. Detalhe: `docs_ai/technical/analytics-events-schema.md`.

> Nenhum dos dois arquivos-fonte é redundante do outro — `analytics-events.md` é o contrato mais
> amplo (implementado + proposto, funil de conversão); `analytics-events-schema.md` é o schema GA4
> efetivamente implementado (uso de feature, não funil). Ambos permanecem como referência de
> detalhe; esta seção só consolida a visão geral.

**Regra de manutenção:** qualquer evento novo/alterado exige atualização do arquivo de detalhe
correspondente no mesmo PR. Injetar `AnalyticsHelper`/`AnalyticsTracker` via Hilt — nunca
`FirebaseAnalytics` diretamente, nunca `logEvent` em Composable.

---

### 8.2 Testes

- **~66 arquivos de teste unitário** — JUnit4 + Robolectric + coroutines-test + room-testing, em
  `android/*/src/test/`.
- **3 testes androidTest** de Room/DAO.
- **Comando:** `.\android\gradlew.bat test` (Windows) / `./gradlew test` (a partir de `android/`).

Estratégia de teste por camada e cobertura por feature: ver `docs_ai/testing/` — visão geral, não
repetida aqui.

### 8.3 Build e Release Local

#### 8.3.1 Comandos

```bash
# A partir de android/, gradlew.bat no Windows
./gradlew build              # Build completo
./gradlew assembleDebug      # APK debug
./gradlew assembleRelease    # APK release
./gradlew lint                # Lint estático
./gradlew test                # Testes
./gradlew ktlintCheck         # Lint de formatação
./gradlew detekt              # Análise estática
```

#### 8.3.2 Processo de release (Firebase App Distribution)

Ordem obrigatória, sem pular etapas:

1. **Commit** — stage de todos os arquivos modificados, mensagem descritiva.
2. **Push** — `git push origin main`.
3. **Clean build** — `.\android\gradlew.bat clean assembleRelease --no-build-cache` (nunca usar
   cache em release).
4. **Upload** — `.\android\gradlew.bat appDistributionUploadRelease`.

**Worker Cloudflare:** ao mudar `integrations/cloudflare/ai-diagnosis-worker/src/`, rodar
`npx wrangler deploy` **antes** do commit Android.

Checklist completo por stack (Android + Cloudflare Pages/Workers) e changelog: ver skill
`checar-release` — não repetido aqui.

---

## 9. Riscos técnicos

### 9.1 Divergências corrigidas em revisões anteriores (histórico)

| Item | Documentos com valor errado | Valor incorreto citado | Valor real confirmado em 2026-07-16 |
|---|---|---|---|
| versionName/versionCode | ANDROID_TECNICO, BUILD_SYSTEM, CLOUDFLARE, STORAGE, DATA_FLOW, API_MAP, MODULES, SERVICES, DEPENDENCY_AUDIT, analytics-events(-schema), CLAUDE.md | `0.23.0`/56 (ou `0.16.0`/46 em BUILD_SYSTEM) | `0.25.0`/60 — reconfirmado e atualizado nesta revisão (2026-07-23) para `0.30.1`/67 |
| compileSdk | ANDROID_TECNICO, BUILD_SYSTEM | 36 | 37 |
| Total de módulos Gradle | ANDROID_TECNICO, MODULES | 15 (alegando CLAUDE.md errado) | 16 — CLAUDE.md estava certo; faltava `:coreRecommendation`. Reconfirmado ainda válido em 2026-07-23 (módulos consumer; Pro cresceu à parte, ver 2.4) |
| Total de Workers Cloudflare | CLOUDFLARE, API_MAP | 3 | 5 — faltavam `signallq-diagnostic-worker` e `game-latency-probe-worker`. Reconfirmado ainda válido em 2026-07-23 |
| Versão do schema Room | STORAGE, ANDROID_TECNICO, DATA_FLOW, MODULES, SERVICES, CLAUDE.md | v10 (ou "v12" no CLAUDE.md) | v14 (`SignallQDatabase`, GH#1027 — +bandaWifi). Reconfirmado ainda v14 em 2026-07-23, sem migration nova |

### 9.2 Divergência corrigida nesta revisão (2026-07-23)

`coreRecommendation` estava documentado (seção 2.4, correção histórica) como "ainda não integrada a
UI/monetização" — impreciso. O código real (`ResultadoVelocidadeScreen.kt`, import
`io.signallq.app.core.recommendation.RecommendationDecision`) mostra que a engine **já está
integrada à UI** via `RecommendationCard`; apenas a integração com monetização real (AdMob/
afiliados) segue pendente. Mesma correção já havia sido feita em `docs_ai/FUNCIONAL.md`
(seção RF-02) em 2026-07-16 — este documento estava desalinhado com aquele até agora.

### 9.3 Dados não confirmados / pendências

- Endpoints exatos do `signallq-diagnostic-worker`: cobertos em detalhe em
  `docs_ai/CONTRATOS/openapi/signallq-diagnostic-worker.yaml` — algumas regras de validação de
  payload (`validateSnapshot`) ficaram marcadas como "não confirmado" no próprio contrato.
- Timeout OkHttp do `AiDiagnosisRepository`: não confirmado contra o código nesta revisão.
- Prompt version atual do worker de IA: não reconfirmado contra `src/index.ts` nesta revisão.
- Defasagem exata do Compose BOM (ver 5.1) e status do plano de atualização em 2 PRs: não
  reconfirmados contra histórico de PRs nesta revisão — marcado `[a confirmar]`.
- Metas formais de performance/escalabilidade de backend (D1, Workers): não encontradas em código
  nem doc ativa — `[a confirmar]` (ver seção 7).
