# Cloudflare Integration — Android SignallQ

**Última atualização:** 2026-07-05 (v0.23.0, versionCode 56)
**Fonte:** `integrations/cloudflare/*/wrangler.toml`, `src/index.ts`, `worker.js`, `featureDiagnostico/ai/`

---

## Workers do projeto

O SignallQ opera três Cloudflare Workers, todos em `integrations/cloudflare/`:

| Diretório | Name (wrangler.toml) | Propósito |
|---|---|---|
| `ai-diagnosis-worker` | `linka-ai-diagnosis-worker` | Motor de IA de diagnóstico (LLM). Detalhado nas seções abaixo |
| `signallq-admin-worker` | `signallq-admin` | Backend do painel admin + ingest do app. Ver `admin-api-schema.md` e `ENDPOINTS_MAPPING.md` |
| `signallq-privacy-worker` | `signallq-privacy` | Página pública de política de privacidade (HTML estático servido no edge). Ver seção final deste arquivo |

---

## 1. Worker: linka-ai-diagnosis-worker

**Name (wrangler.toml):** `linka-ai-diagnosis-worker`

**URL de produção:** `https://linka-ai-diagnosis-worker.giammattey-luiz.workers.dev`

**Endpoint consumido pelo app:** `POST /api/ai/diagnostico-conexao`

**Plataforma:** Cloudflare Workers (edge serverless)

---

## 2. Configuração (wrangler.toml)

| Variável de ambiente | Valor | Descrição |
|---|---|---|
| `AI_MODEL` | `@cf/qwen/qwen3-30b-a3b-fp8` | Modelo padrão (Qwen3 30B MoE FP8) |

O modelo pode ser sobrescrito por variável de ambiente em deploy — o worker lê `env.AI_MODEL ?? DEFAULT_MODEL`.

---

## 3. Modelo Padrão

**Provider primário: Gemini 2.0 Flash.** Adicionar a secret `GEMINI_API_KEY` ativa o `GeminiFlashProvider` como provider primário (tentado primeiro pelo `AiProviderRouter`), com Qwen3/Cloudflare como fallback automático. Sem essa secret, Qwen3/CF é o único provider cloud. Em falha de ambos, o cliente Kotlin usa o fallback local (sem IA externa).

**Modelo do fallback cloud (Qwen3/Cloudflare Workers AI):** `@cf/qwen/qwen3-30b-a3b-fp8` (Qwen3 30B MoE FP8) — `DEFAULT_MODEL` em `src/index.ts` e `AI_MODEL` em `wrangler.toml`.

**Política de modelos:** Llama/Meta NÃO deve ser configurado como padrão nem como fallback cloud (regra registrada no `wrangler.toml`).

Histórico de modelos testados (não são o padrão):
- Gemma 7B-IT (`@cf/google/gemma-7b-it`): fraco para prompt complexo
- Gemma 2 9B (`@hf/google/gemma-2-9b-it`): formato `@hf/` incompatível com messages API
- Gemma 4 26B (`@cf/google/gemma-4-26b-a4b-it`): gerava 2500+ tokens de reasoning, timeout > 30s — descartado
- Qwen3 30B MoE FP8: modelo atual, sem timeouts, qualidade adequada

A fonte de verdade é `wrangler.toml` e `DEFAULT_MODEL` em `src/index.ts`.

---

## 4. Integração no App Android

**Módulo responsável:** `:featureDiagnostico`

**Classe cliente:** `AiDiagnosisRepository` (`featureDiagnostico/ai/`)

**Factory de payload:** `DiagnosisAiContextFactory` — monta o JSON schema v3 (`diagnostico_v3_raw`)

**Schema aceito pelo worker:** v1, v2, v3 (retrocompatível)

**Transporte:** OkHttp 4.12.0 — POST JSON

---

## 5. Fluxo de Chamada

```
DiagnosticOrchestrator.executar()
    → DiagnosisAiContextFactory.fromRaw(snapshotRede, resultadosLocais, preferencias)
    → AiDiagnosisRepository.diagnosticar(contexto)
        → POST https://linka-ai-diagnosis-worker.giammattey-luiz.workers.dev/api/ai/diagnostico-conexao
        → Response JSON → AiDiagnosisResult
    → Fallback: AiFallbackFactory.fromLocal() se timeout ou erro
```

---

## 6. Deploy

Para atualizar o worker:

```bash
# No diretório integrations/cloudflare/ai-diagnosis-worker/
npx wrangler deploy
```

**Regra do processo de release (CLAUDE.md):** quando houver mudanças em `integrations/cloudflare/ai-diagnosis-worker/src/`, executar `npx wrangler deploy` ANTES do commit Android.

---

## 7. Arquivos do Worker

| Arquivo | Propósito |
|---|---|
| `wrangler.toml` | Configuração do worker — name, compatibility_date, AI_MODEL |
| `src/index.ts` | Lógica principal: roteamento, montagem do prompt, chamada ao modelo, parser de resposta |
| `package.json` | Dependências Node (wrangler) |

---

## 8. Worker: signallq-privacy (política de privacidade)

**Name (wrangler.toml):** `signallq-privacy` — `main = worker.js` (sem `src/`)

**Propósito:** servir a página pública de política de privacidade do SignallQ (exigida pela Play Store e pela LGPD). O HTML é embutido como string estática no `worker.js` e devolvido no edge — sem D1, sem IA, sem autenticação.

**Rotas:**

| Método | Path | Resposta |
|---|---|---|
| GET | `/health` | `ok` (200, texto puro) |
| GET | qualquer outro | HTML da política (200, `text/html; charset=utf-8`, `Cache-Control: public, max-age=86400`) |

**Conteúdo:** dados coletados e finalidade, dados NÃO coletados, uso, compartilhamento (Cloudflare para IA, Firebase Analytics/Crashlytics), armazenamento, permissões, direitos LGPD, contato. Última atualização do texto: 28/06/2026.

**Deploy:** `npx wrangler deploy` no diretório `integrations/cloudflare/signallq-privacy-worker/`.
