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
`OperadoraDirectoryResolver`) e os dois endpoints admin novos do worker (upload de logo como BLOB
base64 direto no D1 — R2 descartado por decisão de produto em 2026-07-14, ver seção "Decisões de
arquitetura" abaixo —, edição parcial de `ProviderSupport`).

## Suíte automatizada do worker

```
npm run verify
```

Resultado: **65/65 testes passando** (55 pré-existentes + 10 novos desta PR), `tsc --noEmit` limpo.

Novos testes (`test/index.test.ts`):

| # | Cenário | Esperado | Obtido |
|---|---|---|---|
| 1 | Edição parcial de `support` (só `websiteUrl`) | `sacPhone` cadastrado antes continua intocado | ✅ |
| 2 | Edição de `support` com valor `null` explícito | remove o canal (não só ignora) | ✅ |
| 3 | Edição de `support` de provedor inexistente | 404 tratado | ✅ |
| 4 | Edição de `support` sem cookie de sessão | 401 | ✅ |
| 5 | Upload de logo grava BLOB base64 no D1 (sem R2) | 201, `ProviderLogo.url` resolve pra rota própria do worker (`GET /providers/:id/logo`) | ✅ |
| 6 | `GET /providers/:id/logo` serve o binário gravado no D1 | 200, bytes idênticos aos enviados no upload, `content-type` = o mesmo do upload (`image/png`) | ✅ |
| 7 | `GET /providers/:id/logo` de provedor sem logo com blob | 404 tratado, nunca 500 cru | ✅ |
| 8 | Upload de logo acima de 500KB (tamanho razoável de logo aceito) | 413 tratado, nunca 500 cru, nunca finge sucesso | ✅ |
| 9 | Upload de logo de provedor inexistente | 404 | ✅ |
| 10 | Upload de logo com Content-Type não-`image/*` | 400 | ✅ |
| 11 | (suíte pré-existente) nenhuma regressão | 55/55 mantidos | ✅ |

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

## Validação manual via HTTP real — GH#965 revisado (BLOB D1, sem R2), 2026-07-14

R2 foi descartado por decisão de produto (Cloudflare exige cartão mesmo no tier grátis) enquanto a
PR #966 ainda estava com CI vermelho de Ktlint. Este round reaplica as 6 migrations (incluindo a
nova `006_gh965_provider_logo_d1.sql`) em D1 local e valida o fluxo de logo ponta a ponta contra o
worker real via `npx wrangler dev --local` — não só a suíte `node --test` com `FakeD1Database`.

| Chamada | Resultado |
|---|---|
| `npx wrangler d1 execute signallq-diagnostic-db --local --file=migrations/006_gh965_provider_logo_d1.sql` | Aplicada sem erro sobre as 5 migrations anteriores |
| `POST /admin/auth/bootstrap` + `/admin/auth/login` | 201 / 200 + `Set-Cookie` |
| `POST /admin/providers` (cria `regional_manual_teste`) | 201 |
| `POST /admin/providers/regional_manual_teste/logo` com 13 bytes reais (`Content-Type: image/png`) | 201, `url` = `https://signallq-diagnostic.giammattey-luiz.workers.dev/providers/regional_manual_teste/logo` (URL absoluta, sem R2) |
| `GET /providers/regional_manual_teste/logo` | 200, `content-type: image/png`, `content-length: 13` — bytes comparados via `cmp` contra o arquivo original enviado: **idênticos** |
| `GET /providers/regional_manual_teste` | 200, `logo.url` aponta pra própria rota do worker, `logo.version=1` |
| `POST /admin/providers/regional_manual_teste/logo` com 586KB (`/dev/urandom`) | 413, `{"error":"Logo too large. Max 500KB, got 586KB."}` — nunca 500 cru |
| `GET /providers/nao_tem_logo_nenhuma/logo` (provedor sem blob no D1) | 404 |

Ambiente derrubado (processo `wrangler dev` finalizado por PID) e `.wrangler/`/`.dev.vars` removidos
ao final — não commitados.

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
- **R2 descartado, logo vive no D1 (#965, decisão revisada 2026-07-14)**: a ideia original desta PR
  era hospedar a logo em R2 (a conta Cloudflare usada neste projeto não tinha R2 habilitado —
  `wrangler r2 bucket list` → "Please enable R2 through the Cloudflare Dashboard", code 10042). Antes
  de habilitar R2, o Luiz decidiu descartar essa rota: a Cloudflare exige cartão de crédito
  cadastrado pra habilitar R2 mesmo no tier grátis, fricção que não vale a pena pagar agora pra um
  asset pequeno (poucos KB, volume baixo de operadoras de cauda longa). A logo agora é gravada como
  BLOB base64 direto no D1 já usado pelo worker (`provider_assets.data_base64`/`content_type`,
  migration `006_gh965_provider_logo_d1.sql` — só 2 colunas nullable adicionadas, menor migration
  possível) e servida pela própria rota `GET /providers/:id/logo` do worker. Sem binding R2 em
  `wrangler.toml`. Upload limitado a 500KB (`MAX_LOGO_BYTES`) — retorna 413 tratado acima disso,
  nunca 500 cru. `test/fake-r2.ts` foi removido (sem mais nenhum uso de R2 no projeto).
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

  > **Atualizado em #970 (ver seção mais abaixo)**: essa adoção aconteceu — os 3 componentes citados
  > acima agora consomem a cadeia local -> remoto -> fallback.

---

# TESTING.md — validação GH#971/#969/#970 (fix seed embutido + wiring remoto no app)

PR aberta a partir de `origin/main` (já com #964/#966 mergeadas). Resolve os 3 achados documentados
como follow-up nas seções anteriores deste arquivo: seed vazio sobrescrevendo o catálogo embutido
(#971), motor de diagnóstico remoto nunca chamado pelo fluxo real (#969), e diretório remoto de
operadora nunca chamado pela UI (#970). **Issue #967 (deploy real em produção) fica fora deste PR.**

## #971 — fix do wrangler.toml + reforço no código

**Causa raiz dupla**, as duas corrigidas:
1. `wrangler.toml` tinha `PROVIDER_DIRECTORY_SEED_JSON = "[]"` / `GAME_CATALOG_SEED_JSON = "[]"` /
   `GAME_PROFILE_SEED_JSON = "[]"` — string **não vazia**, então `if (!env.X)` (usado pra decidir se
   cai no catálogo embutido) nunca disparava. Corrigido pra `""` (vazio), consistente com
   `DIAGNOSTIC_RULESET_JSON` (que já estava certo).
2. `loadSeedProviders` em `provider-directory.ts` (diferente de `loadSeedCatalog`/`loadSeedProfiles`
   em `game-catalog.ts`, que já faziam isso) não tratava array vazio parseado como "sem seed real" —
   `Array.isArray(parsed) ? parsed : SEEDED_PROVIDERS` retornava `[]` mesmo sendo array válido vazio.
   Corrigido pra `Array.isArray(parsed) && parsed.length > 0 ? parsed : SEEDED_PROVIDERS`, mesmo
   padrão do `game-catalog.ts`. Isso é defesa em profundidade: mesmo que a var volte a ser configurada
   como `"[]"` por engano no futuro, o código não sobe vazio.

### Suíte automatizada

```
npm run verify
```

Resultado: **67/67 testes passando** (65 pré-existentes + 2 novos desta PR), `tsc --noEmit` limpo.

| # | Cenário | Esperado | Obtido |
|---|---|---|---|
| 1 | `PROVIDER_DIRECTORY_SEED_JSON="[]"` (config antiga do wrangler.toml, simulada explicitamente no teste) | cai no catálogo embutido (`brisanet` via `by-asn/28126`) | ✅ |
| 2 | `GAME_CATALOG_SEED_JSON="[]"` + `GAME_PROFILE_SEED_JSON="[]"` (idem) | cai no catálogo embutido de jogos (`valorant` presente, filtro por plataforma funciona) | ✅ |

### Validação manual via HTTP real (`wrangler dev --local` + D1 local), 2026-07-14

Migrations 001-006 reaplicadas em D1 local limpo. `wrangler.toml` já com a correção (vars vazias).
`.dev.vars` temporário criado só para bootstrap admin local (`ADMIN_AUTH_PEPPER`/
`ADMIN_BOOTSTRAP_TOKEN`), removido ao final — nunca commitado.

| Chamada | Resultado |
|---|---|
| `GET /providers/by-asn/28126` (D1 vazio, sem sync-seed) | 200, `brisanet` — cai no catálogo embutido pela via de leitura direta (`getProviderByAsn` → D1 sem match → `loadSeedProviders`), nem precisa de sync-seed explícito |
| `GET /providers/search?q=claro` (D1 vazio) | 200, `claro` — mesma via de fallback direto |
| `POST /admin/providers/sync-seed` | `{"ok":true,"synced":2}` — sincroniza os 2 provedores do catálogo embutido (`claro`, `brisanet`) pro D1; **antes da correção, este endpoint documentava `synced:0`** (ver achado registrado na seção GH#953 acima) |
| `POST /api/diagnostic/evaluate` (payload 2.4GHz + 5GHz disponível) | 200, `evaluationSource=BUNDLED_LOCAL`, `wifi_24ghz_slow_with_5ghz_available` — confirma que o endpoint que o Android chama (`RemoteDiagnosticRepository`) responde corretamente |
| `POST /admin/games/sync-seed` | `{"ok":true,"syncedGames":6,"syncedProfiles":4}` — catálogo embutido de jogos (6 jogos) sincronizado corretamente, confirma fix do `GAME_CATALOG_SEED_JSON`/`GAME_PROFILE_SEED_JSON` também na via de sync explícito |
| `GET /games/catalog?platform=PC` (após sync) | 200, 3 itens (Fortnite entre eles) filtrados por plataforma |
| `POST /admin/providers` (cria `regional_teste_pr971`, provedor que NUNCA existe no catálogo local do app) | 201, `syncedIdentifiers=2` |
| `GET /providers/search?q=Regional%20Teste%20PR971` | 200, 1 item — prova o cenário "operadora só no diretório remoto" (#970) contra o worker real |
| `GET /providers/search?q=vivo` | 200, `items:[]` — correto: "Vivo" não está (nem deve estar) no catálogo embutido do worker, é 100% local no app (`BancoOperadoras`); o diretório remoto é só cauda longa |
| `GET /providers/search?q=Provedor%20Que%20Nao%20Existe%20Em%20Lugar%20Nenhum` | 200, `items:[]` — cenário "operadora em lugar nenhum" |

Ambiente derrubado (processos `wrangler`/`workerd` finalizados por PID) e `.wrangler/`/`.dev.vars`
removidos ao final — não commitados.

## #969 — motor de diagnóstico remoto ligado ao `DiagnosticOrchestrator`

`DiagnosticOrchestrator.executar` (agora `suspend`) passou a delegar inteiramente pra
`RemoteDiagnosticRepository.evaluate()`, que já implementava a estratégia remoto-primeiro/
fallback-local (GH#962). Nenhum call site precisou mudar além da assinatura `suspend` — todos já
rodavam dentro de `viewModelScope.launch`/`Flow.collect`/`withContext` (coroutines), confirmado por
`gradlew :featureDiagnostico:compileDebugKotlin :app:compileDebugKotlin` limpo.

### Suíte automatizada (JVM, `MockWebServer`)

```
gradlew.bat :featureDiagnostico:testDebugUnitTest :app:testDebugUnitTest
```

Resultado: **BUILD SUCCESSFUL, 0 falhas** em toda a suíte (`featureDiagnostico` + `app`). Novo arquivo
`DiagnosticOrchestratorTest.kt` (3 testes, via `MockWebServer` real — não mock de `RemoteDiagnosticRepository`):

| # | Cenário | Esperado | Obtido |
|---|---|---|---|
| 1 | Worker remoto responde 200 com JSON válido | `snapshotFlow` fica `concluido` com a `decisao.id` vinda do relatório remoto | ✅ |
| 2 | Worker indisponível (`SocketPolicy.DISCONNECT_AT_START`, timeout curto) | `snapshotFlow` fica `concluido` (nunca `erro`, nunca trava) com `decisao.id` do motor **local** (`DiagnosticRunner`), nunca o id fabricado do fixture remoto | ✅ |
| 3 | Worker responde 500 | idem — cai pro motor local sem exceção | ✅ |

## #970 — diretório remoto de operadora ligado à UI

Threading de callbacks (não `hiltViewModel()` — este app é 100% data-driven, `ViewModel`s só
existem na `Activity`; ver decisão abaixo): `MainActivity` injeta `OperadoraDirectoryResolver` via
Hilt e repassa 4 funções (`resolveOperadoraIdentidadeLocal`/`Remota`,
`resolveOperadoraContatoLocal`/`Remoto`) por `AppShell` → `HomeScreen`/`ResultadoVelocidadeScreen` →
`OperadoraBottomSheet`. Composable helper `rememberResolvedOperadoraIdentity`/
`rememberResolvedOperadoraContact` (`ui/component/ResolvedOperadoraState.kt`) resolve local primeiro
(síncrono, sem corrotina — zero mudança de comportamento pras ~12 operadoras principais) e só cai
pro `LaunchedEffect` assíncrono (diretório remoto) quando o catálogo local não tem match.

`OperadoraBadge` ganhou overload pra `ResolvedOperadoraIdentity` (renderiza `logoRes` bundled,
`logoUrl` remota via Coil `SubcomposeAsyncImage` com fallback pro badge de monograma em
loading/erro, ou monograma direto) sem alterar o overload antigo (`ContatoOperadora`, ainda usado
pela lista "outras operadoras", sempre local). `OperadoraContactCard` passou a receber
`identidade`+`contato` resolvidos (botão "Abrir na Play Store" só aparece quando
`ResolvedOperadoraContact.grupo != null`, hoje só preenchido pra `LOCAL`).

### Suíte automatizada

Coberta pela mesma suíte JVM acima. Testes específicos relevantes:

| Arquivo | Cobre |
|---|---|
| `OperadoraDirectoryResolverTest` (`:app`, pré-existente, sem alteração de comportamento) | Os 3 níveis (local, remoto, fallback) — refatoração interna (`resolveLocalIdentity`/`resolveLocalContact` extraídos como funções públicas síncronas) mantém os 7 testes verdes |

Não foi adicionado teste de Compose UI instrumentado — este projeto não tem esse padrão estabelecido
(`~66 arquivos de teste unitário` + `3 androidTest de Room/DAO`, nenhum teste de Compose).

### O que foi validado de fato, e o que NÃO foi

**Validado:**
- `gradlew :featureDiagnostico:compileDebugKotlin :app:compileDebugKotlin` — compila limpo com o
  novo threading de callbacks através de `AppShell`/`HomeScreen`/`ResultadoVelocidadeScreen`/
  `OperadoraBottomSheet`/`OperadoraBadge`/`OperadoraContactCard`.
- `gradlew :featureDiagnostico:testDebugUnitTest :app:testDebugUnitTest` — 0 falhas, incluindo
  `OperadoraDirectoryResolverTest` (os 3 níveis local/remoto/fallback na camada de dados que a UI
  consome, sem alteração de comportamento após a refatoração `resolveLocalIdentity`/
  `resolveLocalContact`).
- `gradlew :app:ktlintCheck` — limpo.
- Worker real (`wrangler dev --local` + D1 local) respondendo aos 3 cenários (local/remoto/fallback)
  que o `ProviderDirectoryRepository` do Android consome — ver seção #971 acima
  (`regional_teste_pr971`, `vivo`, nome inexistente).

**NÃO validado nesta PR** (honestidade sobre o limite real da evidência, não estimativa):
- Não rodei o app em emulador/dispositivo físico apontando pro worker local — não há emulador/
  dispositivo disponível neste ambiente de execução do agente. A integração ponta-a-ponta real
  (UI Compose renderizada → `OkHttp` real → worker local → D1 local) **não foi exercitada
  visualmente**, só por compilação + testes JVM (`Robolectric` não usado aqui, resolver é testado
  puro/sem Android runtime) + o worker validado isoladamente via `curl`.
- Recomendação pro Rhodolfo/QA: antes de aprovar, rodar o app debug num device/emulador real
  apontando `DIAGNOSTIC_WORKER_URL` pro worker local (ou produção após #967), navegar até uma tela
  com `OperadoraBadge`/`OperadoraContactCard`/`OperadoraBottomSheet` com uma operadora fora das ~12
  locais, e confirmar visualmente os 3 estados (local sem flicker, remoto com fallback depois
  atualizando, indisponível sem travar).

## Decisões de arquitetura registradas nesta PR

- **Sem `hiltViewModel()` em Composables leaf**: este app nunca usou esse padrão (todo `ViewModel` é
  instanciado só na `Activity` via `by viewModels()`, e `AppShell`/telas são 100% data-driven —
  parâmetros + lambdas, nunca acessam DI diretamente). Introduzir `hiltViewModel()` exigiria uma nova
  dependência (`androidx.hilt:hilt-navigation-compose`) e um padrão novo só pra esta feature — decisão
  conservadora: manter o padrão existente, injetar `OperadoraDirectoryResolver` na `MainActivity` e
  threadar como parâmetros/lambdas até os componentes finais.
- **Coil adicionado (`coil-compose:2.7.0`)**: única forma padrão de carregar `ResolvedOperadoraIdentity.logoUrl`
  (imagem remota) em Compose; não havia biblioteca de carregamento de imagem de rede no projeto antes
  (nenhuma tela carregava imagem remota até agora). Versão 2.x (não 3.x) por estabilidade/simplicidade
  de setup — sem necessidade de recursos exclusivos do Coil 3.
- **Local-primeiro sempre síncrono**: `resolveLocalIdentity`/`resolveLocalContact` (extraídos de
  `OperadoraDirectoryResolver`) são funções puras não-`suspend` — a resolução via `remember(...)` na
  primeira composição nunca introduz frame de loading pras ~12 operadoras já catalogadas, cumprindo
  literalmente "sem alteração de comportamento" pedido pela issue.
- **`ResolvedOperadoraContact.grupo` (campo novo, default `null`)**: só assim dava pra preservar o
  botão "Abrir na Play Store" pras 12 operadoras locais sem inventar esse dado pro diretório remoto
  (que não tem essa informação) — ver kdoc do campo em `OperadoraDirectoryResolver.kt`.
