// =============================================================================
// Validação pós-geração de qualidade do português em campos de texto livre
// da resposta da IA (textoLaudo, resumo).
//
// Contexto (issue de qualidade de texto, comparável ao PR #25 do
// network-diagnostics-service — o motor irmão NDS teve o mesmo problema e foi
// corrigido reforçando o prompt com few-shot + validação programática
// pós-geração): mesmo com a regra 8/8a do SYSTEM_PROMPT já instruindo
// linguagem simples e jargão sempre explicado entre parênteses, o modelo nem
// sempre obedece na prática. Este módulo não tenta consertar o texto (a IA
// nunca decide, só narra — princípio já adotado pela regra 18a do
// SYSTEM_PROMPT) — ele só detecta violação e aciona um fallback determinístico
// e simples, construído a partir de dados que o próprio Worker já tem
// (status/titulo/impacto), sem nunca quebrar a resposta HTTP.
// =============================================================================

// Lista de termos técnicos que exigem explicação parentética próxima quando
// aparecem em textoLaudo/resumo. Mesma lista de espírito usada no motor irmão
// NDS. "roteador" e "Wi-Fi" ficam de fora de propósito — são termos do dia a
// dia do usuário leigo brasileiro, não jargão técnico.
export const TERMOS_TECNICOS_SENSIVEIS: readonly string[] = [
  "bufferbloat",
  "jitter",
  "rssi",
  "rsrp",
  "rsrq",
  "sinr",
  "dns",
  "cgnat",
  "gateway",
  "throughput",
  "latência",
  "latencia",
  "packet loss",
  "perda de pacotes",
  "ping",
  "handshake",
  "firmware",
  "qos",
  "mtu",
  "nat",
  "vpn",
  "ssid",
  "beamforming",
  "mesh",
  "backhaul",
];

// Termos de tecnologia móvel que já são citados só em "evidencias" pela regra
// 15b do SYSTEM_PROMPT — mantidos aqui só para referência/typo, não usados na
// validação de textoLaudo (são esperados em "evidencias", não no laudo).

export type ProblemaQualidadeTexto =
  | { tipo: "termo_tecnico_sem_explicacao"; termo: string; trecho: string }
  | { tipo: "mojibake"; trecho: string };

export type ResultadoValidacaoTexto = {
  ok: boolean;
  problemas: ProblemaQualidadeTexto[];
};

// Sequências clássicas de mojibake causadas por UTF-8 decodificado como
// Latin-1/CP1252 (ou vice-versa) — comuns quando alguma camada no meio do
// caminho (provider HTTP, serialização) perde o encoding correto.
// Ex.: "é" -> "Ã©", "’" -> "â€™", "ção" -> "Ã§Ã£o".
const MOJIBAKE_PATTERN = /Ã[\x80-\xBF]|â€[\x80-\x9F]|Â[\x80-\xBF]|Ã|Ã/;

// Detecta um termo técnico da lista quando ele NÃO está seguido (dentro de uma
// janela curta de caracteres, cobrindo "termo (explicação)") por um parêntese
// de explicação. Heurística simples e intencionalmente tolerante a falso
// negativo (preferimos deixar passar um caso ambíguo a barrar texto correto).
function encontrarTermosSemExplicacao(texto: string): Array<{ termo: string; trecho: string }> {
  const lower = texto.toLowerCase();
  const encontrados: Array<{ termo: string; trecho: string }> = [];
  for (const termo of TERMOS_TECNICOS_SENSIVEIS) {
    let fromIndex = 0;
    while (true) {
      const idx = lower.indexOf(termo, fromIndex);
      if (idx === -1) break;
      const fim = idx + termo.length;
      // Janela de até 40 caracteres depois do termo para achar "(explicação)".
      const janela = texto.slice(fim, fim + 40);
      const temParenteseLogoDepois = /^\s*\(/.test(janela);
      // Também aceita explicação que já vem ANTES do termo, ex.:
      // "oscilação da conexão (jitter)" — parêntese fechando logo depois do termo.
      const antesDoTermo = texto.slice(Math.max(0, idx - 40), idx);
      const pareceExplicacaoAntes = /\([^)]*$/.test(antesDoTermo) && /^[^(]*\)/.test(janela);
      if (!temParenteseLogoDepois && !pareceExplicacaoAntes) {
        encontrados.push({
          termo,
          trecho: texto.slice(Math.max(0, idx - 20), Math.min(texto.length, fim + 20)),
        });
      }
      fromIndex = fim;
    }
  }
  return encontrados;
}

function encontrarMojibake(texto: string): string | null {
  const m = texto.match(MOJIBAKE_PATTERN);
  if (!m) return null;
  const idx = m.index ?? 0;
  return texto.slice(Math.max(0, idx - 15), Math.min(texto.length, idx + 15));
}

// Valida um campo de texto livre destinado ao usuário leigo (textoLaudo ou
// resumo). Retorna todos os problemas encontrados (não para no primeiro).
export function validarTextoParaLeigo(texto: string): ResultadoValidacaoTexto {
  const problemas: ProblemaQualidadeTexto[] = [];

  if (typeof texto !== "string" || !texto.trim()) {
    return { ok: true, problemas };
  }

  for (const termoSemExplicacao of encontrarTermosSemExplicacao(texto)) {
    problemas.push({
      tipo: "termo_tecnico_sem_explicacao",
      termo: termoSemExplicacao.termo,
      trecho: termoSemExplicacao.trecho,
    });
  }

  const mojibake = encontrarMojibake(texto);
  if (mojibake) {
    problemas.push({ tipo: "mojibake", trecho: mojibake });
  }

  return { ok: problemas.length === 0, problemas };
}

// =============================================================================
// Fallback determinístico
// =============================================================================
// Quando textoLaudo/resumo falham na validação, o Worker NUNCA falha a
// requisição HTTP nem tenta "consertar" o texto da IA (isso seria a IA
// decidindo de novo, fora do princípio "a IA narra, não decide"). Em vez
// disso, substitui os dois campos por uma versão simples e genérica, montada
// só com dados que o próprio Worker já calculou/recebeu (status, título,
// impacto por perfil) — sem jargão, sem risco de mojibake (texto fixo em
// UTF-8 literal no código-fonte).

const ROTULO_STATUS: Record<string, string> = {
  excelente: "sua conexão está excelente",
  bom: "sua conexão está boa",
  regular: "sua conexão está com um desempenho mediano",
  ruim: "sua conexão está com problemas",
  critico: "sua conexão está com um problema sério",
  inconclusivo: "não foi possível concluir o diagnóstico com os dados atuais",
};

// Constrói um textoLaudo de fallback simples, em português correto e sem
// jargão, a partir de campos que o Worker já possui localmente (nunca inventa
// dado novo). Usado só quando o texto gerado pela IA reprova a validação.
export function construirTextoLaudoFallback(parsed: Record<string, unknown>): string {
  const status = typeof parsed.status === "string" ? parsed.status : "inconclusivo";
  const titulo = typeof parsed.titulo === "string" && parsed.titulo.trim() ? parsed.titulo.trim() : null;
  const rotulo = ROTULO_STATUS[status] ?? ROTULO_STATUS.inconclusivo;

  const acoes = Array.isArray(parsed.acoesRecomendadas) ? parsed.acoesRecomendadas : [];
  const primeiraAcao = acoes
    .map((a) => (a && typeof a === "object" ? (a as Record<string, unknown>).titulo : null))
    .find((t): t is string => typeof t === "string" && t.trim().length > 0);

  const partes: string[] = [];
  partes.push(titulo ? `${titulo}: ${rotulo}.` : `${rotulo.charAt(0).toUpperCase()}${rotulo.slice(1)}.`);
  if (primeiraAcao) {
    partes.push(`O que fazer agora: ${primeiraAcao.trim()}.`);
  } else {
    partes.push("Repita o teste em outro horário para confirmar o resultado.");
  }
  partes.push(
    "Este resumo foi simplificado automaticamente porque a explicação detalhada não ficou clara o suficiente.",
  );

  return partes.join(" ");
}

export function construirResumoFallback(parsed: Record<string, unknown>): string {
  const status = typeof parsed.status === "string" ? parsed.status : "inconclusivo";
  const rotulo = ROTULO_STATUS[status] ?? ROTULO_STATUS.inconclusivo;
  return `${rotulo.charAt(0).toUpperCase()}${rotulo.slice(1)}.`;
}

// Ponto único chamado pelo Worker após o parse da resposta da IA. Valida
// textoLaudo e resumo; se qualquer um reprovar, substitui AMBOS pelo fallback
// determinístico (mantém os dois campos consistentes entre si) e retorna o
// diagnóstico da troca para logging. Nunca lança exceção — pensado para
// nunca quebrar a resposta HTTP do Worker.
export function aplicarValidacaoDeQualidade(
  parsed: Record<string, unknown>,
): { substituido: boolean; motivos: ProblemaQualidadeTexto[] } {
  const textoLaudo = typeof parsed.textoLaudo === "string" ? parsed.textoLaudo : "";
  const resumo = typeof parsed.resumo === "string" ? parsed.resumo : "";

  const resultadoLaudo = validarTextoParaLeigo(textoLaudo);
  const resultadoResumo = validarTextoParaLeigo(resumo);

  if (resultadoLaudo.ok && resultadoResumo.ok) {
    return { substituido: false, motivos: [] };
  }

  parsed.textoLaudo = construirTextoLaudoFallback(parsed);
  parsed.resumo = construirResumoFallback(parsed);

  return {
    substituido: true,
    motivos: [...resultadoLaudo.problemas, ...resultadoResumo.problemas],
  };
}
