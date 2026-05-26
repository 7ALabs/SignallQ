// =============================================================================
// Linka AI Diagnosis Worker
// =============================================================================
// Recebe POST /api/ai/diagnostico-conexao com payload de diagnostico de rede e
// retorna um JSON v2 com:
//   - diagnostico estruturado (status/titulo/resumo/textoLaudo/impacto/etc.)
//   - separacao explicita velocidade x estabilidade
//   - hipoteses descartadas com base em evidencia
//   - perguntas contextuais para refinar o diagnostico
//   - metadados comerciais do modelo de IA usado (nao expoe o id Cloudflare bruto)
//
// O modelo principal continua vindo de env.AI_MODEL (default em DEFAULT_MODEL).
// O cliente Kotlin permanece consumindo apenas este Worker, nunca env.AI direto.
// =============================================================================

type Env = {
  AI: any;
  AI_MODEL?: string;
};

const MAX_BODY_BYTES = 64_000;
// Modelo padrao: Gemma 7B-IT (Google) via binding Cloudflare Workers AI.
// Modelo @cf/ nativo — suporta formato messages, sem reasoning tokens,
// termina inferencia em <15s no free tier.
// Modelos descartados:
//   @cf/google/gemma-4-26b-a4b-it — gerava 2500+ tokens de reasoning → timeout >30s
//   @hf/google/gemma-2-9b-it     — formato @hf/ incompativel com messages API
// Llama/Meta NAO e padrao nem fallback cloud — politica do projeto.
const DEFAULT_MODEL = "@cf/google/gemma-7b-it";

// SCHEMA versionado do payload de saida; o cliente Kotlin precisa aceitar 1 e 2.
const SCHEMA_VERSION = "2" as const;

const SYSTEM_PROMPT = `Você é o motor de diagnóstico inteligente do app LINKA, especializado em conexões de internet doméstica no Brasil.
Você recebe APENAS dados brutos coletados pelo app (métricas numéricas, contexto de rede sem rótulos, histórico de medições, opcionalmente feedback do usuário). Toda interpretação, classificação, decisão, conclusão e recomendação é responsabilidade SUA — o payload NÃO contém análise prévia, não confie em campos como status/titulo/decisão pré-computados (eles não existem no payload).
REGRAS INVIOLÁVEIS:
1. Responda exclusivamente em JSON válido, seguindo o schema informado. Não use markdown, não explique fora do JSON e não adicione texto antes ou depois.
2. Nunca invente dados. Use apenas métricas e contexto brutos presentes no JSON recebido. Não há análise local, classificação prévia nem rótulos no payload — você cria toda a análise.
3. Separe claramente velocidade de estabilidade:
   - Download e upload indicam capacidade de banda.
   - Latência, jitter, perda de pacotes e bufferbloat indicam estabilidade.
   - Se download/upload estiverem bons, mas latência/jitter estiverem ruins, diga claramente que o problema não é falta de Mbps, e sim estabilidade.
4. Não declare causa raiz absoluta quando os dados não permitirem. Use "causa provável" e reduza a confiança quando faltarem testes comparativos, como cabo vs Wi-Fi, perto vs longe do roteador, outro horário, outro servidor, perda de pacote ou rota.
5. Analise os dados em conjunto. Não conclua com base em uma métrica isolada quando houver outras evidências relevantes.
6. Use os números reais presentes no payload. Exemplo correto: "latência de 101 ms e jitter de 25,1 ms indicam instabilidade". Exemplo errado: "latência alta" sem número.
7. Declare hipóteses descartadas somente quando houver evidência real para descartar. Exemplo: "velocidade insuficiente descartada porque download de 294 Mbps e upload de 411 Mbps indicam boa banda".
8. O campo textoLaudo deve ser um parágrafo corrido de 3 a 5 linhas, direto, específico e conclusivo. Ele deve conter:
   - leitura principal dos dados;
   - o que foi descartado, se houver base;
   - causa provável;
   - impacto prático.
9. As ações recomendadas devem ser práticas, específicas e ordenadas por prioridade. Nunca gere ações genéricas como "verifique a internet" ou "contate o provedor" sem explicar quando e por quê.
10. Antes de recomendar contato com o provedor, priorize validações locais quando aplicável:
   - testar perto do roteador;
   - comparar Wi-Fi e cabo, se possível;
   - repetir em outro horário;
   - verificar dispositivos consumindo banda;
   - avaliar roteador, sinal Wi-Fi ou canal.
11. O impacto por uso deve refletir apenas o que os dados indicam. Não diga que jogos terão alta latência se a latência medida estiver boa. Não diga que streaming está comprometido se a velocidade estiver boa e não houver perda/jitter relevante.
12. Quando houver histórico de 7 ou 30 dias, use esse histórico obrigatoriamente no resumo, no laudo e nas ações.
13. Use status "inconclusivo" SOMENTE quando downloadMbps, uploadMbps E latenciaMs estiverem TODAS ausentes/null no campo "metricasAtuais". Se qualquer uma dessas métricas existir com valor numérico, PROIBIDO retornar "inconclusivo" — você DEVE produzir um diagnóstico real ("bom", "regular", "ruim" ou "critico"). WiFi fraco (rssi abaixo de -75 dBm) NÃO é justificativa para "inconclusivo" quando há dados de speedtest: anote o WiFi fraco em limitesDaAnalise e diagnostique com os números disponíveis. "inconclusivo" com dados reais é uma mentira para o usuário.
14. Gere perguntas contextuais quando elas ajudarem a refinar o diagnóstico. As perguntas devem ser curtas, com opções objetivas e úteis para alimentar uma nova análise.
15. O objeto modeloIa será normalizado pelo Worker com base no modelo realmente usado. Não invente família, versão, tamanho ou nome comercial — pode deixar vazio que o Worker sobrescreve.
15a. Em perguntasContextuais, agrupe por tema sempre que possível (ex.: "Wi-Fi", "ISP", "Roteador", "Dispositivo", "Histórico", "Cobertura", "Operadora"). Cada pergunta DEVE ter um campo "tema" curto (1-2 palavras) que identifique o agrupamento — a UI do app vai empilhar perguntas do mesmo tema em um único card expansível, então perguntas de temas diferentes devem ter temas distintos. Quando o tema não couber, use "Geral".
15b. REDE MÓVEL: quando connectionType == "mobile" e o bloco "movel" estiver presente no payload, interprete os campos:
   - RSRP (rsrpDbm): > -85 = bom, -85 a -100 = médio, -100 a -110 = ruim, < -110 = péssimo.
   - SINR (sinrDb): > 10 = bom, 0 a 10 = médio, < 0 = ruim (interferência alta).
   - RSRQ (rsrqDb): > -10 = bom, -10 a -15 = médio, < -15 = ruim.
   - Tecnologia: "5G SA"/"5G NSA"/"4G"/"3G"/"2G". Mencione explicitamente no laudo (ex.: "5G NSA da Vivo na banda n78").
   - Banda (bandaMovel): cite quando disponível (ex.: "n78", "B3 (1800 MHz)").
   - Roaming true: mencione como possível causa de cobrança/latência elevada.
   - NÃO recomende ações de Wi-Fi (mudar canal, trocar de banda 2.4/5, mover roteador) em rede móvel — o usuário não tem controle sobre infraestrutura da operadora.
   - Recomende: testar em local com melhor cobertura, comparar com outro horário/lugar, verificar plano contratado da operadora, considerar Wi-Fi calling se disponível.
   - Tema das perguntas em rede móvel pode incluir "Cobertura" ou "Operadora" além dos genéricos.
   - Em campos do bloco "movel" ausentes (null/omitidos), use "limitesDaAnalise" para citar a falta — não invente valores.
16. O título deve ser específico e refletir o problema real detectado pelas métricas; só use status "inconclusivo" se realmente faltarem dados.
17. PROIBIDO usar títulos genéricos como "Internet lenta", "Conexão ruim" ou "Problema na rede" quando as métricas permitirem identificar o problema real. Exemplos:
   - Se download e upload estão bons mas latência/jitter altos → título deve refletir estabilidade (ex.: "Conexão instável", "Velocidade boa, estabilidade ruim", "Latência e jitter elevados").
   - Se a velocidade está abaixo do esperado mas latência normal → título deve apontar velocidade (ex.: "Velocidade abaixo do contratado", "Banda insuficiente").
   - Se há perda de pacotes significativa → título deve mencionar perda (ex.: "Perda de pacotes detectada").
   - "Internet lenta" só é aceitável quando NÃO há dados de latência/jitter/perda E download/upload estão de fato baixos.

EXEMPLO OBRIGATÓRIO:
Entrada (payload simplificado):
- download 294 Mbps
- upload 411 Mbps
- latência 101 ms
- jitter 25,1 ms

Saída esperada (campos críticos):
- classificacaoTecnica.velocidade.avaliacao = "boa"
- classificacaoTecnica.estabilidade.avaliacao = "ruim"
- problemaPrincipal.tipo = "estabilidade"
- resumo: "A velocidade está boa, mas a conexão está instável."
- titulo: ALGO COMO "Conexão instável" ou "Velocidade boa, estabilidade ruim". NUNCA "Internet lenta".
- hipotesesDescartadas inclui: {"hipotese": "velocidade insuficiente", "motivo": "download de 294 Mbps e upload de 411 Mbps indicam boa banda"}
- impacto.jogos: "Alta latencia" ou "Instavel" (NÃO "OK", porque latência 101 ms compromete jogos)
- impacto.streaming: "OK" (download alto sustenta streaming)
- impacto.videochamada: "Comprometida" ou "Instavel" (jitter 25 ms compromete chamadas)`;

// Prompt e schema para modo chat (follow-up do usuário após o diagnóstico inicial).
// O Worker detecta este modo quando `feedbackUsuario` está presente no payload.
const CHAT_SYSTEM_PROMPT = `Você é o assistente de rede do app LINKA.
O usuário acabou de ver o diagnóstico da conexão e tem uma pergunta de follow-up.
Responda DIRETAMENTE à pergunta com orientações práticas e específicas.
REGRAS:
1. NÃO repita o diagnóstico já feito — o usuário já o leu.
2. Vá direto à solução ou à informação pedida.
3. Use os dados de rede do payload para contextualizar, mas o foco é responder a pergunta.
4. Responda em português brasileiro, tom direto e prático. Máximo 5 linhas no textoLaudo.
5. Responda exclusivamente em JSON válido seguindo o schema informado.`;

const CHAT_SCHEMA_HINT = `Schema JSON de retorno (responda APENAS com este formato, sem markdown):
{
  "schemaVersion": "2",
  "source": "cloudflare_ai",
  "generatedAt": 0,
  "modeloIa": {},
  "status": "bom",
  "titulo": "Resposta",
  "resumo": "<1 frase resumindo a resposta>",
  "textoLaudo": "<resposta direta e prática à pergunta do usuário, 3-5 linhas>",
  "classificacaoTecnica": {"velocidade":{"avaliacao":"inconclusiva","justificativa":""},"estabilidade":{"avaliacao":"inconclusiva","justificativa":""},"wifi":{"avaliacao":"inconclusiva","justificativa":""},"dns":{"avaliacao":"inconclusiva","justificativa":""},"fibra":{"avaliacao":"inconclusiva","justificativa":""}},
  "problemaPrincipal": {"tipo":"sem_problema","descricao":"","confianca":0.5},
  "hipotesesDescartadas": [],
  "impacto": {"navegacao":"OK","streaming":"OK","videochamada":"OK","jogos":"OK","trabalho":"OK"},
  "acoesRecomendadas": [],
  "perguntasContextuais": [],
  "evidencias": [],
  "limitesDaAnalise": []
}`;

// Hint do schema v2 enviado junto ao prompt do usuario. O Worker tambem
// sobrescreve campos criticos pos-parse (modeloIa, schemaVersion, source,
// generatedAt) para garantir consistencia mesmo se a IA divergir.
const SCHEMA_HINT = `Schema JSON de retorno (responda APENAS com este formato, sem markdown):
{
  "schemaVersion": "2",
  "source": "cloudflare_ai",
  "generatedAt": <epoch_ms>,
  "modeloIa": {
    "//": "ESTE OBJETO É NORMALIZADO PELO WORKER COM BASE NO MODELO REALMENTE USADO. NÃO INVENTE FAMÍLIA, VERSÃO, TAMANHO OU NOME COMERCIAL. Pode deixar valores vazios — o Worker sobrescreve.",
    "idInterno": "<sera substituido pelo Worker>",
    "provedor": "<sera substituido pelo Worker>",
    "familia": "<sera substituido pelo Worker>",
    "versao": "<sera substituido pelo Worker>",
    "tamanho": "<sera substituido pelo Worker>",
    "variante": "<sera substituido pelo Worker>",
    "nomeExibicao": "<sera substituido pelo Worker>",
    "nomeCompletoComercial": "<sera substituido pelo Worker>",
    "descricaoComercial": "<sera substituido pelo Worker>",
    "textoRodape": "<sera substituido pelo Worker>"
  },
  "status": "excelente"|"bom"|"regular"|"ruim"|"critico"|"inconclusivo",
  "titulo": "<5-8 palavras, conclusao direta>",
  "resumo": "<1-2 frases em linguagem simples para leigo>",
  "textoLaudo": "<paragrafo de 3-5 linhas: leitura principal, o que foi descartado, causa provavel, impacto pratico>",
  "classificacaoTecnica": {
    "velocidade":   {"avaliacao": "boa"|"regular"|"ruim"|"inconclusiva", "justificativa": "<frase com numeros reais>"},
    "estabilidade": {"avaliacao": "boa"|"regular"|"ruim"|"inconclusiva", "justificativa": "<frase com numeros reais>"},
    "wifi":         {"avaliacao": "boa"|"regular"|"ruim"|"inconclusiva", "justificativa": "<frase>"},
    "dns":          {"avaliacao": "boa"|"regular"|"ruim"|"inconclusiva", "justificativa": "<frase>"},
    "fibra":        {"avaliacao": "boa"|"regular"|"ruim"|"inconclusiva", "justificativa": "<frase>"}
  },
  "problemaPrincipal": {
    "tipo": "velocidade"|"estabilidade"|"wifi"|"roteador"|"isp"|"dns"|"fibra"|"dispositivo"|"historico"|"sem_problema"|"inconclusivo",
    "descricao": "<descricao especifica com numeros reais>",
    "confianca": <0.0 a 1.0>
  },
  "hipotesesDescartadas": [
    {"hipotese": "<ex.: velocidade insuficiente>", "motivo": "<por que foi descartada com numeros reais>"}
  ],
  "impacto": {
    "navegacao":    "OK"|"Lento"|"Instavel"|"Indisponivel",
    "streaming":    "OK"|"Comprometido"|"Instavel"|"Indisponivel",
    "videochamada": "OK"|"Comprometida"|"Instavel"|"Indisponivel",
    "jogos":        "OK"|"Alta latencia"|"Instavel"|"Indisponivel",
    "trabalho":     "OK"|"Comprometido"|"Instavel"|"Indisponivel"
  },
  "acoesRecomendadas": [
    {
      "titulo": "<acao curta>",
      "descricao": "<instrucao especifica ao caso, com numeros se aplicavel>",
      "prioridade": "alta"|"media"|"baixa",
      "tipo": "validacao_local"|"ajuste_roteador"|"ajuste_dispositivo"|"contato_isp"|"observacao"|"reteste",
      "executavelNoApp": true|false
    }
  ],
  "perguntasContextuais": [
    {
      "id": "<slug>",
      "tema": "<1-2 palavras: Wi-Fi|ISP|Roteador|Dispositivo|Historico|Geral>",
      "pergunta": "<pergunta curta>",
      "opcoes": [
        {"id": "<slug>", "rotulo": "<texto curto>"}
      ]
    }
  ],
  "evidencias": [
    {"label": "<nome da metrica>", "valor": "<valor com unidade>", "interpretacao": "<o que significa no contexto>"}
  ],
  "limitesDaAnalise": ["<limitacao desta analise>"]
}`;

// =============================================================================
// Mapeamento comercial do modelo
// =============================================================================
// Recebe o id interno usado em env.AI.run (ex.: "@cf/google/gemma-4-26b-a4b-it")
// e retorna metadados que podem ser exibidos ao usuario (UI rodape).
// O id interno NUNCA deve aparecer para o usuario final.
type CommercialModelInfo = {
  idInterno: string;
  provedor: string;
  familia: string;
  versao: string | null;
  tamanho: string | null;
  variante: string | null;
  nomeExibicao: string;
  nomeCompletoComercial: string;
  descricaoComercial: string;
  textoRodape: string;
};

function getCommercialModelInfo(model: string): CommercialModelInfo {
  const id = model || "";
  const lower = id.toLowerCase();

  // Gemma 7B-IT — modelo padrao atual. Match cobre "@cf/google/gemma-7b-it".
  if (/gemma-?7b/.test(lower)) {
    return {
      idInterno: id,
      provedor: "cloudflare_workers_ai",
      familia: "Gemma",
      versao: "1",
      tamanho: "7B",
      variante: lower.includes("-it") || lower.includes("instruct") ? "Instruction Tuned" : null,
      nomeExibicao: "Gemma 7B",
      nomeCompletoComercial: "Linka IA — Gemma 7B",
      descricaoComercial: "Diagnóstico inteligente de conexão",
      textoRodape: "Motor de análise: Linka IA — Gemma 7B",
    };
  }

  // Gemma 2 9B. Match cobre "@hf/google/gemma-2-9b-it" e variantes.
  if (/gemma-?2.*9b/.test(lower)) {
    return {
      idInterno: id,
      provedor: "cloudflare_workers_ai",
      familia: "Gemma",
      versao: "2",
      tamanho: "9B",
      variante: lower.includes("-it") || lower.includes("instruct") ? "Instruction Tuned" : null,
      nomeExibicao: "Gemma 2 9B",
      nomeCompletoComercial: "Linka IA — Gemma 2 9B",
      descricaoComercial: "Diagnóstico inteligente otimizado para respostas rápidas",
      textoRodape: "Motor de análise: Linka IA — Gemma 2 9B",
    };
  }

  // Gemma 4 26B (modelo descartado — timeout). Match cobre "@cf/google/gemma-4-26b-a4b-it".
  if (/gemma-?4.*26b/.test(lower)) {
    const variante = lower.includes("-it") || lower.includes("instruct")
      ? "Instruction Tuned"
      : null;
    return {
      idInterno: id,
      provedor: "cloudflare_workers_ai",
      familia: "Gemma",
      versao: "4",
      tamanho: "26B",
      variante,
      nomeExibicao: "Gemma 4 26B",
      nomeCompletoComercial: "Linka IA — Gemma 4 26B",
      descricaoComercial: "Diagnóstico inteligente otimizado para respostas rápidas",
      textoRodape: "Motor de análise: Linka IA — Gemma 4 26B",
    };
  }

  // Gemma generico (qualquer outra versao Gemma configurada manualmente).
  if (lower.includes("gemma")) {
    const tamanho = (lower.match(/(\d+)b/) || [])[1];
    const versao = (lower.match(/gemma-?(\d+(?:\.\d+)?)/) || [])[1] || null;
    return {
      idInterno: id,
      provedor: "cloudflare_workers_ai",
      familia: "Gemma",
      versao,
      tamanho: tamanho ? `${tamanho.toUpperCase()}B` : null,
      variante: lower.includes("-it") || lower.includes("instruct") ? "Instruction Tuned" : null,
      nomeExibicao: "Gemma",
      nomeCompletoComercial: "Linka IA — Gemma",
      descricaoComercial: "Diagnóstico inteligente de conexão",
      textoRodape: "Motor de análise: Linka IA — Gemma",
    };
  }

  // Llama: reconhecido apenas para retrocompat caso AI_MODEL seja configurado
  // manualmente. NAO e usado como fallback automatico — a politica do projeto
  // proibe Llama/Meta como motor padrao ou fallback cloud do Linka.
  if (lower.includes("llama")) {
    const tamanho = (lower.match(/(\d+)b/) || [])[1];
    const versao = (lower.match(/llama-?(\d+(?:\.\d+)?)/) || [])[1] || null;
    return {
      idInterno: id,
      provedor: "cloudflare_workers_ai",
      familia: "Llama",
      versao,
      tamanho: tamanho ? `${tamanho.toUpperCase()}B` : null,
      variante: lower.includes("instruct-fp8-fast")
        ? "instruct-fp8-fast"
        : lower.includes("instruct")
          ? "instruct"
          : null,
      nomeExibicao: "Linka IA",
      nomeCompletoComercial: "Linka IA",
      descricaoComercial: "Diagnóstico inteligente de conexão",
      textoRodape: "Motor de análise: Linka IA",
    };
  }

  // Generico (modelo desconhecido). Nao expor id interno ao usuario.
  return {
    idInterno: id,
    provedor: id ? "cloudflare_workers_ai" : "desconhecido",
    familia: "Outro",
    versao: null,
    tamanho: null,
    variante: null,
    nomeExibicao: "IA",
    nomeCompletoComercial: "Linka IA",
    descricaoComercial: "Diagnóstico inteligente de conexão",
    textoRodape: "Motor de análise: Linka IA",
  };
}

// =============================================================================
// Utilidades HTTP/JSON
// =============================================================================

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json; charset=utf-8" },
  });
}

function errorResponse(code: string, status: number): Response {
  return jsonResponse({ error: { code } }, status);
}

async function readJsonLimited(req: Request): Promise<unknown | null> {
  const buf = await req.arrayBuffer();
  if (buf.byteLength > MAX_BODY_BYTES) return null;
  try {
    return JSON.parse(new TextDecoder().decode(buf));
  } catch {
    return null;
  }
}

// Extrai a string de texto da resposta da IA. Workers AI retorna shapes
// diferentes por modelo (Llama: { response: "..." }; Gemma/chat: pode vir
// { response: { ... } }, { choices: [{ message: { content: "..." } }] },
// { result: { response: "..." } }, etc.). Esta funcao percorre os pontos
// mais comuns ate achar uma string nao-vazia.
function extractRawText(aiResult: unknown): string {
  if (typeof aiResult === "string") return aiResult;
  if (aiResult == null || typeof aiResult !== "object") return "";

  const obj = aiResult as Record<string, unknown>;

  // 1) { response: "..." } (Llama, alguns modelos do binding [ai])
  if (typeof obj.response === "string") return obj.response;

  // 2) { response: { ... } } — Gemma 4 26B as vezes encapsula
  if (obj.response && typeof obj.response === "object") {
    const inner = obj.response as Record<string, unknown>;
    if (typeof inner.text === "string") return inner.text;
    if (typeof inner.content === "string") return inner.content;
    if (typeof inner.message === "string") return inner.message;
    if (inner.message && typeof inner.message === "object") {
      const msg = inner.message as Record<string, unknown>;
      if (typeof msg.content === "string") return msg.content;
    }
    if (Array.isArray(inner.choices) && inner.choices.length > 0) {
      const c = inner.choices[0] as Record<string, unknown>;
      if (typeof c.text === "string") return c.text;
      if (c.message && typeof c.message === "object") {
        const m = c.message as Record<string, unknown>;
        if (typeof m.content === "string") return m.content;
      }
    }
  }

  // 3) Estilo OpenAI chat completion: { choices: [{ message: { content: "..." } }] }
  if (Array.isArray(obj.choices) && obj.choices.length > 0) {
    const c = obj.choices[0] as Record<string, unknown>;
    if (typeof c.text === "string") return c.text;
    if (c.message && typeof c.message === "object") {
      const m = c.message as Record<string, unknown>;
      if (typeof m.content === "string") return m.content;
    }
  }

  // 4) { result: { response: "..." } } (envelope da REST API Cloudflare)
  if (obj.result && typeof obj.result === "object") {
    return extractRawText(obj.result);
  }

  // 5) Campos diretos comuns (text, content, output, message)
  if (typeof obj.text === "string") return obj.text;
  if (typeof obj.content === "string") return obj.content;
  if (typeof obj.output === "string") return obj.output;
  if (typeof obj.message === "string") return obj.message;
  if (obj.message && typeof obj.message === "object") {
    const m = obj.message as Record<string, unknown>;
    if (typeof m.content === "string") return m.content;
  }

  return "";
}

function extractJson(text: string): string {
  // Remove blocos markdown ``` se a IA insistir em usa-los apesar do prompt
  const fenced = text.match(/```(?:json)?\s*([\s\S]*?)```/);
  if (fenced) return fenced[1].trim();
  // Recorta entre o primeiro { e o ultimo }
  const start = text.indexOf("{");
  const end = text.lastIndexOf("}");
  if (start !== -1 && end > start) return text.slice(start, end + 1);
  return text.trim();
}

// =============================================================================
// Handler
// =============================================================================

export default {
  async fetch(req: Request, env: Env): Promise<Response> {
    try {
      const url = new URL(req.url);
      if (url.pathname !== "/api/ai/diagnostico-conexao") return errorResponse("not_found", 404);
      if (req.method !== "POST") return errorResponse("method_not_allowed", 405);

      const payload = await readJsonLimited(req);
      if (payload == null) return errorResponse("payload_invalid", 400);

      const model = env.AI_MODEL ?? DEFAULT_MODEL;
      const modelInfo = getCommercialModelInfo(model);
      // Log tecnico: id Cloudflare bruto fica somente nos logs do Worker, nunca
      // na resposta exibida ao usuario. Util para auditar qual modelo respondeu.
      console.log("AI diagnosis model:", model);

      // Detecta modo chat: quando feedbackUsuario esta presente o cliente envia
      // uma pergunta de follow-up — o Worker muda tom e schema para resposta direta.
      const isChat = !!(payload as Record<string, unknown>).feedbackUsuario;
      const systemPrompt = isChat ? CHAT_SYSTEM_PROMPT : SYSTEM_PROMPT;
      const schemaHint = isChat ? CHAT_SCHEMA_HINT : SCHEMA_HINT;
      console.log("AI diagnosis mode:", isChat ? "chat" : "diagnostic");

      // Modo streaming SSE: ativado via ?stream=true. Retorna ReadableStream com
      // chunks no formato "data: {\"response\":\"token\"}\n\n" e termina com
      // "data: [DONE]\n\n". O Workers AI para gemma-7b-it emite este formato
      // nativamente com stream:true — nao e necessario transformar o stream.
      // O endpoint sem ?stream=true continua retornando JSON completo (inalterado).
      const isStream = url.searchParams.get("stream") === "true";
      if (isStream) {
        let streamResult: ReadableStream;
        try {
          streamResult = await env.AI.run(model, {
            messages: [
              { role: "system", content: systemPrompt },
              {
                role: "user",
                content: `Dados do diagnóstico:\n${JSON.stringify(payload)}\n\n${schemaHint}`,
              },
            ],
            max_tokens: 5000,
            temperature: 0.2,
            stream: true,
          }) as ReadableStream;
        } catch (aiErr: unknown) {
          const errMsg = aiErr instanceof Error ? aiErr.message : String(aiErr);
          console.error("env.AI.run stream FAILED, model:", model, "error:", errMsg);
          return errorResponse("ai_run_failed", 503);
        }
        return new Response(streamResult, {
          headers: {
            "Content-Type": "text/event-stream",
            "Cache-Control": "no-cache",
          },
        });
      }

      // Nao usamos response_format json_schema do Workers AI nesta versao porque:
      //  - Suporte ainda e instavel/inconsistente para varios modelos do binding [ai];
      //  - Habilitar JSON Mode antes de validacao especifica para Gemma 4 26B
      //    pode quebrar o fluxo (resposta vazia, truncada ou nao-JSON).
      // Preferimos schema hint forte no prompt + extracao tolerante (extractJson)
      // + sobrescrita defensiva pos-parse. Quando houver validacao clara de que
      // JSON Mode nao quebra Gemma/Workers AI, trocar para
      // response_format: { type: "json_schema", json_schema: ... }.
      let aiResult: unknown;
      try {
        aiResult = await env.AI.run(model, {
          messages: [
            { role: "system", content: systemPrompt },
            {
              role: "user",
              content:
                `Dados do diagnóstico:\n${JSON.stringify(payload)}\n\n${schemaHint}`,
            },
          ],
          // Gemma 4 26B-a4b consome tokens em um campo `reasoning` antes de
          // emitir `content`. Reasoning: 500-2500 tokens; JSON final: 700-1100.
          // Total tipico: ~3500 tokens. 5000 garante margem sem ultrapassar
          // o budget de inferencia — reduzido de 6000 pois o gargalo real era
          // o readTimeout de 30 s no Android (agora 90 s).
          max_tokens: 5000,
          temperature: 0.2,
        });
        console.log("env.AI.run completed, model:", model, "result type:", typeof aiResult);
      } catch (aiErr: unknown) {
        const errMsg = aiErr instanceof Error ? aiErr.message : String(aiErr);
        console.error("env.AI.run FAILED, model:", model, "error:", errMsg);
        return errorResponse("ai_run_failed", 503);
      }

      // Extrai texto bruto da resposta da IA. Diferentes modelos no Workers AI
      // retornam shapes diferentes:
      //   - Llama: { response: "<texto>" }
      //   - Gemma 4 26B: { response: { ... } } com choices/text dentro, ou
      //     ainda { result: { response: "..." } } / { choices: [{...}] }.
      // Tentamos varios pontos comuns ate achar uma string.
      const raw: string = extractRawText(aiResult);

      let parsed: Record<string, unknown>;
      try {
        parsed = JSON.parse(extractJson(raw)) as Record<string, unknown>;
      } catch {
        // Log tecnico: registra primeiros 1000 chars pra inspecao via wrangler tail.
        console.log("ai_json_parse_failed raw[0..1000]:", raw.slice(0, 1000));
        return errorResponse("ai_json_parse_failed", 502);
      }

      // Sobrescrita defensiva: garantimos consistencia mesmo se a IA inventar
      // ou divergir desses campos. modeloIa nunca confia no que a IA disse.
      parsed.schemaVersion = SCHEMA_VERSION;
      parsed.source = "cloudflare_ai";
      parsed.generatedAt = Date.now();
      parsed.modeloIa = modelInfo;

      return jsonResponse(parsed);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      console.error("Worker internal_error:", msg);
      return errorResponse("internal_error", 500);
    }
  },
};
