/**
 * Worker de sonda de latencia — GH#935 (tela Jogos, estrategia REGIONAL_ESTIMATE).
 *
 * Unica responsabilidade: responder o mais rapido possivel a um GET/HEAD, sem
 * corpo relevante, para o cliente medir round-trip time. NENHUMA logica de
 * jogo, autenticacao ou estado aqui — de proposito, para o dado nunca ser
 * confundido com "ping real da partida" (ver JOGOS_TESTE_CONEXAO_SPEC.md).
 *
 * A Cloudflare roteia a requisicao pelo PoP mais proximo do usuario por
 * anycast (GRU para a maior parte do Brasil) — e o que da a este endpoint a
 * semantica de "sonda regional controlada pelo SignallQ", sem exigir Cloudflare
 * Spectrum (UDP dedicado, custo novo fora de escopo desta issue).
 */

export default {
  async fetch(request: Request): Promise<Response> {
    const url = new URL(request.url);

    const corsHeaders = {
      "access-control-allow-origin": "*",
      "access-control-allow-methods": "GET, HEAD, OPTIONS",
      "access-control-max-age": "86400",
      "cache-control": "no-store",
    };

    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders });
    }

    if (url.pathname !== "/probe" && url.pathname !== "/") {
      return new Response(null, { status: 404, headers: corsHeaders });
    }

    if (request.method !== "GET" && request.method !== "HEAD") {
      return new Response(null, { status: 405, headers: corsHeaders });
    }

    return new Response(null, {
      status: 204,
      headers: corsHeaders,
    });
  },
};
