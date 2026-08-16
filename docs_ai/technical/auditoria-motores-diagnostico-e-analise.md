# Auditoria — motores de diagnóstico e análise (Android + Cloudflare Workers)

- **Status:** ativo
- **Última validação:** 2026-08-05
- **Fonte de verdade:** este documento é um retrato pontual (auditoria), não um documento vivo de arquitetura — para o estado corrente, sempre recruzar com o código (`android/`, `integrations/cloudflare/`) antes de agir sobre qualquer achado aqui
- **Escopo:** monorepo `signallq` (Android + Workers Cloudflare) e interconexão com `signallq-web` e `buildea-admin`
- **Responsável:** levantamento assistido por IA a pedido de Luiz; sem dono de processo designado

Baseado em leitura direta do código-fonte (não de documentação — ver seção 4 sobre contratos desatualizados).

## 1. Motores no app Android (`signallq/android`)

### `core/diagnostico` — motor local, sempre autoritativo

| Motor | Descrição |
|---|---|
| `DiagnosticRunner` | Orquestrador puro: chama todos os engines de dimensão, agrega via `FindingEngine`, pontua via `ScoreEngine`, devolve `DiagnosticReport`. É o "motor embarcado" final de qualquer fallback. |
| `FindingEngine` | Desempata causas concorrentes (`score = severidade × confiança`), marca hipóteses descartadas. |
| `ScoreEngine` + `ScoreEvidenceBuilder` | Nota 0–100 por dimensão, pesos por tipo de conexão, "tetos" por métrica crítica isolada. |
| `WifiSignalQualityEngine`, `InternetDiagnosticEngine`, `DnsDiagnosticEngine`, `FibraSignalQualityEngine`, `MobileSignalDiagnosticEngine`, `HistoricalDegradationEngine`, `WifiChannelDiagnosticEngine` | Um por dimensão (Wi-Fi, internet, DNS, fibra/GPON, sinal móvel, tendência histórica, canal Wi-Fi). |
| `MetricClassifier` | Fonte única de thresholds — mas **`SinalScreen.kt` ainda não foi migrada** para usá-lo (thresholds duplicados lá). |
| `UsageProfileClassifier` / `GameReadinessClassifier` | Perfis de uso (streaming/jogos/trabalho) e prontidão para jogos — vocabulários deliberadamente separados. |
| `DiagnosticoGuiadoEngine` / `ModoGamerEngine` | Fluxos guiados por objetivo do usuário; única fonte do veredito (IA só explica, nunca altera). |

### `feature/diagnostico` — orquestração e ponte remota

| Motor | Descrição |
|---|---|
| `DiagnosticOrchestrator` | Monta input, chama `RemoteDiagnosticRepository.evaluateShadow()`, publica `StateFlow`. |
| `RemoteDiagnosticRepository` | Cliente do worker `signallq-diagnostic`. Modo produção = **shadow**: motor local sempre decide; avaliação remota roda em paralelo só para telemetria de divergência. |
| `RecomendacaoPraticaEngine` | Gera recomendações práticas (REC-01..14) a partir do achado principal. Renomeado no passado por colidir com o motor abaixo. |
| ~~`SignallQOrchestrator` (pulse)~~ | **Removido em GH#1682** (posterior a esta auditoria) — era a camada de chat/IA, sem nenhum consumidor de UI. O laudo em linguagem natural hoje vem de `MainViewModel.analisarProblema()` chamando `AiDiagnosisRepository` direto, sem orquestrador. |

### `core/recommendation` — motor separado, propósito comercial (não técnico)

`RecommendationEngine`: decide qual card mostrar (dica > afiliado > oferta > anúncio), 100% local, catálogo embarcado. Já foi desambiguado por nome do `RecomendacaoPraticaEngine`.

### `core/network`

`ConnectivityDiagnosisEngine` (gateway→DNS→rota externa, detecta captive portal), `TopologiaRedeEngine` (classifica papel de vizinhos Wi-Fi — **já substituiu 3 motores concorrentes antigos**, consolidação documentada), `ClassificadorSaudeGpon` (ITU-T G.984).

**Não são motores concorrentes ativos hoje** — as duas duplicidades óbvias (`SignallQOrchestrator`/`DiagnosticOrchestrator` e os dois `RecommendationEngine`) já foram resolvidas por composição/renomeação em auditorias anteriores. Achado não solicitado: `UptimeNarrativaEngine` (`feature/history`) não tem consumidor de produção — parece código morto ou feature não plugada.

## 2. Motores remotos (Cloudflare Workers)

| Worker | Nome real | Função | Storage |
|---|---|---|---|
| `ai-diagnosis-worker` | `linka-ai-diagnosis-worker` | Laudo em linguagem natural via LLM (Gemini 2.0 Flash → fallback Qwen3/Workers AI) | nenhum |
| `game-latency-probe-worker` | `signallq-game-latency-probe` | Sonda RTT regional, sem lógica | nenhum |
| `signallq-admin-worker` | `signallq-admin` | Backend do painel (métricas, feature flags, Firebase/Google Play, releases) | D1 |
| `signallq-diagnostic-worker` | `signallq-diagnostic` | Motor determinístico remoto + diretório de operadoras + catálogo de jogos + log de divergência local×remoto | D1 |
| `signallq-privacy-worker` | `signallq-privacy` | Página estática de privacidade | nenhum |

## 3. Interconexão multi-plataforma (confirmada por grep cruzado)

| Worker | Android | signallq-web | buildea-admin |
|---|---|---|---|
| ai-diagnosis | Sim | — | — |
| game-latency-probe | Sim | Sim (direto do browser) | — |
| admin | Sim (ingest + flags públicas) | Sim (proxy server-side) | Sim (é a UI do worker) |
| diagnostic | Sim | Sim (proxy server-side) | — |
| privacy | Sim (link) | — | — |

Não é arquitetura Android-exclusiva: `signallq-web` já consome 3 dos 5 workers e `buildea-admin` é literalmente o frontend do `signallq-admin-worker`.

**O ponto mais importante do audit:** não existe um "motor universal" único. O motor de diagnóstico existe **duplicado** — uma implementação em Kotlin (`core/diagnostico`, autoritativa, roda no device) e uma em TypeScript (`diagnostic-engine.ts`, roda no worker). Elas são mantidas em paralelo via **shadow mode**: o worker roda em paralelo só para comparação, e diferenças viram eventos em `diagnostic_divergences` (D1). Ou seja, o app já assume que os dois podem divergir e mede isso — não tenta fingir que é um único contrato.

## 4. Local vs. remoto — necessidade de sync

- **Diretório de operadoras** (`ProviderDirectoryCacheEntity`): cache pull-based com TTL vindo do worker, sem WorkManager.
- **Analytics** (`AnalyticsOutboxEntity`): fila outbox local com retry/backoff, unidirecional (app→worker).
- **Feature flags**: pull sob demanda + fallback hardcoded em memória.
- **Não há sync bidirecional nem push (FCM) real** entre Room e D1. Isso é adequado para o perfil do app (diagnóstico é local-first por natureza — não pode depender de rede para funcionar), mas os 2 contratos OpenAPI "aspiracionais" (`analytics-events`, `integrations-api`) descrevem rotas que **nunca existiram** no código — são fictícios, não desatualizados por deriva.

### Contratos OpenAPI vs. código real (`docs_ai/CONTRATOS/openapi/`)

| Arquivo | Situação |
|---|---|
| `ai-diagnosis-worker.yaml` | Sincronizado nas rotas existentes, mas não documenta `?stream=true` nem o "modo chat" (`feedbackUsuario`) |
| `game-latency-probe-worker.yaml` | Sincronizado |
| `signallq-admin-api.yaml` | Desatualizado e incompleto — dezenas de rotas reais não documentadas (`/admin/local-ads*`, `/admin/app-updates*`, `/admin/system-health*`, `/admin/cloudflare-usage`, integrações Google Play/Firebase, `/ingest/waitlist`, endpoints públicos `/feature-flags`, `/flags`, `/local-ads`, `/app-updates`) |
| `signallq-diagnostic-worker.yaml` | Bem sincronizado |
| `signallq-analytics-events.yaml` | Fictício — servidor e rotas documentadas não correspondem ao código real (`/ingest/analytics`, `/ingest/diagnostic`, `/ingest/ai-usage`) |
| `signallq-integrations-api.yaml` | Fictício — mesmo padrão, rotas reais vivem sob `/admin/integrations/*` no admin-worker |
| `signallq-privacy-worker.yaml` | Sincronizado |

## 5. Autenticação de API — estado real

| Mecanismo | Onde | Avaliação |
|---|---|---|
| Sessão PBKDF2 (100k) + token opaco SHA-256, D1, 7 dias | Login humano do painel (admin + diagnostic worker) | Adequado, implementado corretamente, **duplicado byte-a-byte entre 2 workers** (dívida de manutenção, não de segurança) |
| `INGEST_KEY` fixa embutida no APK | Ingest de telemetria Android→admin-worker | Não é segredo real — qualquer APK pode ser decompilado. Funciona como chave de baixo atrito, não como autenticação forte |
| `SITE_INGEST_KEY` | Ingest via proxy server-side do site | Correto — nunca sai do servidor Next.js |
| `DIAGNOSTIC_PROXY_SECRET` via header interno | Worker-to-worker (admin→diagnostic) | Correto, contorna erro 1042 da Cloudflare entre `*.workers.dev` |
| Sem autenticação | `POST /ingest/provider-detection` e `POST /ingest/diagnostic-divergence` no diagnostic-worker | Gap real — qualquer client pode poluir a tabela de divergências ou o diretório de operadoras sem qualquer chave, diferente do padrão já aplicado no admin-worker |
| Sem auth (intencional) | `/health`, `/feature-flags`, `/flags`, `/local-ads`, `/app-updates`, `/diagnostic/evaluate`, `/providers/*`, `/games/catalog` | Correto — dado não sensível, precisa ser lido sem fricção |

## 6. Custos/hosting

Todos os 5 workers rodam em tier gratuito de Cloudflare (confirmado em `docs_ai/operations/INFRASTRUCTURE_COSTS.md`, validado 2026-07-23): 100k requests/dia, D1 5GB/5M reads-dia/100k writes-dia, 10k neurons/dia Workers AI. R2 foi deliberadamente descartado (exige cartão mesmo no free tier) — BLOBs de logo ficam em D1. Gatilhos de upgrade já documentados (>80k req/dia, >8k neurons/dia, >4M rows/dia D1).

## 7. Recomendação — Cloudflare vs. Supabase/Vercel/GitHub

**Manter Cloudflare.** Todo o backend roda em free tier hoje, com acoplamento nativo entre as peças (service bindings worker-to-worker, Pages Functions como proxy, D1 único). Migrar para Supabase trocaria D1→Postgres (ganho real em queries relacionais) mas perderia Workers AI grátis; migrar hosting para Vercel fragmentaria o proxy que já funciona via Cloudflare Pages. GitHub não é alternativa de runtime, só CI (já em uso). O único ganho real de Supabase seria auth gerenciada — resolvível extraindo `auth.ts` (hoje duplicado) para um pacote compartilhado dentro do próprio monorepo Cloudflare, sem trocar de provedor. Migração só se justificaria com necessidade real de queries relacionais complexas ou realtime — nenhum dos dois é o caso hoje.

## 8. Avaliação multi-plataforma / "motor universal"

Não existe hoje um único motor consumido igualmente por todas as plataformas — existe um motor local autoritativo (Android) com um espelho remoto (Worker) mantido em paralelo por shadow-mode + divergência registrada. Decisão correta para um app de diagnóstico de conectividade (não pode depender de rede para diagnosticar rede), não um problema a corrigir. O risco real é de manutenção: os dois motores (Kotlin e TypeScript) implementam a mesma regra em linguagens diferentes, e `MetricClassifier` (fonte única de threshold no Android) ainda não cobre `SinalScreen.kt` nem os thresholds do worker remoto — 3 lugares com risco de threshold divergente, não 2. Consolidar thresholds tem retorno maior que qualquer migração de infraestrutura.

## 9. Autenticação — recomendação objetiva

Adicionar pelo menos o mesmo Bearer de escopo limitado já usado no admin-worker aos dois endpoints de ingest sem auth do diagnostic-worker (`/ingest/provider-detection`, `/ingest/diagnostic-divergence`). É a única lacuna de segurança concreta encontrada; o resto do desenho é proporcional ao risco real dos dados envolvidos.
