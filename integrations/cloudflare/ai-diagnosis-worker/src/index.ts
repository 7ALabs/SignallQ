// =============================================================================
// SignallQ AI Diagnosis Worker
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

import {
  AiProviderRouter,
  GeminiFlashProvider,
  QwenCFProvider,
} from "./providers";
import type { ProviderResult } from "./providers";
import { extractJson, stripThinkingTokens } from "./text-parsing.ts";

type Env = {
  AI: any;
  AI_MODEL?: string;
  // Provedor primário de IA (opcional — sem esta var usa Qwen/CF como primário).
  GEMINI_API_KEY?: string;
  // Ingestão de métricas no painel admin (opcional — sem estas vars o worker
  // continua funcionando normalmente, apenas não reporta ao D1).
  ADMIN_WORKER_URL?: string;
  ADMIN_SECRET?: string;
};

const MAX_BODY_BYTES = 64_000;
// Modelo padrao: Qwen3 30B MoE FP8 (Alibaba/Qwen) via Cloudflare Workers AI.
// MoE com 30B total / 3B ativos — inferencia rapida, boa qualidade para JSON
// estruturado e portugues. FP8 quantizado, sem reasoning tokens extras.
// Modelos descartados:
//   @cf/google/gemma-7b-it       — Gemma v1, 8K contexto, deprecation planejado, fraco para prompt complexo
//   @cf/google/gemma-4-26b-a4b-it — gerava 2500+ tokens de reasoning → timeout >30s
//   @hf/google/gemma-2-9b-it     — formato @hf/ incompativel com messages API
// Llama/Meta NAO e padrao nem fallback cloud — politica do projeto.
const DEFAULT_MODEL = "@cf/qwen/qwen3-30b-a3b-fp8";

// SCHEMA versionado do payload de saida; o cliente Kotlin precisa aceitar 1 e 2.
const SCHEMA_VERSION = "2" as const;

// Versao do prompt/schema de ENTRADA (SIG-282), espelhando o versionamento de
// AI_PROMPT_VERSION em AiModels.kt (lado Android). Nao confundir com
// SCHEMA_VERSION acima, que versiona o schema de SAIDA da resposta da IA.
//
// Historico de versoes do prompt:
//   diagnostico_v2          — schema v2 com Llama 3.3 70B como padrao.
//   diagnostico_v2_gemma4   — troca para Gemma 4 26B como motor padrao.
//   diagnostico_v3_raw      — payload reformatado: somente dados brutos, IA
//                             faz toda a analise.
//   diagnostico_v4_guided   — adiciona achadosLocais (decisao do engine local),
//                             mas o campo ainda chegava vazio/ausente na
//                             pratica (Fase 1 do payload real ainda nao tinha
//                             sido implementada no Android) — regra 18 ficava
//                             inerte na maior parte das analises.
//   diagnostico_v5_local_primary (ATUAL) — achadosLocais agora chega de fato
//                             (Fase 1 em producao) com rttGatewayMs; regra 18
//                             deixa de ser inerte: confianca >= 0.75 faz o
//                             motor local ser a decisao primaria, a IA so
//                             valida/explica. Nova regra 18a: quando o motor
//                             local (Fases 3a/3b) enviar impacto pronto por
//                             perfil de uso (aba 9, vocabulario proprio
//                             OK/Instavel/Comprometido — separado do
//                             vocabulario canonico de status
//                             excelente/bom/regular/ruim/critico/inconclusivo),
//                             a IA narra o valor recebido e nao decide mais o
//                             campo impacto. CHAT_SYSTEM_PROMPT e a rota de
//                             chat estao fora de escopo desta versao.
const AI_PROMPT_VERSION = "diagnostico_v5_local_primary" as const;

const SYSTEM_PROMPT = `Você é o motor de diagnóstico inteligente do app SignallQ, especializado em conexões de internet doméstica no Brasil.
Você recebe dados brutos coletados pelo app (métricas numéricas, contexto de rede sem rótulos, histórico de medições, opcionalmente feedback do usuário). Quando o campo "achadosLocais" estiver presente no payload, ele contém a conclusão do motor determinístico local — use-o como decisão primária (regra 18), valide contra as métricas e explique em linguagem clara. Quando "achadosLocais" estiver ausente, toda a análise é sua.
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
11. O impacto por uso deve refletir apenas o que os dados indicam. Não diga que jogos terão alta latência se a latência medida estiver boa. Não diga que streaming está comprometido se a velocidade estiver boa e não houver perda/jitter relevante. Exceção: quando o payload trouxer impacto por perfil já pronto do motor local, siga a regra 18a em vez desta (narrar, não decidir).
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
18. CAMPO achadosLocais (schema v4, ATIVO em produção desde v5 — o app Android agora envia este campo com dados reais na maioria das análises, não é mais um campo raramente presente): quando presente, use desta forma:
   - achadosLocais.decisaoId e achadosLocais.statusGeral indicam o que o motor local concluiu.
   - achadosLocais.confianca indica o quanto o motor local confia na conclusão (0.0–1.0).
   - Se confianca >= 0.75: a conclusão do motor local é a DECISÃO PRIMÁRIA. Não re-decida do zero — sua função é validar contra os dados brutos, enriquecer e EXPLICAR em linguagem humana o que o motor local já concluiu. O status, o problemaPrincipal.tipo e o título devem ser consistentes com achadosLocais.statusGeral, salvo evidência clara e explícita nos dados brutos que contradiga a conclusão local (situação rara — documente em limitesDaAnalise se isso ocorrer).
   - Se confianca < 0.75: analise os dados com mais liberdade, mas mencione o achado local como hipótese de partida.
   - achadosLocais.resultadosRelevantes lista os ids dos findings que sustentam a conclusão — use-os para citar evidências.
   - achadosLocais.score é orientativo (0–100). Mantenha consistência: status "ok" não combina com score 20.
   - Se download e upload estão bons mas latência/jitter altos → título deve refletir estabilidade (ex.: "Conexão instável", "Velocidade boa, estabilidade ruim", "Latência e jitter elevados").
   - Se a velocidade está abaixo do esperado mas latência normal → título deve apontar velocidade (ex.: "Velocidade abaixo do contratado", "Banda insuficiente").
   - Se há perda de pacotes significativa → título deve mencionar perda (ex.: "Perda de pacotes detectada").
   - "Internet lenta" só é aceitável quando NÃO há dados de latência/jitter/perda E download/upload estão de fato baixos.
18a. CAMPO impacto QUANDO o payload trouxer, dentro de achadosLocais (ou bloco irmão equivalente), os 5 valores de impacto por perfil de uso (navegação/streaming/jogos/videochamada/trabalho) já PRONTOS: esses valores vêm do motor local (classificador determinístico de perfis de uso, aba 9 do produto). Você NÃO decide nem recalcula esses valores — sua única função é NARRAR/EXPLICAR o valor recebido em linguagem humana no textoLaudo/resumo, e replicar no campo "impacto" da sua resposta os MESMOS valores recebidos (apenas convertidos para o vocabulário de saída do schema, ver mapeamento abaixo). PROIBIDO escolher um valor de impacto diferente do que veio pronto.
   - Vocabulário de ENTRADA do motor local para perfis de uso (aba 9) é PRÓPRIO e SEPARADO do vocabulário de "status": "OK" | "Instavel" | "Comprometido" — não confunda com excelente/bom/regular/ruim/critico/inconclusivo (esse é o vocabulário de "status", nunca use "excelente" ou "critico" dentro do campo impacto).
   - Mapeamento de referência ao narrar (adapte ao rótulo mais específico do schema de impacto quando a métrica indicar a causa, ex. "Alta latencia" para jogos): OK → "OK"; Instavel → "Instavel" (ou rótulo específico do perfil, ex. "Lento", "Alta latencia"); Comprometido → "Comprometido"/"Indisponivel" conforme severidade.
   - Quando o payload NÃO trouxer impacto pronto por perfil — caso ainda comum enquanto o motor local está em rollout — o campo impacto continua sendo decidido por você, seguindo a regra 11 (refletir apenas o que os dados indicam).

EXEMPLO OBRIGATÓRIO (payload SEM impacto pronto por perfil — impacto decidido por você conforme regra 11; quando o payload trouxer impacto pronto, siga a regra 18a em vez deste exemplo):
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
const CHAT_SYSTEM_PROMPT = `Você é o SignallQ, assistente técnico de conexão à internet.

COMPORTAMENTO:
1. Responda com resolução COMPLETA incluindo passo a passo detalhado.
   - Para ajustes técnicos (trocar canal Wi-Fi, configurar DNS, acessar painel do roteador),
     explique cada etapa: onde acessar, o que clicar, que valor colocar, como salvar.
   - Inclua IPs de painel comuns (192.168.0.1, 192.168.1.1) e caminhos de menu quando relevante.
   - Não limite o tamanho da resposta. Qualidade e completude valem mais que brevidade.

2. Faça perguntas de volta quando necessário para dar instruções mais precisas.
   - Exemplos: "Qual a marca e modelo do seu roteador?", "Está usando Wi-Fi 2.4 ou 5 GHz?",
     "O problema acontece em todos os dispositivos ou só neste?"
   - Continue a conversa até entender e resolver o problema do usuário.

3. RESTRIÇÃO DE TEMA — INVIOLÁVEL.
   Responda APENAS sobre: internet, Wi-Fi, roteador, modem, velocidade, latência, DNS, fibra,
   sinal, rede móvel (4G/5G), operadora, configuração de rede, e uso de aplicativos
   QUANDO relacionado a conexão (streaming travando, jogo com lag, chamada caindo).

   Para qualquer outro assunto, responda educadamente:
   "Só posso ajudar com questões de conexão e rede. Para [tema], consulte [sugestão]."

   Não responda perguntas sobre: saúde, finanças, religião, política, receitas, código,
   matemática, história, entretenimento, ou qualquer tema não relacionado a redes.

4. USE DADOS DO PAYLOAD quando disponíveis.
   - Referencie métricas reais: "sua latência de 22 ms está boa, mas o bufferbloat de 182 ms
     explica as travadas durante chamadas".
   - Personalize com base no contexto da rede: operadora, tipo de conexão, SSID, sinal.
   - Não invente dados que não estão no payload.

5. TOM E FORMATO.
   - Tom: técnico mas acessível, direto, sem enrolação.
   - Use listas numeradas para passos sequenciais.
   - Use bullet points para alternativas ou opções.
   - Negrito para termos-chave (**SQM**, **canal 1 ou 11**, **5 GHz**).
   - Não use emojis.
   - Responda em português brasileiro com "você".
   - Não se apresente — o usuário já sabe que está falando com o SignallQ.

6. FORMATO DE RESPOSTA depende do modo:
   - Modo NÃO-streaming (sem ?stream=true): responda exclusivamente em JSON válido seguindo o schema informado. Não use markdown, não explique fora do JSON e não adicione texto antes ou depois.
   - Modo streaming (?stream=true): responda em TEXTO PURO em português brasileiro. Use markdown leve (negrito, listas numeradas, bullet points). NÃO use JSON, NÃO envolva em blocos de código. Escreva diretamente a resposta útil ao usuário.`;

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
//
// Vocabulario canonico de "status" (decisao de arquitetura ja fechada, mesmo
// vocabulario usado pelo ScoreEngine/MetricClassifier locais no Android):
// excelente | bom | regular | ruim | critico | inconclusivo — 6 valores, ver
// campo "status" abaixo. NAO confundir com o vocabulario de "impacto" (perfis
// de uso, aba 9), que e PROPRIO e SEPARADO: OK | Instavel | Comprometido (ou
// rotulos mais especificos por perfil, ex. "Alta latencia" para jogos). Ver
// regra 18a do SYSTEM_PROMPT para o mapeamento entre os dois vocabularios.
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

  // Qwen3 30B MoE FP8 — modelo padrao atual. Match cobre "@cf/qwen/qwen3-30b-a3b-fp8".
  if (/qwen3.*30b/.test(lower)) {
    return {
      idInterno: id,
      provedor: "cloudflare_workers_ai",
      familia: "Qwen",
      versao: "3",
      tamanho: "30B",
      variante: lower.includes("fp8") ? "FP8" : null,
      nomeExibicao: "Qwen3 30B",
      nomeCompletoComercial: "SignallQ IA — Qwen3 30B",
      descricaoComercial: "Diagnóstico inteligente de conexão",
      textoRodape: "Motor de análise: SignallQ IA — Qwen3 30B",
    };
  }

  // Qwen generico (qualquer outra versao Qwen configurada manualmente).
  if (lower.includes("qwen")) {
    const tamanho = (lower.match(/(\d+)b/) || [])[1];
    const versao = (lower.match(/qwen-?(\d+(?:\.\d+)?)/) || [])[1] || null;
    return {
      idInterno: id,
      provedor: "cloudflare_workers_ai",
      familia: "Qwen",
      versao,
      tamanho: tamanho ? `${tamanho}B` : null,
      variante: lower.includes("fp8") ? "FP8" : lower.includes("instruct") ? "Instruct" : null,
      nomeExibicao: "Qwen",
      nomeCompletoComercial: "SignallQ IA — Qwen",
      descricaoComercial: "Diagnóstico inteligente de conexão",
      textoRodape: "Motor de análise: SignallQ IA — Qwen",
    };
  }

  // Gemma 7B-IT. Match cobre "@cf/google/gemma-7b-it".
  if (/gemma-?7b/.test(lower)) {
    return {
      idInterno: id,
      provedor: "cloudflare_workers_ai",
      familia: "Gemma",
      versao: "1",
      tamanho: "7B",
      variante: lower.includes("-it") || lower.includes("instruct") ? "Instruction Tuned" : null,
      nomeExibicao: "Gemma 7B",
      nomeCompletoComercial: "SignallQ IA — Gemma 7B",
      descricaoComercial: "Diagnóstico inteligente de conexão",
      textoRodape: "Motor de análise: SignallQ IA — Gemma 7B",
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
      nomeCompletoComercial: "SignallQ IA — Gemma 2 9B",
      descricaoComercial: "Diagnóstico inteligente otimizado para respostas rápidas",
      textoRodape: "Motor de análise: SignallQ IA — Gemma 2 9B",
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
      nomeCompletoComercial: "SignallQ IA — Gemma 4 26B",
      descricaoComercial: "Diagnóstico inteligente otimizado para respostas rápidas",
      textoRodape: "Motor de análise: SignallQ IA — Gemma 4 26B",
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
      nomeCompletoComercial: "SignallQ IA — Gemma",
      descricaoComercial: "Diagnóstico inteligente de conexão",
      textoRodape: "Motor de análise: SignallQ IA — Gemma",
    };
  }

  // Llama: reconhecido apenas para retrocompat caso AI_MODEL seja configurado
  // manualmente. NAO e usado como fallback automatico — a politica do projeto
  // proibe Llama/Meta como motor padrao ou fallback cloud do SignallQ.
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
      nomeExibicao: "SignallQ IA",
      nomeCompletoComercial: "SignallQ IA",
      descricaoComercial: "Diagnóstico inteligente de conexão",
      textoRodape: "Motor de análise: SignallQ IA",
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
    nomeCompletoComercial: "SignallQ IA",
    descricaoComercial: "Diagnóstico inteligente de conexão",
    textoRodape: "Motor de análise: SignallQ IA",
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

// Normaliza o stream SSE upstream para o formato simples que o cliente Android
// espera: "data: {\"response\":\"token\"}\n\n" + "data: [DONE]\n\n".
//
// Modelos no Workers AI emitem dois formatos de SSE:
//  1) Formato simples (Workers AI legado): data: {"response":"token"}
//  2) Formato OpenAI Chat Completions (Qwen3, etc.):
//     data: {"choices":[{"delta":{"content":"token"}}]}
//     com thinking em: {"choices":[{"delta":{"reasoning_content":"..."}}]}
//
// Parâmetro passThinking:
//  - false (default, modo diagnóstico): ignora reasoning_content e filtra
//    tags <think>...</think> inline.
//  - true (modo chat): passa reasoning_content ao cliente envolvido em tags
//    <think>...</think> e não filtra tags inline.
function createStreamNormalizer(upstream: ReadableStream, passThinking = false): ReadableStream {
  let insideThink = false;
  let inReasoning = false; // rastreia bloco contínuo de reasoning_content
  let buffer = "";
  let sentDone = false;

  return new ReadableStream({
    async start(controller) {
      const reader = upstream.getReader();
      const decoder = new TextDecoder();
      const encoder = new TextEncoder();

      function emit(text: string) {
        if (!text) return;
        controller.enqueue(encoder.encode(`data: ${JSON.stringify({ response: text })}\n\n`));
      }

      try {
        while (true) {
          const { done, value } = await reader.read();
          if (done) {
            // Se stream terminou dentro de reasoning, fecha a tag
            if (passThinking && inReasoning) {
              emit("</think>\n\n");
              inReasoning = false;
            }
            if (!sentDone) {
              controller.enqueue(encoder.encode("data: [DONE]\n\n"));
            }
            controller.close();
            break;
          }

          buffer += decoder.decode(value, { stream: true });
          const lines = buffer.split("\n");
          buffer = lines.pop() || "";

          for (const line of lines) {
            if (!line.startsWith("data: ")) continue;

            const data = line.slice(6).trim();
            if (data === "[DONE]") {
              if (passThinking && inReasoning) {
                emit("</think>\n\n");
                inReasoning = false;
              }
              sentDone = true;
              controller.enqueue(encoder.encode("data: [DONE]\n\n"));
              continue;
            }

            let token = "";
            try {
              const parsed = JSON.parse(data);

              // Formato OpenAI: choices[0].delta
              if (parsed.choices && Array.isArray(parsed.choices) && parsed.choices.length > 0) {
                const delta = parsed.choices[0]?.delta;
                if (delta) {
                  if (delta.reasoning_content !== undefined && typeof delta.reasoning_content === "string") {
                    if (passThinking && delta.reasoning_content) {
                      if (!inReasoning) {
                        emit("<think>");
                        inReasoning = true;
                      }
                      emit(delta.reasoning_content);
                    }
                    continue;
                  }
                  // Chegou content normal — se estávamos em reasoning, fecha a tag
                  if (passThinking && inReasoning) {
                    emit("</think>\n\n");
                    inReasoning = false;
                  }
                  token = delta.content ?? "";
                }
              }
              // Formato simples Workers AI: response
              else if (typeof parsed.response === "string") {
                token = parsed.response;
              }
            } catch {
              continue;
            }

            if (!token) continue;

            if (passThinking) {
              // Modo chat: passa tags <think>...</think> inline sem filtrar
              emit(token);
              continue;
            }

            // Modo diagnóstico: filtra <think>...</think> inline
            if (token.includes("<think>")) {
              insideThink = true;
              const before = token.slice(0, token.indexOf("<think>"));
              emit(before);
              if (token.includes("</think>")) {
                insideThink = false;
                const after = token.slice(token.indexOf("</think>") + 8);
                emit(after);
              }
              continue;
            }
            if (insideThink) {
              if (token.includes("</think>")) {
                insideThink = false;
                const after = token.slice(token.indexOf("</think>") + 8);
                emit(after);
              }
              continue;
            }

            emit(token);
          }
        }
      } catch (err) {
        controller.error(err);
      }
    },
  });
}

// =============================================================================
// Ingestão no painel admin
// Fire-and-forget via ctx.waitUntil — nunca bloqueia nem quebra o diagnóstico.
// =============================================================================

function generateId(): string {
  return `diag_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 7)}`;
}

async function ingestToPainel(
  env: Env,
  sessionId: string,
  payload: Record<string, unknown>,
  parsed: Record<string, unknown>,
  model: string,
): Promise<void> {
  if (!env.ADMIN_WORKER_URL || !env.ADMIN_SECRET) return;

  const headers = {
    "Content-Type": "application/json",
    Authorization: `Bearer ${env.ADMIN_SECRET}`,
  };
  const baseUrl = env.ADMIN_WORKER_URL.replace(/\/$/, "");
  const now = Math.floor(Date.now() / 1000);

  // Extrair métricas do payload (campos que o app Android envia)
  const metricas = (payload.metricasAtuais ?? {}) as Record<string, unknown>;
  const networkType = (payload.connectionType as string | undefined)
    ?? (payload.tipoConexao as string | undefined)
    ?? "unknown";
  const status  = (parsed.status  as string | undefined) ?? "unknown";
  const titulo  = (parsed.titulo  as string | undefined) ?? "";
  const issues: string[] = titulo ? [titulo] : [];

  const scoreRaw = (parsed.pontuacao ?? parsed.score) as number | undefined;
  const score    = typeof scoreRaw === "number" ? Math.round(scoreRaw) : null;

  await Promise.allSettled([
    fetch(`${baseUrl}/ingest/diagnostic`, {
      method: "POST",
      headers,
      body: JSON.stringify({
        id: sessionId,
        created_at: now,
        network_type: networkType,
        status,
        score,
        download_mbps:  (metricas.downloadMbps  ?? metricas.download)  as number | null,
        upload_mbps:    (metricas.uploadMbps    ?? metricas.upload)    as number | null,
        latency_ms:     (metricas.latenciaMs    ?? metricas.latency)   as number | null,
        jitter_ms:      (metricas.jitterMs      ?? metricas.jitter)    as number | null,
        packet_loss:    (metricas.perdaPacotes  ?? metricas.packetLoss) as number | null,
        issues,
      }),
    }),
    fetch(`${baseUrl}/ingest/ai-usage`, {
      method: "POST",
      headers,
      body: JSON.stringify({
        id: `${sessionId}_ai`,
        session_id: sessionId,
        created_at: now,
        model,
      }),
    }),
  ]);
}

// =============================================================================
// Handler
// =============================================================================

export default {
  async fetch(req: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
    try {
      const url = new URL(req.url);

      if (url.pathname === "/health" && req.method === "GET") {
        return jsonResponse({ status: "ok", worker: "ai-diagnosis-worker", timestamp: new Date().toISOString() });
      }

      if (url.pathname !== "/api/ai/diagnostico-conexao") return errorResponse("not_found", 404);
      if (req.method !== "POST") return errorResponse("method_not_allowed", 405);

      const payload = await readJsonLimited(req);
      if (payload == null) return errorResponse("payload_invalid", 400);

      const sessionId = generateId();

      const model = env.AI_MODEL ?? DEFAULT_MODEL;
      const modelInfo = getCommercialModelInfo(model);
      // Log tecnico: id Cloudflare bruto fica somente nos logs do Worker, nunca
      // na resposta exibida ao usuario. Util para auditar qual modelo respondeu.
      console.log("AI diagnosis model:", model);

      // Monta a cadeia de providers: Gemini Flash (se configurado) → Qwen/CF.
      // Adicionar GEMINI_API_KEY como secret do Worker ativa o provider primário.
      const providers = [];
      if (env.GEMINI_API_KEY) providers.push(new GeminiFlashProvider(env.GEMINI_API_KEY));
      providers.push(new QwenCFProvider(env.AI, model, modelInfo as Record<string, unknown>));
      const router = new AiProviderRouter(providers);

      // Detecta modo chat: quando feedbackUsuario esta presente o cliente envia
      // uma pergunta de follow-up — o Worker muda tom e schema para resposta direta.
      const isChat = !!(payload as Record<string, unknown>).feedbackUsuario;
      const systemPrompt = isChat ? CHAT_SYSTEM_PROMPT : SYSTEM_PROMPT;
      const schemaHint = isChat ? CHAT_SCHEMA_HINT : SCHEMA_HINT;
      console.log("AI diagnosis mode:", isChat ? "chat" : "diagnostic", "promptVersion:", AI_PROMPT_VERSION);

      // Modo streaming SSE: ativado via ?stream=true. Retorna ReadableStream com
      // chunks no formato "data: {\"response\":\"token\"}\n\n" e termina com
      // "data: [DONE]\n\n". Workers AI emite este formato nativamente com
      // stream:true. O stream passa por createStreamNormalizer para suprimir
      // tokens de <think>...</think> que modelos reasoning podem emitir.
      //
      // Chat streaming: resposta em texto puro (sem JSON) para exibição direta
      // na UI de chat. O schema hint é omitido e o prompt instrui texto livre.
      const isStream = url.searchParams.get("stream") === "true";
      if (isStream) {
        const streamUserContent = isChat
          ? `Contexto da rede do usuário:\n${JSON.stringify(payload)}\n\nPergunta do usuário: ${(payload as Record<string, unknown>).feedbackUsuario}\n\nResponda em texto puro, direto e prático. Não use JSON.`
          : `Dados do diagnóstico:\n${JSON.stringify(payload)}\n\n${schemaHint}`;

        let streamResult: ReadableStream;
        try {
          const routerStream = await router.callStream(
            systemPrompt,
            streamUserContent,
            isChat ? 8000 : 5000,
            0.2,
          );
          streamResult = routerStream.stream;
        } catch (aiErr: unknown) {
          const errMsg = aiErr instanceof Error ? aiErr.message : String(aiErr);
          console.error("router.callStream FAILED:", errMsg);
          return errorResponse("ai_run_failed", 503);
        }
        return new Response(createStreamNormalizer(streamResult, isChat), {
          headers: {
            "Content-Type": "text/event-stream",
            "Cache-Control": "no-cache",
          },
        });
      }

      // Nao usamos response_format json_schema do Workers AI nesta versao porque
      // o suporte ainda e instavel/inconsistente para varios modelos do binding [ai].
      // Preferimos schema hint forte no prompt + extracao tolerante (extractJson
      // com stripThinkingTokens) + sobrescrita defensiva pos-parse.
      let providerResult: ProviderResult;
      try {
        const userContent = `Dados do diagnóstico:\n${JSON.stringify(payload)}\n\n${schemaHint}`;
        providerResult = await router.call(systemPrompt, userContent, isChat ? 8000 : 5000, 0.2);
        console.log("router.call completed, provider:", providerResult.providerId);
      } catch (aiErr: unknown) {
        const errMsg = aiErr instanceof Error ? aiErr.message : String(aiErr);
        console.error("router.call FAILED:", errMsg);
        return errorResponse("ai_run_failed", 503);
      }

      const raw: string = providerResult.text;

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
      parsed.modeloIa = providerResult.modeloIa;

      // Ingerir métricas no painel admin de forma assíncrona, sem bloquear resposta.
      ctx.waitUntil(
        ingestToPainel(
          env,
          sessionId,
          payload as Record<string, unknown>,
          parsed,
          providerResult.effectiveModelId,
        ),
      );

      return jsonResponse(parsed);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : String(err);
      console.error("Worker internal_error:", msg);
      return errorResponse("internal_error", 500);
    }
  },
};
