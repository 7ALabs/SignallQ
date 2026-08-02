import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";

// #1317 — providers.ts não é importado diretamente (como index.test.ts já
// evita importar index.ts): os providers usam parameter properties no
// construtor (`constructor(private readonly apiKey: string)`), sintaxe que o
// loader strip-only de TS do `node --test` não suporta. Mesma estratégia já
// usada pro SYSTEM_PROMPT em index.test.ts — lemos o fonte como texto pra
// testar sem puxar o módulo inteiro (que também arrastaria `fetch`/env do
// Worker). Reproduz aqui a mesma função de decisão que GeminiFlashProvider
// usa em runtime (isGeminiThinkingConfigRejection) e valida contra o texto
// real do arquivo pra pegar drift se a lógica for editada sem atualizar o teste.
const PROVIDERS_SOURCE = readFileSync(
  fileURLToPath(new URL("../src/providers.ts", import.meta.url)),
  "utf-8",
);

// Validado em produção via wrangler tail (2026-07-22): o 400 real da Gemini
// vem com corpo genérico, sem citar o campo rejeitado — por isso o retry é
// disparado em qualquer 400, não filtrado pelo texto da mensagem.
function isGeminiThinkingConfigRejection(status: number, _detail: string): boolean {
  return status === 400;
}

test("providers.ts: isGeminiThinkingConfigRejection existe, é exportada e retenta em qualquer 400", () => {
  assert.match(
    PROVIDERS_SOURCE,
    /export function isGeminiThinkingConfigRejection\(status: number, _detail: string\): boolean \{\s*return status === 400;/,
    "a implementação real em providers.ts deveria bater com a lógica testada aqui",
  );
});

test("isGeminiThinkingConfigRejection: true para 400 com corpo genérico (caso real de produção, GH#1317)", () => {
  const detail = '{"error":{"code":400,"message":"Request contains an invalid argument.","status":"INVALID_ARGUMENT"}}';
  assert.equal(isGeminiThinkingConfigRejection(400, detail), true);
});

test("isGeminiThinkingConfigRejection: true para 400 citando thinkingConfig explicitamente também", () => {
  const detail = 'Invalid JSON payload received. Unknown name "thinkingConfig" at \'generation_config\'';
  assert.equal(isGeminiThinkingConfigRejection(400, detail), true);
});

test("isGeminiThinkingConfigRejection: false para outros status (retry só faz sentido em 400)", () => {
  assert.equal(isGeminiThinkingConfigRejection(500, "internal error"), false);
  assert.equal(isGeminiThinkingConfigRejection(429, "rate limited"), false);
  assert.equal(isGeminiThinkingConfigRejection(401, "invalid api key"), false);
});

test("providers.ts: call() retenta sem thinkingConfig só quando isGeminiThinkingConfigRejection for true", () => {
  assert.match(
    PROVIDERS_SOURCE,
    /if \(isGeminiThinkingConfigRejection\(res\.status, detail\)\) \{/,
    "o retry no método call() deveria ser guardado por isGeminiThinkingConfigRejection",
  );
});
