# AI Flow — Android SignallQ

**Última atualização:** 2026-07-05 (v0.23.0, versionCode 56)
**Fonte:** código real (featureDiagnostico, di/AppModule.kt, integrations/cloudflare/ai-diagnosis-worker)

---

## 1. Visão Geral

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

**Fallback Gemini:** com a secret `GEMINI_API_KEY` configurada, o worker usa Gemini 2.0 Flash como provider primário e Qwen/CF como fallback automático. Sem a secret, Qwen/CF é o único provider cloud. Llama/Meta não é padrão nem fallback (política do projeto).

**Alternativas/legado (não são o padrão):**
- `@cf/google/gemma-7b-it` — Gemma v1, fraco para prompt complexo
- `@hf/google/gemma-2-9b-it` — formato incompatível com messages API
- `@cf/google/gemma-4-26b-a4b-it` — descartado (gerava timeout > 30s)

**Persona da IA:** "SignallQ"

---

## 4. Payload — Schema atual

Montado por `DiagnosisAiContextFactory.fromRaw()`. O worker aceita schemas anteriores para retrocompatibilidade.

A versão de prompt atual do worker é `diagnostico_v5_local_primary` (`AI_PROMPT_VERSION` em `src/index.ts`): os achados do motor local são enviados como entrada e a IA refina/expande em cima deles. `schemaVersion` do contexto (`DiagnosisAiContext`) é enviado ao worker e registrado no evento `ia_laudo_solicitado`.

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
- Cota diária: `CotaIaRepository` (rolling 24h — DataStore separado, não usa `linkaPreferencias`)
