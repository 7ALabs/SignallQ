import { SystemErrorLog, AppVersionDetail, OperatorRecord } from "../types/admin";

export interface InfraAlert {
  id: string;
  source: string;
  message: string;
  severity: "high" | "medium" | "low";
  timestamp: string;
}

export interface AiAlert {
  id: string;
  type: "critical" | "warning" | "info";
  title: string;
  description: string;
  timestamp: string;
}

export const mockInfraAlerts: InfraAlert[] = [
  {
    id: "inf_al_01",
    source: "analytics_db",
    message: "Analytics DB com latência elevada em horário de pico de telemetria.",
    severity: "high",
    timestamp: "Há 12 min"
  },
  {
    id: "inf_al_02",
    source: "worker",
    message: "Cloudflare Workers subrequest limit exceeded nas requisições da zona Nordeste.",
    severity: "high",
    timestamp: "Há 32 min"
  },
  {
    id: "inf_al_03",
    source: "android_app",
    message: "Crash Outbreak: Taxa de crash-free rate sob v1.3.0-beta1 caiu abaixo de 98.4% devido a NullPointerException no scanner de WiFi.",
    severity: "medium",
    timestamp: "Há 2 horas"
  }
];

export const mockAiAlerts: AiAlert[] = [
  {
    id: "al_ai_01",
    type: "warning",
    title: "Limite de Custo Mensal 80% Atingido",
    description: "Os dispêndios agregados das APIs de laudos atingiram $158,40 contra o limite teto operacional de $200,00.",
    timestamp: "Há 10 min"
  },
  {
    id: "al_ai_02",
    type: "critical",
    title: "Anomalia de Latência no Qwen 2.5 Engine",
    description: "Workers AI registrou pico de 2.4 segundos na resolução de pareceres térmicos na região de Campinas.",
    timestamp: "Há 1 hora"
  },
  {
    id: "al_ai_03",
    type: "info",
    title: "Handover preventivo de DNS ativado",
    description: "SDK Android migrou de canal local de IA para Gemini 1.5 Flash de forma bem-sucedida após erro de timeout local.",
    timestamp: "Há 3 horas"
  }
];

export interface ErrorMetricSummary {
  activeErrors: string;
  events24h: string;
  impactedUsers: string;
  mainSources: string;
}

export interface ErrorByEndpointEntry {
  name: string;
  erros: number;
}

export const mockErrorMetricSummary: Record<"production" | "staging", ErrorMetricSummary> = {
  production: {
    activeErrors: "3 ativos",
    events24h: "551 eventos",
    impactedUsers: "1.480",
    mainSources: "Android / AI Gateway",
  },
  staging: {
    activeErrors: "1 ativo",
    events24h: "34 eventos",
    impactedUsers: "15",
    mainSources: "Android / AI Gateway",
  },
};

export const mockErrorByEndpoint: Record<"production" | "staging", ErrorByEndpointEntry[]> = {
  production: [
    { name: "AI Gateway", erros: 382 },
    { name: "Android App", erros: 1544 },
    { name: "Edge Worker", erros: 45 },
    { name: "Analytics DB", erros: 8 },
  ],
  staging: [
    { name: "AI Gateway", erros: 12 },
    { name: "Android App", erros: 45 },
    { name: "Edge Worker", erros: 45 },
    { name: "Analytics DB", erros: 2 },
  ],
};

export const mockSystemErrors: SystemErrorLog[] = [
  {
    id: "err_901aef",
    timestamp: "2026-06-21T10:28:44-07:00",
    source: "ai_gateway",
    category: "ia",
    message: "HTTP 504 Gateway Timeout while contacting Gemini API",
    stackTrace: "Error: Gateway Timeout\n  at fetchWithRetry (src/services/aiGroup.ts:241:18)\n  at processDiagnosis (src/workers/analytics.ts:54:12)",
    count: 382,
    environment: "production",
    resolved: false,
    affectedUserCount: 290,
  },
  {
    id: "err_302b1f",
    timestamp: "2026-06-21T09:41:05-07:00",
    source: "android_app",
    category: "app",
    message: "android.net.wifi.WifiManager.getConnectionInfo() returns NullPointerException",
    stackTrace: "java.lang.NullPointerException: Attempt to invoke virtual method 'java.lang.String android.net.wifi.WifiInfo.getSSID()' on a null object reference\n  at com.signallq.app.diag.WifiScanner.scan(WifiScanner.kt:144)\n  at com.signallq.app.Service.runDiagnosis(WorkerService.kt:82)",
    count: 124,
    environment: "production",
    resolved: false,
    affectedUserCount: 88,
  },
  {
    id: "err_bc2191",
    timestamp: "2026-06-21T08:52:19-07:00",
    source: "worker",
    category: "backend",
    message: "Cloudflare Worker subrequest limit exceeded: 50 active HTTP subrequests limit during peak latency",
    stackTrace: "WorkerError: subrequest limit exceeded\n  at async handleRequest (index.js:144)\n  at async fetch (index.js:12)",
    count: 45,
    environment: "staging",
    resolved: false,
    affectedUserCount: 15,
  },
  {
    id: "err_dcb291",
    timestamp: "2026-06-21T04:12:00-07:00",
    source: "analytics_db",
    category: "integration",
    message: "FATAL: remaining connection slots are reserved for non-replication superuser connections",
    stackTrace: "AnalyticsDbError: ingestion latency exceeded\n  at Client.connect (node_modules/banco de analytics/lib/client.js:188)\n  at Pool.query (node_modules/banco de analytics/lib/pool.js:45)",
    count: 8,
    environment: "production",
    resolved: true,
    affectedUserCount: 41,
    resolvedBy: "felipe@signallq.io",
    resolvedAt: "2026-06-21T09:10:00-07:00",
    resolutionNote: "Pool de conexões da Analytics DB redimensionado; sem recorrência em 48h.",
  },
  {
    id: "err_726b1a",
    timestamp: "2026-06-20T21:30:14-07:00",
    source: "android_app",
    category: "app",
    message: "SecurityException: Location permission denied for speedtest cellular ping mapping",
    stackTrace: "java.lang.SecurityException: \"gps\" location provider requires ACCESS_FINE_LOCATION permission.\n  at android.os.Parcel.createExceptionOrNull(Parcel.java:3057)\n  at android.os.Parcel.createException(Parcel.java:3041)",
    count: 1420,
    environment: "production",
    resolved: true,
    affectedUserCount: 1102,
    resolvedBy: "camilo@signallq.io",
    resolvedAt: "2026-06-21T08:00:00-07:00",
    resolutionNote: "Corrigido no app v0.18.2 — permissão de localização agora é solicitada antes do ping cellular.",
  }
];

export const mockAppVersions: AppVersionDetail[] = [
  {
    id: "v_0_18_1",
    versionCode: "0.18.1",
    buildNumber: 1045,
    releaseDate: "2026-06-19",
    activeInstallsCount: 4820,
    rolloutPercentage: 100,
    crashFreeRatePercentage: 99.42,
    status: "stable",
    notes: "Implementado diagnóstico por cabo de ethernet nativo em dongles USB-C, melhoria no parsing do bufferbloat com decaimento exponencial, e atualizada SDK do Gemini para o modelo flash.",
    platform: "Android",
    source: "Google Play + Firebase",
    activeUsersPercentage: 61,
    diagnosticsCount: 112500,
    successRatePercentage: 96.4,
    crashesCount: 120,
    anrsCount: 3,
    diagnosticErrorsCount: 12,
    aiErrorsCount: 4
  },
  {
    id: "v_0_17_0",
    versionCode: "0.17.0",
    buildNumber: 1024,
    releaseDate: "2026-06-05",
    activeInstallsCount: 1890,
    rolloutPercentage: 100,
    crashFreeRatePercentage: 98.92,
    status: "stable",
    notes: "Correção de vazamento de memória no serviço de monitoramento em segundo plano da rede móvel e atualização dos servidores padrão de speedtest no Brasil para RJ/SP.",
    platform: "Android",
    source: "Google Play + Firebase",
    activeUsersPercentage: 24,
    diagnosticsCount: 44200,
    successRatePercentage: 94.1,
    crashesCount: 310,
    anrsCount: 7,
    diagnosticErrorsCount: 45,
    aiErrorsCount: 19
  },
  {
    id: "v_0_16_0",
    versionCode: "0.16.0",
    buildNumber: 998,
    releaseDate: "2026-03-12",
    activeInstallsCount: 710,
    rolloutPercentage: 100,
    crashFreeRatePercentage: 97.45,
    status: "deprecated",
    notes: "Legada com suporte a API antiga do Google Cloud. Usuários sendo forçados a atualizar para as versões mais novas.",
    platform: "Android",
    source: "Google Play + Firebase",
    activeUsersPercentage: 9,
    diagnosticsCount: 16500,
    successRatePercentage: 91.8,
    crashesCount: 880,
    anrsCount: 22,
    diagnosticErrorsCount: 140,
    aiErrorsCount: 62
  },
  {
    id: "v_ios_futuro",
    versionCode: "iOS futuro",
    buildNumber: 0,
    releaseDate: "Planejado",
    activeInstallsCount: 0,
    rolloutPercentage: 0,
    crashFreeRatePercentage: 100,
    status: "planned",
    notes: "Preparado para futuro app iOS e integração com a App Store Connect. Sincronização desabilitada nas configurações centrais.",
    platform: "iOS",
    source: "App Store Connect",
    activeUsersPercentage: 0,
    diagnosticsCount: 0,
    successRatePercentage: 100,
    crashesCount: 0,
    anrsCount: 0,
    diagnosticErrorsCount: 0,
    aiErrorsCount: 0
  }
];

export const mockOperatorsList: OperatorRecord[] = [
  {
    id: "op_claro_m",
    name: "Claro Celular",
    country: "Brasil",
    type: "mobile",
    testCount: 48900,
    averageDownloadMbps: 124.5,
    averageUploadMbps: 28.2,
    averageLatencyMs: 38,
    packetLossAverage: 0.15,
    averageScorePercentage: 88,
  },
  {
    id: "op_vivo_m",
    name: "Vivo Celular",
    country: "Brasil",
    type: "mobile",
    testCount: 42100,
    averageDownloadMbps: 108.2,
    averageUploadMbps: 32.4,
    averageLatencyMs: 34,
    packetLossAverage: 0.12,
    averageScorePercentage: 90,
  },
  {
    id: "op_tim_m",
    name: "TIM Celular",
    country: "Brasil",
    type: "mobile",
    testCount: 38200,
    averageDownloadMbps: 98.4,
    averageUploadMbps: 24.1,
    averageLatencyMs: 42,
    packetLossAverage: 0.22,
    averageScorePercentage: 85,
  },
  {
    id: "op_claro_f",
    name: "Claro Fibra / Net",
    country: "Brasil",
    type: "fiber",
    testCount: 22400,
    averageDownloadMbps: 412.5,
    averageUploadMbps: 188.4,
    averageLatencyMs: 12,
    packetLossAverage: 0.05,
    averageScorePercentage: 92,
  },
  {
    id: "op_vivo_f",
    name: "Vivo Fibra",
    country: "Brasil",
    type: "fiber",
    testCount: 18900,
    averageDownloadMbps: 454.0,
    averageUploadMbps: 384.2,
    averageLatencyMs: 8,
    packetLossAverage: 0.02,
    averageScorePercentage: 96,
  },
  {
    id: "op_desktop_f",
    name: "Desktop Internet",
    country: "Brasil",
    type: "fiber",
    testCount: 6200,
    averageDownloadMbps: 310.2,
    averageUploadMbps: 154.9,
    averageLatencyMs: 15,
    packetLossAverage: 0.08,
    averageScorePercentage: 87,
  }
];
