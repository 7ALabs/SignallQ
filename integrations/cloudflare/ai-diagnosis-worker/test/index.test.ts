import { test } from "node:test";
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { extractJson, stripThinkingTokens } from "../src/text-parsing.ts";

// SYSTEM_PROMPT nao e exportado por src/index.ts (worker so exporta o
// handler default), entao lemos o fonte como texto em vez de importa-lo —
// evita puxar o resto do modulo (providers, env do Worker) para o teste.
const INDEX_SOURCE = readFileSync(
  fileURLToPath(new URL("../src/index.ts", import.meta.url)),
  "utf-8",
);

// =============================================================================
// stripThinkingTokens — remove blocos <think>...</think> emitidos por modelos
// com reasoning (Qwen3, DeepSeek, QwQ) antes da resposta real.
// =============================================================================

test("stripThinkingTokens: remove bloco <think> fechado antes do JSON", () => {
  const input = '<think>preciso analisar as metricas...</think>{"status":"bom"}';
  assert.equal(stripThinkingTokens(input), '{"status":"bom"}');
});

test("stripThinkingTokens: remove bloco <think> nao fechado (thinking truncado) ate o fim do texto", () => {
  const input = '<think>preciso analisar as metricas mas o texto foi truncado aqui';
  assert.equal(stripThinkingTokens(input), "");
});

test("stripThinkingTokens: remove multiplos blocos <think> na mesma resposta", () => {
  const input = '<think>primeiro pensamento</think>{"a":1}<think>segundo pensamento</think>';
  assert.equal(stripThinkingTokens(input), '{"a":1}');
});

test("stripThinkingTokens: texto sem tags <think> permanece intacto (com trim)", () => {
  const input = '  {"status":"bom"}  ';
  assert.equal(stripThinkingTokens(input), '{"status":"bom"}');
});

test("stripThinkingTokens: string vazia retorna string vazia", () => {
  assert.equal(stripThinkingTokens(""), "");
});

// =============================================================================
// extractJson — extrai o JSON da resposta bruta da IA, tolerando thinking
// tokens, blocos markdown ```json e texto ao redor do objeto.
// =============================================================================

test("extractJson: extrai JSON puro sem nenhum wrapper", () => {
  const input = '{"status":"bom","titulo":"Conexao OK"}';
  assert.equal(extractJson(input), '{"status":"bom","titulo":"Conexao OK"}');
});

test("extractJson: extrai JSON de dentro de bloco markdown ```json", () => {
  const input = '```json\n{"status":"bom"}\n```';
  assert.equal(extractJson(input), '{"status":"bom"}');
});

test("extractJson: extrai JSON de dentro de bloco markdown ``` sem a palavra json", () => {
  const input = '```\n{"status":"bom"}\n```';
  assert.equal(extractJson(input), '{"status":"bom"}');
});

test("extractJson: remove thinking tokens antes de extrair o JSON", () => {
  const input = '<think>vou montar o schema...</think>{"status":"bom"}';
  assert.equal(extractJson(input), '{"status":"bom"}');
});

test("extractJson: recorta do primeiro { ao ultimo } quando ha texto ao redor", () => {
  const input = 'Aqui esta o diagnostico: {"status":"bom"} — espero que ajude!';
  assert.equal(extractJson(input), '{"status":"bom"}');
});

test("extractJson: combina thinking tokens + bloco markdown", () => {
  const input = '<think>analisando...</think>```json\n{"status":"regular","titulo":"Instavel"}\n```';
  assert.equal(extractJson(input), '{"status":"regular","titulo":"Instavel"}');
});

test("extractJson: JSON aninhado preserva chaves internas ao recortar pelo ultimo }", () => {
  const input = '{"problemaPrincipal":{"tipo":"velocidade","confianca":0.8}}';
  assert.equal(extractJson(input), '{"problemaPrincipal":{"tipo":"velocidade","confianca":0.8}}');
});

test("extractJson: sem chaves no texto retorna o texto (trimado) como fallback", () => {
  const input = "  resposta sem json nenhum  ";
  assert.equal(extractJson(input), "resposta sem json nenhum");
});

test("extractJson: string vazia retorna string vazia", () => {
  assert.equal(extractJson(""), "");
});

// =============================================================================
// SYSTEM_PROMPT — regra 10/15b nao pode recomendar roteador/Wi-Fi de forma
// incondicional quando connectionType == "mobile" (regressao #521 / #851).
// =============================================================================

test("SYSTEM_PROMPT: regra 10 nao recomenda roteador/Wi-Fi de forma incondicional para rede movel", () => {
  const regra10 = INDEX_SOURCE.split(/\n11\./)[0].split(/\n10\./)[1];
  assert.ok(regra10, "regra 10 deveria existir no SYSTEM_PROMPT");
  assert.match(regra10, /mobile.*PROIBIDO mencionar roteador/s);
});

test("SYSTEM_PROMPT: regra 15b proibe roteador/Wi-Fi em rede movel mesmo sem o bloco 'movel' no payload", () => {
  const regra15b = INDEX_SOURCE.split(/\n16\./)[0].split(/15b\./)[1];
  assert.ok(regra15b, "regra 15b deveria existir no SYSTEM_PROMPT");
  assert.match(regra15b, /esta regra vale SEMPRE, independente de o bloco "movel"/);
  assert.match(regra15b, /PROIBIDO recomendar, sugerir ou mencionar roteador/);
});
