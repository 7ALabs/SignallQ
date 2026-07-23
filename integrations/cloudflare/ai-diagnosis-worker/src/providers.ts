// =============================================================================
// SignallQ AI Diagnosis Worker — Provider Router (Fase 5)
// Primary:  Gemini 2.0 Flash  (Google Generative Language API)
// Fallback: Qwen3 30B MoE FP8 (Cloudflare Workers AI)
//
// Ambos os providers normalizam a saída para o mesmo tipo ProviderResult,
// de modo que index.ts não precisa saber qual provedor respondeu além do
// logging e do campo modeloIa.
// =============================================================================

export type TokenUsage = {
  promptTokens: number;
  completionTokens: number;
  totalTokens: number;
};

export type ProviderResult = {
  /** Texto bruto devolvido pelo modelo (JSON ainda não parseado). */
  text: string;
  /** Identificador curto do provider que respondeu. */
  providerId: string;
  /** Objeto a ser escrito em parsed.modeloIa antes da resposta final. */
  modeloIa: Record<string, unknown>;
  /** ID de modelo usado para logs e ingestão de métricas. */
  effectiveModelId: string;
  /** GH#758 — tokens consumidos, quando o provider expõe essa informação. */
  usage?: TokenUsage;
};

export interface AiProvider {
  readonly id: string;
  call(
    systemPrompt: string,
    userContent: string,
    maxTokens: number,
    temperature: number,
  ): Promise<ProviderResult>;
  callStream(
    systemPrompt: string,
    userContent: string,
    maxTokens: number,
    temperature: number,
  ): Promise<ReadableStream>;
}

// =============================================================================
// Gemini 2.0 Flash
// =============================================================================

const GEMINI_MODEL_ID = "gemini-flash-latest";
const GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";

const GEMINI_MODELO_IA: Record<string, unknown> = {
  idInterno: `google/${GEMINI_MODEL_ID}`,
  provedor: "google_generative_ai",
  familia: "Gemini",
  versao: "Flash (latest)",
  tamanho: null,
  variante: "Flash",
  nomeExibicao: "SignallQ IA",
  nomeCompletoComercial: "SignallQ IA",
  descricaoComercial: "Diagnóstico inteligente de conexão",
  textoRodape: "Motor de análise: SignallQ IA",
};

// #1317 — validado direto em producao via wrangler tail (2026-07-22): o 400
// da Gemini para este payload vem com corpo GENERICO, sem citar o campo
// rejeitado --  {"error":{"code":400,"message":"Request contains an invalid
// argument.","status":"INVALID_ARGUMENT"}} -- entao nao da pra distinguir
// "rejeitou thinkingConfig" de outro 400 pelo texto da mensagem. Retry sem
// thinkingConfig acontece em QUALQUER 400 (o campo e o unico coringa do
// payload introduzido pelo GH#898; se a causa fosse outra, o retry so
// devolveria o mesmo 400 de novo e cairia pro Qwen do mesmo jeito -- custo de
// 1 chamada HTTP extra, sem risco de mascarar erro novo).
//
// Funcao pura (fora da classe de proposito: parameter properties do
// construtor do provider nao passam pelo strip-only TS loader do
// `node --test`, entao a logica testavel precisa ficar fora da classe pra
// ganhar cobertura unitaria real sem mockar toda a classe).
export function isGeminiThinkingConfigRejection(status: number, _detail: string): boolean {
  return status === 400;
}

export class GeminiFlashProvider implements AiProvider {
  readonly id = "gemini_flash";

  constructor(private readonly apiKey: string) {}

  // #1317 — a doc atual da Gemini API (ai.google.dev/gemini-api/docs/thinking,
  // consultada em 2026-07-22) não documenta mais `thinkingConfig.thinkingBudget`
  // para o alias "gemini-flash-latest" (hoje já resolvendo pra geração nova,
  // GA anunciado na mesma data) — só `thinking_level`. Sem acesso a
  // GEMINI_API_KEY real neste ambiente pra confirmar o nome exato do campo
  // aceito pela geração nova, `body()` fica resiliente ao drift em vez de
  // apostar num nome: manda `thinkingConfig` normalmente (mantém o comportamento
  // do GH#898 pra modelos que ainda aceitam o campo) e, se a API responder 400
  // citando `thinkingConfig`/`thinking`, tenta de novo sem esse bloco antes de
  // desistir e cair pro fallback Qwen — hoje esse retry nunca acontecia, então
  // 100% das chamadas iam direto pro Qwen sem sinal nenhum.
  private body(
    systemPrompt: string,
    userContent: string,
    maxTokens: number,
    temperature: number,
    omitThinkingConfig = false,
  ): string {
    const generationConfig: Record<string, unknown> = {
      maxOutputTokens: maxTokens,
      temperature,
    };
    if (!omitThinkingConfig) {
      // GH#898 QA fix — o alias "gemini-flash-latest" pode resolver para um
      // modelo com "thinking" habilitado por padrao, que consome parte do
      // maxOutputTokens em tokens de raciocinio invisiveis antes do texto
      // final, cortando o JSON no meio por MAX_TOKENS. thinkingBudget: 0
      // desliga o raciocinio nos modelos que ainda aceitam esse campo.
      generationConfig.thinkingConfig = { thinkingBudget: 0 };
    }
    return JSON.stringify({
      systemInstruction: { parts: [{ text: systemPrompt }] },
      contents: [{ role: "user", parts: [{ text: userContent }] }],
      generationConfig,
    });
  }

  private headers(): Record<string, string> {
    return {
      "Content-Type": "application/json",
      "X-goog-api-key": this.apiKey,
    };
  }

  async call(
    systemPrompt: string,
    userContent: string,
    maxTokens: number,
    temperature: number,
  ): Promise<ProviderResult> {
    const url = `${GEMINI_BASE_URL}/${GEMINI_MODEL_ID}:generateContent`;
    let res = await fetch(url, {
      method: "POST",
      headers: this.headers(),
      body: this.body(systemPrompt, userContent, maxTokens, temperature),
    });
    if (!res.ok) {
      const detail = await res.text().catch(() => "");
      if (isGeminiThinkingConfigRejection(res.status, detail)) {
        console.error(`[AiRouter] ${this.id} rejeitou thinkingConfig (${detail.slice(0, 200)}), retentando sem o campo`);
        res = await fetch(url, {
          method: "POST",
          headers: this.headers(),
          body: this.body(systemPrompt, userContent, maxTokens, temperature, true),
        });
        if (!res.ok) {
          const retryDetail = await res.text().catch(() => "");
          throw new Error(`Gemini ${res.status} (retry sem thinkingConfig): ${retryDetail.slice(0, 200)}`);
        }
      } else {
        throw new Error(`Gemini ${res.status}: ${detail.slice(0, 200)}`);
      }
    }
    const json = (await res.json()) as Record<string, unknown>;
    const text = extractGeminiText(json);
    return {
      text,
      providerId: this.id,
      modeloIa: GEMINI_MODELO_IA,
      effectiveModelId: GEMINI_MODEL_ID,
      usage: extractGeminiUsage(json),
    };
  }

  async callStream(
    systemPrompt: string,
    userContent: string,
    maxTokens: number,
    temperature: number,
  ): Promise<ReadableStream> {
    const url = `${GEMINI_BASE_URL}/${GEMINI_MODEL_ID}:streamGenerateContent?alt=sse`;
    let res = await fetch(url, {
      method: "POST",
      headers: this.headers(),
      body: this.body(systemPrompt, userContent, maxTokens, temperature),
    });
    if (!res.ok) {
      const detail = await res.text().catch(() => "");
      if (isGeminiThinkingConfigRejection(res.status, detail)) {
        console.error(`[AiRouter] ${this.id} stream rejeitou thinkingConfig (${detail.slice(0, 200)}), retentando sem o campo`);
        res = await fetch(url, {
          method: "POST",
          headers: this.headers(),
          body: this.body(systemPrompt, userContent, maxTokens, temperature, true),
        });
      }
      if (!res.ok) throw new Error(`Gemini stream ${res.status}`);
    }
    if (!res.body) throw new Error("Gemini stream: empty body");
    return normalizeGeminiStream(res.body);
  }
}

function extractGeminiText(json: Record<string, unknown>): string {
  const candidates = json.candidates as Array<Record<string, unknown>> | undefined;
  if (!candidates?.length) throw new Error("Gemini: empty candidates");
  const parts = (candidates[0].content as Record<string, unknown> | undefined)
    ?.parts as Array<Record<string, unknown>> | undefined;
  if (!parts?.length) throw new Error("Gemini: empty parts");
  const text = parts[0].text;
  if (typeof text !== "string" || !text) throw new Error("Gemini: no text in parts[0]");
  return text;
}

// GH#758 — Gemini expõe consumo real em usageMetadata; sem isso, ai_usage no
// admin fica sempre com tokens/custo zerados apesar do laudo ser real.
function extractGeminiUsage(json: Record<string, unknown>): TokenUsage | undefined {
  const usageMetadata = json.usageMetadata as Record<string, unknown> | undefined;
  if (!usageMetadata) return undefined;
  const promptTokens = usageMetadata.promptTokenCount;
  const completionTokens = usageMetadata.candidatesTokenCount;
  const totalTokens = usageMetadata.totalTokenCount;
  if (typeof promptTokens !== "number" && typeof totalTokens !== "number") return undefined;
  return {
    promptTokens: typeof promptTokens === "number" ? promptTokens : 0,
    completionTokens: typeof completionTokens === "number" ? completionTokens : 0,
    totalTokens:
      typeof totalTokens === "number"
        ? totalTokens
        : (typeof promptTokens === "number" ? promptTokens : 0) +
          (typeof completionTokens === "number" ? completionTokens : 0),
  };
}

// Converte o SSE do Gemini para o formato do Workers AI:
//   Gemini:     data: {"candidates":[{"content":{"parts":[{"text":"..."}]}}]}
//   Workers AI: data: {"response":"..."}
function normalizeGeminiStream(body: ReadableStream): ReadableStream {
  const decoder = new TextDecoder();
  const encoder = new TextEncoder();
  const reader = body.getReader();
  let buf = "";

  return new ReadableStream({
    async pull(controller) {
      try {
        for (;;) {
          const { done, value } = await reader.read();
          if (done) {
            controller.enqueue(encoder.encode("data: [DONE]\n\n"));
            controller.close();
            return;
          }
          buf += decoder.decode(value, { stream: true });
          const lines = buf.split("\n");
          buf = lines.pop() ?? "";
          for (const line of lines) {
            if (!line.startsWith("data:")) continue;
            const raw = line.slice(5).trim();
            if (!raw || raw === "[DONE]") continue;
            try {
              const obj = JSON.parse(raw) as Record<string, unknown>;
              const cands = obj.candidates as Array<Record<string, unknown>> | undefined;
              const parts = (
                (cands?.[0]?.content as Record<string, unknown> | undefined)?.parts
              ) as Array<Record<string, unknown>> | undefined;
              const token = parts?.[0]?.text;
              if (typeof token === "string" && token) {
                controller.enqueue(
                  encoder.encode(`data: ${JSON.stringify({ response: token })}\n\n`),
                );
              }
            } catch {
              // evento SSE malformado — ignorar
            }
          }
        }
      } catch (err) {
        controller.error(err);
      }
    },
  });
}

// =============================================================================
// Qwen3 30B MoE FP8 via Cloudflare Workers AI
// =============================================================================

export class QwenCFProvider implements AiProvider {
  readonly id = "qwen_cf";

  constructor(
    private readonly ai: { run: (model: string, opts: unknown) => Promise<unknown> },
    private readonly model: string,
    private readonly modeloIa: Record<string, unknown>,
  ) {}

  async call(
    systemPrompt: string,
    userContent: string,
    maxTokens: number,
    temperature: number,
  ): Promise<ProviderResult> {
    const result = await this.ai.run(this.model, {
      messages: [
        { role: "system", content: systemPrompt },
        { role: "user", content: userContent },
      ],
      max_tokens: maxTokens,
      temperature,
    });
    const text = extractCFText(result);
    return {
      text,
      providerId: this.id,
      modeloIa: this.modeloIa,
      effectiveModelId: this.model,
      usage: extractCFUsage(result),
    };
  }

  async callStream(
    systemPrompt: string,
    userContent: string,
    maxTokens: number,
    temperature: number,
  ): Promise<ReadableStream> {
    const result = await this.ai.run(this.model, {
      messages: [
        { role: "system", content: systemPrompt },
        { role: "user", content: userContent },
      ],
      max_tokens: maxTokens,
      temperature,
      stream: true,
    });
    return result as ReadableStream;
  }
}

// Suporta os vários shapes que o Workers AI pode retornar dependendo do modelo.
function extractCFText(result: unknown): string {
  if (typeof result === "string") return result;
  if (!result || typeof result !== "object") {
    throw new Error("CF AI: null/non-object result");
  }
  const r = result as Record<string, unknown>;

  if (typeof r.response === "string") return r.response;

  if (r.response && typeof r.response === "object") {
    const inner = r.response as Record<string, unknown>;
    if (typeof inner.text === "string") return inner.text;
    if (typeof inner.content === "string") return inner.content;
  }

  if (r.result && typeof r.result === "object") {
    const inner = r.result as Record<string, unknown>;
    if (typeof inner.response === "string") return inner.response;
  }

  const choices = r.choices as Array<Record<string, unknown>> | undefined;
  if (Array.isArray(choices) && choices.length) {
    const c = choices[0];
    const msg = c.message as Record<string, unknown> | undefined;
    if (typeof msg?.content === "string") return msg.content;
    const delta = c.delta as Record<string, unknown> | undefined;
    if (typeof delta?.content === "string") return delta.content;
    if (typeof c.text === "string") return c.text;
  }

  if (typeof r.text === "string") return r.text;

  throw new Error(
    `CF AI: formato de resposta desconhecido: ${JSON.stringify(result).slice(0, 200)}`,
  );
}

// GH#758 — Workers AI expõe `usage` no formato OpenAI-compatible (prompt_tokens/
// completion_tokens/total_tokens) para vários modelos; quando ausente, retorna
// undefined em vez de inventar número (fica 0 no ingest, não é encoberto).
function extractCFUsage(result: unknown): TokenUsage | undefined {
  if (!result || typeof result !== "object") return undefined;
  const r = result as Record<string, unknown>;
  const usage = (r.usage ?? (r.result as Record<string, unknown> | undefined)?.usage) as
    | Record<string, unknown>
    | undefined;
  if (!usage) return undefined;
  const promptTokens = usage.prompt_tokens;
  const completionTokens = usage.completion_tokens;
  const totalTokens = usage.total_tokens;
  if (typeof promptTokens !== "number" && typeof totalTokens !== "number") return undefined;
  return {
    promptTokens: typeof promptTokens === "number" ? promptTokens : 0,
    completionTokens: typeof completionTokens === "number" ? completionTokens : 0,
    totalTokens:
      typeof totalTokens === "number"
        ? totalTokens
        : (typeof promptTokens === "number" ? promptTokens : 0) +
          (typeof completionTokens === "number" ? completionTokens : 0),
  };
}

// =============================================================================
// Router — tenta providers em ordem, retorna o primeiro que responder
// =============================================================================

export type RouterStreamResult = {
  stream: ReadableStream;
  providerId: string;
};

export class AiProviderRouter {
  constructor(private readonly providers: AiProvider[]) {}

  async call(
    systemPrompt: string,
    userContent: string,
    maxTokens: number,
    temperature: number,
  ): Promise<ProviderResult> {
    let lastErr: unknown;
    for (const p of this.providers) {
      try {
        console.log(`[AiRouter] tentando ${p.id}`);
        const result = await p.call(systemPrompt, userContent, maxTokens, temperature);
        console.log(`[AiRouter] ${p.id} respondeu`);
        return result;
      } catch (err) {
        console.error(
          `[AiRouter] ${p.id} falhou:`,
          err instanceof Error ? err.message : String(err),
        );
        lastErr = err;
      }
    }
    throw lastErr ?? new Error("AiProviderRouter: todos os providers falharam");
  }

  async callStream(
    systemPrompt: string,
    userContent: string,
    maxTokens: number,
    temperature: number,
  ): Promise<RouterStreamResult> {
    let lastErr: unknown;
    for (const p of this.providers) {
      try {
        console.log(`[AiRouter] stream tentando ${p.id}`);
        const stream = await p.callStream(systemPrompt, userContent, maxTokens, temperature);
        console.log(`[AiRouter] stream ${p.id} respondeu`);
        return { stream, providerId: p.id };
      } catch (err) {
        console.error(
          `[AiRouter] stream ${p.id} falhou:`,
          err instanceof Error ? err.message : String(err),
        );
        lastErr = err;
      }
    }
    throw lastErr ?? new Error("AiProviderRouter: todos os stream providers falharam");
  }
}
