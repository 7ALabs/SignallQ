# TESTING.md — validação GH#953 (worker `signallq-diagnostic`)

Evidência de teste da migração + correções P0/P1/P2 (issues #954, #955, #956, #957, #958, #959,
#960, #961). Cobre suíte automatizada (`node --test`) e validação manual via `wrangler dev --local`
+ D1 local + `curl`, batendo contra o worker de verdade (não só mock).

## Suíte automatizada

```
npm run verify
```

Resultado: **55/55 testes passando**, `tsc --noEmit` limpo.

32 testes originais (paridade preservada) + 23 novos cobrindo as correções desta PR.

## Cenários testados (automatizado)

| # | Cenário | Esperado | Obtido |
|---|---|---|---|
| 1 | `latency_high` em 149ms | não dispara | ✅ não disparou |
| 2 | `latency_high` em 150ms (GT estrito) | não dispara | ✅ não disparou |
| 3 | `latency_high` em 151ms | dispara | ✅ disparou |
| 4 | `bufferbloat_elevated` em 29ms | não dispara | ✅ não disparou |
| 5 | `bufferbloat_elevated` em 30ms | não dispara | ✅ não disparou |
| 6 | `bufferbloat_elevated` em 31ms | dispara | ✅ disparou |
| 7 | `bufferbloat_critical` em 99ms | não dispara (só elevated) | ✅ confirmado |
| 8 | `bufferbloat_critical` em 100ms | não dispara | ✅ não disparou |
| 9 | `bufferbloat_critical` em 101ms | dispara | ✅ disparou |
| 10 | `jitter_elevated` em 20ms / 21ms | não dispara / dispara | ✅ ambos corretos |
| 11 | perda de pacotes 0.99% / 1% / 3% | nenhum / moderate / critical | ✅ os 3 corretos |
| 12 | DNS latency 150ms / 151ms / 300ms / 301ms | nenhum / elevated / elevated / high | ✅ os 4 corretos |
| 13 | score pondera diferente por tipo de conexão (wifi vs fibra vs móvel) | score de wifi < fibra e < móvel (estabilidade pesa mais em wifi) | ✅ confirmado |
| 14 | `evaluationSource` sem D1 nem `DIAGNOSTIC_RULESET_JSON` | `BUNDLED_LOCAL` | ✅ |
| 15 | `evaluationSource` com `DIAGNOSTIC_RULESET_JSON` válido | `REMOTE` | ✅ |
| 16 | `evaluationSource` com ruleset `PUBLISHED` no D1 | `REMOTE` | ✅ |
| 17 | D1 indisponível (exceção no `prepare()`) | nunca 500 cru; payload válido, `decisao.status=inconclusive` | ✅ confirmado, HTTP 200 |
| 18 | `gameReadiness` expõe os 4 perfis reais do catálogo | `COMPETITIVE`, `COMPETITIVE_EXTREME`, `MULTIPLAYER_MODERATE`, `SPORTS_COMPETITIVE` — nunca fps/moba/casual | ✅ |
| 19 | `gameReadiness` COMPETITIVE_EXTREME fronteiras (good=30/attention=80) | bom/atencao/atencao/ruim em 30/31/80/81ms | ✅ os 4 corretos |
| 20 | `gameReadiness` MULTIPLAYER_MODERATE fronteiras (good=60/attention=150) | bom/atencao/atencao/ruim em 60/61/150/151ms | ✅ os 4 corretos |
| 21 | admin auth: senha errada | 401, sem `set-cookie` | ✅ |
| 22 | admin auth: rota `/admin/*` sem cookie | 401 | ✅ |
| 23 | admin auth: sessão expirada | 401 | ✅ |
| 24 | `/ingest/provider-detection` payload `null` | 400 tratado (não 500) | ✅ |
| 25 | `/ingest/provider-detection` `asn` com tipo errado | 400 tratado | ✅ |
| 26 | `/ingest/provider-detection` body não-JSON | 400 tratado | ✅ |
| 27 | review queue com 3 `installationHash` distintos em 5 dias | fila = elegível, `distinctInstallationsApprox=3`, `distinctDays=5` | ✅ |
| 28 | review queue com o MESMO `installationHash` repetido 8x em 8 dias | nunca elegível sozinho | ✅ confirmado — não aparece na fila |
| 29 | CORS preflight `OPTIONS` | 204 + `Access-Control-Allow-Origin` | ✅ |
| 30 | CORS em resposta normal | headers presentes | ✅ |

## Validação manual via HTTP real (`wrangler dev --local` + D1 local)

Rodado com `npx wrangler dev --local --port 8791` após aplicar as 5 migrations em D1 local
(`wrangler d1 execute signallq-diagnostic-db --local --file=...`). Todas as chamadas abaixo foram
feitas com `curl` contra o worker real rodando localmente — não mock, não simulação em memória.

**Nota de ambiente**: `compatibility_date` do `wrangler.toml` foi ajustado de `2026-07-14` para
`2026-05-09` (mesmo valor já usado por `ai-diagnosis-worker`/`game-latency-probe-worker`) porque o
binário local do `wrangler` instalado não suporta datas de compatibilidade além de `2026-05-14`.
Isso é uma defasagem normal entre o pacote `wrangler` e o runtime `workerd` embutido, não afeta o
deploy real em produção (a edge da Cloudflare sempre suporta a data atual).

| Chamada | Resultado |
|---|---|
| `GET /health` | 200, lista de rotas |
| `POST /diagnostic/evaluate` (snapshot saudável) | 200, `decisao.status=ok`, `evaluationSource=BUNDLED_LOCAL` |
| `POST /diagnostic/evaluate` (latencyMs=151) | `latency_high` presente, score=60, veredicto=regular |
| `POST /diagnostic/evaluate` (loadedLatencyMs=101, latencyMs=1) | `bufferbloat_critical` + `bufferbloat_elevated` presentes |
| `POST /diagnostic/evaluate` (sem DB populado) | `gameReadiness=[]` (D1 vazio sem sync-seed — comportamento correto, precisa seed) |
| `POST /admin/auth/bootstrap` sem secrets configurados | 500 tratado, `{"error":"Admin bootstrap not configured."}` — nunca crash cru |
| `POST /admin/auth/bootstrap` com `.dev.vars` configurado | 201, admin criado |
| `POST /admin/auth/login` senha correta | 200 + `Set-Cookie: session=...` + headers CORS |
| `POST /admin/auth/login` senha errada | 401 `{"error":"Invalid credentials."}` |
| `POST /admin/games/sync-seed` | 200, `syncedGames=6, syncedProfiles=4` |
| `POST /diagnostic/evaluate` (após sync-seed) | `gameReadiness` com os 4 perfis reais, status "bom" |
| `POST /admin/providers/sync-seed` | 200, `synced=0` — **achado**: `PROVIDER_DIRECTORY_SEED_JSON="[]"` no `wrangler.toml` sobrescreve o fallback pros seeds embutidos (`SEEDED_PROVIDERS`) com array vazio real. Ver nota em `README.md`; fora do escopo de #954-#961, registrar issue de follow-up. |
| `POST /ingest/provider-detection` payload `null` | 400 tratado |
| `POST /ingest/provider-detection` `asn` string | 400 tratado |
| `POST /ingest/provider-detection` válido com `installationHash` | 202, `eligibleForEnrichment=false` (1 install só) |
| `OPTIONS /admin/providers/review-queue` | 204 + headers CORS completos |
| `POST /admin/diagnostic/rulesets` + `/publish` (ruleset customizado v8) | 201 / 200 |
| `POST /diagnostic/evaluate` após publish | `evaluationSource=REMOTE`, `engineVersion=4`, finding `custom_latency` do ruleset publicado (prova que o D1 tem precedência real) |
| `POST /admin/diagnostic/rulesets/8/rollback` | 200 |
| `POST /diagnostic/evaluate` após rollback (sem published) | `evaluationSource=BUNDLED_LOCAL` (volta pro bundled corretamente) |
| `POST /diagnostic/evaluate` (fiber.rxPowerDbm=-30, com ruleset bundled) | `fiber_rx_power_critical` presente, copy em 2ª pessoa ("A leitura da sua fibra..."), score=35 com teto `TETO_FIBRA_RX_CRITICA=35` aplicado (score bruto ponderado 54 → capado em 35) |
| `POST /diagnostic/evaluate` (mobile 5G rsrp=-115 sinr=-2) | `mobile_signal_poor_5g`, score=67 (dimensão sinalMovel=40 "ruim" pesando 30% em conexão MOVEL) |
| `POST /diagnostic/evaluate` (wifi 2.4GHz rssi=-85, download=10) | `wifi_signal_critical_24ghz`, score=57 |
| `GET /admin/providers/review-queue` sem cookie | 401 |

Ambiente derrubado (`Stop-Process`) e `.wrangler/`/`.dev.vars` removidos ao final — não commitados.

## O que precisou de ajuste depois de testar

1. **Score da fixture "saudavel_monitorar"**: o teste original esperava `score >= 90` com a fórmula
   linear antiga (que saturava em 100 sem nenhum finding). Com o score ponderado real
   (`ScoreEngine.kt`), RSSI -57dBm/5GHz e bufferbloat de 8ms caem em "bom" (não "excelente"),
   gerando score ~87 — correto e esperado. Ajustada a fixture pra métricas realmente excelentes
   (RSSI -50, bufferbloat delta <5ms) pra manter a asserção `>=90` válida e continuar testando o
   fluxo `saudavel_monitorar` de forma significativa.
2. **Fila de review de provedores**: o teste original dependia do bug de #956 (MAX(x,3) fabricado)
   pra passar — 5 hits do mesmo device (sem `installationHash`) já enfileiravam. Corrigido pra usar
   3 `installationHash` distintos, provando o comportamento real pós-fix; adicionado teste negativo
   simétrico (mesmo hash repetido nunca enfileira sozinho).
3. **`evaluationSource` ausente no payload público**: durante a implementação percebi que
   `DiagnosticReportPayload` (o que `/diagnostic/evaluate` de fato retorna) nunca expunha
   `evaluationSource` — o campo existia só no `DiagnosticResult` interno, descartado na conversão
   pro payload de resposta. Corrigir só o motor (#954) sem propagar pro payload público deixaria o
   bug "consertado por baixo" mas invisível pro consumidor real (app/Console). Adicionado o campo
   em `DiagnosticReportPayload` e testado via HTTP real (ver seção anterior).
4. **`compatibility_date` incompatível com o `wrangler` local**: bloqueava `wrangler dev --local`
   inteiro (nem o `/health` respondia). Ajustado pro mesmo valor já usado por dois workers irmãos.
5. **`buildDiagnosticReport` precisou de um parâmetro novo (`gameProfiles`)**: pra classificar
   `gameReadiness` contra o catálogo real sem o módulo de report acessar D1 diretamente, o caller
   (`index.ts`) busca os perfis via `listGameProfiles(env)` e passa como argumento — mantém
   `diagnostic-report.ts` puro/testável sem mock de D1.

## Decisões de arquitetura registradas nesta PR

- **CORS (#960)**: aplicado globalmente (não só `/admin/*`), replicando o padrão já usado em
  `signallq-admin-worker` (`ALLOWED_ORIGIN` + `corsHeaders()`), em vez de inventar um padrão
  parcial novo. Ver README.md § CORS.
- **`diagnostic_divergences` (#961 item 4)**: removida da migration 001 — schema morto, nunca
  usado em nenhum código, worker nunca deployado com `database_id` real (sem dado em produção pra
  migrar). Se um dia for necessário registrar divergência local-vs-remoto, recriar com wiring
  completo (endpoint + call site), não só o schema solto.
- **Bufferbloat threshold (#955)**: os valores da regra declarativa (`bundled-ruleset.ts`) foram
  ajustados numericamente (30ms/100ms) comparando `loadedLatencyMs` bruto ao threshold, igual já
  fazia antes — não foi implementado o cálculo de delta (`loadedLatencyMs - latencyMs`) na regra
  declarativa porque o motor de regras só compara campo-vs-valor-estático, não campo-vs-campo. Já o
  **score engine novo** (`score-engine.ts`) usa o delta real, mais fiel ao `MetricClassifier.kt`.
  Documentado inline no código.
- **Dimensões "fibra" e "velocidade" do score engine**: `MetricClassifier.kt`/`ScoreEngine.kt` não
  documentam tabelas de faixa pra saúde óptica (RX) nem pra download/upload — são extrapolações
  documentadas a partir dos thresholds já usados em `bundled-ruleset.ts` (`FIBER_RX_POWER_LOW=-27dBm`,
  `DOWNLOAD_LOW=25Mbps`). Ver comentário no topo de `src/score-engine.ts`.

---

# TESTING.md — validação GH#962/#965 (client Android + diretório remoto de operadora)

PR empilhada sobre `feat/953-worker-diagnostico-integracao` (depende do merge da #964 antes).
Cobre client Android novo (`RemoteDiagnosticRepository`, `ProviderDirectoryRepository`,
`OperadoraDirectoryResolver`) e os dois endpoints admin novos do worker (upload de logo via R2,
edição parcial de `ProviderSupport`).

## Suíte automatizada do worker

```
npm run verify
```

Resultado: **64/64 testes passando** (55 pré-existentes + 9 novos desta PR), `tsc --noEmit` limpo.

Novos testes (`test/index.test.ts`):

| # | Cenário | Esperado | Obtido |
|---|---|---|---|
| 1 | Edição parcial de `support` (só `websiteUrl`) | `sacPhone` cadastrado antes continua intocado | ✅ |
| 2 | Edição de `support` com valor `null` explícito | remove o canal (não só ignora) | ✅ |
| 3 | Edição de `support` de provedor inexistente | 404 tratado | ✅ |
| 4 | Edição de `support` sem cookie de sessão | 401 | ✅ |
| 5 | Upload de logo sem binding `PROVIDER_LOGOS` | 501 tratado, nunca 500 cru, nunca finge sucesso | ✅ |
| 6 | Upload de logo com R2 (`FakeR2Bucket`) configurado | grava objeto, `ProviderLogo.url` resolve pro asset real | ✅ |
| 7 | Upload de logo de provedor inexistente (com R2 configurado) | 404 | ✅ |
| 8 | Upload de logo com Content-Type não-`image/*` | 400 | ✅ |
| 9 | (suíte pré-existente) nenhuma regressão | 55/55 mantidos | ✅ |

## Suíte automatizada do Android (JVM/Robolectric)

```
gradlew.bat :featureDiagnostico:testDebugUnitTest :app:testDebugUnitTest
```

Resultado: **`featureDiagnostico` 415/415** e **`app` suíte completa** passando, sem regressão.
Novos testes cobrindo GH#962/#965 (via `MockWebServer` e `mockk`, sem depender de processo externo
no CI):

| Arquivo | Cenários |
|---|---|
| `DiagnosticSnapshotMapperTest` | Mapeamento `DiagnosticInput` -> JSON do contrato remoto: snapshot vazio, banda 2.4/5GHz, speed/quality/loadedLatencyMs derivado, `hasInternet=false` quando não há nenhuma métrica de velocidade, dns/fibra/mobile/histórico presentes, fibra "down" sem métrica óptica não é enviada |
| `RemoteDiagnosticReportMapperTest` | Mapeamento 1:1 dos buckets de `DiagnosticResult`, score com dimensões remotas como `EvidenceScore` informativo, `perfisUso`/`gameReadiness` sempre vazios no mapper (calculados localmente pelo caller), status desconhecido cai pra `inconclusive`, `scoreEngineResultado` ausente mapeia pra `null` |
| `RemoteDiagnosticRepositoryTest` | Resposta 200 válida usa relatório remoto (com `perfisUso`/`gameReadiness` locais mesclados); HTTP 500, corpo vazio, JSON inválido e conexão derrubada (`SocketPolicy.DISCONNECT_AT_START`) caem pro motor local sem travar; `evaluateRemote` isolado retorna `null` em qualquer falha |
| `ProviderDirectoryRepositoryTest` | `findById` mapeia logo+contato; 404 retorna `null`; `searchByName` pega o primeiro item da busca; sem resultado retorna `null`; nome em branco nunca dispara chamada de rede; worker fora do ar (porta sem listener) retorna `null` sem exceção |
| `OperadoraDirectoryResolverTest` (`:app`) | Os 3 níveis de fallback: operadora principal 100% local (nunca chama o repositório remoto); operadora móvel principal via `resolverMovel`; operadora só no diretório remoto (logo+contato); remoto encontrado mas sem `logoUrl` não inventa logo (cai pro fallback); nada encontrado em lugar nenhum (fallback genérico, `hasAnyContact=false`); nome nulo/em branco nunca chama rede; falha do repositório remoto nunca lança exceção |

## Validação manual via HTTP real (`wrangler dev --local` + D1 local)

Rodado com `npx wrangler dev --local --port 8791` após aplicar as 5 migrations em D1 local. Todas
as chamadas abaixo foram feitas com `curl` contra o worker real rodando localmente — não mock.

| Chamada | Resultado |
|---|---|
| `GET /health` | 200, lista de rotas inclui `/admin/providers/:providerId/support` e `/admin/providers/:providerId/logo` |
| `POST /admin/auth/bootstrap` + `/admin/auth/login` | 201 / 200 + `Set-Cookie` |
| `POST /admin/providers` (upsert `regional_wrangler_dev` com `sacPhone`+`websiteUrl`) | 201, `syncedIdentifiers=2` |
| `GET /providers/regional_wrangler_dev` | 200, `support.sacPhone="0800111222"`, `support.websiteUrl="https://old.example.com"` |
| `PUT /admin/providers/regional_wrangler_dev/support` (só `websiteUrl`+`whatsappUrl`) | 200 |
| `GET /providers/regional_wrangler_dev/support` após a edição | `sacPhone` continua `"0800111222"` (intocado), `websiteUrl` e `whatsappUrl` atualizados — prova a edição parcial real, não só no fake D1 |
| `PUT /admin/providers/nao_existe_de_verdade/support` | 404 |
| `PUT /admin/providers/.../support` sem cookie | 401 |
| `POST /admin/providers/regional_wrangler_dev/logo` sem R2 configurado | 501, `{"error":"R2 bucket not configured..."}` — nunca finge sucesso |
| `POST /admin/providers/.../logo` com `Content-Type: application/json` (inválido) | 400 |
| `GET /providers/search?q=Regional` | 200, retorna `regional_wrangler_dev` |
| `POST /api/diagnostic/evaluate` com o **payload exato produzido por `DiagnosticSnapshotMapper` (Android)** — `wifi.band=2_4_GHZ`, `rssiDbm=-82`, `linkSpeedMbps=40`, `speed.downloadMbps=18`, `quality.latencyMs=60` | 200, `wifi_signal_critical_24ghz` + `wifi_link_very_slow` + `upload_low`/`download_low` — prova que o contrato JSON do client Android é aceito e interpretado corretamente pelo motor real, não só validado contra o mock do `RemoteDiagnosticReportMapperTest` |

Ambiente derrubado (`Stop-Process` nos processos `workerd`) e `.wrangler/`/`.dev.vars` removidos ao
final — não commitados.

## Decisões de arquitetura registradas nesta PR

- **Estratégia local vs. remoto (#962)**: `RemoteDiagnosticRepository.evaluate()` tenta o worker com
  timeout curto (connect 3s / read 4s / write 3s, teto adicional de 5s) e cai pro motor local
  (`DiagnosticRunner.run`) em qualquer falha — sem rede, timeout, HTTP não-2xx, corpo vazio ou JSON
  inválido. **Não foi wireada no `DiagnosticOrchestrator`** (o fluxo principal do app continua 100%
  local, síncrono) — isso exigiria tornar o fluxo de diagnóstico assíncrono, mudança de Composable/
  ViewModel fora do escopo desta issue (que pede explicitamente "sem alteração de nenhuma
  Composable/tela"). Fica pronta para adoção numa issue futura.
- **`perfisUso`/`gameReadiness` sempre locais, mesmo com relatório remoto**: o worker expõe versões
  simplificadas desses dois campos (perfil de uso deriva só do score geral; `gameReadiness` usa 4
  perfis de catálogo remoto — `COMPETITIVE_EXTREME`/`COMPETITIVE`/`SPORTS_COMPETITIVE`/
  `MULTIPLAYER_MODERATE` — que não correspondem às 3 categorias locais `FPS_COMPETITIVO`/
  `CLOUD_GAMING`/`MOBILE_COMPETITIVO` do `GameReadinessClassifier`). Forçar essa correspondência
  inventaria dado. `RemoteDiagnosticRepository` sempre recalcula os dois localmente a partir do
  mesmo `DiagnosticInput` usado pro snapshot remoto — são puros/determinísticos, não dependem de
  rede. Documentado no kdoc de `RemoteDiagnosticReportMapper`.
- **Score remoto não é recombinado pelo `ScoreEngine` local**: as dimensões que o worker expõe em
  `scoreEngineResultado.dimensoes` usam ids simplificados ("internet"/"wifi"/"dns"/...) diferentes da
  taxonomia interna do `ScoreEngine.kt` local ("estabilidade"/"wifiRedeLocal"/...). O `score` final já
  vem pronto do worker; as dimensões viram `EvidenceScore` só informativo.
- **R2 pendente (#965)**: a conta Cloudflare usada neste projeto ainda não tem R2 habilitado
  (`wrangler r2 bucket list` → "Please enable R2 through the Cloudflare Dashboard", code 10042,
  verificado em 2026-07-14). O binding `[[r2_buckets]]` fica comentado em `wrangler.toml` com o
  passo a passo de ativação manual. `uploadProviderLogo` retorna 501 tratado enquanto isso — nunca
  finge sucesso. **Ação humana pendente**: habilitar R2 no dashboard + `wrangler r2 bucket create
  signallq-provider-logos` + descomentar o binding + configurar `PROVIDER_LOGO_PUBLIC_BASE_URL`.
- **Endpoint de edição de `support` é parcial, não substitui `upsertProvider`**: `PUT
  /admin/providers/:id/support` só edita os campos de contato presentes no payload — chave ausente
  não mexe em nada, chave com valor `null`/vazio remove o canal. Criado como endpoint dedicado (em
  vez de forçar o admin a reenviar o provider inteiro toda vez que só quer trocar um telefone).
- **Resolução de operadora (`OperadoraDirectoryResolver`, `:app`) não foi wireada nos Composables
  existentes** (`OperadoraBadge`, `OperadoraContactCard`, `OperadoraBottomSheet`): esses componentes
  hoje consomem `ContatoOperadora`/`OperadoraVisualIdentity` de forma síncrona; o resolver introduz
  chamada suspensa (rede), que exigiria estado de loading na UI — mudança de Composable/ViewModel
  fora do escopo desta issue (mesmo princípio do #962: só camada de dados). Fica pronto para adoção
  futura pela tela que hoje já usa `BancoOperadoras.resolver`/`resolverMovel` diretamente
  (`MainViewModel.kt:1791`).
