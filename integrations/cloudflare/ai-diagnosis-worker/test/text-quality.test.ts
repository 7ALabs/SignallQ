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
// BLOQUEIO 1 (PR #1828) — falso-positivo do termo "nat" casado por substring
// =============================================================================

test("validarTextoParaLeigo: nao reprova 'Anatel', 'assinatura' e 'natural' (substring de 'nat')", () => {
  const texto =
    "De acordo com a Anatel, sua assinatura de internet garante uma velocidade natural de navegacao estavel.";
  const resultado = validarTextoParaLeigo(texto);
  assert.equal(resultado.ok, true);
  assert.deepEqual(resultado.problemas, []);
});

test("validarTextoParaLeigo: continua detectando 'gateway' sem explicacao na mesma frase comum", () => {
  const texto =
    "De acordo com a Anatel, sua assinatura de internet depende do gateway configurado corretamente.";
  const resultado = validarTextoParaLeigo(texto);
  assert.equal(resultado.ok, false);
  const termos = resultado.problemas
    .filter((p) => p.tipo === "termo_tecnico_sem_explicacao")
    .map((p) => (p as { termo: string }).termo);
  assert.ok(termos.includes("gateway"));
  assert.ok(!termos.includes("nat"));
});

test("validarTextoParaLeigo: ainda detecta 'NAT' isolado (ocorrencia real do termo, nao substring)", () => {
  const texto = "O problema esta no NAT do roteador, que esta mal configurado.";
  const resultado = validarTextoParaLeigo(texto);
  assert.equal(resultado.ok, false);
  const termos = resultado.problemas
    .filter((p) => p.tipo === "termo_tecnico_sem_explicacao")
    .map((p) => (p as { termo: string }).termo);
  assert.ok(termos.includes("nat"));
});

// =============================================================================
// BLOQUEIO 2 (PR #1828) — explicacao parentetica exigida so na 1a ocorrencia
// =============================================================================

test("validarTextoParaLeigo: aceita termo explicado na primeira mencao e repetido livremente depois", () => {
  const texto = "A latência (demora) está alta. Essa latência afeta chamadas.";
  const resultado = validarTextoParaLeigo(texto);
  assert.equal(resultado.ok, true);
  assert.deepEqual(resultado.problemas, []);
});

test("validarTextoParaLeigo: ainda reprova quando nem a primeira ocorrencia tem explicacao", () => {
  const texto = "A latência está alta. Essa latência afeta chamadas.";
  const resultado = validarTextoParaLeigo(texto);
  assert.equal(resultado.ok, false);
  assert.ok(
    resultado.problemas.some(
      (p) => p.tipo === "termo_tecnico_sem_explicacao" && (p as { termo: string }).termo === "latência",
    ),
  );
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

// =============================================================================
// BLOQUEIO 3 (PR #1828) — fallback nao pode propagar jargao/mojibake de
// titulo/acaoRecomendada interpolados sem checagem
// =============================================================================

test("construirTextoLaudoFallback: titulo com jargao sem explicacao nao contamina o fallback (reproducao do revisor)", () => {
  // Reproducao real do revisor: titulo interpolado sem checagem gerava
  // "Problema de DNS detectado: ... Troque o DNS do seu roteador", que
  // reprovava a propria validacao de qualidade.
  const parsed = {
    status: "ruim",
    titulo: "Problema de DNS detectado",
    acoesRecomendadas: [{ titulo: "Troque o DNS do seu roteador" }],
  };
  const texto = construirTextoLaudoFallback(parsed);
  assert.equal(validarTextoParaLeigo(texto).ok, true);
  // Nao deve ter interpolado titulo/acao com jargao sem explicacao.
  assert.doesNotMatch(texto, /DNS/);
});

test("construirTextoLaudoFallback: titulo com mojibake nao contamina o fallback (reproducao do revisor)", () => {
  const parsed = {
    status: "critico",
    titulo: "ConexÃ£o instÃ¡vel",
  };
  const texto = construirTextoLaudoFallback(parsed);
  assert.equal(validarTextoParaLeigo(texto).ok, true);
  assert.doesNotMatch(texto, /Ã/);
});

test("construirTextoLaudoFallback: acaoRecomendada com mojibake nao contamina o fallback", () => {
  const parsed = {
    status: "regular",
    titulo: "Conexao instavel",
    acoesRecomendadas: [{ titulo: "ReinÃ­cie o roteador" }],
  };
  const texto = construirTextoLaudoFallback(parsed);
  assert.equal(validarTextoParaLeigo(texto).ok, true);
  assert.doesNotMatch(texto, /Ã/);
});

test("construirTextoLaudoFallback: titulo seguro continua sendo interpolado normalmente", () => {
  const parsed = {
    status: "bom",
    titulo: "Conexao estavel",
    acoesRecomendadas: [{ titulo: "Nenhuma acao necessaria" }],
  };
  const texto = construirTextoLaudoFallback(parsed);
  assert.equal(validarTextoParaLeigo(texto).ok, true);
  assert.match(texto, /Conexao estavel/);
  assert.match(texto, /Nenhuma acao necessaria/);
});

test("aplicarValidacaoDeQualidade: fallback com titulo/acao contendo jargao nao propaga o problema (reproducao completa do revisor)", () => {
  const parsed: Record<string, unknown> = {
    status: "ruim",
    titulo: "Problema de DNS detectado",
    textoLaudo: "O CGNAT esta causando problema de RSSI na sua rede.",
    resumo: "CGNAT e RSSI ruins.",
    acoesRecomendadas: [{ titulo: "Troque o DNS do seu roteador" }],
  };
  const resultado = aplicarValidacaoDeQualidade(parsed);
  assert.equal(resultado.substituido, true);
  assert.equal(validarTextoParaLeigo(parsed.textoLaudo as string).ok, true);
  assert.equal(validarTextoParaLeigo(parsed.resumo as string).ok, true);
});
