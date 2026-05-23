# Roadmap de Execução — Issues #1–24
## Sequência, Dependências e Paralelização

**Projeto**: LINKA Android v0.9.3 — Eficiência  
**Horizonte**: 3–4 sprints (6–8 semanas)  
**Atualizado**: 2026-05-23

---

## Timeline Recomendada

### 🔴 **SPRINT 1: Segurança Crítica (Semanas 1–2)**

**Lead**: Rodrigo + Gema  
**Bloqueadores**: Todas as outras issues dependem indiretamente destes

#### **Issue #1: Rotacionar Keystore** (Rodrigo, Gema)
- **Duração**: 3–4h
- **Pré-requisito**: Acesso ao histórico Git e Play Console
- **Ações**:
  1. `git log --all --full-history -- key.properties` para auditar
  2. Se houve exposição: `git filter-repo --path key.properties` para purgação
  3. Rotacionar keystore + novo upload key no Play Console
  4. Documentar em `docs_ai/operations/KEY_ROTATION_LOG.md`
- **Validação**:
  - [ ] Git log vazio para `key.properties`
  - [ ] Play Console com novo cert thumbprint registrado
- **Bloqueadores**: Nenhum
- **Desbloqueia**: Nenhuma (paralela com #2)

#### **Issue #2: Network Security Config** (Rodrigo, Gema)
- **Duração**: 4–5h
- **Ações**:
  1. Criar `app/src/main/res/xml/network_security_config.xml`
  2. Configurar domain-config para localhosts (10.x, 172.16.x, 192.168.x)
  3. Remover `usesCleartextTraffic="true"` de `AndroidManifest.xml`
  4. Testar: `assembleRelease`, smoke test manual
- **Validação**:
  - [ ] `./gradlew assembleRelease` sucesso
  - [ ] Network Profiler: HTTP local funciona, HTTPS público funciona
  - [ ] Teste de regressão: speedtest, diagnóstico Wi-Fi, DNS não quebram
- **Bloqueadores**: Nenhum
- **Desbloqueia**: #3, #5 (podem rodar em paralelo)

**Fim Sprint 1**: Issues #1 e #2 merged para main, docs atualizadas

---

### 🟠 **SPRINT 2: Arquitetura & Qualidade (Semanas 2–4)**

**Leads**: Camilo (DI), Marina (testes)  
**Parallelizável**: #3 + #4 + #5 + #16

#### **Issue #3: Introduzir Hilt** (Camilo)
- **Duração**: 8–10h
- **Pré-requisito**: #2 merged (segurança estável)
- **Ações**:
  1. Adicionar dependências Hilt em `app/build.gradle.kts`
  2. Criar `HiltApplication` subclass
  3. Anotar `MainActivity` com `@AndroidEntryPoint`
  4. Criar `NetworkModule`, `DatabaseModule`, `RepositoryModule`
  5. Refatorar `MainViewModel` de `lazy { }` para `@HiltViewModel`
  6. Migrar `MonitoramentoWorker`, `AppDatabase` para injeção
- **Arquivos principais**: `app/src/main/kotlin/di/`, `MainViewModel.kt`
- **Validação**:
  - [ ] App compila sem `lazy { }` blocks
  - [ ] `./gradlew test` passa (testes existentes)
  - [ ] MainViewModel injeta sem erros
- **Bloqueadores**: Nenhum
- **Desbloqueia**: #4 (refactor `!!` será mais fácil), #7 (Dispatchers)

#### **Issue #4: Erradicar !! Operators** (Camilo)
- **Duração**: 5–6h
- **Pré-requisito**: #3 (para injetar mock em testes)
- **Ações**:
  1. Adicionar Detekt rule `ExplicitNullChecks`
  2. Varrer e listar todos os `!!` (15+ ocorrências)
  3. Refatorar 1 arquivo por vez: `MainViewModel.kt` → `let`, `?.apply`, destructuring
  4. Adicionar testes para casos null (ex.: `effectiveTs == null`)
- **Arquivos principais**: `MainViewModel.kt`, `HomeScreen.kt`, `ExecutorSpeedtestCloudflare.kt`
- **Validação**:
  - [ ] `./gradlew detekt` não reporta `ExplicitNullChecks`
  - [ ] Testes para null-safety cases adicionados
- **Bloqueadores**: #3 (precisa Hilt para mockar)
- **Desbloqueia**: #6, #7

#### **Issue #5: Detekt + Ktlint + CI** (Marina + Camilo)
- **Duração**: 6–7h
- **Pré-requisito**: Nenhum
- **Ações**:
  1. Adicionar Detekt plugin e ruleset em `build.gradle.kts`
  2. Adicionar Ktlint com Spotless
  3. Criar `.github/workflows/lint.yml` (assembleDebug + test + detekt + ktlint)
  4. Criar script `scripts/lint.ps1` para dev local
  5. Documentar em `CONTRIBUTING.md`
- **Validação**:
  - [ ] `./scripts/lint.ps1` passa localmente
  - [ ] GitHub Actions workflow ativado em PR
- **Bloqueadores**: Nenhum
- **Desbloqueia**: Nenhuma (infra para todas)

#### **Issue #16: Elevar Cobertura em core\*** (Marina)
- **Duração**: 8–10h
- **Pré-requisito**: #5 (para rodar coverage CI)
- **Ações**:
  1. Rodar `./gradlew testDebugCoverage` para baseline
  2. Focar em `coreDatabase`, `coreNetwork`, `coreDatastore` (atualmente ~10%)
  3. Adicionar testes: Room migrations, parsing, DataStore serialization
  4. Meta: ≥70% em cada módulo core
- **Validação**:
  - [ ] `./gradlew testDebugCoverage` ≥70% em `core*`
  - [ ] Coverage report no Codecov (se configurado)
- **Bloqueadores**: #5 (para infra)
- **Desbloqueia**: #9, #20 (validação de otimizações)

**Fim Sprint 2**: Issues #3, #4, #5, #16 merged. Hilt + lint infra ativa.

---

### 🟡 **SPRINT 3: Logging & Dispatchers (Semanas 3–4)**

**Lead**: Camilo  
**Parallelizável**: #6 + #7 + #8

#### **Issue #6: Logger Abstrato com Timber** (Camilo)
- **Duração**: 4–5h
- **Pré-requisito**: #3 (Hilt), #5 (build setup)
- **Ações**:
  1. Definir interface `Logger` com `d/w/e` methods
  2. Implementar `TimberLogger`
  3. Criar Hilt module `LoggerModule` com `@Singleton @Provides`
  4. Substituir `Log.*` em 35 ocorrências (MonitoramentoWorker, WorkManager, etc.)
  5. Adicionar Timber setup em Application
- **Validação**:
  - [ ] Nenhum `Log.d/w/e` direto no código (grep confirm)
  - [ ] Testes mockam Logger sem problema
- **Bloqueadores**: #3
- **Desbloqueia**: Nenhuma (melhor testability, não bloqueia)

#### **Issue #7: Dispatchers Explícitos** (Camilo)
- **Duração**: 5–6h
- **Pré-requisito**: #3 (Hilt), #4 (refactor `!!`)
- **Ações**:
  1. Criar `DispatcherProvider` interface com `io()`, `main()`, `default()`
  2. Hilt module fornece `RealDispatcherProvider` (prod) vs `TestDispatcherProvider` (teste)
  3. Injetar em repositórios: `SinalRepository`, `UptimeRepository`, `WifiRepository`
  4. Substituir `launch { }` por `launch(ioDispatcher) { }` onde apropriado
- **Validação**:
  - [ ] Testes usam `StandardTestDispatcher`
  - [ ] Nenhum blocking em Main thread (verificar com Detekt)
- **Bloqueadores**: #3
- **Desbloqueia**: #20 (otimizações em MonitoramentoWorker)

#### **Issue #8: allowBackup + dataExtractionRules** (Rodrigo)
- **Duração**: 3–4h
- **Pré-requisito**: Nenhum
- **Ações**:
  1. Definir `android:allowBackup="false"` em AndroidManifest
  2. Criar `app/src/main/res/xml/backup_rules.xml` excluindo databases sensíveis
  3. Referenciar em `android:dataExtractionRules="@xml/backup_rules"` (Android 12+)
  4. Testar: adb backup/restore não captura dados
- **Validação**:
  - [ ] `adb backup -f backup.ab io.linka.app` não inclui Room/DataStore files
  - [ ] Histórico ainda restaurável se necessário
- **Bloqueadores**: Nenhum
- **Desbloqueia**: Nenhuma (segurança pontual)

**Fim Sprint 3**: Logging, Dispatchers, backup hardened. Codebase mais testable.

---

### 🟢 **SPRINT 4: Performance & Otimizações (Semanas 4–6)**

**Leads**: Camilo (arquitetura), Brás (mobile)  
**Crítico**: #17–24 dependem de #3 (Hilt para injeção) e #7 (Dispatchers)

#### **Issue #17: Detecção de Rede Medida** (Brás)
- **Duração**: 4–5h
- **Pré-requisito**: #7 (para chamar `ConnectivityManager`)
- **Ações**:
  1. Criar `NetworkType` enum (WIFI_UNLIMITED, WIFI_METERED, etc.)
  2. Implementar `detectNetworkType()` em novo `NetworkUtils.kt`
  3. Injetar em `ExecutorSpeedtestCloudflare`
  4. Ajustar payload (50/10/10/3 MB por tipo)
  5. Adicionar UX badge "Metered Network" em tela de resultado
- **Validação**:
  - [ ] Speedtest em Wi-Fi metered ajusta payload ≤10 MB
  - [ ] Tela exibe badge corretamente
  - [ ] Erro vs. throughput real ≤10%
- **Bloqueadores**: #7
- **Desbloqueia**: #18 (reduz carga de rede)

#### **Issue #18: Ping Concorrente Relaxado** (Brás)
- **Duração**: 3–4h
- **Pré-requisito**: #17 (contexto do speedtest)
- **Ações**:
  1. Reduzir intervalo de ping 300ms → 2000ms (300ms era muito agressivo)
  2. Ou: rodar ping apenas antes/depois do throughput (série)
  3. Validar latência em real-time na tela
- **Validação**:
  - [ ] Throughput com novo intervalo ≤5% desvio
  - [ ] UX não mostra stutter em ping display
- **Bloqueadores**: #17
- **Desbloqueia**: #19

#### **Issue #19: ConnectionPool Adaptativo** (Camilo)
- **Duração**: 4–5h
- **Pré-requisito**: #7 (Dispatchers, ConnectivityManager)
- **Ações**:
  1. Criar factory para `createHttpClient(isWifi: Boolean)` → poolSize/idleTime adaptativo
  2. Registrar `NetworkCallback` para reconectar ao mudar rede
  3. 2 instâncias de OkHttpClient (Wi-Fi vs celular)
- **Validação**:
  - [ ] Battery drain em Doze -5–10% vs. antes
  - [ ] HTTP requests funcionam sem latência extra
- **Bloqueadores**: #7
- **Desbloqueia**: #20, #24

#### **Issue #20: Monitoramento com combine()** (Camilo)
- **Duração**: 5–6h
- **Pré-requisito**: #7 (Dispatchers), #19 (conexão estável)
- **Ações**:
  1. Refatorar `MonitoramentoWorker` com `combine()` em lugar de `.first()` cascata
  2. Coleta paralela: sinal + uptime + wifi + etc. em 100ms (vs. 400ms)
  3. Cache local em DataStore para state agregado
- **Validação**:
  - [ ] Worker tempo de coleta ≤100ms
  - [ ] Battery drain -5–8% vs. antes
- **Bloqueadores**: #7, #19
- **Desbloqueia**: #21

#### **Issue #21: HTTP Timeouts Global + Backoff** (Camilo)
- **Duração**: 4–5h
- **Pré-requisito**: #19 (OkHttpClient setup)
- **Ações**:
  1. Adicionar `callTimeout(15s)` + `connectTimeout(5s)` + `readTimeout(10s)`
  2. Implementar backoff exponencial (1s, 2s, 3s) em `MonitoramentoWorker`
- **Validação**:
  - [ ] Worker completa em <30s mesmo com rede lenta
  - [ ] Retry lógica testa com MockWebServer (simule timeouts)
- **Bloqueadores**: #19
- **Desbloqueia**: #22

#### **Issue #22: MainActivity com combine()** (Camilo)
- **Duração**: 5–7h
- **Pré-requisito**: #3 (Hilt), #7 (Dispatchers)
- **Ações**:
  1. Criar `MainUiState` data class agregando ~5 grupos
  2. ViewModel com `combine()` + `distinctUntilChanged()` + `stateIn()`
  3. MainActivity coleta apenas `appState` (em vez de 40 individuais)
  4. Validar com Layout Inspector
- **Validação**:
  - [ ] Recomposições ≥30% reduzidas (Layout Inspector)
  - [ ] Frame time ≤16ms em navegação
- **Bloqueadores**: #3
- **Desbloqueia**: #23

#### **Issue #23: ResultadoVelocidadeScreen + remember** (Claudete, Camilo)
- **Duração**: 4–5h
- **Pré-requisito**: #22 (context), #16 (test coverage)
- **Ações**:
  1. `OrbitSymbolSmall()` com `remember` para offsets/cores
  2. Seções dinâmicas → `LazyColumn` com `key`
  3. Validar frame time em Pixel 4a/Moto G7
- **Validação**:
  - [ ] Frame time ≤16ms em scroll
  - [ ] UX visualmente idêntica
- **Bloqueadores**: #22
- **Desbloqueia**: Nenhuma (standalone)

#### **Issue #24: OkHttp DNS/Health Caching** (Camilo)
- **Duração**: 3–4h
- **Pré-requisito**: #19 (OkHttpClient setup)
- **Ações**:
  1. Criar dois OkHttpClient: `okHttpSpeedtest` (no-store) + `okHttpAux` (5 MB cache, max-age=300)
  2. Injetar via Hilt qualifiers
  3. Health checks, DNS → `okHttpAux`; speedtest → `okHttpSpeedtest`
- **Validação**:
  - [ ] Dados móveis -5–10% em uso típico
  - [ ] Network Profiler: health checks repetidas em <5min retornam 304
- **Bloqueadores**: #19
- **Desbloqueia**: Nenhuma (standalone)

**Fim Sprint 4**: Todas as otimizações de dados/energia/performance landadas.

---

### 🎨 **SPRINT 5: Design System & UX (Semanas 6–7)**

**Leads**: Claudete (design), Gema (docs)  
**Pré-requisito**: #3 (Hilt), anteriores estáveis

#### **Issue #10: Strings i18n** (Claudete)
- **Duração**: 4–5h
- **Ações**: Mover hardcoded strings (`OrbitWelcomeState`, etc.) para `strings.xml`
- **Validação**: `grep -r "\".*\"" app/src/main/kotlin/ui | grep -v @String` ≤5 ocorrências

#### **Issue #11: Acessibilidade** (Claudete)
- **Duração**: 5–6h
- **Ações**: Auditar `contentDescription`, rodar Accessibility Scanner, TalkBack 100% nas rotas principais
- **Validação**: Manual em device + Accessibility Scanner verde

#### **Issue #12: UiState Padrão Selado** (Gema, Camilo)
- **Duração**: 4–5h
- **Ações**: Criar `UiState<T>` sealed, refatorar telas (`ResultadoVelocidadeScreen`, `HomeScreen`)
- **Validação**: Todas telas usam `UiState<T>` (Loading/Success/Error/Empty)

#### **Issue #13: Auditar Permissões** (Rodrigo)
- **Duração**: 2–3h
- **Ações**: Revisar `AndroidManifest.xml`, testar uso real de cada permissão
- **Validação**: Documentar em `docs_ai/android/PERMISSIONS_AUDIT.md`

#### **Issue #14: Consolidar Docs + ADRs** (Gema)
- **Duração**: 5–6h
- **Ações**: Consolidar `docs/` vs `docs_ai/`, criar ADRs para decisões (#3 Hilt, #19 dual client, etc.)
- **Validação**: Índice mestre em `docs_ai/INDEX.md` linkando todos

#### **Issue #15: TODOs Órfãos** (Gema)
- **Duração**: 2–3h
- **Ações**: Varrer e converter TODOs em issues ou remover
- **Validação**: `grep -r "TODO" app/src/main/kotlin` retorna vazio

**Fim Sprint 5**: UX polida, docs consolidadas, acessibilidade auditada.

---

## Dependência Graph

```
SPRINT 1
├─ #1 Keystore ────────────────┐
└─ #2 Network Config ──────────┤
                               ↓
SPRINT 2 (paralelo)        BLOQUEADOR P0
├─ #3 Hilt ────────────────────┬────────────┐
├─ #4 !! (depende #3) ─────────┤           │
├─ #5 Detekt (infra) ──────────┤           │
└─ #16 Tests (depende #5) ─────┤           │
                               │           │
SPRINT 3 (paralelo)            │           │
├─ #6 Logger (depende #3) ──────┤           │
├─ #7 Dispatchers (depende #3) ─┤           │
├─ #8 Backup config ────────────┤           │
                               │           │
SPRINT 4 (CRITICAL)           │           │
├─ #17 Metered (depende #7) ───┤           │
├─ #18 Ping (depende #17) ──────┤           │
├─ #19 ConnPool (depende #7) ───┤           │
├─ #20 Combine (depende #7,#19) ┤           │
├─ #21 Timeout (depende #19) ───┤           │
├─ #22 MainActivity (depende #3)┼────────────┤
├─ #23 Result (depende #22) ────┤           │
└─ #24 Cache (depende #19) ─────┤           │
                               │           │
SPRINT 5                        │           │
├─ #10 i18n (design) ────────────┤           │
├─ #11 A11y (design) ────────────┤           │
├─ #12 UiState (depende #3) ─────┤           │
├─ #13 Perms (security) ─────────┤           │
├─ #14 Docs (depende anteriores) ┤           │
└─ #15 TODOs ────────────────────┤           │
                               ↑           ↑
                            STABLE       TESTED
```

---

## Checklist de Conclusão

- [ ] **Sprint 1 done**: #1, #2 merged, docs atualizadas
- [ ] **Sprint 2 done**: #3, #4, #5, #16 merged, CI ativo
- [ ] **Sprint 3 done**: #6, #7, #8 merged, logging/dispatchers refatorados
- [ ] **Sprint 4 done**: #17–24 merged, benchmark de bateria/dados/perf validados
- [ ] **Sprint 5 done**: #10–15 merged, docs consolidadas, acessibilidade auditada
- [ ] **Final**: Release candidate v0.9.3 pronto, CHANGELOG atualizado, Play Store internal testing

---

## Notas para Agentes

1. **Paralelização**: Sprints 2–4 podem rodar múltiplos agentes em paralelo
2. **Comunicação**: Diariamente, atualize status em ADR/comentários de issue
3. **Bloqueadores**: Se ficar travado, mencione no Slack/GitHub issue (não silenciosamente)
4. **Code Review**: Cada PR precisa de ≥1 review de agente diferente antes de merge
5. **Testing**: Validações de issue executadas localmente antes de push

---

