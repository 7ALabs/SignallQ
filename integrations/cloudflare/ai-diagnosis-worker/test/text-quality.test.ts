import { test } from "node:test";
import assert from "node:assert/strict";
import {
  validarTextoParaLeigo,
  aplicarValidacaoDeQualidade,
  construirTextoLaudoFallback,
  construirResumoFallback,
} from "../src/text-quality.ts";

// =============================================================================
// validarTextoParaLeigo — jargao sem explicacao parentetica
// =============================================================================

test("validarTextoParaLeigo: rejeita jargao tecnico citado sem explicacao entre parenteses", () => {
  const texto = "O CGNAT e o alto RSSI indicam degradacao no throughput da sua rede.";
  const resultado = validarTextoParaLeigo(texto);
  assert.equal(resultado.ok, false);
  const termos = resultado.problemas
    .filter((p) => p.tipo === "termo_tecnico_sem_explicacao")
    .map((p) => (p as { termo: string }).termo);
  assert.ok(termos.includes("cgnat"));
  assert.ok(termos.includes("rssi"));
  assert.ok(termos.includes("throughput"));
});

test("validarTextoParaLeigo: rejeita jitter sem explicacao mesmo em frase bem formada", () => {
  const texto = "Sua conexao apresenta jitter alto, o que explica os cortes na chamada.";
  const resultado = validarTextoParaLeigo(texto);
  assert.equal(resultado.ok, false);
  assert.ok(resultado.problemas.some((p) => p.tipo === "termo_tecnico_sem_explicacao"));
});

test("validarTextoParaLeigo: aceita jitter quando explicado entre parenteses logo apos o termo", () => {
  const texto =
    "Sua conexao apresenta jitter (oscilacao no tempo de resposta) alto, o que explica os cortes na chamada.";
  const resultado = validarTextoParaLeigo(texto);
  assert.equal(resultado.ok, true);
  assert.deepEqual(resultado.problemas, []);
});

test("validarTextoParaLeigo: aceita explicacao entre parenteses antes do termo tecnico", () => {
  const texto = "Sua conexao esta oscilando (jitter) e isso pode causar travamentos em chamadas.";
  const resultado = validarTextoParaLeigo(texto);
  assert.equal(resultado.ok, true);
});

test("validarTextoParaLeigo: nao marca 'roteador' e 'Wi-Fi' como jargao (termos do dia a dia)", () => {
  const texto = "Tente usar a rede Wi-Fi de 5 GHz perto do roteador e repita o teste.";
  const resultado = validarTextoParaLeigo(texto);
  assert.equal(resultado.ok, true);
});

// =============================================================================
// validarTextoParaLeigo — mojibake (UTF-8 mal decodificado)
// =============================================================================

test("validarTextoParaLeigo: rejeita mojibake classico (Ã© no lugar de é)", () => {
  const texto = "Sua conexÃ£o estÃ¡ instÃ¡vel no momento.";
  const resultado = validarTextoParaLeigo(texto);
  assert.equal(resultado.ok, false);
  assert.ok(resultado.problemas.some((p) => p.tipo === "mojibake"));
});

test("validarTextoParaLeigo: rejeita mojibake de apostrofo (â€™)", () => {
  const texto = "O usuÃ¡rio nÃ£o consegue acessar, itâ€™s a known issue.";
  const resultado = validarTextoParaLeigo(texto);
  assert.equal(resultado.ok, false);
  assert.ok(resultado.problemas.some((p) => p.tipo === "mojibake"));
});

// =============================================================================
// validarTextoParaLeigo — texto correto deve passar
// =============================================================================

test("validarTextoParaLeigo: aceita texto correto em portugues simples sem jargao", () => {
  const texto =
    "Sua internet esta funcionando bem. A velocidade medida e boa para as suas atividades do dia a dia. Repita o teste em outro horario se notar lentidao.";
  const resultado = validarTextoParaLeigo(texto);
  assert.equal(resultado.ok, true);
  assert.deepEqual(resultado.problemas, []);
});

test("validarTextoParaLeigo: string vazia e considerada valida (nada para validar)", () => {
  assert.equal(validarTextoParaLeigo("").ok, true);
});

// =============================================================================
// aplicarValidacaoDeQualidade — fallback deterministico
// =============================================================================

test("aplicarValidacaoDeQualidade: substitui textoLaudo e resumo quando ha jargao sem explicacao", () => {
  const parsed: Record<string, unknown> = {
    status: "regular",
    titulo: "Conexao instavel",
    textoLaudo: "O CGNAT esta causando problema de RSSI na sua rede.",
    resumo: "CGNAT e RSSI ruins.",
    acoesRecomendadas: [{ titulo: "Reinicie o roteador" }],
  };
  const resultado = aplicarValidacaoDeQualidade(parsed);
  assert.equal(resultado.substituido, true);
  assert.ok(resultado.motivos.length > 0);
  assert.match(parsed.textoLaudo as string, /Conexao instavel|Reinicie o roteador/);
  assert.ok((parsed.resumo as string).length > 0);
});

test("aplicarValidacaoDeQualidade: substitui quando ha mojibake", () => {
  const parsed: Record<string, unknown> = {
    status: "bom",
    titulo: "Tudo certo",
    textoLaudo: "Sua conexÃ£o estÃ¡ boa.",
    resumo: "Tudo certo.",
  };
  const resultado = aplicarValidacaoDeQualidade(parsed);
  assert.equal(resultado.substituido, true);
  assert.doesNotMatch(parsed.textoLaudo as string, /Ã/);
});

test("aplicarValidacaoDeQualidade: nao mexe no texto quando ele ja esta correto e sem jargao", () => {
  const parsed: Record<string, unknown> = {
    status: "bom",
    titulo: "Conexao estavel",
    textoLaudo: "Sua internet esta funcionando bem, sem sinais de instabilidade no momento.",
    resumo: "Sua conexao esta boa.",
  };
  const original = { ...parsed };
  const resultado = aplicarValidacaoDeQualidade(parsed);
  assert.equal(resultado.substituido, false);
  assert.equal(parsed.textoLaudo, original.textoLaudo);
  assert.equal(parsed.resumo, original.resumo);
});

test("construirTextoLaudoFallback: gera texto sem jargao mesmo sem acoesRecomendadas", () => {
  const texto = construirTextoLaudoFallback({ status: "critico", titulo: "Falha na rede" });
  assert.match(texto, /Falha na rede/);
  assert.equal(validarTextoParaLeigo(texto).ok, true);
});

test("construirResumoFallback: gera resumo curto valido para status desconhecido", () => {
  const texto = construirResumoFallback({ status: "algo_inesperado" });
  assert.ok(texto.length > 0);
  assert.equal(validarTextoParaLeigo(texto).ok, true);
});
