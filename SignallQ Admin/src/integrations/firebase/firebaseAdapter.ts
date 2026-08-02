import { apiClient } from "../../services/apiClient";
import {
  mockFirebaseStatus,
  mockFirebaseAnalytics,
  mockFirebaseCrashlytics,
  mockFirebaseAppVersions,
  mockFirebaseCrashIssues,
  mockFirebaseManagementStatus,
  mockFirebaseRemoteConfigStatus,
  mockFirebaseAppCheckStatus,
  mockFirebaseAppDistributionStatus,
  mockFirebaseFcmDeliveryStatus,
} from "./firebase.mock";
import {
  FirebaseIntegrationStatus,
  FirebaseAnalyticsSummary,
  FirebaseCrashlyticsSummary,
  FirebaseAppVersionCrashStats,
  FirebaseAppVersionsResult,
  FirebaseCrashIssue,
  FirebaseCrashIssuesResult,
  FirebaseIntegrationSyncResult,
  FirebaseManagementStatus,
  FirebaseRemoteConfigStatus,
  FirebaseAppCheckStatus,
  FirebaseAppDistributionStatus,
  FirebaseFcmDeliveryStatus,
} from "./firebase.types";
import { DashboardFilters } from "../../services/adminMetricsService";

// O painel NÃO acessa o Firebase diretamente.
// Em modo mock: usa dados locais para desenvolvimento.
// Em modo real: todas as chamadas vão ao Cloudflare Admin Worker, que detém
// as credenciais do Firebase service account e serve dados já normalizados.

// Sentinela: indica que os dados não estão disponíveis nesta rota.
export const NOT_AVAILABLE = null;

// Formata timestamp ISO do worker no mesmo padrão pt-BR usado pelos demais
// cards de integração (ex.: "21/06/2026 14:05"). Sem valor real ainda: fallback.
function formatSyncTimestamp(isoTimestamp: string | null | undefined): string {
  if (!isoTimestamp) return "Nunca sincronizado";
  const date = new Date(isoTimestamp);
  if (Number.isNaN(date.getTime())) return "Nunca sincronizado";
  return date.toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function buildQuery(filters: DashboardFilters): string {
  const params = new URLSearchParams();
  if (filters.environment) params.set("environment", filters.environment);
  if (filters.period) params.set("period", filters.period);
  return params.toString() ? `?${params.toString()}` : "";
}

// Estrutura crua retornada pela rota GA4 do worker.
interface GA4WorkerResponse {
  source: string;
  data?: {
    rows?: Array<{
      dimensionValues: Array<{ value: string }>;
      metricValues: Array<{ value: string }>;
    }>;
    dimensionHeaders?: Array<{ name: string }>;
    metricHeaders?: Array<{ name: string }>;
  };
}

// Estrutura crua retornada pela rota de status do worker (ver admin-api-schema.md).
interface FirebaseStatusWorkerResponse {
  source: string;
  projectId?: string;
  status: "connected" | "mock" | "attention" | "planned" | "disabled";
  hasCredentials: boolean;
  ga4PropertyConfigured: boolean;
  lastSyncTimestamp?: string | null;
  eventsImported?: number;
  crashesImported?: number;
}

export async function getFirebaseIntegrationStatus(): Promise<FirebaseIntegrationStatus> {
  if (apiClient.isMockEnabled()) {
    return apiClient.simulateFetch(mockFirebaseStatus, {});
  }

  const raw = await apiClient.request<FirebaseStatusWorkerResponse>(
    "GET",
    "/admin/integrations/firebase/status"
  );

  // O worker não expõe "platform" nesta rota (fixo aqui na UI). lastSync e os
  // contadores vêm do estado persistido em D1 (admin_settings/'firebase_sync'),
  // gravado pelo último POST /admin/integrations/firebase/sync. Se nunca houve
  // sync bem-sucedido, o worker retorna lastSyncTimestamp: null.
  return {
    enabled: raw.hasCredentials,
    status: raw.status,
    message: raw.hasCredentials
      ? "Sincronizado via Conta de Serviço do Google Cloud Platform (GCP)"
      : "Credenciais do Firebase ainda não configuradas no Admin Worker",
    platform: "Android (Firebase Analytics + Crashlytics)",
    lastSyncTimestamp: formatSyncTimestamp(raw.lastSyncTimestamp),
    eventsImported: raw.eventsImported ?? 0,
    crashesImported: raw.crashesImported ?? 0,
  };
}

export async function getFirebaseAnalyticsSummary(
  filters: DashboardFilters = {}
): Promise<FirebaseAnalyticsSummary | null> {
  if (apiClient.isMockEnabled()) {
    const result = await apiClient.simulateFetch(mockFirebaseAnalytics, filters);
    if (filters.environment === "staging") {
      return {
        ...result,
        activeUsersToday: Math.round(result.activeUsersToday * 0.12),
        sessions7d: result.sessions7d != null ? Math.round(result.sessions7d * 0.12) : null,
        crashFreeUsersPercentage: 99.8,
        crashFreeSessionsPercentage: 99.9,
      };
    }
    return result;
  }

  const raw = await apiClient.request<GA4WorkerResponse>(
    "GET",
    `/admin/integrations/firebase/analytics${buildQuery(filters)}`
  );

  // Rota retorna source != "firebase_analytics" ou sem rows: dados não disponíveis.
  if (raw.source !== "firebase_analytics" || !raw.data?.rows?.length) {
    return NOT_AVAILABLE;
  }

  // Parsear as rows da GA4 API.
  // dimensionValues[0] = data YYYYMMDD
  // metricValues[0] = activeUsers, [1] = sessions, [2] = crashAffectedUsers
  let activeUsersToday = 0;
  let sessions7d = 0;

  for (const row of raw.data.rows) {
    const activeUsersRaw = row.metricValues?.[0]?.value;
    if (activeUsersRaw) {
      activeUsersToday += parseInt(activeUsersRaw, 10) || 0;
    }
    const sessionsRaw = row.metricValues?.[1]?.value;
    if (sessionsRaw) {
      sessions7d += parseInt(sessionsRaw, 10) || 0;
    }
  }

  return {
    activeUsersToday,
    // Soma real das sessões GA4 na janela de 7 dias já pedida ao runReport
    // (dateRanges: 7daysAgo-today, ver handleFirebaseAnalytics no worker).
    // Sem cálculo de variação vs. período anterior — ver comentário no tipo.
    sessions7d,
    averageSessionsDurationMs: 0,
    crashFreeUsersPercentage: 0,
    crashFreeSessionsPercentage: 0,
    topEvents: [],
  };
}

// Estrutura crua retornada pela rota de crashlytics do worker (todos os
// branches: no_credentials/no_data_yet/error/bigquery — ver
// handleFirebaseCrashlytics no signallq-admin-worker).
interface FirebaseCrashlyticsWorkerResponse {
  source: string;
  unresolvedCrashes?: number;
  crashFreeUsersPercentage?: number;
  affectedUsers?: number;
  message?: string;
}

export async function getFirebaseCrashlyticsSummary(
  filters: DashboardFilters = {}
): Promise<FirebaseCrashlyticsSummary> {
  if (apiClient.isMockEnabled()) {
    const result = await apiClient.simulateFetch(mockFirebaseCrashlytics, filters);
    if (filters.environment === "staging") {
      return {
        source: "bigquery",
        unresolvedCrashes: 1,
        affectedUsers: 4,
        crashFreeUsersPercentage: 99.8,
      };
    }
    return result;
  }

  const raw = await apiClient.request<FirebaseCrashlyticsWorkerResponse>(
    "GET",
    `/admin/integrations/firebase/crashlytics${buildQuery(filters)}`
  );

  // O worker nunca deixa de mandar "source"/crashFreeUsersPercentage — mesmo
  // sem dado real ele responde com 100/0 neutros. O indicador de "dado real"
  // é source==="bigquery"; qualquer outro valor é honesto-vazio, tratado
  // pela UI (nunca "stub" nesta rota — mesma ressalva já documentada em
  // getFirebaseAppVersions).
  return {
    source: raw.source as FirebaseCrashlyticsSummary["source"],
    unresolvedCrashes: raw.unresolvedCrashes ?? 0,
    crashFreeUsersPercentage: raw.crashFreeUsersPercentage ?? 100,
    affectedUsers: raw.affectedUsers,
    message: raw.message,
  };
}

export async function getFirebaseAppVersions(
  filters: DashboardFilters = {}
): Promise<FirebaseAppVersionsResult> {
  if (apiClient.isMockEnabled()) {
    const result = await apiClient.simulateFetch(mockFirebaseAppVersions, filters);
    if (filters.environment === "staging") {
      return {
        source: "bigquery",
        versions: result.map((v) => ({
          ...v,
          crashCount: Math.round(v.crashCount * 0.1),
          nonFatalCount: Math.round(v.nonFatalCount * 0.1),
          status: "stable" as const,
        })),
      };
    }
    return { source: "bigquery", versions: result };
  }

  const raw = await apiClient.request<{
    source: string;
    versions: Array<{ version: string; totalCrashes: number; affectedUsers: number }>;
    environmentScope?: "all";
  }>("GET", `/admin/integrations/firebase/versions${buildQuery(filters)}`);

  const source = raw.source as FirebaseAppVersionsResult["source"];

  // #880 (achado 3): "no_credentials" (Firebase não configurado), "no_data_yet"
  // (export do BigQuery ainda sem linhas) e "error" não têm dado real — mas cada
  // um é um motivo DIFERENTE, e antes os três colapsavam num único `null`, que a
  // UI sempre traduzia como "Firebase Crashlytics não está configurado" mesmo
  // quando as credenciais estavam OK e só faltava volume. Agora o `source` real
  // do worker é propagado pra UI escolher a mensagem certa (crashFreeReason).
  if (source === "no_credentials" || source === "no_data_yet" || source === "error") {
    return { source, versions: [], environmentScope: raw.environmentScope };
  }

  return {
    source,
    versions: (raw.versions ?? []).map((v): FirebaseAppVersionCrashStats => ({
      appVersion: v.version,
      crashCount: v.totalCrashes ?? 0,
      nonFatalCount: 0, // worker não separa fatal/não-fatal por versão ainda.
      crashFreeUsersPercentage: 100, // worker não calcula base de usuários por versão ainda.
      status: (v.totalCrashes ?? 0) > 100 ? "critical" : (v.totalCrashes ?? 0) > 20 ? "unstable" : "stable",
    })),
    // GH#1042 — repassa o sinal de honestidade do worker (#879): o export do
    // Crashlytics/BigQuery não filtra por environment. Antes esse campo nunca
    // era lido pelo frontend, então nenhuma tela avisava o usuário.
    environmentScope: raw.environmentScope,
  };
}

// Estrutura crua retornada pela rota de crash-issues do worker (ver
// handleFirebaseCrashIssues). appVersion/deviceModel ainda não são expostos
// pelo worker (ver nota no worker sobre schema do BigQuery não confirmado),
// mas o adapter já os aceita como opcionais para quando existirem.
interface FirebaseCrashIssuesWorkerResponse {
  source: string;
  issues?: Array<{
    id: string;
    title: string;
    totalCrashes: number;
    affectedUsers: number;
    lastSeen: number;
    appVersion?: string;
    deviceModel?: string;
  }>;
}

export async function getFirebaseCrashIssues(
  filters: DashboardFilters = {}
): Promise<FirebaseCrashIssuesResult> {
  if (apiClient.isMockEnabled()) {
    const issues = await apiClient.simulateFetch(mockFirebaseCrashIssues, filters);
    return { source: "bigquery", issues };
  }

  const raw = await apiClient.request<FirebaseCrashIssuesWorkerResponse>(
    "GET",
    `/admin/integrations/firebase/crash-issues${buildQuery(filters)}`
  );

  return {
    source: raw.source as FirebaseCrashIssuesResult["source"],
    issues: (raw.issues ?? []).map((i): FirebaseCrashIssue => ({
      id: i.id ?? "",
      title: i.title ?? "Unknown crash",
      totalCrashes: i.totalCrashes ?? 0,
      affectedUsers: i.affectedUsers ?? 0,
      lastSeen: i.lastSeen ?? 0,
      appVersion: i.appVersion,
      deviceModel: i.deviceModel,
    })),
  };
}

export async function syncFirebaseMetrics(): Promise<{ jobId: string; status: string; startedAt: string; message?: string; source?: string }> {
  if (apiClient.isMockEnabled()) {
    return apiClient.simulateFetch({
      jobId: "job_fb_mock_" + Date.now().toString(36),
      status: "started",
      startedAt: new Date().toISOString(),
    });
  }
  const raw = await apiClient.request<{ ok: boolean; source: string; syncedAt?: string; message?: string }>(
    "POST",
    "/admin/integrations/firebase/sync",
    {}
  );
  return {
    jobId: raw.syncedAt ?? "",
    // "no_data_yet" nao e falha - e o worker rodou a query, so nao achou
    // sessao nenhuma no periodo (ex: 1 dia sem uso). So "error" de verdade
    // (BigQuery/credencial) deve virar status "error" na UI.
    status: raw.source === "error" ? "error" : "started",
    startedAt: raw.syncedAt ?? new Date().toISOString(),
    // GH#873-followup: propaga a mensagem real do worker (ex: erro do
    // BigQuery) em vez de deixar a UI cair num "worker retornou erro" generico.
    message: raw.message,
    source: raw.source,
  };
}

// --- GH#1343/#1344: inventário técnico (Management, Remote Config, App Check, App
// Distribution, FCM delivery) — as 5 rotas expõem sempre o mesmo shape de status
// (`hasCredentials`/`status`/`lastSyncTimestamp` + payload próprio) e o mesmo shape de sync
// (`{ status: "ok" | "error" | "not_configured", message?, syncedAt? }`).

interface FirebaseInventoryStatusWorkerResponse {
  source: string;
  status: "connected" | "disabled";
  hasCredentials: boolean;
  lastSyncTimestamp?: string | null;
}

interface FirebaseInventorySyncWorkerResponse {
  status: "ok" | "error" | "not_configured";
  message?: string;
  syncedAt?: string;
}

function toSyncResult(raw: FirebaseInventorySyncWorkerResponse): FirebaseIntegrationSyncResult {
  return { status: raw.status, message: raw.message, syncedAt: raw.syncedAt };
}

export async function getFirebaseManagementStatus(): Promise<FirebaseManagementStatus> {
  if (apiClient.isMockEnabled()) {
    return apiClient.simulateFetch(mockFirebaseManagementStatus, {});
  }
  const raw = await apiClient.request<
    FirebaseInventoryStatusWorkerResponse & {
      project: FirebaseManagementStatus["project"];
      androidApps?: FirebaseManagementStatus["androidApps"];
    }
  >("GET", "/admin/integrations/firebase/management/status");
  return {
    hasCredentials: raw.hasCredentials,
    status: raw.status,
    lastSyncTimestamp: raw.lastSyncTimestamp ?? null,
    project: raw.project ?? null,
    androidApps: raw.androidApps ?? [],
  };
}

export async function syncFirebaseManagement(): Promise<FirebaseIntegrationSyncResult> {
  if (apiClient.isMockEnabled()) {
    return { status: "ok", syncedAt: new Date().toISOString() };
  }
  const raw = await apiClient.request<FirebaseInventorySyncWorkerResponse>(
    "POST",
    "/admin/integrations/firebase/management/sync"
  );
  return toSyncResult(raw);
}

export async function getFirebaseRemoteConfigStatus(): Promise<FirebaseRemoteConfigStatus> {
  if (apiClient.isMockEnabled()) {
    return apiClient.simulateFetch(mockFirebaseRemoteConfigStatus, {});
  }
  const raw = await apiClient.request<
    FirebaseInventoryStatusWorkerResponse & {
      parameterCount?: number;
      parameterKeys?: string[];
    }
  >("GET", "/admin/integrations/firebase/remote-config/status");
  return {
    hasCredentials: raw.hasCredentials,
    status: raw.status,
    lastSyncTimestamp: raw.lastSyncTimestamp ?? null,
    parameterCount: raw.parameterCount ?? 0,
    parameterKeys: raw.parameterKeys ?? [],
  };
}

export async function syncFirebaseRemoteConfig(): Promise<FirebaseIntegrationSyncResult> {
  if (apiClient.isMockEnabled()) {
    return { status: "ok", syncedAt: new Date().toISOString() };
  }
  const raw = await apiClient.request<FirebaseInventorySyncWorkerResponse>(
    "POST",
    "/admin/integrations/firebase/remote-config/sync"
  );
  return toSyncResult(raw);
}

export async function getFirebaseAppCheckStatus(): Promise<FirebaseAppCheckStatus> {
  if (apiClient.isMockEnabled()) {
    return apiClient.simulateFetch(mockFirebaseAppCheckStatus, {});
  }
  const raw = await apiClient.request<
    FirebaseInventoryStatusWorkerResponse & { services?: FirebaseAppCheckStatus["services"] }
  >("GET", "/admin/integrations/firebase/app-check/status");
  return {
    hasCredentials: raw.hasCredentials,
    status: raw.status,
    lastSyncTimestamp: raw.lastSyncTimestamp ?? null,
    services: raw.services ?? null,
  };
}

export async function syncFirebaseAppCheck(): Promise<FirebaseIntegrationSyncResult> {
  if (apiClient.isMockEnabled()) {
    return { status: "ok", syncedAt: new Date().toISOString() };
  }
  const raw = await apiClient.request<FirebaseInventorySyncWorkerResponse>(
    "POST",
    "/admin/integrations/firebase/app-check/sync"
  );
  return toSyncResult(raw);
}

export async function getFirebaseAppDistributionStatus(): Promise<FirebaseAppDistributionStatus> {
  if (apiClient.isMockEnabled()) {
    return apiClient.simulateFetch(mockFirebaseAppDistributionStatus, {});
  }
  const raw = await apiClient.request<
    FirebaseInventoryStatusWorkerResponse & { releases?: FirebaseAppDistributionStatus["releases"] }
  >("GET", "/admin/integrations/firebase/app-distribution/status");
  return {
    hasCredentials: raw.hasCredentials,
    status: raw.status,
    lastSyncTimestamp: raw.lastSyncTimestamp ?? null,
    releases: raw.releases ?? [],
  };
}

export async function syncFirebaseAppDistribution(): Promise<FirebaseIntegrationSyncResult> {
  if (apiClient.isMockEnabled()) {
    return { status: "ok", syncedAt: new Date().toISOString() };
  }
  const raw = await apiClient.request<FirebaseInventorySyncWorkerResponse>(
    "POST",
    "/admin/integrations/firebase/app-distribution/sync"
  );
  return toSyncResult(raw);
}

export async function getFirebaseFcmDeliveryStatus(): Promise<FirebaseFcmDeliveryStatus> {
  if (apiClient.isMockEnabled()) {
    return apiClient.simulateFetch(mockFirebaseFcmDeliveryStatus, {});
  }
  const raw = await apiClient.request<
    FirebaseInventoryStatusWorkerResponse & { androidDeliveryData?: unknown[] }
  >("GET", "/admin/integrations/firebase/fcm-delivery/status");
  return {
    hasCredentials: raw.hasCredentials,
    status: raw.status,
    lastSyncTimestamp: raw.lastSyncTimestamp ?? null,
    androidDeliveryData: raw.androidDeliveryData ?? [],
  };
}

export async function syncFirebaseFcmDelivery(): Promise<FirebaseIntegrationSyncResult> {
  if (apiClient.isMockEnabled()) {
    return { status: "ok", syncedAt: new Date().toISOString() };
  }
  const raw = await apiClient.request<FirebaseInventorySyncWorkerResponse>(
    "POST",
    "/admin/integrations/firebase/fcm-delivery/sync"
  );
  return toSyncResult(raw);
}
