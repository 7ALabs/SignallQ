import { DiagnosticSession, DiagnosticsSummary, AggregateRow } from "../types/diagnostics";

export const mockDiagnosticSessions: DiagnosticSession[] = [
  {
    id: "diag_8f3d1e90",
    deviceId: "dev_samsung_s24_982",
    deviceModel: "Samsung Galaxy S24 Ultra",
    osVersion: "Android 14 (API 34)",
    appVersion: "v1.2.4",
    timestamp: "2026-06-21T10:15:00-07:00",
    networkType: "mobile",
    environment: "production",
    operator: "Vivo Brasil",
    score: 28,
    speed: {
      downloadMbps: 18.4,
      uploadMbps: 1.2,
      latencyMs: 142,
      jitterMs: 24,
      packetLossPercentage: 4.2,
      bufferbloatGrade: "D",
    },
    networkStrength: {
      type: "mobile",
      signalStrengthDbm: -108,
      signalQualityPercentage: 25,
      carrierName: "Vivo Brasil",
      frequencyBandGhz: 0.7, // 700MHz LTE (Band 28)
    },
    issues: [
      {
        issue: "upload_bottleneck",
        severity: "critical",
        description: "Taxa de upload inferior a 2 Mbps impede streaming ou uploads pesados.",
      },
      {
        issue: "mobile_congestion_suspected",
        severity: "attention",
        description: "Latência elevada e perda de pacotes indicam possível congestionamento na ERB de celular.",
      }
    ],
    aiStatus: "completed",
    aiSummaryReport: "O diagnóstico indica que o dispositivo está conectado à Vivo Brasil via rede móvel com sinal fraco (RSSI -108 dBm) na faixa de 700MHz. Há um gargalo crítico na taxa de upload (1.2 Mbps) associado a uma flutuação considerável de jitter. Recomenda-se desativar o plano de dados em segundo plano ou aproximar-se de uma janela para transicionar para uma portadora de maior frequência (3.5GHz 5G).",
    distChannel: "firebase_app_distribution",
    buildType: "release",
  },
  {
    id: "diag_2a9c4e12",
    deviceId: "dev_pixel_7a_339",
    deviceModel: "Google Pixel 7a",
    osVersion: "Android 14 (API 34)",
    appVersion: "v1.2.4",
    timestamp: "2026-06-21T10:04:12-07:00",
    networkType: "wifi",
    environment: "production",
    score: 34,
    speed: {
      downloadMbps: 4.8,
      uploadMbps: 35.2,
      latencyMs: 98,
      jitterMs: 18,
      packetLossPercentage: 1.2,
      bufferbloatGrade: "F",
    },
    networkStrength: {
      type: "wifi",
      signalStrengthDbm: -82,
      signalQualityPercentage: 35,
      ssid: "Minha_Casa_WiFi",
      frequencyBandGhz: 2.4,
      channel: 6
    },
    issues: [
      {
        issue: "wifi_signal_weak",
        severity: "critical",
        description: "Sinal de Wi-Fi fraco (-82 dBm) no canal 6 altamente congestionado.",
      },
      {
        issue: "bufferbloat_upload",
        severity: "attention",
        description: "Aumento drástico de latência durante transferência simultânea na rede local.",
      }
    ],
    aiStatus: "completed",
    aiSummaryReport: "A conexão local sofre com forte atenuação eletromagnética na banda de 2.4 GHz, potencializada pelo congestionamento do canal 6. A velocidade de download despencou para 4.8 Mbps. Sugere-se forçar a conexão para a rede de 5 GHz do roteador (SSID equivalente) ou realocar o roteador diminuindo barreiras físicas.",
    distChannel: "firebase_app_distribution",
    buildType: "release",
  },
  {
    id: "diag_3ff1a678",
    deviceId: "dev_xiaomi_13_404",
    deviceModel: "Xiaomi 13 Pro",
    osVersion: "Android 13 (API 33)",
    appVersion: "v1.2.3",
    timestamp: "2026-06-21T09:48:33-07:00",
    networkType: "fiber",
    environment: "production",
    score: 96,
    speed: {
      downloadMbps: 450.2,
      uploadMbps: 210.6,
      latencyMs: 8,
      jitterMs: 1.5,
      packetLossPercentage: 0,
      bufferbloatGrade: "A+",
    },
    networkStrength: {
      type: "fiber",
      signalStrengthDbm: -45,
      signalQualityPercentage: 98,
      ssid: "SignallQ_HQ_Optic"
    },
    issues: [],
    aiStatus: "none",
    distChannel: "play_store",
    buildType: "release",
  },
  {
    id: "diag_56a67e10",
    deviceId: "dev_moto_g84_112",
    deviceModel: "Motorola Moto G84",
    osVersion: "Android 13 (API 33)",
    appVersion: "v1.2.4",
    timestamp: "2026-06-21T09:12:05-07:00",
    networkType: "wifi",
    environment: "staging",
    score: 72,
    speed: {
      downloadMbps: 95.4,
      uploadMbps: 88.1,
      latencyMs: 122,
      jitterMs: 14,
      packetLossPercentage: 0.8,
      bufferbloatGrade: "C",
    },
    networkStrength: {
      type: "wifi",
      signalStrengthDbm: -58,
      signalQualityPercentage: 80,
      ssid: "Staging_Lab",
      frequencyBandGhz: 5
    },
    issues: [
      {
        issue: "dns_latency_high",
        severity: "attention",
        description: "Tempo de resposta do servidor DNS está acima do limiar crítico de 100ms.",
      }
    ],
    aiStatus: "completed",
    aiSummaryReport: "Embora os canais de radiofrequência física estejam operando com ótimo sinal (-58 dBm em 5GHz), o servidor DNS local (192.168.1.1) demorou 122ms para resolver as requisições de teste. Recomendável ajustar o roteador dhcp para servidores públicos confiáveis (Cloudflare 1.1.1.1 ou Google 8.8.8.8) para reduzir gargalos de navegação.",
    distChannel: "firebase_app_distribution",
    buildType: "debug",
  },
  {
    id: "diag_bb01c224",
    deviceId: "dev_samsung_a54_501",
    deviceModel: "Samsung Galaxy A54 5G",
    osVersion: "Android 14 (API 34)",
    appVersion: "v1.2.4",
    timestamp: "2026-06-21T08:31:00-07:00",
    networkType: "ethernet",
    environment: "production",
    score: 98,
    speed: {
      downloadMbps: 940.5,
      uploadMbps: 910.2,
      latencyMs: 3,
      jitterMs: 0.4,
      packetLossPercentage: 0,
      bufferbloatGrade: "A+",
    },
    issues: [],
    aiStatus: "none",
    distChannel: "sideload",
    buildType: "release",
  },
  {
    id: "diag_901c27df",
    deviceId: "dev_realme_gt_661",
    deviceModel: "Realme GT Master Edition",
    osVersion: "Android 13 (API 33)",
    appVersion: "v1.1.9",
    timestamp: "2026-06-21T08:15:22-07:00",
    networkType: "wifi",
    environment: "production",
    score: 68,
    speed: {
      downloadMbps: 64.9,
      uploadMbps: 12.4,
      latencyMs: 82,
      jitterMs: 9.1,
      packetLossPercentage: 1.8,
      bufferbloatGrade: "B",
    },
    networkStrength: {
      type: "wifi",
      signalStrengthDbm: -74,
      signalQualityPercentage: 54,
      ssid: "Public_Free_WiFi"
    },
    issues: [
      {
        issue: "gateway_slow",
        severity: "attention",
        description: "O primeiro salto da conexão (gateway padrão) está apresentando latência de rádio acima de 45ms.",
      }
    ],
    aiStatus: "failed",
    aiSummaryReport: "Erro ao gerar diagnóstico inteligente: O fornecedor de IA principal reportou timeout de 15s. Utilizado fallback local limitado.",
    distChannel: "firebase_app_distribution",
    buildType: "release",
  }
];

export const mockDiagnosticsSummary: DiagnosticsSummary = {
  totalTests: 184500,
  criticalIssuesCount: 14201,
  attentionIssuesCount: 32670,
  averageDownloadMbps: 284,
  averageUploadMbps: 92,
  averageLatencyMs: 24,
  averageScore: 78,
  averageJitterMs: 8,
  averagePacketLossPercentage: 0.7,
  issueDistribution: {
    wifi_signal_weak: 12450,
    bufferbloat_upload: 9812,
    dns_latency_high: 8011,
    mobile_congestion_suspected: 7120,
    gateway_slow: 4120,
    packet_loss: 3450,
    upload_bottleneck: 5880,
    unknown: 1420
  }
};

export interface IssueDetail {
  title: string;
  probability: string;
  technicalCause: string;
  impactMetrics: { key: string; val: string }[];
  cloudflareEdgeWorkflow: string;
  remediationRecipeAndroid: string;
}

export const mockIssueDetailsMap: Record<string, IssueDetail> = {
  "Wi-Fi fraco": {
    title: "Wi-Fi Fraco (Atenuação Local RF)",
    probability: "31% dos relatos",
    technicalCause: "Forte incidência de reflexão de ondas eletromagnéticas nas bandas de 2.4/5.0GHz provocada por barreiras de alvenaria. Alta atenuação de sinal (RSSI abaixo de -80 dBm).",
    impactMetrics: [
      { key: "Aumento de Jitter", val: "+14ms" },
      { key: "Perda de pacotes", val: "0.8% - 2.5%" },
      { key: "Queda de Throughput", val: "Aprox. -65%" }
    ],
    cloudflareEdgeWorkflow: "Roteador do API Gateway intercepta solicitações em conformidade com o SSID e envia gatilho push de otimização de banda de rádio.",
    remediationRecipeAndroid: "Ativar algoritmo 'SmartWiFi' no SDK móvel para forçar a desconexão automática e migração imediata para canais 5GHz com menor atenuação."
  },
  "Bufferbloat upload": {
    title: "Bufferbloat de Upload (Saturação de Buffer local)",
    probability: "18% de degradação",
    technicalCause: "Saturação das filas FIFO nas interfaces de rede do roteador de borda do usuário resultando em atraso exacerbado quando o upload é forçado à taxa máxima.",
    impactMetrics: [
      { key: "Latência sob estresse", val: "+180ms" },
      { key: "Grade de Conexão", val: "F" },
      { key: "Estabilidade", val: "Instável" }
    ],
    cloudflareEdgeWorkflow: "Detecção síncrona nos tempos de requisições de analytics e rotas auxiliares. Identificação de jitter sob carga.",
    remediationRecipeAndroid: "Implementar algoritmo SQM (Smart Queue Management) baseado em fq_codel no roteador ou escalonar downloads/uploads no app móvel."
  },
  "DNS lento": {
    title: "Latência Elevada de DNS (Servidores Desajustados)",
    probability: "14% dos gargalos",
    technicalCause: "Uso de servidores de DNS locais de operadoras com capacidade de processamento saturada de recursivos ou alta latência de resolução de nome.",
    impactMetrics: [
      { key: "Tempo de resolução", val: ">150ms" },
      { key: "Atraso de handshake", val: "+80ms" },
      { key: "Timeouts", val: "Média de 1.2%" }
    ],
    cloudflareEdgeWorkflow: "Laudo de IA envia automaticamente sugestão em formato JSON para que o aplicativo passe a ignorar o DNS padrão da rede local.",
    remediationRecipeAndroid: "Substituir endpoints DNS de rádio de forma determinística por provedores Anycast públicos e resilientes (Cloudflare 1.1.1.1 ou Google 8.8.8.8)."
  },
  "Rede móvel congestionada": {
    title: "Congestionamento de Estação de Rádio Base celular (ERB)",
    probability: "11% de picos",
    technicalCause: "Alta densidade de usuários ativos conectados à mesma portadora/célula LTE/5G (congestionamento físico do canal do espectro de radiofrequência).",
    impactMetrics: [
      { key: "Sinal de portadora", val: "-108 dBm" },
      { key: "Velocidade Média celular", val: "5-15 Mbps" },
      { key: "Jitter celular", val: "18-28 ms" }
    ],
    cloudflareEdgeWorkflow: "Armazenamento do dump em tabela de telemetria, sinalizando áreas e coordenadas geográficas de antenas sobrecarregadas.",
    remediationRecipeAndroid: "Utilizar API móvel cellular para tentar forçar handover ou alertar o usuário para alternar para a rede Wi-Fi."
  },
  "Gateway lento": {
    title: "Latência Elevada no Primeiro Salto (Gateway Local Lento)",
    probability: "7% das ocorrências",
    technicalCause: "Alto tempo de processamento ou congestionamento de rádio entre o dispositivo e o endereço IP do gateway padrão da rede doméstica.",
    impactMetrics: [
      { key: "Ping até Gateway", val: ">45ms" },
      { key: "Packet loss local", val: "0.2%" },
      { key: "Score afetado", val: "69/100" }
    ],
    cloudflareEdgeWorkflow: "Sinalização síncrona de bypass de rede local para evitar gargalos entre o Worker e o dispositivo.",
    remediationRecipeAndroid: "Sinalizar reinício inteligente da interface Wi-Fi móvel ou recomendar a readequação física do roteador doméstico do usuário."
  }
};

export interface IntelligenceItem {
  id: string;
  problem: string;
  occurrence: number;
  variation: string;
  variationType: "up" | "down";
  scoreAffected: number;
  impact: string;
}

export const mockDiagnosticIntelligence: IntelligenceItem[] = [
  {
    id: "intel_1",
    problem: "Wi-Fi fraco",
    occurrence: 31,
    variation: "+8% vs semana anterior",
    variationType: "up",
    scoreAffected: 62,
    impact: "Chamadas, jogos e vídeo instáveis"
  },
  {
    id: "intel_2",
    problem: "Bufferbloat upload",
    occurrence: 18,
    variation: "-2%",
    variationType: "down",
    scoreAffected: 64,
    impact: "Travamentos durante upload, videochamadas e jogos"
  },
  {
    id: "intel_3",
    problem: "DNS lento",
    occurrence: 14,
    variation: "+4%",
    variationType: "up",
    scoreAffected: 71,
    impact: "Demora para abrir sites e apps"
  },
  {
    id: "intel_4",
    problem: "Rede móvel congestionada",
    occurrence: 11,
    variation: "+12%",
    variationType: "up",
    scoreAffected: 58,
    impact: "Internet lenta mesmo com sinal bom"
  },
  {
    id: "intel_5",
    problem: "Gateway lento",
    occurrence: 7,
    variation: "+1%",
    variationType: "up",
    scoreAffected: 69,
    impact: "Lentidão local entre dispositivo e roteador"
  }
];

export const mockAggregateData: AggregateRow[] = [
  {
    networkType: "Wi-Fi (Rede Local)",
    diagnosticsCount: 132840,
    avgScore: 74,
    avgDownload: "142 Mbps",
    avgUpload: "48 Mbps",
    avgPing: "28 ms",
    avgJitter: "9 ms",
    avgLoss: "0.8%",
    topIssue: "Wi-Fi fraco (31%)",
    trend: "up",
    trendLabel: "Aumento de ruído local"
  },
  {
    networkType: "Rede Móvel (Cellular)",
    diagnosticsCount: 38740,
    avgScore: 68,
    avgDownload: "45 Mbps",
    avgUpload: "12 Mbps",
    avgPing: "48 ms",
    avgJitter: "14 ms",
    avgLoss: "1.5%",
    topIssue: "Rede móvel congestionada (11%)",
    trend: "up",
    trendLabel: "Interferência em horários de pico"
  },
  {
    networkType: "Fibra (Banda Larga)",
    diagnosticsCount: 11070,
    avgScore: 92,
    avgDownload: "480 Mbps",
    avgUpload: "245 Mbps",
    avgPing: "8 ms",
    avgJitter: "1.2 ms",
    avgLoss: "0.05%",
    topIssue: "Nenhum detectado (92%)",
    trend: "stable",
    trendLabel: "Estabilidade mecânica impecável"
  },
  {
    networkType: "Ethernet Cabeada",
    diagnosticsCount: 1850,
    avgScore: 96,
    avgDownload: "910 Mbps",
    avgUpload: "880 Mbps",
    avgPing: "3 ms",
    avgJitter: "0.4 ms",
    avgLoss: "0.01%",
    topIssue: "Gateway lento (7%)",
    trend: "down",
    trendLabel: "Melhoria linear"
  }
];
