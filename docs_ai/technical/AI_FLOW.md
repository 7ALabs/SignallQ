# AI Flow — Android SignallQ

**Status:** ativo
**Última validação:** 2026-07-23
**Fonte de verdade:** código real (`featureDiagnostico`, `integrations/cloudflare/ai-diagnosis-worker`) — este documento é derivado, não normativo
**Escopo:** fluxo de diagnóstico assistido por IA no app Android e o worker que o atende
**Responsável:** Camilo (Backend Android + Workers)

---

## 1. Objetivo técnico

Documentar como o app SignallQ envia dados de diagnóstico de rede a um Worker Cloudflare para
gerar um laudo assistido por IA, incluindo o fallback local quando a IA está indisponível.

## 2. Visão geral da solução

O app integra IA via **Cloudflare Worker** externo. Não há inferência local — todo o processamento LLM é feito no worker. O fallback local (`AiFallbackFactory`) entra apenas se o worker falhar ou timeout.

```
MainViewModel
    → DiagnosticOrchestrator.executar()
        → DiagnosticRunner.run(input)            [engines locais stateless]
        → DiagnosisAiContextFactory.fromRaw()    [monta payload — prompt diagnostico_v5_local_primary]
        → AiDiagnosisRepository.diagnosticar()  [POST HTTP via OkHttp]
            → linka-ai-diagnosis-worker          [Cloudflare Worker]
                → Gemini 2.0 Flash (primário) / Qwen3 30B MoE FP8 (fallback cloud)
                → resposta JSON
            → AiDiagnosisResult                 [parseado pelo app]
        → AiFallbackFactory                     [se timeout ou erro]
```

---

## 2. Endpoint

**URL:** `https://linka-ai-diagnosis-worker.giammattey-luiz.workers.dev/api/ai/diagnostico-conexao`

**Worker name (wrangler.toml):** `linka-ai-diagnosis-worker`

**Método:** POST

**Content-Type:** application/json

---

## 3. Modelo de IA

**Modelo do provider fallback (Cloudflare Workers AI):** `@cf/qwen/qwen3-30b-a3b-fp8` (Qwen3 30B MoE FP8). Provider primário é Gemini 2.0 Flash — ver seção "Fallback Gemini" abaixo.

Configurado em `wrangler.toml`:
```toml
AI_MODEL = "@cf/qwen/qwen3-30b-a3b-fp8"
```

e `DEFAULT_MODEL` no `src/index.ts`:
```ts
const DEFAULT_MODEL = "@cf/qwen/qwen3-30b-a3b-fp8";
```

**Provider primário Gemini:** com a secret `GEMINI_API_KEY` configurada, o worker usa Gemini (`providers.ts`, model id `gemini-flash-latest` — alias da Google que resolve para a versão Flash mais recente disponível, hoje Gemini 2.0 Flash) como provider primário e Qwen/CF como fallback automático. Sem a secret, Qwen/CF é o único provider cloud. Llama/Meta não é padrão nem fallback (política do projeto).

**Alternativas/legado (não são o padrão):**
- `@cf/google/gemma-7b-it` — Gemma v1, fraco para prompt complexo
- `@hf/google/gemma-2-9b-it` — formato incompatível com messages API
- `@cf/google/gemma-4-26b-a4b-it` — descartado (gerava timeout > 30s)

**Persona da IA:** "SignallQ"

---

## 4. Payload — Schema atual

Montado por `DiagnosisAiContextFactory.fromRaw()`. O worker aceita schemas anteriores para retrocompatibilidade.

A versão de prompt atual do worker é `diagnostico_v6_explicacao_humana` (`AI_PROMPT_VERSION` em `src/index.ts` — atualizado desde a v5 citada em versões anteriores deste documento): os achados do motor local são enviados como entrada e a IA refina/expande em cima deles. `schemaVersion` do contexto (`DiagnosisAiContext`) é enviado ao worker e registrado no evento `ia_laudo_solicitado`.

Campos enviados: tipo de conexão, snapshot Wi-Fi (RSSI, canal, frequência), latência, jitter, perda de pacotes, download/upload Mbps, DNS (servidor atual, latência), histórico (médias 7d/30d), dados do ISP, configuração do usuário (plano, operadora, estado/cidade).

---

## 5. Engines de Diagnóstico Local (DiagnosticRunner)

Executados antes da chamada à IA — produzem o relatório local que também alimenta o payload:

| Engine | Entrada | Saída |
|---|---|---|
| `WifiSignalQualityEngine` | RSSI, frequência, link speed | `WifiQualityResult` |
| `InternetDiagnosticEngine` | snapshot internet, flag wifi confiável | `DiagnosticResult` |
| `WifiChannelDiagnosticEngine` | redes vizinhas, canal conectado | `DiagnosticResult` |
| `DnsDiagnosticEngine` | IP DNS, latência, grade | `DiagnosticResult` |
| `HistoricalDegradationEngine` | médias 7d/30d, tendência | `DiagnosticResult` |
| `FibraSignalQualityEngine` | rxPowerDbm, txPowerDbm, temperatura | `DiagnosticResult` |
| `MobileSignalDiagnosticEngine` | RSRP, RSRQ, SINR, tecnologia | `DiagnosticResult` |
| `DiagnosticDecisionEngine` | resultados de todos os engines | `DiagnosticResult` (decisão final) |

Todos residem em `:featureDiagnostico`. São stateless — recebem dados brutos e retornam resultado sem efeitos colaterais.

---

## 6. Chat / Pulse (SignallQOrchestrator)

Fluxo conversacional pós-diagnóstico:

```
SignallQOrchestrator
    → SignallQState (enum: Idle, Collecting, Thinking, Analyzing, Done, Error)
    → SignallQSnapshot (data class — estado atual da sessão)
    → DynamicQuestionEngine (gera perguntas contextuais baseadas no estado da rede)
    → POST worker /api/ai/diagnostico-conexao (reutiliza contexto do diagnóstico)
```

Sessões persistidas em Room: `ChatSessionEntity` + `ChatMessageEntity` (tabelas adicionadas em v10/v0.12.0).

**Repository:** `ChatDiagnosticoIaRepository` — gerencia histórico de sessões.
**ViewModel:** `ChatDiagnosticoIaViewModel` — controla estado do chat.

---

## 7. Fallback Local

**Classe:** `AiFallbackFactory`

Ativado quando:
- Timeout na chamada ao worker
- Erro HTTP (5xx, network error)
- Sem internet

Retorna um `AiDiagnosisResult` construído a partir dos resultados dos engines locais, sem texto gerado por LLM.

---

## 8. Armazenamento de Resultados

- Diagnósticos: estado em `MainViewModel.snapshotDiagnostico` (StateFlow, não persistido em Room)
- Sessões de chat: `SignallQDatabase` — tabelas `chat_sessions` e `chat_messages`
- Cota/orçamento diário de IA: não há mecanismo de limite client-side no app (a classe
  `CotaIaRepository` citada em versões anteriores deste documento não existe no código). O
  controle real de orçamento é server-side, no Worker Admin: `aiDailyBudgetUsd` em
  `admin_settings`, que dispara o alerta `AI_BUDGET` quando o custo das últimas 24h excede o
  limite (ver `docs_ai/technical/admin-api-schema.md`, seção `/admin/settings`). Não bloqueia
  chamadas do app, só alerta o painel.

## 9. Riscos técnicos

- Sem cota client-side: um dispositivo com uso anômalo pode gerar custo de IA sem o app impedir
  localmente — a única salvaguarda hoje é o alerta `AI_BUDGET` no painel Admin (reativo, não
  preventivo).
- Fallback local (`AiFallbackFactory`) não gera texto explicativo por LLM — a experiência do
  usuário degrada para dados brutos dos engines quando a IA está indisponível.
