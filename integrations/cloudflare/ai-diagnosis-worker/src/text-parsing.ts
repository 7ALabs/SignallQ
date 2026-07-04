// =============================================================================
// Utilitarios puros de parsing de texto da resposta da IA.
// Extraidos de index.ts para permitir teste unitario com `node --test`
// (o loader ESM nativo do Node exige extensao explicita nos imports locais,
// e index.ts importa ./providers sem extensao, incompativel fora do wrangler).
// =============================================================================

// Remove blocos <think>...</think> que modelos com reasoning (Qwen3, DeepSeek,
// QwQ) emitem antes da resposta real. Caso o bloco nao esteja fechado (thinking
// truncado), remove do <think> ate o fim do texto.
export function stripThinkingTokens(text: string): string {
  return text.replace(/<think>[\s\S]*?<\/think>/g, "").replace(/<think>[\s\S]*$/, "").trim();
}

export function extractJson(text: string): string {
  // Remove thinking tokens antes de extrair JSON
  const cleaned = stripThinkingTokens(text);
  // Remove blocos markdown ``` se a IA insistir em usa-los apesar do prompt
  const fenced = cleaned.match(/```(?:json)?\s*([\s\S]*?)```/);
  if (fenced) return fenced[1].trim();
  // Recorta entre o primeiro { e o ultimo }
  const start = cleaned.indexOf("{");
  const end = cleaned.lastIndexOf("}");
  if (start !== -1 && end > start) return cleaned.slice(start, end + 1);
  return cleaned.trim();
}
