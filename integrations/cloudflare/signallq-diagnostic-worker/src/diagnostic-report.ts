import type {
  DiagnosticFinding,
  DiagnosticFlowCode,
  DiagnosticResult,
  DiagnosticSnapshot,
  GameProfileRecord,
} from "./contracts.ts";
import { veredictoHumanoDoScore, type VeredictoHumano } from "./score-engine.ts";

type ReportStatus = "ok" | "info" | "attention" | "critical" | "inconclusive";

export interface DiagnosticCard {
  id: string;
  titulo: string;
  status: ReportStatus;
  evidencia: string | null;
  mensagemUsuario: string;
  recomendacao: string | null;
  categoria: string;
  podeConcluir: boolean;
  categoriaOrigem: string | null;
}

export interface ScoreDimension {
  id: string;
  score: number;
}

export interface ScoreEnginePayload {
  score: number;
  /** GH#958 — veredito humano (Excelente/Bom/Regular/Fraco) obrigatorio junto
   *  da metrica crua, conforme design system (`linka-design`). */
  veredictoHumano: VeredictoHumano;
  dimensoes: ScoreDimension[];
}

export interface UsageProfilePayload {
  profileId: string;
  label: string;
  status: ReportStatus;
}

/** GH#957 — vocabulario proprio da aba de jogos (Bom/Atencao/Ruim/SemDados),
 *  nao o ReportStatus generico de 5 niveis. Categorias vem do catalogo real
 *  (`game-catalog.ts`, GameProfileRecord.profileCode) — nunca fps/moba/casual
 *  inventados. */
export type GameReadinessStatus = "bom" | "atencao" | "ruim" | "sem_dados";

export interface GameReadinessPayload {
  profileCode: string;
  label: string;
  status: GameReadinessStatus;
}

export interface DiagnosticReportPayload {
  /** GH#954 — precisa ser propagado do DiagnosticResult; antes o payload
   *  publico simplesmente descartava o campo (nao existia aqui, mesmo o
   *  motor calculando certo). REMOTE | CACHED_LOCAL | BUNDLED_LOCAL. */
  evaluationSource: DiagnosticResult["evaluationSource"];
  wifiResultados: DiagnosticCard[];
  internetResultados: DiagnosticCard[];
  mobileResultados: DiagnosticCard[];
  fibraResultados: DiagnosticCard[];
  dnsResultados: DiagnosticCard[];
  historicoResultados: DiagnosticCard[];
  wifiCanalResultados: DiagnosticCard[];
  redeResultados: DiagnosticCard[];
  decisao: DiagnosticCard;
  achadosSecundarios: DiagnosticCard[];
  hipotesesDescartadas: DiagnosticCard[];
  dadosAusentes: string[];
  limitacoesEquipamentoLocal: string[];
  recomendacoes: DiagnosticCard[];
  scoreEngineResultado: ScoreEnginePayload;
  perfisUso: UsageProfilePayload[];
  gameReadiness: GameReadinessPayload[];
  aiAssist?: {
    version: number;
    mode: "single_shot_explainer";
    shouldInvoke: boolean;
    reason: string;
    systemPrompt: string;
    userPrompt: string;
    expectedOutputSchema: {
      format: "json";
      fields: string[];
    };
  };
  geradoEmMs: number;
}

const FLOW_TITLES: Record<DiagnosticFlowCode, string> = {
  isp_externo: "Problema no provedor ou fora da rede local",
  wifi_local: "Problema local no Wi-Fi",
  dns: "Problema no DNS",
  fibra: "Problema na fibra",
  rede_movel: "Problema no sinal móvel",
  historico_degradacao: "Degradação recente detectada",
  internet_instavel: "Internet instável",
  sem_dados_suficientes: "Dados insuficientes para concluir",
  saudavel_monitorar: "Conexão saudável no momento",
};

const FLOW_ORIGIN: Record<DiagnosticFlowCode, string | null> = {
  isp_externo: "isp",
  wifi_local: "wifi",
  dns: "dns",
  fibra: "fibra",
  rede_movel: "mobile",
  historico_degradacao: "historico",
  internet_instavel: "local",
  sem_dados_suficientes: null,
  saudavel_monitorar: null,
};

const RECOMMENDATION_TEXT: Record<string, string> = {
  VERIFY_LINK_AND_PROVIDER: "Verifique se a internet realmente caiu e, se persistir, acione o provedor.",
  CHECK_LINK_STABILITY: "Revise interferência, cabos e estabilidade do link antes de um novo teste.",
  VERIFY_UPLOAD_PATH: "Cheque uploads em segundo plano e confirme se o roteador ou provedor não está limitando o envio.",
  ENABLE_QOS_OR_SQM: "Ative QoS ou SQM no roteador para reduzir latência sob carga.",
  CHECK_FIBER_SIGNAL_WITH_PROVIDER: "Compartilhe a leitura óptica com o provedor e peça checagem da fibra.",
  MOVE_TO_BETTER_5G_COVERAGE: "Mude de posição e compare 4G e 5G para confirmar cobertura ruim.",
  CHECK_WIFI_AND_ROUTER_LOAD: "Refaça o teste perto do roteador e reduza tráfego paralelo durante a medição.",
  MOVE_CLOSER_TO_ROUTER: "Aproxime-se do roteador e repita o teste para validar se o sinal melhora.",
  CHECK_WIFI_INTERFERENCE: "Verifique interferência de redes vizinhas e aparelhos próximos.",
  COMPARE_WITH_FASTER_DNS: "Compare com outro DNS confiável e veja se a abertura dos serviços melhora.",
  RETEST_AND_CHECK_PROVIDER_CONGESTION: "Repita o teste em outro horário para validar congestionamento do provedor.",
  REDUCE_BACKGROUND_TRAFFIC: "Pause downloads e streams paralelos para reduzir o jitter.",
  CHECK_BACKGROUND_UPLOADS: "Confira uploads automáticos em nuvem e sincronizações rodando em segundo plano.",
  VERIFY_PLAN_AND_ACTIVE_DEVICES: "Compare com seu plano e com a quantidade de dispositivos ativos na rede.",
  SWITCH_TO_5GHZ: "Se possível, troque da rede 2.4 GHz para 5 GHz.",
  MONITOR_5G_COVERAGE: "Continue monitorando a cobertura 5G e compare em outro ponto da área.",
  CHANGE_WIFI_CHANNEL: "Troque o canal do Wi-Fi para reduzir disputa com redes vizinhas.",
  CONTACT_PROVIDER_WITH_EVIDENCE: "Guarde os horários e resultados e acione o provedor com evidências.",
  RESTART_ROUTER_AND_RETEST: "Reinicie o roteador e repita a medição para confirmar se o sintoma persiste.",
  RESTART_ROUTER_OR_CONTACT_PROVIDER: "Reinicie o roteador e, se nada mudar, registre chamado no provedor.",
  MONITOR_CONNECTION_AND_RETEST: "Monitore e refaça o teste para confirmar se a oscilação volta a ocorrer.",
  IMPROVE_WIFI_SIGNAL: "Melhore a posição no ambiente ou ajuste o roteador para fortalecer o sinal.",
  COMPARE_7D_VS_30D_AND_CONTACT_PROVIDER: "Use a comparação do histórico recente com o baseline para abrir chamado se a queda persistir.",
  MONITOR_HISTORY_AND_RETEST: "Continue acompanhando o histórico e repita o teste no pior horário percebido.",
};

const FINDING_COPY: Record<string, { titulo: string; mensagem: string; origem?: string | null }> = {
  INTERNET_UNAVAILABLE: {
    titulo: "Internet indisponível",
    mensagem: "Não conseguimos confirmar download útil na sua conexão agora, o que indica falha importante no acesso.",
    origem: "isp",
  },
  PACKET_LOSS_HIGH: {
    titulo: "Perda de pacotes alta",
    mensagem: "Sua conexão está perdendo pacotes demais, o que impacta chamadas, jogos e estabilidade geral.",
  },
  UPLOAD_ZERO: {
    titulo: "Upload zerado",
    mensagem: "O envio de dados na sua conexão praticamente não está acontecendo, o que prejudica chamadas, backup e uso interativo.",
  },
  BUFFERBLOAT_CRITICAL: {
    titulo: "Latência sob carga muito alta",
    mensagem: "Quando sua rede entra em uso, a latência sobe demais e deixa a conexão lenta para tarefas sensíveis.",
  },
  FIBER_RX_POWER_LOW: {
    titulo: "Sinal óptico fraco",
    mensagem: "A leitura da sua fibra está abaixo do ideal e isso pode explicar instabilidade ou quedas.",
    origem: "fibra",
  },
  MOBILE_SIGNAL_POOR_5G: {
    titulo: "Sinal móvel ruim",
    mensagem: "A qualidade do seu sinal móvel está ruim e pode limitar velocidade e estabilidade.",
    origem: "mobile",
  },
  HIGH_LATENCY_JITTER: {
    titulo: "Latência e jitter altos",
    mensagem: "Sua conexão está variando demais no tempo de resposta, o que piora jogos, chamadas e navegação sensível.",
  },
  WIFI_SIGNAL_CRITICAL: {
    titulo: "Sinal Wi-Fi crítico",
    mensagem: "O sinal do seu Wi-Fi está muito fraco neste ponto e pode ser a principal causa da instabilidade.",
    origem: "wifi",
  },
  WIFI_SIGNAL_WEAK: {
    titulo: "Sinal Wi-Fi fraco",
    mensagem: "O sinal do seu Wi-Fi está abaixo do ideal e isso pode reduzir velocidade e estabilidade.",
    origem: "wifi",
  },
  PACKET_LOSS_MODERATE: {
    titulo: "Perda de pacotes moderada",
    mensagem: "Há perda de pacotes acima do ideal na sua conexão, o que já afeta a qualidade.",
  },
  DNS_LATENCY_HIGH: {
    titulo: "DNS lento",
    mensagem: "O DNS da sua conexão está respondendo devagar e isso pode aumentar o tempo para abrir sites e serviços.",
    origem: "dns",
  },
  BUFFERBLOAT_ELEVATED: {
    titulo: "Latência sob carga elevada",
    mensagem: "A latência da sua conexão cresce quando a rede é exigida e isso pode ser sentido como travamentos ou lentidão.",
  },
  LATENCY_HIGH: {
    titulo: "Latência alta",
    mensagem: "O tempo de resposta da sua internet está acima do ideal para uso sensível.",
  },
  JITTER_HIGH: {
    titulo: "Jitter elevado",
    mensagem: "Sua conexão está oscilando no tempo de resposta mais do que deveria.",
  },
  UPLOAD_LOW: {
    titulo: "Upload baixo",
    mensagem: "A taxa de upload da sua conexão está abaixo do esperado para uso confortável.",
  },
  DOWNLOAD_LOW: {
    titulo: "Download baixo",
    mensagem: "A taxa de download da sua conexão está abaixo do esperado para sua experiência.",
  },
  WIFI_LINK_VERY_SLOW: {
    titulo: "Link Wi-Fi muito lento",
    mensagem: "A velocidade do link Wi-Fi negociado pelo seu dispositivo está muito baixa para um bom desempenho.",
    origem: "wifi",
  },
  CONNECTED_TO_24GHZ: {
    titulo: "Conectado na rede 2.4 GHz",
    mensagem: "Você está usando 2.4 GHz mesmo com 5 GHz disponível, o que pode limitar seu desempenho.",
    origem: "wifi",
  },
  MOBILE_SIGNAL_ACCEPTABLE_5G: {
    titulo: "Sinal móvel aceitável",
    mensagem: "Seu sinal móvel está utilizável, mas ainda vale monitorar a estabilidade no local.",
    origem: "mobile",
  },
  DNS_LATENCY_ELEVATED: {
    titulo: "DNS com atenção",
    mensagem: "O DNS da sua conexão não está crítico, mas já responde mais devagar do que o ideal.",
    origem: "dns",
  },
  HISTORY_DEGRADATION_DETECTED: {
    titulo: "Degradação recente detectada",
    mensagem: "O histórico recente da sua conexão está pior que o baseline e sugere uma queda real de qualidade.",
    origem: "historico",
  },
  HISTORY_POSSIBLE_DEGRADATION: {
    titulo: "Possível degradação recente",
    mensagem: "Seu histórico mostra sinais de piora e vale confirmar em novos testes.",
    origem: "historico",
  },
  WIFI_CHANNEL_CONGESTED: {
    titulo: "Canal Wi-Fi congestionado",
    mensagem: "Muitas redes estão disputando o mesmo canal do seu Wi-Fi.",
    origem: "wifi",
  },
  ISP_PROBLEM_DETECTED: {
    titulo: "Rede local respondeu bem",
    mensagem: "O acesso ao seu roteador parece saudável e o gargalo está mais para fora da sua rede local.",
    origem: "isp",
  },
  ROUTER_SLOW_RESPONSE: {
    titulo: "Roteador respondendo devagar",
    mensagem: "O tempo de resposta até o seu roteador já está alto, o que aponta para gargalo local.",
    origem: "roteador",
  },
  POSSIBLE_WIFI_INTERFERENCE: {
    titulo: "Possível interferência no Wi-Fi",
    mensagem: "Há sinais de que seu Wi-Fi também pode estar contribuindo para a experiência ruim.",
    origem: "wifi",
  },
  INTERNET_PROBLEM_DETECTED: {
    titulo: "Problema na internet",
    mensagem: "Sua internet apresenta falha relevante sem um sinal forte de causa local dominante.",
    origem: "isp",
  },
  INTERNET_QUALITY_ATTENTION: {
    titulo: "Internet com atenção",
    mensagem: "Sua internet não está crítica, mas já mostra qualidade abaixo do ideal.",
    origem: "isp",
  },
  WIFI_NEEDS_ATTENTION: {
    titulo: "Wi-Fi precisa de atenção",
    mensagem: "Mesmo sem grande falha de internet, seu Wi-Fi local precisa de ajuste.",
    origem: "wifi",
  },
};

function mapStatus(status: DiagnosticResult["overallStatus"] | DiagnosticFinding["severity"]): ReportStatus {
  if (status === "CRITICAL" || status === "ERROR") return "critical";
  if (status === "ATTENTION" || status === "WARNING") return "attention";
  if (status === "OK" || status === "INFO") return "ok";
  return "inconclusive";
}

function categoryForBucket(category: DiagnosticFinding["category"]): keyof Pick<
  DiagnosticReportPayload,
  "wifiResultados" | "internetResultados" | "mobileResultados" | "fibraResultados" | "dnsResultados" | "historicoResultados" | "wifiCanalResultados" | "redeResultados"
> {
  switch (category) {
    case "wifi":
      return "wifiResultados";
    case "internet":
      return "internetResultados";
    case "mobile":
      return "mobileResultados";
    case "fibra":
      return "fibraResultados";
    case "dns":
      return "dnsResultados";
    case "historico":
      return "historicoResultados";
    case "wifi-canal":
      return "wifiCanalResultados";
    case "decisao":
      return "redeResultados";
  }
}

function prettyEvidence(snapshot: DiagnosticSnapshot, finding: DiagnosticFinding): string | null {
  switch (finding.findingCode) {
    case "PACKET_LOSS_HIGH":
    case "PACKET_LOSS_MODERATE":
      return typeof snapshot.quality?.packetLossPercent === "number" ? `Perda ${snapshot.quality.packetLossPercent.toFixed(1)}%` : null;
    case "LATENCY_HIGH":
    case "HIGH_LATENCY_JITTER":
    case "INTERNET_QUALITY_ATTENTION":
    case "ISP_PROBLEM_DETECTED":
      return typeof snapshot.quality?.latencyMs === "number" ? `Latência ${Math.round(snapshot.quality.latencyMs)} ms` : null;
    case "JITTER_HIGH":
      return typeof snapshot.quality?.jitterMs === "number" ? `Jitter ${snapshot.quality.jitterMs.toFixed(1)} ms` : null;
    case "DOWNLOAD_LOW":
      return typeof snapshot.speed?.downloadMbps === "number" ? `Download ${snapshot.speed.downloadMbps.toFixed(1)} Mbps` : null;
    case "UPLOAD_LOW":
    case "UPLOAD_ZERO":
      return typeof snapshot.speed?.uploadMbps === "number" ? `Upload ${snapshot.speed.uploadMbps.toFixed(1)} Mbps` : null;
    case "WIFI_SIGNAL_WEAK":
    case "WIFI_SIGNAL_CRITICAL":
      return typeof snapshot.wifi?.rssiDbm === "number" ? `RSSI ${snapshot.wifi.rssiDbm} dBm` : null;
    case "WIFI_LINK_VERY_SLOW":
      return typeof snapshot.wifi?.linkSpeedMbps === "number" ? `Link ${snapshot.wifi.linkSpeedMbps} Mbps` : null;
    case "DNS_LATENCY_HIGH":
    case "DNS_LATENCY_ELEVATED":
      return typeof snapshot.dns?.latencyMs === "number" ? `DNS ${Math.round(snapshot.dns.latencyMs)} ms` : null;
    case "FIBER_RX_POWER_LOW":
      return typeof snapshot.fiber?.rxPowerDbm === "number" ? `RX ${snapshot.fiber.rxPowerDbm.toFixed(1)} dBm` : null;
    case "MOBILE_SIGNAL_POOR_5G":
    case "MOBILE_SIGNAL_ACCEPTABLE_5G":
      return typeof snapshot.mobile?.rsrpDbm === "number" ? `RSRP ${snapshot.mobile.rsrpDbm} dBm` : null;
    case "ROUTER_SLOW_RESPONSE":
      return typeof snapshot.gateway?.rttMs === "number" ? `Gateway ${Math.round(snapshot.gateway.rttMs)} ms` : null;
    default:
      return null;
  }
}

function buildCardFromFinding(snapshot: DiagnosticSnapshot, finding: DiagnosticFinding): DiagnosticCard {
  const copy = FINDING_COPY[finding.findingCode] ?? {
    titulo: finding.findingCode.replaceAll("_", " "),
    mensagem: "Foi detectado um achado técnico que merece atenção.",
    origem: null,
  };

  return {
    id: finding.matchedRuleId,
    titulo: copy.titulo,
    status: mapStatus(finding.severity),
    evidencia: prettyEvidence(snapshot, finding),
    mensagemUsuario: copy.mensagem,
    recomendacao: RECOMMENDATION_TEXT[finding.recommendationId] ?? null,
    categoria: finding.category,
    podeConcluir: finding.confidence !== "LOW",
    categoriaOrigem: copy.origem ?? null,
  };
}

function buildDecisionCard(result: DiagnosticResult): DiagnosticCard {
  return {
    id: `DECISAO-${result.primaryFlow.toUpperCase()}`,
    titulo: FLOW_TITLES[result.primaryFlow],
    status: result.primaryFlow === "sem_dados_suficientes" ? "inconclusive" : mapStatus(result.overallStatus),
    evidencia: null,
    mensagemUsuario: result.humanSummary,
    recomendacao: result.humanResolution[0] ?? null,
    categoria: "decisao",
    podeConcluir: result.primaryFlow !== "sem_dados_suficientes",
    categoriaOrigem: FLOW_ORIGIN[result.primaryFlow],
  };
}

function buildSecondaryCard(flow: DiagnosticFlowCode, result: DiagnosticResult): DiagnosticCard {
  return {
    id: `SEC-${flow.toUpperCase()}`,
    titulo: FLOW_TITLES[flow],
    status: flow === "sem_dados_suficientes" ? "inconclusive" : mapStatus(result.overallStatus),
    evidencia: null,
    mensagemUsuario: `Além da decisão principal, há evidências compatíveis com ${FLOW_TITLES[flow].toLowerCase()}.`,
    recomendacao: result.humanResolution[1] ?? result.humanResolution[0] ?? null,
    categoria: "decisao",
    podeConcluir: flow !== "sem_dados_suficientes",
    categoriaOrigem: FLOW_ORIGIN[flow],
  };
}

function buildRecommendationCards(result: DiagnosticResult): DiagnosticCard[] {
  return result.humanResolution.map((text, index) => ({
    id: `REC-${index + 1}`,
    titulo: index === 0 ? "Próxima ação recomendada" : `Ação recomendada ${index + 1}`,
    status: "info",
    evidencia: null,
    mensagemUsuario: text,
    recomendacao: result.nextBestChecks[index] ?? null,
    categoria: "recomendacao",
    podeConcluir: result.primaryFlow !== "sem_dados_suficientes",
    categoriaOrigem: FLOW_ORIGIN[result.primaryFlow],
  }));
}

function buildDiscardedHypotheses(result: DiagnosticResult): DiagnosticCard[] {
  return result.secondaryFlows
    .filter((flow) => flow !== result.primaryFlow)
    .slice(1)
    .map((flow, index) => ({
    id: `HIP-${index + 1}`,
    titulo: `${FLOW_TITLES[flow]} não foi a hipótese principal`,
    status: "info",
    evidencia: null,
    mensagemUsuario: `Esse caminho foi considerado, mas perdeu prioridade para ${FLOW_TITLES[result.primaryFlow].toLowerCase()}.`,
    recomendacao: null,
    categoria: "decisao",
    podeConcluir: true,
    categoriaOrigem: FLOW_ORIGIN[flow],
  }));
}

function clamp(value: number): number {
  return Math.max(0, Math.min(100, Math.round(value)));
}

function buildScoreDimensions(snapshot: DiagnosticSnapshot, result: DiagnosticResult): ScoreDimension[] {
  const dimensions: ScoreDimension[] = [];
  if (typeof snapshot.quality?.latencyMs === "number" || typeof snapshot.quality?.packetLossPercent === "number") {
    dimensions.push({ id: "internet", score: clamp(result.score) });
  }
  if (typeof snapshot.wifi?.rssiDbm === "number" || typeof snapshot.wifi?.linkSpeedMbps === "number") {
    dimensions.push({ id: "wifi", score: clamp(result.primaryFlow === "wifi_local" ? result.score : result.score + 12) });
  }
  if (typeof snapshot.dns?.latencyMs === "number") {
    dimensions.push({ id: "dns", score: clamp(result.primaryFlow === "dns" ? result.score : result.score + 10) });
  }
  if (typeof snapshot.fiber?.rxPowerDbm === "number") {
    dimensions.push({ id: "fibra", score: clamp(result.primaryFlow === "fibra" ? result.score : result.score + 10) });
  }
  if (typeof snapshot.mobile?.rsrpDbm === "number") {
    dimensions.push({ id: "mobile", score: clamp(result.primaryFlow === "rede_movel" ? result.score : result.score + 10) });
  }
  if (typeof snapshot.historical?.avgDownload7d === "number" || typeof snapshot.historical?.avgDownload30d === "number") {
    dimensions.push({ id: "historico", score: clamp(result.primaryFlow === "historico_degradacao" ? result.score : result.score + 8) });
  }
  return dimensions.length > 0 ? dimensions : [{ id: "geral", score: clamp(result.score) }];
}

function usageStatusFromScore(score: number): ReportStatus {
  if (score >= 80) return "ok";
  if (score >= 60) return "info";
  if (score >= 40) return "attention";
  return "critical";
}

function buildUsageProfiles(result: DiagnosticResult): UsageProfilePayload[] {
  return [
    { profileId: "navegacao", label: "Navegação", status: usageStatusFromScore(result.score) },
    { profileId: "streaming", label: "Streaming", status: usageStatusFromScore(result.score - 5) },
    { profileId: "videochamada", label: "Videochamada", status: usageStatusFromScore(result.score - 15) },
    { profileId: "jogos", label: "Jogos", status: usageStatusFromScore(result.score - 20) },
    { profileId: "trabalho", label: "Trabalho", status: usageStatusFromScore(result.score - 10) },
  ];
}

/** GH#957 — pior faixa entre latencia/jitter/perda vence, usando os thresholds
 *  do proprio GameProfileRecord (`good`/`attention` max) ja implementado em
 *  `game-catalog.ts`. Sem faixa aplicavel (nenhum threshold do perfil bate com
 *  dado disponivel no snapshot) retorna "sem_dados" em vez de inventar status. */
function classifyAgainstProfile(snapshot: DiagnosticSnapshot, profile: GameProfileRecord): GameReadinessStatus {
  const severidades: number[] = [];

  const latencia = snapshot.quality?.latencyMs;
  if (typeof latencia === "number" && (profile.latencyGoodMax != null || profile.latencyAttentionMax != null)) {
    if (profile.latencyGoodMax != null && latencia <= profile.latencyGoodMax) severidades.push(0);
    else if (profile.latencyAttentionMax != null && latencia <= profile.latencyAttentionMax) severidades.push(1);
    else severidades.push(2);
  }

  const jitter = snapshot.quality?.jitterMs;
  if (typeof jitter === "number" && (profile.jitterGoodMax != null || profile.jitterAttentionMax != null)) {
    if (profile.jitterGoodMax != null && jitter <= profile.jitterGoodMax) severidades.push(0);
    else if (profile.jitterAttentionMax != null && jitter <= profile.jitterAttentionMax) severidades.push(1);
    else severidades.push(2);
  }

  const perda = snapshot.quality?.packetLossPercent;
  if (typeof perda === "number" && (profile.lossGoodMax != null || profile.lossAttentionMax != null)) {
    if (profile.lossGoodMax != null && perda <= profile.lossGoodMax) severidades.push(0);
    else if (profile.lossAttentionMax != null && perda <= profile.lossAttentionMax) severidades.push(1);
    else severidades.push(2);
  }

  const download = snapshot.speed?.downloadMbps;
  if (typeof download === "number" && (profile.downloadGoodMin != null || profile.downloadAttentionMin != null)) {
    if (profile.downloadGoodMin != null && download >= profile.downloadGoodMin) severidades.push(0);
    else if (profile.downloadAttentionMin != null && download >= profile.downloadAttentionMin) severidades.push(1);
    else severidades.push(2);
  }

  if (
    typeof snapshot.quality?.loadedLatencyMs === "number"
    && typeof snapshot.quality?.latencyMs === "number"
    && (profile.bufferbloatGoodMax != null || profile.bufferbloatAttentionMax != null)
  ) {
    const delta = Math.max(0, snapshot.quality.loadedLatencyMs - snapshot.quality.latencyMs);
    if (profile.bufferbloatGoodMax != null && delta <= profile.bufferbloatGoodMax) severidades.push(0);
    else if (profile.bufferbloatAttentionMax != null && delta <= profile.bufferbloatAttentionMax) severidades.push(1);
    else severidades.push(2);
  }

  if (severidades.length === 0) return "sem_dados";
  const pior = Math.max(...severidades);
  return pior === 0 ? "bom" : pior === 1 ? "atencao" : "ruim";
}

/** GH#957 — usa o catalogo real de perfis de sensibilidade de jogos
 *  (`game-catalog.ts`, 4 perfis: competitivo extremo/competitivo/esporte
 *  competitivo/multiplayer moderado). Perfis vem de `listGameProfiles(env)` no
 *  caller (index.ts) — este modulo nao acessa D1 diretamente. */
function buildGameReadiness(snapshot: DiagnosticSnapshot, profiles: GameProfileRecord[]): GameReadinessPayload[] {
  return profiles.map((profile) => ({
    profileCode: profile.profileCode,
    label: profile.displayName,
    status: classifyAgainstProfile(snapshot, profile),
  }));
}

function buildEquipmentLimitations(snapshot: DiagnosticSnapshot, result: DiagnosticResult): string[] {
  const limitations: string[] = [];
  if ((snapshot.connection?.type === "WIFI" || snapshot.wifi) && typeof snapshot.wifi?.rssiDbm !== "number") {
    limitations.push("O ambiente atual não informou o RSSI do Wi-Fi.");
  }
  if ((snapshot.fiber === null || snapshot.fiber === undefined) && result.missingInputs.includes("fiber_rx_power_dbm")) {
    limitations.push("Este equipamento não forneceu métricas de fibra para confirmar a camada óptica.");
  }
  if (typeof snapshot.gateway?.rttMs !== "number") {
    limitations.push("Não houve leitura de RTT do gateway para separar com máxima confiança problema local de ISP.");
  }
  return limitations;
}

export function buildDiagnosticReport(
  snapshot: DiagnosticSnapshot,
  result: DiagnosticResult,
  gameProfiles: GameProfileRecord[] = [],
): DiagnosticReportPayload {
  const buckets = {
    wifiResultados: [] as DiagnosticCard[],
    internetResultados: [] as DiagnosticCard[],
    mobileResultados: [] as DiagnosticCard[],
    fibraResultados: [] as DiagnosticCard[],
    dnsResultados: [] as DiagnosticCard[],
    historicoResultados: [] as DiagnosticCard[],
    wifiCanalResultados: [] as DiagnosticCard[],
    redeResultados: [] as DiagnosticCard[],
  };

  for (const finding of result.findings) {
    const card = buildCardFromFinding(snapshot, finding);
    buckets[categoryForBucket(finding.category)].push(card);
  }

  const scoreDimensions = buildScoreDimensions(snapshot, result);

  return {
    evaluationSource: result.evaluationSource,
    ...buckets,
    decisao: buildDecisionCard(result),
    achadosSecundarios: result.secondaryFlows
      .filter((flow) => flow !== result.primaryFlow)
      .map((flow) => buildSecondaryCard(flow, result)),
    hipotesesDescartadas: buildDiscardedHypotheses(result),
    dadosAusentes: result.missingInputs,
    limitacoesEquipamentoLocal: buildEquipmentLimitations(snapshot, result),
    recomendacoes: buildRecommendationCards(result),
    scoreEngineResultado: {
      score: clamp(result.score),
      veredictoHumano: veredictoHumanoDoScore(clamp(result.score)),
      dimensoes: scoreDimensions,
    },
    perfisUso: buildUsageProfiles(result),
    gameReadiness: buildGameReadiness(snapshot, gameProfiles),
    geradoEmMs: Date.parse(result.evaluatedAt),
  };
}

/** GH#959 — fallback minimo garantido: qualquer falha total do motor (D1
 *  indisponivel, excecao interna) ainda retorna um payload valido no
 *  contrato, nunca 500 cru nem payload vazio sem explicacao. */
export function buildInconclusiveReport(reason: string): DiagnosticReportPayload {
  const empty: DiagnosticCard[] = [];
  return {
    evaluationSource: "BUNDLED_LOCAL",
    wifiResultados: empty,
    internetResultados: empty,
    mobileResultados: empty,
    fibraResultados: empty,
    dnsResultados: empty,
    historicoResultados: empty,
    wifiCanalResultados: empty,
    redeResultados: empty,
    decisao: {
      id: "DECISAO-INCONCLUSIVO",
      titulo: "Nao foi possivel concluir sua analise",
      status: "inconclusive",
      evidencia: null,
      mensagemUsuario: `Nao conseguimos concluir sua analise agora. ${reason}`,
      recomendacao: "Tente novamente em instantes. Se persistir, entre em contato com o suporte.",
      categoria: "decisao",
      podeConcluir: false,
      categoriaOrigem: null,
    },
    achadosSecundarios: empty,
    hipotesesDescartadas: empty,
    dadosAusentes: [],
    limitacoesEquipamentoLocal: [],
    recomendacoes: empty,
    scoreEngineResultado: {
      score: 0,
      veredictoHumano: "fraco",
      dimensoes: [{ id: "geral", score: 0 }],
    },
    perfisUso: [],
    gameReadiness: [],
    geradoEmMs: Date.now(),
  };
}
