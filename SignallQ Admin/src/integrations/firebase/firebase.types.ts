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
  // GH#1042 (achado colateral): o worker já expõe `environmentScope: "all"` desde a #879 —
  // o export do Crashlytics/BigQuery não tem coluna equivalente a `environment` do D1, então
  // esse dado nunca é filtrado por production/staging, mesmo quando o painel está em modo
  // "Produção". Antes o frontend nunca lia esse campo (sinal de honestidade morria no caminho).
  environmentScope?: "all";
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

// --- GH#1343/#1344: as 5 integrações "inventário técnico" do plano de UX (item 2, bloco
// "Inventário técnico") — status/config, não série temporal. Todas compartilham o mesmo shape de
// status (`hasCredentials`/`lastSyncTimestamp` + payload próprio) e o mesmo shape de resultado de
// sync (`status: "ok" | "error" | "not_configured"`), espelhando o padrão já usado por
// `GooglePlayStoreListingStatus`/`GooglePlayStoreListingSyncResult`.

/** Resultado de POST .../sync — mesmo shape nas 5 integrações Firebase desta rodada. */
export interface FirebaseIntegrationSyncResult {
  status: "ok" | "error" | "not_configured";
  message?: string;
  syncedAt?: string;
}

export interface FirebaseAndroidApp {
  appId: string;
  displayName: string;
  packageName: string;
  state: string;
}

export interface FirebaseProjectInfo {
  projectId: string;
  projectNumber: string;
  displayName: string;
  state: string;
}

/** GET .../management/status — inventário do projeto Firebase + Android apps cadastrados. */
export interface FirebaseManagementStatus {
  hasCredentials: boolean;
  status: "connected" | "disabled";
  lastSyncTimestamp: string | null;
  project: FirebaseProjectInfo | null;
  androidApps: FirebaseAndroidApp[];
}

/**
 * GET .../remote-config/status — só contagem e nomes de chave (issue #1349: valor do parâmetro
 * é dívida registrada, o worker não expõe hoje).
 */
export interface FirebaseRemoteConfigStatus {
  hasCredentials: boolean;
  status: "connected" | "disabled";
  lastSyncTimestamp: string | null;
  parameterCount: number;
  parameterKeys: string[];
}

/**
 * GET .../app-check/status — payload cru da Firebase App Check REST API
 * (`{ services: [{ name, state }], nextPageToken? }`), guardado como veio — o worker não
 * normaliza. UI trata como desconhecido até ter pelo menos um provedor configurado.
 */
export interface FirebaseAppCheckStatus {
  hasCredentials: boolean;
  status: "connected" | "disabled";
  lastSyncTimestamp: string | null;
  services: { services?: FirebaseAppCheckServiceEntry[]; nextPageToken?: string } | null;
}

export interface FirebaseAppCheckServiceEntry {
  name: string;
  state: string;
}

export interface FirebaseAppDistributionRelease {
  name: string;
  displayVersion: string;
  buildVersion: string;
  createTime: string;
  releaseNotesText: string | null;
}

/** GET .../app-distribution/status — releases reais do último sync. */
export interface FirebaseAppDistributionStatus {
  hasCredentials: boolean;
  status: "connected" | "disabled";
  lastSyncTimestamp: string | null;
  releases: FirebaseAppDistributionRelease[];
}

/**
 * GET .../fcm-delivery/status — delivery data por app/dia (FCM Data API). Hoje sempre vazio: o
 * SignallQ não envia push ainda, então não há mensagem para medir entrega.
 */
export interface FirebaseFcmDeliveryStatus {
  hasCredentials: boolean;
  status: "connected" | "disabled";
  lastSyncTimestamp: string | null;
  androidDeliveryData: unknown[];
}
