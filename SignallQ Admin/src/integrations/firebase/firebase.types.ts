export interface FirebaseIntegrationStatus {
  enabled: boolean;
  status: "connected" | "mock" | "attention" | "planned" | "disabled";
  message: string;
  platform: string;
  lastSyncTimestamp: string;
  eventsImported: number;
  crashesImported: number;
}

export interface FirebaseEventMetric {
  eventName: string;
  count: number;
  uniques: number;
  trend: "up" | "down" | "stable";
}

export interface FirebaseAnalyticsSummary {
  activeUsersToday: number;
  /**
   * Total de sessões GA4 nos últimos 7 dias — soma de metricValues[1] ("sessions")
   * do runReport já consultado por getFirebaseAnalyticsSummary (mesma janela
   * 7daysAgo-today usada para activeUsers). Card "Sessões (7d)" do Centro de
   * Controle (Overview, spec Lia Md3DashboardContent.dc.html:27). Só o total é
   * real — o worker não calcula variação vs. período anterior (precisaria de uma
   * segunda janela GA4), então o veredito "+18%" do protótipo NÃO é implementado
   * aqui (não inventar tendência sem cálculo real).
   */
  sessions7d: number | null;
  averageSessionsDurationMs: number;
  crashFreeUsersPercentage: number;
  crashFreeSessionsPercentage: number;
  topEvents: FirebaseEventMetric[];
}

// Espelha o shape real de GET /admin/integrations/firebase/crashlytics —
// worker sempre retorna 200 com "source" indicando se o dado é real
// (source==="bigquery") ou honesto-vazio (no_credentials/no_data_yet/error).
// crashFreeUsersPercentage e unresolvedCrashes sempre vêm preenchidos (0/100
// como neutro nos branches sem dado); affectedUsers só existe quando
// source==="bigquery"; message só existe em no_data_yet/error.
export interface FirebaseCrashlyticsSummary {
  source: "bigquery" | "no_credentials" | "no_data_yet" | "error";
  unresolvedCrashes: number;
  crashFreeUsersPercentage: number;
  affectedUsers?: number;
  message?: string;
}

export interface FirebaseAppVersionCrashStats {
  appVersion: string;
  crashCount: number;
  nonFatalCount: number;
  crashFreeUsersPercentage: number;
  status: "stable" | "unstable" | "critical";
}

// #880 (achado 3): antes getFirebaseAppVersions colapsava no_credentials/
// no_data_yet/error em `null`, indistinguível pra UI ("não configurado" mesmo
// quando configurado, só sem volume ainda). Mesmo padrão de source explícito
// já usado em FirebaseCrashIssuesResult/FirebaseCrashlyticsSummary.
export interface FirebaseAppVersionsResult {
  source: "bigquery" | "no_credentials" | "no_data_yet" | "error";
  versions: FirebaseAppVersionCrashStats[];
}

// Espelha o shape real de GET /admin/integrations/firebase/crash-issues.
// appVersion/deviceModel são opcionais: o worker ainda não os expõe (schema
// do BigQuery não confirmado sem credencial — ver comentário em
// handleFirebaseCrashIssues no worker). UI trata ausência como "-", nunca
// inventa valor.
export interface FirebaseCrashIssue {
  id: string;
  title: string;
  totalCrashes: number;
  affectedUsers: number;
  lastSeen: number;
  appVersion?: string;
  deviceModel?: string;
}

export interface FirebaseCrashIssuesResult {
  source: "bigquery" | "no_credentials" | "no_data_yet" | "error";
  issues: FirebaseCrashIssue[];
}
